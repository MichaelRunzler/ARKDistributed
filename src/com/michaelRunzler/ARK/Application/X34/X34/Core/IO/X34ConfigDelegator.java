package X34.Core.IO;

import java.util.ArrayList;

/**
 * Manages cross-class instancing of the {@link X34Config} object for cases where multiple classes
 * must share an instance of said object.
 * Also manages a multi-object dynamic instance index, for multiple concurrent global instances.
 */
public class X34ConfigDelegator
{
    private static X34Config instance = null;
    private static ArrayList<X34Config> dynamicInstances = new ArrayList<>();

    /**
     * Gets the available X34Config global instance.
     * @return the current global X34Config instance
     */
    public static X34Config getMainInstance()
    {
        if(instance == null){
            instance = new X34Config();
        }
        return instance;
    }

    /**
     * Voids the current global X34Config instance and creates a new one.
     * @return the new global instance
     */
    public static X34Config refreshMainInstance()
    {
        if(instance != null){
            instance = null;
        }
        instance = new X34Config();
        return instance;
    }

    /**
     * Sets the current global instance to the specified object.
     * @param newInstance the X34Config object to set the global reference to
     */
    public static void setMainInstance(final X34Config newInstance) {
        instance = newInstance;
    }

    /**
     * Generates a new dynamic X34Config instance with the specified ID, or gets an existing
     * one if one exists.
     * @param ID the ID of the desired X34Config global instance to get or create
     * @return the X34Config global instance at the specified ID, or a new instance if none existed
     */
    public static X34Config getDynamicInstance(short ID)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        if(dynamicInstances.get((int)ID) == null){
            dynamicInstances.set((int)ID, new X34Config());
        }

        return dynamicInstances.get((int)ID);
    }

    /**
     * Sets the specified ID in the dynamic instance index to the provided X34Config.
     * @param ID the ID of the desired X34Config global instance to replace
     * @param newInstance the X34Config object to replace the desired ID with
     */
    public static void setDynamicInstance(short ID, X34Config newInstance)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        dynamicInstances.set(ID, newInstance);
    }
}