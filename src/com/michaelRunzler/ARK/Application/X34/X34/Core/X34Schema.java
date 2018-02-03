package X34.Core;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Encapsulates configuration information and metadata for retrieval routines executed by the X34Core.
 */
public class X34Schema implements Serializable
{
    public String query;
    public String type;
    public HashMap<String, String> metadata;

    public X34Schema(String query, String type, HashMap<String, String> meta)
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
    public boolean validate()
    {
        if(query == null || query.isEmpty() || type == null || type.isEmpty()) return false;
        else return true;
    }
}
