package X34.Core;

import com.sun.istack.internal.NotNull;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.IOTools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Stores hash and source data for images stored in the {@link X34Index} class.
 */
public class X34Image implements Serializable
{
    public URL source;
    public String tag;
    public byte[] hash;

    public X34Image(@NotNull URL source, String tag, byte[] hash)
    {
        if(source == null) throw new IllegalArgumentException("Source cannot be null.");

        this.source = source;
        this.tag = tag == null ? "" : tag;
        this.hash = hash == null ? new byte[16] : hash;
    }

    /**
     * Writes the remote image data represented by this object to a file on the local drive.
     * @param parent the directory in which to place the new file
     * @param overwriteExisting set this to true if an attempt should be made to overwrite existing files with the same name as the remote file
     * @return true if the file write was successful, false if otherwise
     * @throws IOException if a critical error was encountered during the I/O process
     */
    public boolean writeToFile(@NotNull File parent, boolean overwriteExisting) throws IOException
    {
        if(parent == null || (parent.exists() && !overwriteExisting)) throw new IllegalArgumentException("Destination file is invalid or already exists");
        if(source == null) throw new IOException("Source URI is invalid");

        File f = new File(parent, this.source.getFile());

        IOTools.getFileFromURL(this.source.toString(), f, overwriteExisting);
        return f.exists() && f.getName().equals(this.source.getFile());
    }

    /**
     * Verifies the checksum of this object against the remote copy of its linked image.
     * Verification is done using the MD5 cryptographic hash, and is not secure.
     * @return true if the data passed verification, false otherwise
     */
    public boolean verifyIntegrity()
    {
        if(hash == null || hash.length != 16 || source == null) throw new IllegalArgumentException("Invalid hash data or source URI");

        try {
            return ARKArrayUtil.compareByteArrays(MessageDigest.getInstance("MD5").digest(IOTools.getBytesFromURL(this.source.toString())), this.hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            return false;
        }
    }
}
