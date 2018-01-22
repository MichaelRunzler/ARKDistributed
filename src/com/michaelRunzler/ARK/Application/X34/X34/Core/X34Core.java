package X34.Core;

import X34.Core.IO.X34ConfigIO;
import X34.Core.IO.X34IndexDelegator;
import X34.Core.IO.X34IndexIO;
import X34.Processors.X34ProcessorRegistry;
import X34.Processors.X34RetrievalProcessor;
import com.sun.istack.internal.NotNull;
import core.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;

import javax.xml.bind.ValidationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Runs repository queries and retrieval operations.
 */
public class X34Core
{
    private XLoggerInterpreter log;
    private X34IndexIO loader;

    /**
     * Default constructor. Uses the main instance from the {@link X34IndexDelegator}
     * for its reference to the {@link X34ConfigIO config} and {@link X34IndexIO} index I/O objects.
     */
    public X34Core()
    {
        log = new XLoggerInterpreter();
        loader = X34IndexDelegator.getMainInstance();
        log.logEvent("Initialization complete.");
    }

    /**
     * Same as the default constructor, but uses the specified dynamic instance from the {@link X34IndexDelegator}
     * for its reference to the {@link X34ConfigIO config} and {@link X34IndexIO} index I/O objects.
     * @param IID the instance ID to pass to the Delegator
     */
    public X34Core(short IID)
    {
        log = new XLoggerInterpreter();
        loader = X34IndexDelegator.getDynamicInstance(IID);
        log.logEvent("Initialization complete.");
    }

    /**
     * Runs a retrieval operation with the specified parameters.
     * @param config the {@link X34Schema} to pull configuration information from. This Schema must pass validity checks,
     *               or a {@link ValidationException} will be thrown.
     * @return the list of new {@link X34Image images} from the retrieval operation. May be null or zero-length.
     * @throws ValidationException if any elements used in the retrieval operation fail validation checks
     * @throws IOException if any unrecoverable I/O errors occur during retrieval
     */
    public ArrayList<X34Image> retrieve(@NotNull X34Schema config) throws ValidationException, IOException
    {
        log.logEvent("Validating schema...");
        // Validate the incoming Schema.
        if(!config.validate()) throw new ValidationException("Schema failed to pass validation");
        log.logEvent("Validated successfully.");

        log.logEvent("Loading processor...");
        // Grab the Processor instance specified by the Schema.
        X34RetrievalProcessor processor = X34ProcessorRegistry.getProcessorForID(config.type);
        if(processor == null) throw new IOException("Specified processor ID could not be found");
        log.logEvent("Loaded!");

        // Get the index specified by the Schema from the index loader. If it errors out, assume that it does not exist
        // and create an empty one.
        log.logEvent("Loading index...");
        X34Index index;
        try{
            index = loader.loadIndex(config.query, config.type);
            log.logEvent("Index loaded.");
        }catch (IOException e){
            index = new X34Index(config.query);
            log.logEvent("No index available. New index created.");
        }

        // Run retrieval
        log.logEvent("Retrieving...");
        ArrayList<X34Image> newImages = processor.process(index, config);

        // If the index doesn't have a meta tag indicating processor origin, add it.
        if(!index.metadata.containsKey("processor")) index.metadata.put("processor", processor.getID());

        // Check if we got any new images from the processor. If so, save the index and return them. If not, skip saving
        // the index, since it will be the same as when loaded, and return an empty array.
        if(newImages == null || newImages.size() == 0){
            log.logEvent("No new images available from processor.");
            return new ArrayList<>();
        }
        else{
            log.logEvent("New image" + (newImages.size() > 1 ? "s" : "") + " available. Saving index...");
            loader.saveIndex(index);
            log.logEvent("Index save successful.");
            return newImages;
        }
    }

    /**
     * Writes a series of {@link X34Image images} to disk. Names files according to the result of calling
     * {@link X34RetrievalProcessor#getFilenameFromURL(URL)} on the Image's URL.
     * @param images the list of images to write to disk
     * @param parent the parent directory to write image file to
     * @param processorID the ID of the processor to use while obtaining filenames from the images' URLs
     * @param overwriteExisting set this to true if you wish to attempt to overwrite existing files with the same names if
     *                          they exist
     * @param createDirs set this to true if directory creation should be attempted for the provided parent directory
     * @throws IOException if a non-recoverable I/O error is encountered during file write or deletion
     */
    public void writeImagesToFile(ArrayList<X34Image> images, File parent, String processorID, boolean overwriteExisting, boolean createDirs) throws IOException
    {
        if(parent == null || (!parent.exists() && !createDirs)) throw new IllegalArgumentException("Destination directory is invalid or does not exist");
        if(processorID == null || processorID.isEmpty()) throw new IllegalArgumentException("Processor ID is invalid");
        if(images == null || images.size() == 0) return;
        if(createDirs && !parent.exists() && !parent.mkdirs()) throw new IOException("Unable to create parent directory");

        X34RetrievalProcessor xpr = X34ProcessorRegistry.getProcessorForID(processorID);
        if(xpr == null) throw new IOException("Cannot get reference to provided processor");

        log.logEvent("Attempting to write " + images.size() + " image" + (images.size() == 1 ? "" : "s" ) + " to disk...");

        int count = 0;
        for(X34Image x : images){
            if(x.writeToFile(parent, overwriteExisting, xpr)){
                log.logEvent("Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " written successfully.");
                count ++;
            }
            else log.logEvent("Failed to write image " + ARKArrayUtil.byteArrayToHexString(x.hash) + ".");
        }

        log.logEvent(count + " of " + images.size() + " image" + (images.size() == 1 ? "" : "s") + " written successfully.");
    }
}
