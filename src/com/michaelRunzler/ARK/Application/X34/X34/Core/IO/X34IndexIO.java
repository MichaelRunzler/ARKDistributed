package X34.Core.IO;

import X34.Core.X34Index;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import core.system.ARKAppCompat;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Loads and saves {@link X34Index indexes}.
 */
public class X34IndexIO
{
    public final String INDEX_FILE_EXTENSION = ".x34i";
    public final String INDEX_REPORT_FILE_EXTENSION = ".index-changes";

    private File parent;
    private final DateFormat df = new SimpleDateFormat("YYYY-MM-DD HH:mm");

    /**
     * Default constructor. Sets the parent directory to the default value as specified by the expression
     * {@code {@link ARKAppCompat#DESKTOP_DATA_ROOT} + "\\X34Indexes"}.
     */
    public X34IndexIO()
    {
        this.parent = new File(ARKAppCompat.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\X34Indexes");
    }

    /**
     * Extended constructor. Sets the parent directory to the specified {@link File}.
     * @param parent the parent directory to pull index files from
     */
    public X34IndexIO(File parent)
    {
        this.parent = parent;
    }

    /**
     * Loads an index with a specified ID.
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

        // neutralize the ID to ensure correct file name searching
        String nid = X34Index.getNeutralSpacedID(id);

        // Generate filename. Filename is comprised of the neutralized ID of the index, followed by its processor ID if it has one, and then the file extension.
        // If a processor ID is present, it and the index ID will be separated by a percent sign.
        File target = assembleIndexFileDescriptor(id, processor);

        X34Index index;

        if(!target.exists()) throw new IOException("Unable to locate index file for specified ID");
        if(!target.canRead()) throw new IOException("Unable to obtain read lock for specified index file");

        ObjectInputStream is = new ObjectInputStream(new FileInputStream(target));

        // If we have more than 1 byte in the file, assume that the index is indeed valid and try to load it.
        if(target.length() > 1){
            try{
                index = (X34Index)is.readObject();
            }catch (ClassNotFoundException | ClassCastException e){
                index = new X34Index(nid);
            }
        }else{
            index = new X34Index(nid);
        }

        is.close();

        return index;
    }

    /**
     * Saves the specified index to disk.
     * @throws IOException if the parent directory is invalid, or the index cannot be written
     */
    public synchronized void saveIndex(@NotNull X34Index index) throws IOException
    {
        // ALL THE INTEGRITY CHECKS
        if(parent == null) throw new IllegalArgumentException("Index parent directory cannot be null");
        if(!parent.exists() && !parent.mkdirs()) throw new IOException("Unable to create index parent directory");
        if(index == null || index.id == null) throw new IllegalArgumentException("Index is invalid or null");

        // correct the internal index ID to match the correctly neutralized version if it doesn't already
        index.id = X34Index.getNeutralSpacedID(index.id);

        // Generate filename. Filename is comprised of the neutralized ID of the index, followed by its processor ID if it has one, and then the file extension.
        // If a processor ID is present, it and the index ID will be separated by a percent sign.
        File target = assembleIndexFileDescriptor(index.id, index.metadata.get("processor"));

        // Generate changelog filename to match index file.
        File changelog = new File(target.getAbsolutePath() + INDEX_REPORT_FILE_EXTENSION);

        boolean isIndexNew = !target.exists();
        if(target.exists() && !target.delete()) throw new IOException("Unable to delete existing index file");
        if(!target.createNewFile()) throw new IOException("Unable to create new index file");
        if(!target.canWrite()) throw new IOException("Unable to obtain write lock for specified index file");

        // now that we're done with that, on to the ACTUAL index write, which is most likely shorter than the integrity checks above
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(target));
        if(index.entries == null || index.entries.size() == 0) os.write(7);
        else os.writeObject(index);
        os.close();

        // Write change data to the changelog file, create it if it does not exist.
        boolean isNew = false;
        if(!changelog.exists()){
            changelog.createNewFile();
            isNew = true;
        }

        // If the file does not already exist, and cannot be created, return without writing change data.
        if(!changelog.exists()) return;

        BufferedWriter br = new BufferedWriter(new FileWriter(changelog, true));

        if(isNew) {
            br.write("=== START OF INDEX CHANGE TRACKING LOG ===");
            br.newLine();
            br.write("Name: " + index.id);
            br.newLine();
            br.write("Processor ID: " + (index.metadata.get("processor") == null ? "None" : index.metadata.get("processor")));
            br.newLine();
            br.write("========");
        }

        String currentTime = df.format(System.currentTimeMillis());
        br.newLine();
        br.write(String.format(isIndexNew ? "%s: Index created with %d entries." : "%s: Entries added to index. New size: %d", currentTime, index.entries.size()));
        br.close();
    }

    /**
     * Checks if an index file exists that matches the specified ID and processor ID.
     * Does not check index validity, only if the file is present and non-zero-length.
     * To get more details about an index file, it must be loaded. For that, use {@link #loadIndex(String, String)}.
     * @param id the ID of the index to load
     * @param processor the ID of the processor that was used to create the target index, provide null to load indices without
     *                  a processor identifier.
     * @return {@code true} if an index file exists that correlates to the provided IDs, {@code false} if otherwise
     */
    public synchronized boolean checkIndex(@NotNull String id, @Nullable String processor)
    {
        if(id == null || id.length() == 0) throw new IllegalArgumentException("ID cannot be null or zero-length");

        File target = assembleIndexFileDescriptor(id, processor);

        return target.exists() && target.length() > 0;
    }

    private File assembleIndexFileDescriptor(String id, String proc) {
        return new File(parent, id.replace("?", "").replace("*", "")  + (proc == null ? "" : "%" + proc) + INDEX_FILE_EXTENSION);
    }

    /**
     * Sets the parent directory to the specified value. The directory will be validated the next time the
     *  {@link X34IndexIO#saveIndex(X34Index)} method is called.
     * @param newParent the directory to load and save indices in
     */
    public synchronized void setParent(@NotNull File newParent){
        if(newParent == null) throw new IllegalArgumentException("Provided parent file cannot be null");
        this.parent = newParent;
    }

    /**
     * Gets the current index parent directory.
     * @return a copy of the {@link File} representing the current base directory in which indices are saved
     */
    public File getParent()
    {
        return new File(parent.getAbsolutePath());
    }
}
