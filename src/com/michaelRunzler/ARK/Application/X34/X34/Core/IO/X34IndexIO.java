package X34.Core.IO;

import X34.Core.X34Index;

import java.io.*;

public class X34IndexIO
{
    public final String INDEX_FILE_EXTENSION = ".x34i";

    private X34Index index;
    private File parent;

    public X34IndexIO()
    {
        this.parent = null;
        this.index = null;
    }

    public X34IndexIO(File parent)
    {
        this.parent = parent;
        this.index = null;
    }

    public X34Index loadIndex(String id) throws IOException
    {
        if(id == null || id.length() == 0) throw new IllegalArgumentException("ID cannot be null or zero-length");
        if(parent == null) throw new IllegalArgumentException("Index parent directory cannot be null");

        File target = new File(parent, id + INDEX_FILE_EXTENSION);

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

        // If nothing else worked, default to this.
        this.index = new X34Index(id);
        return index;
    }

    public void saveIndex() throws IOException
    {
        // ALL THE INTEGRITY CHECKS
        if(parent == null) throw new IllegalArgumentException("Index parent directory cannot be null");
        if(!parent.exists() && !parent.mkdirs()) throw new IOException("Unable to create index parent directory");
        if(index == null || index.id == null) throw new IllegalArgumentException("Index is invalid or null");

        File target = new File(parent, index.id + INDEX_FILE_EXTENSION);

        if(!target.canWrite()) throw new IOException("Unable to obtain write lock for specified index file");
        if(target.exists() && !target.delete()) throw new IOException("Unable to delete existing index file");
        if(!target.createNewFile()) throw new IOException("Unable to create new index file");

        // now that we're done with that, on to the ACTUAL index write, which is most likely shorter than the integrity checks above
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(target));
        if(index.entries == null || index.entries.size() == 0) os.write(7);
        else os.writeObject(index);
        os.flush();
        os.close();
    }

    public X34Index getIndex() {
        return index;
    }

    public void setIndex(X34Index index)
    {
        if(index == null || index.id == null) throw new IllegalArgumentException("Provided index is invalid or null");
        this.index = index;
    }
}
