package X34.Core;

import X34.Processors.X34ProcessorRegistry;
import X34.Processors.X34RetrievalProcessor;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import core.CoreUtil.IOTools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

/**
 * Stores hash and source data for images stored in the {@link X34Index} class.
 */
public class X34Image implements Serializable
{
    public URL source;
    public String tag;
    // Uses a 32-character (16-byte) insecure cryptographic hash generated by either the image server or the client during download
    // to ensure image uniqueness
    public byte[] hash;
    public String processorID;

    /**
     * Primary constructor.
     * @param source the {@link URL} or URI that this image object is associated with
     * @param tag the tag or query string that this image was filed under at time of retrieval. May be null in cases where
     *            the image was pulled from a repository that does not use tagging for indexing.
     * @param hash the cryptographic hash or other UID that this image can be identified with. Usually a 32 or 48 byte
     *             insecure cryptographic hash, preferably MD4 or MD5, although this is not required. Hash lengths upwards
     *             of 256 bytes are not recommended, as this can cause excessive memory and CPU usage with larger arrays of images.
     * @param processorID the ID of the {@link X34.Processors.X34RetrievalProcessor} that retrieved this image, as given by
     *                    {@link X34RetrievalProcessor#getID()} for the specified processor object. Not required, but recommended,
     *                    since this allows for extended processing functionality when using the {@link X34Image#writeToFile(File, boolean)} method,
     *                    in addition to greater flexibility and verification functionality when using other modules.
     */
    public X34Image(@NotNull URL source, @Nullable String tag, @Nullable byte[] hash, @Nullable String processorID)
    {
        if(source == null) throw new IllegalArgumentException("Source cannot be null.");

        this.source = source;
        this.tag = tag == null ? "" : tag;
        this.hash = hash == null ? new byte[16] : hash;
        this.processorID = processorID;
    }

    /**
     * Writes the remote image data represented by this object to a file on the local drive.
     * @param parent the directory in which to place the new file
     * @param overwriteExisting set this to true if an attempt should be made to overwrite existing files with the same name as the remote file
     * @return true if the file already existed and overwrite was disabled, false if the write was successful and the file did not already exist.
     * @throws IOException if a critical error was encountered during the I/O process
     */
    public boolean writeToFile(@NotNull File parent, boolean overwriteExisting) throws IOException
    {
        if(parent == null) throw new IllegalArgumentException("Parent directory cannot be null");
        if(source == null) throw new IOException("Source URI is invalid");

        // If the internal processor ID is null, use the default behavior (last index of '/') instead of the processor derivation.
        File f = new File(parent, this.processorID == null ? this.source.getFile().substring(this.source.getFile().lastIndexOf('/'), this.source.getFile().length())
                : X34ProcessorRegistry.getProcessorForID(this.processorID).getFilenameFromURL(this.source));

        if(f.exists() && !overwriteExisting) return true;

        IOTools.getFileFromURL(this.source.toString(), f, overwriteExisting);
        return false;
    }

    /**
     * Verifies this object against the remote copy of its linked image, checking that its internal references are still valid.
     * Verification is typically done using a 32 or 48 byte insecure cryptographic hash, although exact procedures vary
     * by implementation.
     * @return true if the data passed verification, false otherwise
     * @throws IOException if an unrecoverable I/O error occurred during verification
     */
    public boolean verifyIntegrity() throws IOException
    {
        if(hash == null || hash.length == 0 || source == null) throw new IllegalArgumentException("Invalid hash data or source URI");

        // Using simple length-verification technique instead of hash for now, may change at some point in the future.
        return IOTools.getRemoteFileSize(this.source) > 0;
    }
}
