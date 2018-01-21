package X34.Core;

import core.CoreUtil.ARKArrayUtil;

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
        this.id = id;
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
}
