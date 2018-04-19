package X34.Core;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Encapsulates configuration information and metadata for retrieval routines executed by the X34Core.
 */
public class X34Schema implements Serializable
{
    // Static serial version UID to enable backwards-compatibility with older versions that use <String, String> metadata
    // instead of <String, Object> metadata.
    public static final long serialVersionUID = -2449289277630283033L;

    public String query;
    public String type;
    public HashMap<String, Object> metadata;

    public X34Schema(String query, String type, HashMap<String, Object> meta)
    {
        this.query = query;
        this.type = type;
        this.metadata = meta;
    }

    /**
     * Validates the contents of this Schema, making sure that the query and type are valid. Metadata validity is not
     * guaranteed, since the contents of the metadata map are not forced to conform to any specific standards.
     * @return true if this Schema passed validation, false if otherwise
     */
    public boolean validate() {
        return query != null && !query.isEmpty() && type != null && !type.isEmpty();
    }
}
