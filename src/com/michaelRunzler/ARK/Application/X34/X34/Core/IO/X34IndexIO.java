package X34.Core.IO;

import X34.Core.X34Index;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import core.system.ARKGlobalConstants;

import java.io.*;

/**
 * Loads and saves {@link X34Index indexes}.
 */
public class X34IndexIO
{
    public final String INDEX_FILE_EXTENSION = ".x34i";

    private X34Index index;
    private File parent;

    /**
     * Default constructor. Sets the parent directory to the default value as specified by the expression
     * {@code {@link core.system.ARKGlobalConstants#DESKTOP_DATA_ROOT} + "\\X34Indexes"}.
     */
    public X34IndexIO()
    {
        this.parent = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\X34Indexes");
        this.index = null;
    }

    /**
     * Extended constructor. Sets the parent directory to the specified {@link File}.
     * @param parent the parent directory to pull index files from
     */
    public X34IndexIO(File parent)
    {
        this.parent = parent;
        this.index = null;
    }

    /**
     * Loads an index with a specified ID. If successfully loaded, the loaded index will be cached for future use. Multiple
     * successive calls to this method with the same ID will result in lower process times due to the index being cached.
     * Only one index may be cached at any given time. The cached index will always be the most recent successfully loaded index.
     * @param id the ID of the index to load
     * @param processor the ID of the processor that was used to create the target index, provide null to load indices without
     *                  a processor identifier.
     * @return the index correlating to the provided ID. May be zero-length.
     * @throws IOException if the specified index cannot be found or another non-recoverable I/O error has occurred
     */
    public synchronized X34Index loadIndex(@NotNull String id, @Nullable String processor) throws IOException
    {
        if(id == null || id.length() == 0) throw new IllegalArgumentException("ID cannot be null or zero-length");
        if(parent == null) throw new IllegalArgumentException("Index parent directory cannot be null");

        // Check if the internally stored index is already a loaded copy of the current on-disk index.
        if(this.index != null && this.index.id.equals(id)) return this.index;

        // Generate filename. Filename is comprised of the ID of the index, followed by its processor ID if it has one, and then the file extension.
        // If a processor ID is present, it and the index ID will be separated by a percent sign.
        File target = new File(parent, id + (processor == null || processor.isEmpty() ? "" : "%" + processor) + INDEX_FILE_EXTENSION);

        if(!target.exists()) throw new IOException("Unable to locate index file for specified ID");
        if(!target.canRead()) throw new IOException("Unable to obtain read lock for specified index file");

        ObjectInputStream is = new ObjectInputStream(new FileInputStream(target));

        // If we have 1 or less bytes to work with, assume that there is no valid index in the file and create a new one instead
        if(is.available() > 1){
            try{
                this.index = (X34Index)is.readObject();
                // catch block is ignored because if an exception is thrown, we want the default block to be called instead of returning
            }catch (ClassNotFoundException | ClassCastException ignored){}
        }

        is.close();

        // If nothing else worked, default to this.
        this.index = new X34Index(id);
        return index;
    }

    /**
     * Saves the currently cached index to disk.
     * @throws IOException if the parent directory is invalid, or the index cannot be written
     */
    public synchronized void saveIndex() throws IOException
    {
        // ALL THE INTEGRITY CHECKS
        if(parent == null) throw new IllegalArgumentException("Index parent directory cannot be null");
        if(!parent.exists() && !parent.mkdirs()) throw new IOException("Unable to create index parent directory");
        if(index == null || index.id == null) throw new IllegalArgumentException("Index is invalid or null");

        // Generate filename. Filename is comprised of the ID of the index, followed by its processor ID if it has one, and then the file extension.
        // If a processor ID is present, it and the index ID will be separated by a percent sign.
        File target = new File(parent, index.id + (index.metadata.get("processor") == null ? "" : "%" + index.metadata.get("processor")) + INDEX_FILE_EXTENSION);

        if(target.exists() && !target.delete()) throw new IOException("Unable to delete existing index file");
        if(!target.createNewFile()) throw new IOException("Unable to create new index file");
        if(!target.canWrite()) throw new IOException("Unable to obtain write lock for specified index file");

        // now that we're done with that, on to the ACTUAL index write, which is most likely shorter than the integrity checks above
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(target));
        if(index.entries == null || index.entries.size() == 0) os.write(7);
        else os.writeObject(index);
        os.flush();
        os.close();
    }

    /**
     * Saves the specified index to disk. Replaces the currently cached index in the process, as if the provided index had
     * been read from disk using {@link X34IndexIO#loadIndex(String, String)}.
     * @param index the index to save
     * @throws IOException if the parent directory is invalid, or the index cannot be written
     * @see X34IndexIO#loadIndex(String, String)  for further information on index caching.
     */
    public synchronized void saveIndex(@NotNull X34Index index) throws IOException {
        this.setIndex(index);
        this.saveIndex();
    }

    /**
     * Sets the parent directory to the specified value. The directory will be validated the next time the
     * {@link X34IndexIO#saveIndex()} or {@link X34IndexIO#saveIndex(X34Index)} methods are called.
     * @param newParent the directory to load and save indices in
     */
    public synchronized void setParent(@NotNull File newParent){
        if(newParent == null) throw new IllegalArgumentException("Provided parent file cannot be null");
        this.parent = newParent;
    }

    private void setIndex(X34Index index)
    {
        if(index == null || index.id == null) throw new IllegalArgumentException("Provided index is invalid or null");
        this.index = index;
    }
}
