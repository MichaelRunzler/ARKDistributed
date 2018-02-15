package core.CoreUtil.AUNIL;

import java.util.ArrayList;

/**
 * Part of the ARK Unified Logging System. Manages globally-accessible instances of the Logger Core. Package-private,
 * only accessible to its subclasses and associated classes/objects.
 */
class XLoggerDelegator
{
    private static XLoggerCore instance = null;
    private static ArrayList<XLoggerCore> dynamicInstances = new ArrayList<>();

    /**
     * Gets the available XLoggerCore global instance.
     * @return the current global XLoggerCore instance
     */
    static XLoggerCore getMainInstance()
    {
        if(instance == null){
            instance = new XLoggerCore();
        }
        return instance;
    }

    /**
     * Voids the current global XLoggerCore instance and creates a new one.
     * @return the new global instance
     */
    synchronized static XLoggerCore refreshMainInstance()
    {
        if(instance != null){
            instance = null;
        }
        instance = new XLoggerCore();
        return instance;
    }

    /**
     * Sets the current global instance to the specified object.
     * @param newInstance the XLoggerCore object to set the global reference to
     */
    synchronized static void setMainInstance(final XLoggerCore newInstance) {
        instance = newInstance;
    }

    /**
     * Generates a new dynamic XLoggerCore instance with the specified ID, or gets an existing
     * one if one exists.
     * @param ID the ID of the desired XLoggerCore global instance to get or create
     * @return the XLoggerCore global instance at the specified ID, or a new instance if none existed
     */
    synchronized static XLoggerCore getDynamicInstance(short ID)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        if(dynamicInstances.get((int)ID) == null){
            dynamicInstances.set((int)ID, new XLoggerCore());
        }

        return dynamicInstances.get((int)ID);
    }

    /**
     * Sets the specified ID in the dynamic instance index to the provided XLoggerCore.
     * @param ID the ID of the desired XLoggerCore global instance to replace
     * @param newInstance the XLoggerCore object to replace the desired ID with
     */
    synchronized static void setDynamicInstance(short ID, XLoggerCore newInstance)
    {
        if(ID < 0){
            throw new IllegalArgumentException("ID must be more than 0");
        }

        dynamicInstances.set(ID, newInstance);
    }
}
