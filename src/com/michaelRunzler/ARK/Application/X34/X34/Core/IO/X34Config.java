package X34.Core.IO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Stores and manages application configuration settings. Also manages read/write to/from the config
 * file stored on disk. Use the X34ConfigDelegator class for cross-class instancing.
 *
 * Copied from ARK-Android.
 */
public class X34Config
{
    private HashMap<String, Object> storage;
    private HashMap<String, Object> defaults;
    private HashMap<String, Object> cache;
    private HashMap<String, Boolean> tempFlags;
    private File target;

    /**
     * Constructs a new instance of this object with an empty internal registry and null file target.
     */
    public X34Config() {
        storage = new HashMap<>();
        defaults = new HashMap<>();
        tempFlags = new HashMap<>();
        target = null;
        cache = null;
    }

    /**
     * Constructs a new instance of this object with an empty internal registry and the specified file target.
     * @param target a File representing the desired target configuration file.
     */
    public X34Config(File target){
        storage = new HashMap<>();
        defaults = new HashMap<>();
        tempFlags = new HashMap<>();
        this.target = target;
        cache = null;
    }

    /**
     * Gets a setting value from the stored list.
     * @param key the key to search for in the settings index
     * @return the value corresponding to the provided key, or null if the key does not exist in the index
     */
    public Object getSetting(String key) {
        return storage.getOrDefault(key, null);
    }

    /**
     * Gets a setting value from the stored list.
     * Returns the default value provided if a value was not found at the specified key.
     * Auto-casts the returned value to the same type as the default value if a setting was found at the specified key.
     * @param key the key to search for in the settings index
     * @param defaultValue the value to return if no value is found at the specified index. Also used as a type-reference
     *                     for auto-casting the return value if a key was found.
     * @return the value corresponding to the provided key, or the provided default value if the key does not exist in the index
     */
    public <T> T getSettingOrDefault(String key, T defaultValue)
    {
        try{
            T retV = (T)storage.getOrDefault(key, defaultValue);
            if(retV.getClass() == defaultValue.getClass() || (retV.getClass().getSuperclass() == defaultValue.getClass().getSuperclass()))
            return retV;
            else return defaultValue;
        }catch (ClassCastException e){
            return defaultValue;
        }
    }

    /**
     * Gets a setting value from the stored list.
     * Useful as a compact way of making sure that a specified value exists in the config index one way or another.
     * Returns the default value provided if a value was not found at the specified key.
     * Also stores the provided default value if the setting did not exist in the index.
     * Auto-casts the returned value to the same type as the default value if a setting was found at the specified key.
     * @param key the key to search for in the settings index
     * @param defaultValue the value to return if no value is found at the specified index. Also used as a type-reference
     *                     for auto-casting the return value if a key was found.
     * @return the value corresponding to the provided key, or the provided default value if the key does not exist in the index
     */
    public <T> T getSettingOrStore(String key, T defaultValue)
    {
        T retV = getSettingOrDefault(key, defaultValue);
        if(retV == defaultValue) storeSetting(key, retV);
        return retV;
    }

    /**
     * Gets multiple settings values from the stored index.
     * Automatically culls the output array, removing corresponding entries for any keys
     * that do not exist in the stored index.
     * @param keys one or multiple Strings (or a String array) that represent keys in the stored settings index
     * @return an Object array containing a culled list of settings from the internal index, or null if no keys existed in the index
     */
    public Object[] getMultipleSettingsCulled(String... keys)
    {
        if(keys.length == 0) throw new IllegalArgumentException("Provide one or more key arguments");

        String[] validKeys = new String[keys.length];
        System.arraycopy(keys, 0, validKeys, 0, keys.length);

        // Check to see how many given keys actually exist in the index,
        // and copy them over to a temporary array if they do.
        int validKeyCount = keys.length;
        int validityCounter = 0;

        for(String k : keys)
        {
            if(storage.containsKey(k)){
                validKeys[validityCounter] = k;
                validityCounter ++;
            }else{
                validKeyCount --;
            }
        }

        // Return a null array if no keys matched.
        if(validKeyCount == 0) return null;

        // Iterate through the array of valid keys, find the value of each one, and copy the
        // results over to the output array.
        Object[] retV = new Object[validKeyCount];
        for(int i = 0; i < validKeyCount; i++) {
            retV[i] = storage.get(validKeys[i]);
        }

        return retV;
    }

