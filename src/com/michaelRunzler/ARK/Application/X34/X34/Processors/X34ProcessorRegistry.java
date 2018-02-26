package X34.Processors;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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

        X34RetrievalProcessor[] infs = instantiateObjectsInternal(clss);

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
     * Removes a processor from the internal processor list. Note that this change only persists for this program run, since
     * the loaded processor list is only cached in RAM, and never stored. Any removed processors will re-appear on the next
     * program run if they are eligible to be loaded. The object instance of this processor will also be removed from the internal list.
     * Note that the removal of this instance does <i>not</i> prevent any other threads from using said instance if said
     * thread already has a lock to the instance.
     * Initializes the internal processor list if needed.
     * Calls {@link #removeProcessorFromList(int)} to execute the removal.
     * @param processor the {@link Class} representing the processor to unload. The specified class must extend {@link X34RetrievalProcessor}.
     */
    public static synchronized void removeProcessorFromList(Class<? extends X34RetrievalProcessor> processor)
    {
        if(available == null) {
            try {
                getAvailableProcessors();
            } catch (ClassNotFoundException | IOException e) {
                available = new Class[0];
            }
        }

        if(available.length == 0) return;

        int index = -1;
        for(int i = 0; i < available.length; i++) {
            if(available[i] == processor){
                index = i;
                break;
            }
        }

        removeProcessorFromList(index);
    }

    /**
     * Removes a processor from the internal processor list. Note that this change only persists for this program run, since
     * the loaded processor list is only cached in RAM, and never stored. Any removed processors will re-appear on the next
     * program run if they are eligible to be loaded. The object instance of this processor will also be removed from the internal list.
     * Note that the removal of this instance does <i>not</i> prevent any other threads from using said instance if said
     * thread already has a lock to the instance.
     * Initializes the internal processor list if needed.
     * @param index the index of the processor class (and associated object instance) to remove from the list
     */
    public static synchronized void removeProcessorFromList(int index)
    {
        if(available == null) {
            try {
                getAvailableProcessors();
            } catch (ClassNotFoundException | IOException e) {
                available = new Class[0];
            }
        }

        if(available.length == 0) return;

        if(index == -1) return;

        // Remove the specified class from the available-class list
        available = ARKArrayUtil.remove(available, index);

        // Force-refresh the available-object list if necessary. If it's null, it will be updated when needed.
        if(availableObjects != null) availableObjects = ARKArrayUtil.remove(availableObjects, index);
    }

    /**
     * Loads any external {@link X34RetrievalProcessor} classes from an external JAR-file.
     * Adds any valid classes found by the search to the internal registry, and adds the result of instantiating said
     * classes to the internal object registry.
     * Identical to {@link #getProcessorObjectsFromExternalJar(File)} except for return type.
     * Initializes the internal registry first if necessary.
     * @param jar the JAR-file to load classes from
     * @return the list of classes found by the search
     */
    public static synchronized @Nullable Class[] getProcessorsFromExternalJar(@NotNull File jar)
    {
        Class[] result;
        try {
            result = ClassUtils.findClassesInExternalJar(jar.toURI().toURL(), null, X34RetrievalProcessor.class);
        } catch (ClassNotFoundException | IOException e) {
            return null;
        }

        if(result.length == 0) return result;

        // Check to ensure that the cached lists are valid. If not, the internal processor list hasn't been built yet.
        // Build it first, then continue. Fall back to empty lists if the internal processor list errors out.
        try {
            if(available == null) getAvailableProcessors();
            if(availableObjects == null) getAvailableProcessorObjects();
        } catch (ClassNotFoundException | IOException e) {
            if(available == null) available = new Class[0];
            if(availableObjects == null) availableObjects = new X34RetrievalProcessor[0];
        }

        // Update both the object cache and the class cache to include new instances of the externally-loaded classes.
        Class[] temp = new Class[result.length + available.length];
        System.arraycopy(available, 0, temp, 0, available.length);
        System.arraycopy(result, 0, temp, available.length, result.length);
        available = temp;

        X34RetrievalProcessor[] temp2 = new X34RetrievalProcessor[result.length + availableObjects.length];
        System.arraycopy(availableObjects, 0, temp2, 0, availableObjects.length);
        System.arraycopy(instantiateObjectsInternal(result), 0, temp2, availableObjects.length, result.length);
        availableObjects = temp2;

        return result;
    }

    /**
     * Loads any external {@link X34RetrievalProcessor} classes from an external JAR-file.
     * Adds any valid classes found by the search to the internal registry, and adds the result of instantiating said
     * classes to the internal object registry.
     * Identical to {@link #getProcessorsFromExternalJar(File)} except for return type.
     * Initializes the internal registry first if necessary.
     * @param jar the JAR-file to load classes from
     * @return the result of instantiating all of the classes found by the search
     */
    public static synchronized @Nullable X34RetrievalProcessor[] getProcessorObjectsFromExternalJar(@NotNull File jar)
    {
        Class[] result;
        try {
            result = ClassUtils.findClassesInExternalJar(jar.toURI().toURL(), null, X34RetrievalProcessor.class);
        } catch (ClassNotFoundException | IOException e) {
            return null;
        }

        if(result.length == 0) return new X34RetrievalProcessor[0];

        // Check to ensure that the cached lists are valid. If not, the internal processor list hasn't been built yet.
        // Build it first, then continue. Fall back to empty lists if the internal processor list errors out.
        try {
            if(available == null) getAvailableProcessors();
            if(availableObjects == null) getAvailableProcessorObjects();
        } catch (ClassNotFoundException | IOException e) {
            if(available == null) available = new Class[0];
            if(availableObjects == null) availableObjects = new X34RetrievalProcessor[0];
        }

        // Update both the object cache and the class cache to include new instances of the externally-loaded classes.
        Class[] temp = new Class[result.length + available.length];
        System.arraycopy(available, 0, temp, 0, available.length);
        System.arraycopy(result, 0, temp, available.length, result.length);
        available = temp;

        X34RetrievalProcessor[] temp2 = new X34RetrievalProcessor[result.length + availableObjects.length];
        System.arraycopy(availableObjects, 0, temp2, 0, availableObjects.length);
        X34RetrievalProcessor[] insts = instantiateObjectsInternal(result);
        System.arraycopy(insts, 0, temp2, availableObjects.length, insts.length);
        availableObjects = temp2;

        return insts;
    }

    /**
     * Internal method. Attempts to instantiate all classes in the provided list, then casts them to {@link X34RetrievalProcessor}.
     * Sets indices to null if it cannot complete instantiation or casting.
     * @param clss the classes to instantiate
     * @return the resultant list of objects
     */
    private static X34RetrievalProcessor[] instantiateObjectsInternal(Class[] clss)
    {
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

        return infs;
    }
}
