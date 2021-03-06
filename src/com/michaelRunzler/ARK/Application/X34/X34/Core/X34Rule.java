package X34.Core;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Stores a complete set of {@link X34Schema} instances for use by the {@link X34Core}.
 * This class is typically used to represent a single query across multiple repos, where storing individual {@link X34Schema Schemas} would
 * be prohibitively complex or memory-expensive. This class stores the arguments necessary to generate said {@link X34Schema Schemas}, and
 * only generates the required {@link X34Schema Schemas} when requested via the {@link #getSchemas()} method.
 */
public class X34Rule implements Serializable
{
    private static final long serialVersionUID = -213706100518926267L;

    public String query;
    private String[] processors;
    private HashMap<String, Object> meta;

    /**
     * Constructs a new instance of this object.
     * @param query the query to be used by all {@link X34Schema Schemas} that will be generated by this object
     * @param metadata the metadata array to be used by all {@link X34Schema Schemas} that will be generated by this object
     * @param processors one or more {@link X34.Processors.X34RetrievalProcessor Processor} IDs, one for each {@link X34Schema Schema} that will be generated
     */
    public X34Rule(@NotNull String query, @Nullable HashMap<String, Object> metadata, @NotNull String... processors)
    {
        if(query == null || processors == null || query.isEmpty() || processors.length == 0) throw new IllegalArgumentException("Input query and processor ID list cannot be null or zero-length.");
        this.query = query;
        this.processors = processors;
        this.meta = metadata;
    }

    /**
     * Gets the virtual list of {@link X34Schema Schemas} stored within this object. The resultant list is generated on-demand
     * to save memory and reduce serialization times. All generated {@link X34Schema Schemas} in the result list share their
     * query and metadata, and only differ in their processor ID.
     * @return the generated list of {@link X34Schema Schemas} from this object's virtual storage
     */
    public X34Schema[] getSchemas()
    {
        X34Schema[] temp = new X34Schema[processors == null ? 0 : processors.length];

        for(int i = 0; i < temp.length; i++){
            temp[i] = new X34Schema(query, processors[i], meta);
        }

        return temp;
    }

    /**
     * Gets a copy (not a reference) of the current internal processor ID list.
     * @return a copy of the internal processor ID array used to generate {@link X34Schema Schemas}
     */
    public String[] getProcessorList() {
        return Arrays.copyOf(this.processors, processors.length);
    }

    /**
     * Gets the current metadata map being stored by this object.
     * @return the current metadata map
     */
    public @Nullable HashMap<String, Object> getMetaData() {
        return meta;
    }

    /**
     * Sets this object's metadata map to the specified {@link HashMap}.
     * @param meta the new metadata map to store
     */
    public void setMetaData(@Nullable HashMap<String, Object> meta) {
        this.meta = meta;
    }

    /**
     * Validates each contained virtual {@link X34Schema Schema} by calling {@link X34Schema#validate()} on each index in
     * the array returned by {@link #getSchemas()}.
     * @return {@code true} if all virtual {@link X34Schema Schemas} passed validation, {@code false} otherwise
     */
    public boolean validate()
    {
        if(getSchemas().length == 0) return false;
        for(X34Schema x : getSchemas()) if (!x.validate()) return false;
        return true;
    }
}
