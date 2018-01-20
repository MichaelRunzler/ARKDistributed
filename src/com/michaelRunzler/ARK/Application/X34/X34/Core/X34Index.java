package X34.Core;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Stores {@link X34Image images} in a compact, metadata-enabled format.
 */
public class X34Index
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
}
