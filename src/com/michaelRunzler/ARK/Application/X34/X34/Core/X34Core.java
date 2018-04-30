package X34.Core;

import X34.Core.IO.X34Config;
import X34.Core.IO.X34IndexDelegator;
import X34.Core.IO.X34IndexIO;
import X34.Processors.X34ProcessorRegistry;
import X34.Processors.X34RetrievalProcessor;
import com.sun.istack.internal.NotNull;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static X34.Core.X34CoreMetadataKeyMap.*;

/**
 * Runs repository queries and retrieval operations.
 */
public class X34Core
{
    private XLoggerInterpreter log;
    private X34IndexIO loader;

    private SimpleIntegerProperty maxPaginationProperty;
    private SimpleIntegerProperty paginationProperty;
    
    private SimpleIntegerProperty maxProcessorCountProperty; 
    private SimpleIntegerProperty processorCountProperty;

    private SimpleIntegerProperty maxDownloadProgressProperty;
    private SimpleIntegerProperty downloadProgressProperty;
    
    private SimpleStringProperty  currentProcessorProperty;
    private SimpleStringProperty  currentQueryProperty;
    private SimpleStringProperty  currentDownloadProperty;

    private SimpleBooleanProperty pushToIndexProperty;
    private SimpleBooleanProperty cancelledProperty;
    
    private ObservableMap<Object, ObservableValue> properties;

    public static final int INVALID_INT_PROPERTY_VALUE = -1;

    private boolean propertyBypass;

    /**
     * Default constructor. Uses the main instance from the {@link X34IndexDelegator}
     * for its reference to the {@link X34Config config} and {@link X34IndexIO} index I/O objects.
     */
    public X34Core()
    {
        log = new XLoggerInterpreter();
        loader = X34IndexDelegator.getMainInstance();
        log.logEvent("Initialization complete.");
        propertyBypass = false;
        propertyInit();
    }

    /**
     * Same as the default constructor, but uses the specified dynamic instance from the {@link X34IndexDelegator}
     * for its reference to the {@link X34Config config} and {@link X34IndexIO} index I/O objects.
     * @param IID the instance ID to pass to the Delegator
     */
    public X34Core(short IID)
    {
        log = new XLoggerInterpreter();
        loader = X34IndexDelegator.getDynamicInstance(IID);
        log.logEvent("Initialization complete.");
        propertyBypass = false;
        propertyInit();
    }
    
