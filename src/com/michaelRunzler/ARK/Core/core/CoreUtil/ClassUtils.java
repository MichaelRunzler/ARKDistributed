package core.CoreUtil;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

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
    public static Class[] getClasses(@NotNull Package root, @Nullable Class superclass, @Nullable Class... interfaces) throws ClassNotFoundException, IOException
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
     * Recursive method used to find all classes in a given directory and subdirectories.
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
