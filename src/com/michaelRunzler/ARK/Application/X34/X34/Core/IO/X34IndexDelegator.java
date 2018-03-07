package X34.Core.IO;

import java.util.ArrayList;

/**
 * Manages cross-class instancing of the {@link X34IndexIO} object for cases where multiple classes
 * must share an instance of said object.
 * Also manages a multi-object dynamic instance index, for multiple concurrent global instances.
 */
public class X34IndexDelegator 
{
    private static X34IndexIO instance = null;
    private static ArrayList<X34IndexIO> dynamicInstances = new ArrayList<>();

    /**
     * Gets the available X34IndexIO global instance.
     * @return the current global X34IndexIO instance
     */
    public static X34IndexIO getMainInstance()
    {
        if(instance == null){
            instance = new X34IndexIO();
        }
        return instance;
    }

    /**
     * Voids the current global X34IndexIO instance and creates a new one.
     * @return the new global instance
     */
    public static X34IndexIO refreshMainInstance()
    {
        if(instance != null){
            instance = null;
        }
        instance = new X34IndexIO();
        return instance;
    }

    /**
     * Sets the current global instance to the specified object.
     * @param newInstance the X34IndexIO object to set the global reference to
     */
    public static void setMainInstance(final X34IndexIO newInstance) {
        instance = newInstance;
    }

    /**
     * Generates a new dynamic X34IndexIO instance with the specified ID, or gets an existing
     * one if one exists.
     * @param ID the ID of the desired X34IndexIO global instance to get or create
     * @return the X34IndexIO global instance at the specified ID, or a new instance if none existed
     */
    public static X34IndexIO getDynamicInstance(short ID)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        if(dynamicInstances.get((int)ID) == null){
            dynamicInstances.set((int)ID, new X34IndexIO());
        }

        return dynamicInstances.get((int)ID);
    }

    /**
     * Sets the specified ID in the dynamic instance index to the provided X34IndexIO.
     * @param ID the ID of the desired X34IndexIO global instance to replace
     * @param newInstance the X34IndexIO object to replace the desired ID with
     */
    public static void setDynamicInstance(short ID, X34IndexIO newInstance)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        dynamicInstances.set(ID, newInstance);
    }
}