    /**
     * Gets multiple values from the stored index.
     * Returns an array of the exact same length as the input array, even if some values were not found.
     * @param keys one or multiple Strings (or a String array) that represent keys in the stored settings index
     * @return an Object array containing a list of settings from the internal index. Keys that were not found
     * in the index have their corresponding indices in the output array set to null.
     */
    public Object[] getMultipleSettings(String... keys)
    {
        if(keys.length == 0) throw new IllegalArgumentException("Provide one or more key arguments");

        Object[] retV = new Object[keys.length];
        for(int i = 0; i < keys.length; i++) {
            retV[i] = getSetting(keys[i]);
        }

        return retV;
    }

    /**
     * Dumps this object's currently stored settings index.
     * The returned value is a copy of the stored index, not a direct reference.
     * @return a copy of the currently stored settings index
     */
    public HashMap<String, Object> getAllSettings() {
        return new HashMap<>(storage);
    }

    /**
     * Completely removes a setting entry from the index, acting as if it was never there.
     * @param key the key to search for in the settings index
     */
    public void removeSetting(String key) {
        if(storage.containsKey(key)) storage.remove(key);
    }

    /**
     * Completely removes multiple settings from the index, acting as if they were never there.
     * @param keys one or multiple Strings (or a String array) that represent keys in the stored settings index
     */
    public void removeMultipleSettings(String... keys){
        if(keys.length == 0) throw new IllegalArgumentException("Provide one or more key arguments");

        for(String k : keys){
            removeSetting(k);
        }
    }

    /**
     * Sets a specified key in the internal index to the specified value.
     * If no entry for that key exists, it will be created.
     * Provided object must implement Serializable to allow reading from/writing to config files.
     * @param key the key to search for in the index, or the new key that will be created, if that key does not exist
     * @param value the value to set the existing or new key to
     * @return true if the specified key existed already, false if otherwise
     */
    public boolean storeSetting(String key, Object value)
    {
        if(key.isEmpty()) throw new IllegalArgumentException("Key cannot be zero-length");
        if(value != null && !(value instanceof Serializable)) throw new IllegalArgumentException("Object must be serializable");

        boolean retV = false;
        if(storage.containsKey(key)) {
            storage.remove(key);
            retV = true;
        }

        storage.put(key, value);
        return retV;
    }

    /**
     * Stores multiple settings in keypair format.
     * Keys that exist in the index will be set to the provided value, and keys that do not will be created.
     * Provided objects must implement Serializable to allow reading from/writing to config files.
     * Objects that do not implement Serializable will throw an IllegalArgumentException.
     * @param settings a HashMap containing the list of settings and keys to store
     */
    public void storeMultipleSettings(HashMap<String, Object> settings)
    {
        if(settings.size() == 0) throw new IllegalArgumentException("Input HashMap cannot be null");

        for(String key : settings.keySet()) {
            Object value = settings.get(key);
            storeSetting(key, value);
        }
    }

    /**
     * Stores multiple settings in keypair format.
     * Keys that exist in the index will be set to the provided value, and keys that do not will be created.
     * Only objects that implement Serializable will be added to the index, all others will be ignored.
     * Make sure to check the index after completion to see if any objects were skipped.
     * @param settings a HashMap containing the list of settings and keys to store
     */
    public void storeMultipleSettingsIgnoreNonSerializable(HashMap<String, Object> settings)
    {
        if(settings.size() == 0) throw new IllegalArgumentException("Input HashMap cannot be null");

        for(String key : settings.keySet()) {
            if(storage.containsKey(key)) storage.remove(key);
            storage.put(key, settings.get(key));
        }
    }

