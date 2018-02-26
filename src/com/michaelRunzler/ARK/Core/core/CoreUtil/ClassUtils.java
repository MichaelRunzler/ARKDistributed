package core.CoreUtil;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Provides utilities for manipulating classes in the local filesystem.
 */
public class ClassUtils
{
    /**
     * Scans for any classes residing in the specified package or its subpackages, filtered by the the specified superclass
     * and/or interface(s).
     * Original base code sourced from <a href="https://dzone.com/articles/get-all-classes-within-package">http://dzone.com</a>.
     * @return The classes found by the search
     * @throws ClassNotFoundException if a class file found in the search is not registered with the classloader
     * @throws IOException if a critical I/O error is encountered while searching the package tree
     */
    public static @NotNull Class[] getClasses(@NotNull Package root, @Nullable Class superclass, @Nullable Class... interfaces) throws ClassNotFoundException, IOException
    {
        // Get the name of the provided package.
        String packageName = root.getName();

        // Get and verify the context class loader.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader == null) throw new IOException("A reference to the current ClassLoader instance cannot be obtained");

        // Get and convert to File the list of resources in the provided package.
        Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));
        List<File> dirs = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            try {
                dirs.add(new File(resource.toURI()));
            } catch (URISyntaxException e) {
                dirs.add(new File(resource.getFile()));
            } catch (IllegalArgumentException e){ // URI is not hierarchical, probably a JARfile, use JAR search instead
                return findClassesInInternalJar(resource, packageName, superclass, interfaces);
            }
        }

        // Add any class files in the current package to the class list, then search subdirectories if there are any.
        ArrayList<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName, superclass, interfaces));
        }

        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Loads all valid class files from a specified external directory.
     * Will not load classes from JAR files, use {@link #findClassesInInternalJar(URL, String, Class, Class[])} for that.
     * Use {@link #findClasses(File, String, Class, Class[])} for loading classes from an internal non-JAR directory.
     * @param directory the directory to use as the base for the search
     * @param superclass an optional additional filter requirement. If this argument is non-null, classes will be required to
     *                   have this class as a superclass in order to qualify for addition to the list.
     * @param interfaces an optional additional filter requirement. If this argument is present and non-null, classes will
     *                   be required to implement all of these interfaces in order to qualify for addition to the list.
     * @return a {@link HashMap} containing all of the classes found by the search, along with their corresponding files
     */
    public static @Nullable HashMap<Class, File> loadClassesFromExternalDirectory(@NotNull File directory, @Nullable Class superclass, @Nullable Class... interfaces)
    {
        if(!directory.exists()) return null;

        URL target;
        try{
            target = directory.toURI().toURL();
        }catch (MalformedURLException e){
            return null;
        }

        // Get the contents of the directory.
        File[] contents = IOTools.getFileTree(directory);

        if(contents.length == 1) return null;

        ClassLoader loader = new URLClassLoader(new URL[]{target});
        HashMap<Class, File> results = new HashMap<>();

        // Iterate through the list of files returned by the parser, skipping the error status file at the end of the list.
        for(int i = 0; i < contents.length - 1; i++)
        {
            File f = contents[i];
            // Skip this file if it is a directory, has an incorrect file extension, or does not exist.
            if(f.isDirectory() || !f.getName().contains(".class") || !f.exists()) continue;

            // Get the relative section of the file path for use by the ClassLoader
            String relPath = f.getAbsolutePath();
            relPath = relPath.substring(relPath.indexOf(directory.getAbsolutePath()) + directory.getAbsolutePath().length() + 1);
            relPath = relPath.replace('\\', '.');

            // Try loading the class. If successful, the loaded class will be added to the list. If failed, it will be skipped.
            try{
                Class cls = loader.loadClass(relPath);
                if((superclass == null || cls.getSuperclass().equals(superclass)) && verifyInterfaces(cls, interfaces)) results.put(cls, f);
            }catch (ClassNotFoundException ignored){}
        }

        return results;
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirectories.
     * Only works in directories that are managed by the default {@link ClassLoader} instance. Not suitable for external directories.
     * Use {@link #loadClassesFromExternalDirectory(File, Class, Class[])} for that.
     * Original base code sourced from <a href="https://dzone.com/articles/get-all-classes-within-package">http://dzone.com</a>.
     * @param directory   The base directory to search
     * @param packageName The package name for classes found inside the base directory
     * @param superclass an optional additional filter requirement. If this argument is non-null, classes will be required to
     *                   have this class as a superclass in order to qualify for addition to the list.
     * @param interfaces an optional additional filter requirement. If this argument is present and non-null, classes will
     *                   be required to implement all of these interfaces in order to qualify for addition to the list.
     * @return The classes found by the search
     * @throws ClassNotFoundException if a class file found in the search is not registered with the classloader
     */
    private static List<Class> findClasses(@NotNull File directory, @NotNull String packageName, @Nullable Class superclass, @Nullable Class... interfaces) throws ClassNotFoundException
    {
        List<Class> classes = new ArrayList<>();

        // Get the list of files in the base directory.
        File[] files = directory.listFiles();
        if (!directory.exists() || files == null || files.length == 0) return classes;

        // Search through the file list, recursively searching subpackages and adding .class files to the list.
        for (File file : files) {
            if (file.isDirectory()) classes.addAll(findClasses(file, packageName + "." + file.getName(), superclass, interfaces));
            else if (file.getName().endsWith(".class")) {
                // Get class reference and check for super and interface filters
                Class cls = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                if((superclass == null || cls.getSuperclass().equals(superclass)) && verifyInterfaces(cls, interfaces)) classes.add(cls);
            }
        }

        return classes;
    }

    /**
     * Finds all classes in a given package and its subpackages inside a JAR-file.
     * The JAR-file in question must be accessible using the internal JVM classloader.
     * Heavily modified from Stack Overflow user Dave Dopson's code in <a href=https://stackoverflow.com/questions/176527/how-can-i-enumerate-all-classes-in-a-package-and-add-them-to-a-list>this post</a>.
     * @param jarURL the fully-qualified URL of the JAR to search as given by {@link ClassLoader#getResources(String)} with the package name as the argument
     * @param packageName The package name for classes found inside the base directory. If this is {@code null}, all classes from the JAR will be loaded.
     * @param superclass an optional additional filter requirement. If this argument is non-null, classes will be required to
     *                   have this class as a superclass in order to qualify for addition to the list.
     * @param interfaces an optional additional filter requirement. If this argument is present and non-null, classes will
     *                   be required to implement all of these interfaces in order to qualify for addition to the list.
     * @return The classes found by the search
     * @throws ClassNotFoundException if a class file found in the search is not registered with the classloader
     * @throws IOException if an I/O error is encountered while finding or parsing the JAR in question
     */
    public static @NotNull Class[] findClassesInInternalJar(@NotNull URL jarURL, @Nullable String packageName, @Nullable Class superclass, @Nullable Class... interfaces) throws ClassNotFoundException, IOException
    {
        File f;
        try {
            URI ju = new URI(jarURL.toString().replaceFirst("[.]jar[!].*", ".jar"));
            f = new File(ju);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        }

        Enumeration<JarEntry> entries = new JarFile(f).entries();
        String pkgName = packageName == null ? null : packageName.replace('.', '/');
        List<Class> classes = new ArrayList<>();
        while (entries.hasMoreElements()){
            String className = entries.nextElement().getName();
            if((packageName == null || className.startsWith(pkgName)) && className.contains(".class")){
                Class cls = Class.forName(className.replace('/', '.').replace('\\', '.').replace(".class", ""));
                if((superclass == null || cls.getSuperclass().equals(superclass)) && verifyInterfaces(cls, interfaces)) classes.add(cls);
            }
        }

        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Finds all classes in a given package and its subpackages inside a JAR-file.
     * Heavily modified from Stack Overflow user Dave Dopson's code in <a href=https://stackoverflow.com/questions/176527/how-can-i-enumerate-all-classes-in-a-package-and-add-them-to-a-list>this post</a>.
     * @param jarURL the fully-qualified URL of the JAR to search as given by {@link ClassLoader#getResources(String)} with the package name as the argument
     * @param packageName The package name for classes found inside the base directory. If this is {@code null}, all classes from the JAR will be loaded.
     * @param superclass an optional additional filter requirement. If this argument is non-null, classes will be required to
     *                   have this class as a superclass in order to qualify for addition to the list.
     * @param interfaces an optional additional filter requirement. If this argument is present and non-null, classes will
     *                   be required to implement all of these interfaces in order to qualify for addition to the list.
     * @return The classes found by the search
     * @throws ClassNotFoundException if a class file found in the search is not registered with the classloader
     * @throws IOException if an I/O error is encountered while finding or parsing the JAR in question
     */
    public static @NotNull Class[] findClassesInExternalJar(@NotNull URL jarURL, @Nullable String packageName, @Nullable Class superclass, @Nullable Class... interfaces) throws ClassNotFoundException, IOException
    {
        File f;
        try {
            URI ju = new URI(jarURL.toString().replaceFirst("[.]jar[!].*", ".jar"));
            f = new File(ju);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        }

        Enumeration<JarEntry> entries = new JarFile(f).entries();
        String pkgName = packageName == null ? null : packageName.replace('.', '/');
        List<Class> classes = new ArrayList<>();
        ClassLoader loader = new URLClassLoader(new URL[]{jarURL});

        while (entries.hasMoreElements()){
            String className = entries.nextElement().getName();
            if((packageName == null || className.startsWith(pkgName)) && className.contains(".class")){
                Class cls = loader.loadClass(className.replace('/', '.').replace('\\', '.').replace(".class", ""));
                try {
                    if((superclass == null || cls.getSuperclass() == superclass) && verifyInterfaces(cls, interfaces)) classes.add(cls);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return classes.toArray(new Class[classes.size()]);
    }

    private static boolean verifyInterfaces(Class clazz, Class[] interfaces)
    {
        if(interfaces == null || clazz == null || interfaces.length == 0) return true;
        else if(clazz.getInterfaces().length == 0 || clazz.getInterfaces().length != interfaces.length) return false;

        ArrayList<Class> implemented = new ArrayList<>(Arrays.asList(clazz.getInterfaces()));
        for(Class i : interfaces){
            if(!implemented.contains(i)) return false;
        }

        return true;
    }
}
