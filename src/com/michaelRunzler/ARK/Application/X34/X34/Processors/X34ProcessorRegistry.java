package X34.Processors;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import core.CoreUtil.ClassUtils;

import java.io.File;
import java.io.IOException;

public class X34ProcessorRegistry
{
    // Local cache of results from getAvailableProcessors and sub-methods. Since it is not possible for the class configuration
    // to change after compilation, we can safely assume that the result from said methods will be the same every single time
    // it is run, so we can sacrifice a small amount of memory for much faster run times and fewer disk accesses.
    // Also useful for caching the results from the external load method alongside the internal results.
    private static Class[] available = null;
    private static X34RetrievalProcessor[] availableObjects = null;

    /**
     * Gets the identifiers of all available {@link X34RetrievalProcessor Processor} classes as listed by {@link X34ProcessorRegistry#getAvailableProcessors()}.
     * The returned list is guaranteed to be in the same order as the list of classes returned by {@link X34ProcessorRegistry#getAvailableProcessorObjects()},
     * making direct array-to-array association possible.
     * @return the list of processor identifiers in hierarchical order
     * @see X34ProcessorRegistry#getAvailableProcessors() for more information on processor indexing
     * @see X34ProcessorRegistry#getAvailableProcessorObjects() for more information on processor object retrieval
     */
    public static String[] getAvailableProcessorIDs() throws ClassNotFoundException, IOException
    {
        X34RetrievalProcessor[] clss = getAvailableProcessorObjects();

        if(clss.length == 0) return new String[0];

        String[] ids = new String[clss.length];

        for(int i = 0; i < clss.length; i++){
            ids[i] = clss[i].getID();
        }

        return ids;
    }

    /**
     * Gets the informal name identifiers of all available {@link X34RetrievalProcessor Processor} classes as listed
     * by {@link X34ProcessorRegistry#getAvailableProcessors()}.
     * The returned list is guaranteed to be in the same order as the list of classes returned by {@link X34ProcessorRegistry#getAvailableProcessorObjects()},
     * making direct array-to-array association possible.
     * @return the list of processor informal identifiers in hierarchical order
     * @see X34ProcessorRegistry#getAvailableProcessors() for more information on processor indexing
     * @see X34ProcessorRegistry#getAvailableProcessorObjects() for more information on processor object retrieval
     */
    public static String[] getAvailableProcessorNames() throws ClassNotFoundException, IOException
    {
        X34RetrievalProcessor[] clss = getAvailableProcessorObjects();

        if(clss.length == 0) return new String[0];

        String[] ids = new String[clss.length];

        for(int i = 0; i < clss.length; i++){
            ids[i] = clss[i].getInformalName();
        }

        return ids;
    }

    /**
     * Gets a new object instance of all available {@link X34RetrievalProcessor Processor} classes as listed
     * by {@link X34ProcessorRegistry#getAvailableProcessors()}. Returned objects are precasted to {@link X34RetrievalProcessor}.
     * The returned list is guaranteed to be in the same order as the list of classes returned by {@link X34ProcessorRegistry#getAvailableProcessors()},
     * making direct array-to-array association possible.
     * @return the list of processor informal identifiers in hierarchical order
     * @see X34ProcessorRegistry#getAvailableProcessors()
     */
    public static X34RetrievalProcessor[] getAvailableProcessorObjects() throws ClassNotFoundException, IOException
    {
        // Shortcut the entire method if there is already a cached version of results available.
        if(availableObjects != null) return availableObjects;

        Class[] clss = getAvailableProcessors();

        if(clss.length == 0) return new X34RetrievalProcessor[0];

        X34RetrievalProcessor[] infs = new X34RetrievalProcessor[clss.length];

        for(int i = 0; i < clss.length; i++){
            Class cls = clss[i];

            try {
                infs[i] = ((X34RetrievalProcessor)cls.newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassCastException e) {
                infs[i] = null;
            }
        }

        // Store result in internal cache array for future use.
        availableObjects = infs;
        return infs;
    }

    /**
     * Gets the {@link X34RetrievalProcessor} that matches the provided ID, if there is one available.
     * Uses the {@link X34ProcessorRegistry#getAvailableProcessorObjects()} method to obtain the list of available processors.
     * @param ID the Processor ID to search for
     * @return an object instance of the Processor that matched the ID query, or null if none could be found
     * @see X34ProcessorRegistry#getAvailableProcessors()
     */
    public static X34RetrievalProcessor getProcessorForID(@NotNull String ID)
    {
        if(ID == null || ID.isEmpty()) return null;

        X34RetrievalProcessor[] IDs;

        try {
            IDs = getAvailableProcessorObjects();
        } catch (ClassNotFoundException | IOException e) {
            return null;
        }

        for(X34RetrievalProcessor x : IDs){
            if(x.getID().equals(ID)) return x;
        }

        return null;
    }

    /**
     * Scans for any classes residing in this class's package that conform to the identifying marks of a subclass of
     * {@link X34RetrievalProcessor}. Returns an array of all such classes found in this package or subpackages.
     * Class list is obtained by calling {@link ClassUtils#getClasses(Package, Class, Class[])}.
     * @return The classes found by the search
     * @throws ClassNotFoundException if a class file found in the search is not registered with the classloader
     * @throws IOException if a critical I/O error is encountered while searching the package tree
     * @see ClassUtils#getClasses(Package, Class, Class[]) for more information about the class search algorithm
     */
    public static Class[] getAvailableProcessors() throws ClassNotFoundException, IOException
    {
        // Shortcut the entire method if there is already a cached version of results available.
        if(available != null) return available;

        // Store result in internal cache array for future use.
        available = ClassUtils.getClasses(X34RetrievalProcessor.class.getPackage(), X34RetrievalProcessor.class);
        return available;
    }

    /**
     * Loads any external {@link X34RetrievalProcessor} classes from an external directory.
     * Adds any valid classes found by the search to the internal registry.
     * @param directory the directory to load classes from
     * @return the list of classes found by the search
     */
    public static @Nullable Class[] getProcessorsFromExternalDir(@NotNull File directory)
    {
        Class[] result = ClassUtils.loadClassesFromExternalDirectory(directory, X34RetrievalProcessor.class);
        if(result == null || result.length == 0) return result;

        Class[] temp = new Class[result.length + available.length];
        System.arraycopy(available, 0, temp, 0, available.length);
        System.arraycopy(result, 0, temp, available.length, result.length);
        available = temp;

        return result;
    }
}
