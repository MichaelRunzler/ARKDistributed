package X34.Core.IO;

import java.util.ArrayList;

/**
 * Manages cross-class instancing of the X34ConfigIO object for cases where multiple classes
 * must share an instance of said object.
 * Also manages a multi-object dynamic instance index, for multiple concurrent global instances.
 */
public class X34ConfigDelegator
{
    private static X34ConfigIO instance = null;
    private static ArrayList<X34ConfigIO> dynamicInstances = new ArrayList<>();

    /**
     * Gets the available X34ConfigIO global instance.
     * @return the current global X34ConfigIO instance
     */
    public static X34ConfigIO getMainInstance()
    {
        if(instance == null){
            instance = new X34ConfigIO();
        }
        return instance;
    }

    /**
     * Voids the current global X34ConfigIO instance and creates a new one.
     * @return the new global instance
     */
    public static X34ConfigIO refreshMainInstance()
    {
        if(instance != null){
            instance = null;
        }
        instance = new X34ConfigIO();
        return instance;
    }

    /**
     * Sets the current global instance to the specified object.
     * @param newInstance the X34ConfigIO object to set the global reference to
     */
    public static void setMainInstance(final X34ConfigIO newInstance) {
        instance = newInstance;
    }

    /**
     * Generates a new dynamic X34ConfigIO instance with the specified ID, or gets an existing
     * one if one exists.
     * @param ID the ID of the desired X34ConfigIO global instance to get or create
     * @return the X34ConfigIO global instance at the specified ID, or a new instance if none existed
     */
    public static X34ConfigIO getDynamicInstance(short ID)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        if(dynamicInstances.get((int)ID) == null){
            dynamicInstances.set((int)ID, new X34ConfigIO());
        }

        return dynamicInstances.get((int)ID);
    }

    /**
     * Sets the specified ID in the dynamic instance index to the provided X34ConfigIO.
     * @param ID the ID of the desired X34ConfigIO global instance to replace
     * @param newInstance the X34ConfigIO object to replace the desired ID with
     */
    public static void setDynamicInstance(short ID, X34ConfigIO newInstance)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        dynamicInstances.set(ID, newInstance);
    }
}