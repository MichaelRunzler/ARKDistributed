package RMan.Core.Managers;

import java.io.*;
import java.util.ArrayList;

/**
 * A wrapper class that surrounds an {@link ArrayList}. Provides utility methods for importing, exporting, and managing
 * said {@link ArrayList} (referred to as the Index).
 */
public abstract class CollectionManager<E>
{
    public ArrayList<E> index;

    /**
     * Default constructor. Starts with an empty index.
     */
    public CollectionManager() {
        index = new ArrayList<>();
    }

    /**
     * Serialize the current index and write it to the specified file.
     * @param target The handle to the target file. If it already exists, it will be overwritten if possible.
     * @return {@code true} if the export was successful, {@code false} if an error occurred. Errors will be written
     * to the system console.
     */
    public boolean exportIndex(File target)
    {
        // Ensure the target is at least a valid file reference, even if it doesn't exist yet
        if(target == null || target.isDirectory()) return false;

        try {
            // Prep file for writing
            if(target.exists() && !target.delete()) throw new IOException("Could not delete existing file.");
            if(!target.createNewFile()) throw new IOException("Could not create destination file.");

            // Open stream and write index; clean up.
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(target));
            os.writeObject(index);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Imports an index from a previously exported file.
     * @param source The handle to the source file.
     * @param merge If this is {@code true}, the contents of the incoming index will be merged with the existing index.
     *              Otherwise, the existing index will be replaced with the incoming one.
     * @return {@code true} if the import was successful, {@code false} if an error occurred. Errors will be written
     *      * to the system console.
     */
    @SuppressWarnings("unchecked") // IntelliJ can't induce class casts from object deserialization; we catch it anyway
    public boolean importIndex(File source, boolean merge)
    {
        // Ensure the file is valid and exists
        if(source == null || !source.exists() || source.isDirectory() || !source.canRead()) return false;

        try {
            // Open the stream and read the index. If it's null, something probably went wrong, so declare failure and
            // return. Otherwise, replace or merge with the current index and continue.
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(source));
            ArrayList<E> tmp = (ArrayList<E>)is.readObject();
            is.close();

            if(tmp != null)
            {
                // Go through the new index and add any entries that don't already exist in the current index.
                if(merge)
                    for(E entry : tmp)
                        if(!index.contains(entry)) index.add(entry);
                else index = tmp;
            } else return false;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