    /**
     * Clears the internal settings index completely.
     * Also clears the defaults store, temporary flag registry, and cache.
     */
    public void clearStorage() {
        storage = new HashMap<>();
        cache = new HashMap<>();
        tempFlags = new HashMap<>();
        defaults = new HashMap<>();
    }

    /**
     * Attempts to write the currently stored settings index to the set config file.
     * Will automatically skip writing any entries that have the temporary flag set for them.
     * This will NOT remove them from the index, but simply skip writing them to file.
     * @throws IOException if an error occurred during the write or serialization process
     */
    public void writeStoredConfigToFile() throws IOException
    {
        // Check to make sure the current target is valid.
        if(target == null || target.isDirectory()) throw new IllegalArgumentException("Config target is invalid");

        // If the target is valid, make sure a file exists at the specified location.
        if(target.exists()){
            if(!target.delete()) throw new IOException("Unable to delete current config file");
        }else{
            if(!target.getParentFile().exists() && !target.getParentFile().mkdirs()) throw new IOException("Unable to create necessary config directory path");
        }

        if(!target.createNewFile()) throw new IOException("Unable to create config file");

        // Make sure we can write to the file we just created.
        if(!target.canRead() || !target.canWrite()) throw new IOException("No access rights for written config file");

        // It's not unchecked, I looked: it ALWAYS returns a HashMap.
        HashMap<String, Object> writeCopy = (HashMap<String, Object>)storage.clone();

        // Check if the temporary flag storage is empty. If it is, skip removal checking, as we know there are no flagged entries.
        if(tempFlags.keySet().size() > 0) {
            // Check each entry for temporary flagging, and remove it from to the temporary write array if it is flagged.
            // Skip removal if the specified key does not exist in the main array for obvious reasons.
            for (String k : storage.keySet()) {
                if (tempFlags.containsKey(k) && tempFlags.get(k) && writeCopy.containsKey(k)) writeCopy.remove(k);
            }
        }

        // Now that we are sure that the file is ready for writing, write the copied index to it
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(target));
        os.writeObject(writeCopy);
        os.flush();
        os.close();
    }

    /**
     * Attempts to read a settings index from the currently set config file.
     * If the read succeeds, the currently stored index of this object will be replaced by the read
     * settings, so make sure to store the current index somewhere else if you wish to retain it.
     * @throws IOException if an error occurred during the read or interpretation process
     */
    public void loadConfigFromFile() throws IOException
    {
        // Check to make sure the current target is valid.
        if(target == null || target.isDirectory()) throw new IllegalArgumentException("Config target is invalid");

        // If the target is valid, make sure a file exists at the specified location.
        if(!target.exists() || (target.exists() & !target.getName().contains(".x34c")))
            throw new IOException("No valid config file exists at the specified location");

        // Target is valid. Try to load from it.
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(target));
        Object buffer;
        try{
            buffer = is.readObject();
        } catch(ClassNotFoundException | IOException e){
            throw new IOException("Unable to load from config. Cause: " +
                    (e.getLocalizedMessage() == null ? e.toString() : e.getLocalizedMessage()));
        }

        // Check that the file actually contained a valid HashMap.
        if(!(buffer instanceof HashMap)) throw new IOException("Read succeeded, but the file did not contain valid settings");

        // Store the retrieved data.
        // Not an unchecked cast, but IntelliJ seems to think so...
        storage = (HashMap<String,Object>)buffer;
        is.close();
    }

    /**
     * Gets the current target config file for this object.
     * @return a copy of the File that this object is currently managing
     */
    public File getTarget() {
        return new File(target.getParent(), target.getName());
    }

    /**
     * Sets the config file target of this object.
     * @param target a File representing the target configuration file that this object should manage
     */
    public void setTarget(File target) {
        this.target = target;
    }

    /**
     * Tells this object to copy the contents of its internal index to a separate internal cache.
     * This is useful if, for example, you wish to allow objects to continue writing changes as normal
     * to the main index, but keep a separate concurrent global copy in case a user cancels changes.
     * This object will retain the cached copy until the clearCache() or commitCache() method is called.
     * Calling this method when a cached copy already exists will invalidate the cached copy and create
     * a new one from the master index.
     */
    public void fillCache()
    {
        cache = new HashMap<>();
        cache.putAll(storage);
    }

    /**
     * Clears this object's stored index cache, if present.
     */
    public void clearCache() {
        cache = null;
    }

    /**
     * Commits the currently cached index to the master index. This will erase all currently stored
     * settings in the index and replace them with the cached settings. Calling this method with no
     * cached copy will have no effect on the master index. Clears the cache after completion.
     * If for some reason you wish to retain a copy of the cached settings, call getCache() before
     * calling this method.
     */
    public void commitCache()
    {
        if(cache == null) return;

        storage = new HashMap<>();
        storage.putAll(cache);
        cache = null;
    }

    /**
     * Gets the currently cached index copy. Returned object is a copy of the cached index. Returns null if none is present.
     */
    public HashMap<String, Object> getCache() {
        return cache == null ? null : new HashMap<>(cache);
    }

    /**
     * Sets if a setting is temporary (ex. a cached value that must be persistent while the program is running),
     * and as such, should not be written to file when writeStoredConfigToFile() is called.
     * @param key the key to set the value for. If a key is provided that does not exist in the index,
     *            that entry will NOT be created in the main index, but the is-temporary state flag will
     *            still be set.
     * @param temporary set to true if the related tag should be skipped when writing to file, false if otherwise
     */
    public void setTemporary(String key, boolean temporary)
    {
        if(key.isEmpty()) throw new IllegalArgumentException("Key cannot be zero-length");
        tempFlags.put(key, temporary);
    }

    /**
     * Sets the specified key's default setting to the specified value.
     * If the target value does not exist, it will be created.
     * Provided object must implement Serializable to allow reading from/writing to config files.
     * @param key the default key to search for in the index, or the new default key that will be created, if that key does not exist
     * @param defaultValue the value to set the existing or new default key to
     */
    public void setDefaultSetting(String key, Object defaultValue)
    {
        if(key.length() == 0) throw new IllegalArgumentException("Key cannot be zero-length");
        if(defaultValue != null && !(defaultValue instanceof Serializable)) throw new IllegalArgumentException("Object must be serializable");

        if(defaults.containsKey(key)) defaults.remove(key);

        defaults.put(key, defaultValue);
    }

    /**
     * Gets the default setting for a specified key.
     * @param key the key to search for in the defaults index
     * @return the default value corresponding to the provided key, or null if the key does not exist in the defaults index
     */
    public Object getDefaultSetting(String key) {
        return defaults.getOrDefault(key, null);
    }

    /**
     * Applies a previously set default setting to the main index, if one for the specified key exists.
     * If no corresponding main index entry for the specified key exists, it will be created.
     * @param key the key to search for in the index
     */
    public void loadDefault(String key)
    {
        if(key.length() == 0) throw new IllegalArgumentException("Key cannot be zero-length");

        if(!(defaults.containsKey(key))) return;

        if(storage.containsKey(key)) storage.remove(key);

        storage.put(key, defaults.get(key));
    }

    /**
     * Clears the main index, and copies all default settings in the defaults index to the main index.
     * This is irreversible, use caution.
     * If no defaults index exists (or if it is empty), no action will be taken.
     * The defaults index is left intact, and is treated as read-only.
     */
    public void loadAllDefaults()
    {
        if(defaults == null || defaults.size() == 0) return;
        storage.clear();

        Iterator<String> itr = defaults.keySet().iterator();
        for(int i = 0; i < defaults.keySet().size(); i++){
            String key = itr.next();
            storage.put(key, defaults.get(key));
        }
    }

    /**
     * Clears the internal default settings index completely.
     * This does NOT clear the main index.
     */
    public void clearDefaults() {
        defaults = new HashMap<>();
    }
}