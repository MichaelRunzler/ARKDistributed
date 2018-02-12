package X34.Core;

import X34.Core.IO.X34ConfigIO;
import X34.Core.IO.X34IndexDelegator;
import X34.Core.IO.X34IndexIO;
import X34.Processors.X34ProcessorRegistry;
import X34.Processors.X34RetrievalProcessor;
import com.sun.istack.internal.NotNull;
import core.AUNIL.LogEventLevel;
import core.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

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
     * Runs a retrieval operation with the {@link X34Schema Schema(s)} contained within the supplied {@link X34Rule}.
     * After validating all arguments and their subcomponents, calls {@link #retrieve(X34Schema)} for each valid contained {@link X34Schema},
     * and adds its results to a master {@link ArrayList}, which is then returned.
     * @param configList the {@link X34Rule} to pull the list of {@link X34Schema Schemas} from
     * @return the complete list of {@link X34Image Images} from each retrieval process, compounded into a single {@link ArrayList}
     * @throws ValidationException if the provided {@link X34Rule} or all of its contained {@link X34Schema Schemas} fail validation
     */
    public ArrayList<X34Image> retrieve(@NotNull X34Rule configList) throws ValidationException
    {
        // Check Rule validity
        if(configList == null) throw new ValidationException("Rule failed to pass validation.");
        ArrayList<X34Schema> schemas = new ArrayList<>(Arrays.asList(configList.getSchemas()));

        log.logEvent("Validating schema" + (schemas.size() == 1 ? "" : "s") + "...");

        // Validate Schemas one by one, adding their indexes to the buffer array if they fail validation
        ArrayList<Integer> failed = new ArrayList<>();
        for(int i = 0; i < schemas.size(); i++){
            if(!schemas.get(i).validate()) failed.add(i);
        }

        // If the size of the buffer is the same as the Schema array, we know all of them failed validation
        if(failed.size() == schemas.size()) throw new ValidationException("All Schemas failed to validate.");

        // Otherwise, print the indices of the schema(s) that failed validation (if there are any) and remove them
        if(failed.size() > 0){
            log.logEvent("The following schema" + (failed.size() == 1 ? "" : "s") + " failed to validate:");
            for(int i : failed){
                log.logEvent(schemas.get(i).type + " : " + schemas.get(i).query + " @ index " + i);
                schemas.remove(schemas.get(i));
            }
        }

        // Iterate through the remaining schemas (guaranteed not to be 0, since we already checked that) and run retrieval for all of them,
        // adding their results to the master array as we do so.
        ArrayList<X34Image> returned = new ArrayList<>();
        for(int i = 0; i < schemas.size(); i++){
            log.logEvent("Running retrieval operation " + (i + 1) + " of " + schemas.size());
            try {
                returned.addAll(retrieve(schemas.get(i)));
                log.logEvent("Retrieval " + (i + 1) + " of " + schemas.size() + " completed with no errors.");
            } catch (IOException | ValidationException e) {
                log.logEvent("Retrieval operation returned exception with partial results, see below.");
                log.logEvent(e);
            }
        }

        return returned;
    }

    /**
     * Writes a series of {@link X34Image images} to disk. Names files according to the result of calling
     * {@link X34RetrievalProcessor#getFilenameFromURL(URL)} on the Image's URL.
     * @param images the list of images to write to disk
     * @param parent the parent directory to write image file to
     * @param overwriteExisting set this to true if you wish to attempt to overwrite existing files with the same names if
     *                          they exist
     * @param createDirs set this to true if directory creation should be attempted for the provided parent directory
     * @throws IOException if a non-recoverable I/O error is encountered during file write or deletion
     */
    public void writeImagesToFile(ArrayList<X34Image> images, File parent, boolean overwriteExisting, boolean createDirs) throws IOException
    {
        if(parent == null || (!parent.exists() && !createDirs)) throw new IllegalArgumentException("Destination directory is invalid or does not exist");
        if(images == null || images.size() == 0) return;
        if(createDirs && !parent.exists() && !parent.mkdirs()) throw new IOException("Unable to create parent directory");

        log.logEvent("Attempting to write " + images.size() + " image" + (images.size() == 1 ? "" : "s" ) + " to disk...");

        int count = 0;
        for(X34Image x : images){
            try {
                if(x.writeToFile(parent, overwriteExisting)) log.logEvent("Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " already exists.");
                else{
                    log.logEvent("Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " written successfully.");
                    count ++;
                }
            }catch (IOException e){
                log.logEvent(LogEventLevel.ERROR, "Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " encountered critical write error, see below for details.");
                log.logEvent(e);
            }
        }

        log.logEvent(count + " of " + images.size() + " image" + (images.size() == 1 ? "" : "s") + " written successfully.");
    }
}