    private void propertyInit()
    {
        maxPaginationProperty = new SimpleIntegerProperty();
        paginationProperty = new SimpleIntegerProperty();
        maxProcessorCountProperty = new SimpleIntegerProperty();
        processorCountProperty = new SimpleIntegerProperty();
        currentProcessorProperty = new SimpleStringProperty();
        currentQueryProperty = new SimpleStringProperty();
        currentDownloadProperty = new SimpleStringProperty();
        maxDownloadProgressProperty = new SimpleIntegerProperty();
        downloadProgressProperty = new SimpleIntegerProperty();
        pushToIndexProperty = new SimpleBooleanProperty(true);
        cancelledProperty = new SimpleBooleanProperty();

        Map<Object, ObservableValue> propertyBackingMap = new HashMap<>();
        propertyBackingMap.put(this, maxPaginationProperty);
        propertyBackingMap.put(this, paginationProperty);
        propertyBackingMap.put(this, maxPaginationProperty);
        propertyBackingMap.put(this, maxProcessorCountProperty);
        propertyBackingMap.put(this, processorCountProperty);
        propertyBackingMap.put(this, currentProcessorProperty);
        propertyBackingMap.put(this, currentQueryProperty);
        propertyBackingMap.put(this, currentDownloadProperty);
        propertyBackingMap.put(this, maxDownloadProgressProperty);
        propertyBackingMap.put(this, downloadProgressProperty);
        propertyBackingMap.put(this, pushToIndexProperty);
        propertyBackingMap.put(this, cancelledProperty);

        properties = FXCollections.observableMap(propertyBackingMap);
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

        config = setSchemaMetadataProperties(config);

        if(!propertyBypass) {
            currentQueryProperty.setValue(config.query);
            currentProcessorProperty.setValue(config.type);
            maxProcessorCountProperty.setValue(1);
            processorCountProperty.setValue(1);
        }

        // Run retrieval
        log.logEvent("Retrieving...");
        ArrayList<X34Image> newImages = processor.process(index, config);

        // If the index doesn't have a meta tag indicating processor origin, add it.
        if(!index.metadata.containsKey("processor")) index.metadata.put("processor", processor.getID());

        if(!propertyBypass) {
            currentQueryProperty.setValue(null);
            currentProcessorProperty.setValue(null);
            maxProcessorCountProperty.setValue(INVALID_INT_PROPERTY_VALUE);
            processorCountProperty.setValue(INVALID_INT_PROPERTY_VALUE);
        }

        // If the retrieval was cancelled via the property, return an empty array regardless of what we got from the processor.
        // Otherwise, check if we got any new images from the processor. If so, save the index and return them. If not, skip saving
        // the index, since it will be the same as when loaded, and return an empty array.
        if(cancelledProperty.get()){
            log.logEvent(LogEventLevel.WARNING, "Retrieval cancelled.");
            return new ArrayList<>();
        }else if(newImages == null || newImages.size() == 0){
            log.logEvent("No new images available from processor.");
            return new ArrayList<>();
        }else{
            log.logEvent("New image" + (newImages.size() > 1 ? "s" : "") + " available." + (pushToIndexProperty.get() ?  " Saving index..." : " Index save DISABLED."));
            if(pushToIndexProperty.get()){
                loader.saveIndex(index);
                log.logEvent("Index save successful.");
            }
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
        if(configList == null || !configList.validate()) throw new ValidationException("Rule failed to pass validation.");
        ArrayList<X34Schema> schemas = new ArrayList<>(Arrays.asList(configList.getSchemas()));

        propertyBypass = true;

        currentQueryProperty.setValue(configList.query);
        maxProcessorCountProperty.setValue(schemas.size());

        // Iterate through the available schemas and run retrieval for all of them, adding their results to the master array as we do so.
        ArrayList<X34Image> returned = new ArrayList<>();
        for(int i = 0; i < schemas.size(); i++)
        {
            // If the retrieval has been cancelled, return the current image list, since changes may have already been
            // pushed to their indexes and we don't want to lose any data.
            if(cancelledProperty.get()){
                log.logEvent(LogEventLevel.WARNING, "Retrieval sequence cancelled. Aborting.");
                break;
            }

            X34Schema schema = schemas.get(i);

            schema = setSchemaMetadataProperties(schema);

            processorCountProperty.setValue(i + 1);
            currentProcessorProperty.setValue(schema.type);
            log.logEvent("Running retrieval operation " + (i + 1) + " of " + schemas.size());
            try {
                returned.addAll(retrieve(schema));
                log.logEvent("Retrieval " + (i + 1) + " of " + schemas.size() + " completed with no errors.");
            } catch (IOException e) {
                log.logEvent(LogEventLevel.ERROR, "Retrieval operation returned exception with partial results, see below.");
                log.logEvent(e);
            } catch (ValidationException e){
                log.logEvent(LogEventLevel.WARNING, e.getMessage());
            }
        }

        currentQueryProperty.setValue(null);
        currentProcessorProperty.setValue(null);
        maxProcessorCountProperty.setValue(INVALID_INT_PROPERTY_VALUE);
        processorCountProperty.setValue(INVALID_INT_PROPERTY_VALUE);
        propertyBypass = false;

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
        if(parent == null) throw new IllegalArgumentException("Destination directory cannot be null");
        else if(!parent.exists() && !createDirs) throw new IllegalArgumentException("Destination directory is invalid or does not exist");
        if(images == null || images.size() == 0) return;
        if(createDirs && !parent.exists() && !parent.mkdirs()) throw new IOException("Unable to create parent directory");

        log.logEvent("Attempting to write " + images.size() + " image" + (images.size() == 1 ? "" : "s" ) + " to disk...");

        maxDownloadProgressProperty.set(images.size());

        int count = 0;
        for(int i = 0; i < images.size(); i++)
        {
            if(cancelledProperty.get()){
                log.logEvent(LogEventLevel.WARNING, "Download cancelled.");
                break;
            }

            downloadProgressProperty.set(i + 1);
            X34Image x = images.get(i);
            currentDownloadProperty.set(ARKArrayUtil.byteArrayToHexString(x.hash));
            try {
                if(x.writeToFile(parent, overwriteExisting)) log.logEvent("Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " already exists.");
                else{
                    log.logEvent("Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " written successfully.");
                    count ++;
                }
            }catch (IOException e){
                log.logEvent(LogEventLevel.ERROR, "Error 05031: Image " + ARKArrayUtil.byteArrayToHexString(x.hash) + " encountered critical write error, see below for details.");
                log.logEvent(e);
            }
        }

        log.logEvent(count + " of " + images.size() + " image" + (images.size() == 1 ? "" : "s") + " written successfully.");

        maxDownloadProgressProperty.set(INVALID_INT_PROPERTY_VALUE);
        downloadProgressProperty.set(INVALID_INT_PROPERTY_VALUE);
        currentDownloadProperty.set(null);
    }

    // Schema must have been pre-validated
    private X34Schema setSchemaMetadataProperties(X34Schema schema)
    {
        if(schema.metadata == null) schema.metadata = new HashMap<>();

        schema.metadata.put(PUSH_TO_INDEX, pushToIndexProperty.getValue());
        schema.metadata.put(TELEMETRY_CAPABLE, Boolean.TRUE);
        schema.metadata.put(TELEM_CURR_PAGE_PROP, paginationProperty);
        schema.metadata.put(TELEM_MAX_PAGE_PROP, maxPaginationProperty);
        schema.metadata.put(IS_CANCELLED, cancelledProperty);

        return schema;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        log.disassociate();
    }
    
    //
    // PROPERTY METHODS
    //

    public final SimpleIntegerProperty maxPaginationProperty() {
        return maxPaginationProperty;
    }

    public final SimpleIntegerProperty paginationProperty() {
        return paginationProperty;
    }
    
    public final SimpleIntegerProperty maxProcessorCountProperty() {
        return maxProcessorCountProperty;
    }
    
    public final SimpleIntegerProperty processorCountProperty() {
        return processorCountProperty;
    }

    public final SimpleIntegerProperty maxDownloadProgressProperty() {
        return maxDownloadProgressProperty;
    }

    public final SimpleIntegerProperty downloadProgressProperty() {
        return downloadProgressProperty;
    }
    
    public final SimpleStringProperty currentProcessorProperty() {
        return currentProcessorProperty;
    }

    public final SimpleStringProperty currentQueryProperty() {
        return currentQueryProperty;
    }

    public final SimpleStringProperty currentDownloadProperty() {
        return currentDownloadProperty;
    }

    public final SimpleBooleanProperty pushToIndexProperty(){
        return pushToIndexProperty;
    }

    public final SimpleBooleanProperty cancelledProperty(){
        return cancelledProperty;
    }

    /**
     * Whether or not all {@link X34Schema}s and {@link X34Rule}s executed by this object should push their results to
     * their respective index files. Note that it is the responsibility of {@link X34RetrievalProcessor}s to enforce this
     * setting, and not all processors may be able to enforce it.
     * @param pushToIndex {@code true} if executed {@link X34Schema}s and {@link X34Rule}s should push their changes to their
     *                                indices, {@code false} otherwise
     */
    public void setPushToIndex(boolean pushToIndex){
        pushToIndexProperty.set(pushToIndex);
    }

    /**
     * When called, this will cancel all in-progress retrievals being run on this object. Any successive calls to {@link #retrieve(X34Schema)}
     * or {@link #retrieve(X34Rule)} will reset the underlying property (accessible by calling {@link #cancelledProperty()}), and re-enable
     * retrieval.
     */
    public void cancelRetrieval() {
        cancelledProperty.set(true);
    }

    /**
     * Gets the current property map possessed by this object.
     * @return the current property map. The returned map's keys are of type {@link Object}, and are the class that owns the {@link javafx.beans.property.Property}
     * in question (typically {@link X34Core} for most properties). The map's values are of type {@link ObservableValue},
     * and represent the properties stored within (since {@link ObservableValue} is the lowest-order superclass of all Property values.
     */
    public final ObservableMap<Object, ObservableValue> getProperties() {
        return properties;
    }
}
