package X34.Core;

import X34.Processors.X34ProcessorRegistry;
import core.CoreUtil.ARKArrayUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Stores {@link X34Image images} in a compact, metadata-enabled format.
 */
public class X34Index implements Serializable
{
    public String id;
    public ArrayList<X34Image> entries;
    public HashMap<String, String> metadata;

    public X34Index(String id)
    {
        this.id = getNeutralSpacedID(id);
        this.entries = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Gets an entry from this index by its source ID.
     * @param source the URL to search for in the index
     * @return the index of the matching {@link X34Image image} if found, or -1 if no matching entry is found
     */
    public int getEntryByURI(URL source)
    {
        if(entries == null || entries.size() == 0) return -1;

        for(int i = 0; i < entries.size(); i++){
            X34Image x = entries.get(i);
            if(x.source == source) return i;
        }

        return -1;
    }

    /**
     * Gets an entry from this index by its hash code.
     * @param hash the hashcode to search for in the index
     * @return the index of the matching {@link X34Image image} if found, or -1 if no matching entry is found
     */
    public int getEntryByHash(byte[] hash)
    {
        if(entries == null || entries.size() == 0) return -1;

        for(int i = 0; i < entries.size(); i++){
            X34Image x = entries.get(i);
            if(ARKArrayUtil.compareByteArrays(x.hash, hash)) return i;
        }

        return -1;
    }

    /**
     * Eliminates separator characters and replace them with spaces to ensure consistent index naming.
     * @param id the ID string to neutralize
     * @return the neutralized equivalent of the input ID
     */
    public static String getNeutralSpacedID(String id)
    {
        if(id != null && !id.isEmpty()) return id.replace('_', ' ').replace('+', ' ');
        else return id;
    }

    /**
     * Checks that the provided {@link X34Index Index} has correct hash and URI data, and corrects it if necessary.
     * Automatically detects which processor created this index.
     * Processor detection is done by checking the Index's metadata for the 'processor' tag.
     * Operates by calling {@link X34.Processors.X34RetrievalProcessor#validateIndex(X34Index)} on whichever processor created
     * the provided index.
     * If the index cannot be identified, this method will return {@code false}.
     * @param index the index to validate against the server
     * @return true if validation was successful, false if not all contained images validated successfully
     * @throws IOException if a non-recoverable I/O error was encountered during operation
     * @see X34.Processors.X34RetrievalProcessor#validateIndex(X34Index) for more information on the verification process
     */
    public static boolean validateIndex(X34Index index) throws IOException
    {
        if(index == null || index.entries.size() == 0) return true;
        if(index.metadata.get("processor") == null) return false;

        return X34ProcessorRegistry.getProcessorForID(index.metadata.get("processor")).validateIndex(index);
    }
}
