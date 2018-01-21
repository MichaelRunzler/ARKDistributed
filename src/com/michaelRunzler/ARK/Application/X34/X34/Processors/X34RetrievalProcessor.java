package X34.Processors;

import X34.Core.X34Core;
import X34.Core.X34Image;
import X34.Core.X34Index;
import X34.Core.X34Schema;
import com.sun.istack.internal.NotNull;

import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Superclass for Image Processors, that is, classes that can be run by the {@link X34Core} class in order to retrieve images
 * from a remote repository or server. Takes configuration info from an {@link X34Schema}.
 */
public abstract class X34RetrievalProcessor
{
    /**
     * Processes new {@link X34Image images} into the provided {@link X34Index index} using the configuration data in the provided {@link X34Schema schema}.
     * @param index the index to write new image data to
     * @param schema the schema from which to pull config information and metadata
     * @return the list of new images that were written to the index
     * @throws ValidationException if a provided element fails to pass validation
     */
    public abstract ArrayList<X34Image> process(@NotNull X34Index index, @NotNull X34Schema schema) throws ValidationException;

    /**
     * Checks that the provided {@link X34Index Index} has correct hash and URI data, and corrects it if necessary.
     * The provided index must have been created with this processor, or the method will throw an {@link IllegalArgumentException}.
     * Processor detection is done by checking the Index's metadata for the 'processor' tag, or, if that cannot be found,
     * by checking the contained Images' URIs for identifying marks signifying origin. All images that fail to display such
     * identifying marks will be excluded from validation.
     * @param index the index to validate against the server
     * @return true if validation was successful, false if not all contained images validated successfully
     * @throws IOException if a non-recoverable I/O error was encountered during operation
     * @throws ValidationException if a provided element fails to pass validation
     */
    public abstract boolean validateIndex(@NotNull X34Index index) throws IOException, ValidationException;

    /**
     * Gets this Processor's ID. The ID of a processor is the value that is used by the {@link X34ProcessorRegistry} class to
     * identify each processor..
     * @return this Processor's identification code
     */
    public abstract String getID();

    /**
     * Gets this Processor's informal name. This is the name that should be used to identify this processor in UIs and command-line
     * appplications, anywhere where a more complete name is required.
     * @return this Processor's informal name
     */
    public abstract String getInformalName();
}
