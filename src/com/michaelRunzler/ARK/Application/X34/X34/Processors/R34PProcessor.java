package X34.Processors;

import X34.Core.*;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.IOTools;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static X34.Core.X34CoreMetadataKeyMap.*;

/**
 * Pulls image data from <a href="http://rule34.paheal.net/">Rule 34 Paheal</a>.
 */
public class R34PProcessor extends X34RetrievalProcessor
{
    private final String ID = "R34P";
    private final String INF = "Rule34 Paheal";

    private final String PAGESRV_ROOT       = "http://rule34.paheal.net/post/list/";
    private final String PAGESRV_PID_PREFIX = "/";
    private final String IMG_SECTION_START  = "<section id='imagelist'>";
    private final String IMG_SECTION_END    = "<section id='paginator'>";
    private final String IMG_LINK_START     = "<br><a href=\"";
    private final String IMG_LINK_END       = "\">Image Only</a>";
    private final String LINK_HASH_START    = "/_images/";
    private final String LINK_HASH_END      = "/";
    private final String END_OF_PAGES       = "<title>No Images Found</title>\n";

    @Override
    public ArrayList<X34Image> process(X34Index index, X34Schema schema) throws ValidationException
    {
        if(!schema.validate()) throw new ValidationException("Schema failed to pass validation");
        XLoggerInterpreter log = new XLoggerInterpreter(this.INF + " RT Module");

        SimpleIntegerProperty maxPageProperty = null;
        SimpleIntegerProperty pageProperty = null;
        SimpleStringProperty errorProperty = null;
        SimpleBooleanProperty cancelledProperty = null;

        boolean HAS_TELEMETRY = false;

        // Initialize telemetry variables if they are present and valid
        if(schema.metadata.containsKey(TELEMETRY_CAPABLE) && (boolean)schema.metadata.get(TELEMETRY_CAPABLE)){
            maxPageProperty = (SimpleIntegerProperty)schema.metadata.get(TELEM_MAX_PAGE_PROP);
            pageProperty = (SimpleIntegerProperty)schema.metadata.get(TELEM_CURR_PAGE_PROP);
            errorProperty = (SimpleStringProperty)schema.metadata.get(TELEM_ERROR_STATE_PROP);
            cancelledProperty = (SimpleBooleanProperty)schema.metadata.get(IS_CANCELLED);
            HAS_TELEMETRY = true;
        }

        log.logEvent("Starting retrieval...");
        log.logEvent(LogEventLevel.DEBUG, "----DEBUG INFO DUMP----");
        log.logEvent(LogEventLevel.DEBUG, "Index ID: " + index.id);
        log.logEvent(LogEventLevel.DEBUG, "Index length: " + index.entries.size());
        log.logEvent(LogEventLevel.DEBUG, "Schema query: " + schema.query);
        log.logEvent(LogEventLevel.DEBUG, "Processor ID: " + this.getID());
        log.logEvent(LogEventLevel.DEBUG, "Telemetry enabled: " + HAS_TELEMETRY);

        String page;
        String URLBase = PAGESRV_ROOT + schema.query.replace(' ', '_').toLowerCase().trim() + PAGESRV_PID_PREFIX;
        ArrayList<X34Image> images = new ArrayList<>();

        int currentPage = 1;
        int failed = 0;
        boolean rtlTriggered = false;

        if(maxPageProperty != null) maxPageProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
        if(errorProperty != null) errorProperty.set(null);

        // Loop until we run out of pages.
        do{
            if(pageProperty != null) pageProperty.set(currentPage);
            if(cancelledProperty != null && cancelledProperty.get()){
                log.logEvent(LogEventLevel.WARNING, "Retrieval process cancellation request received, stopping.");
                return null;
            }

            // If we have failed more than 10 pages in a row, assume some kind of critical network error and break loop.
            if(failed > 10){
                log.logEvent(LogEventLevel.CRITICAL, "Error 15040: Failed 10 pages in a row, assuming critical network error and aborting.");
                if(errorProperty != null) errorProperty.set("Critical network error");
                currentPage = -1;
                continue;
            }

            // Get pagedata from URL. Skip page if the read fails.
            try{
                page = ProcessorUtils.tryDataTransfer(URLBase + currentPage, 5);
            }catch (FileNotFoundException e){
                // Force the page to the page end marker, forcing the end-of-page handler, since the server has told us that this is the case.
                page = END_OF_PAGES;
            } catch (IOException e){
                // Detect error code and take action accordingly.
                switch (ProcessorUtils.getHttpErrorCode(e))
                {
                    case 429:
                        // The server has triggered its rate-limiting, wait for about 10s to let it catch up.
                        log.logEvent(LogEventLevel.WARNING, "Adaptive API rate-limiting triggered. Waiting for 10s.");
                        try{Thread.sleep(10000);}catch(InterruptedException e1){continue;}
                        log.logEvent("Resuming retrieval.");
                        // If the rate-limit detection has been tripped twice in a row, assume that we have run across some kind of severe limit, log it as an error.
                        if(rtlTriggered){
                            failed ++;
                            rtlTriggered = false;
                        }
                        else rtlTriggered = true;
                        continue;
                    default:
                        log.logEvent(LogEventLevel.ERROR, "Error 04000: Encountered I/O error during page read, skipping page. Exception details below.");
                        log.logEvent(e);
                        currentPage ++;
                        failed ++;
                        continue;
                }
            }

            // If the page length is 0, assume that the read failed and skip this page.
            // If the page contains the end-of-pages marker, assume that we have hit the end of the valid page range
            // or that the tag wasn't valid in the first place, and stop the loop.
            if(page.length() == 0){
                log.logEvent(LogEventLevel.ERROR, "Error 04001: Pulled data with length of 0, assuming I/O error and skipping page.");
                currentPage ++;
                failed ++;
                continue;
            }else if(page.contains(END_OF_PAGES)){
                if(currentPage > 1) log.logEvent("End of valid entries.");
                else{
                    log.logEvent(LogEventLevel.WARNING, "Tag appears to be invalid (reason: first page returned no-images warning)");
                    if(errorProperty != null) errorProperty.set("Invalid tag");
                }
                currentPage = -1;
                continue;
            }

            log.logEvent("Getting images from page " + currentPage);

            // Find image section start point
            int check = page.indexOf(IMG_LINK_START, page.indexOf(IMG_SECTION_START));
            int limit = page.indexOf(IMG_SECTION_END);
            int count = 0;
            // Loop until we run out of image links in the page.
            while (check > -1 && check < limit)
            {
                // Get image link
                String link = IOTools.getFieldFromData(page, IMG_LINK_START, IMG_LINK_END, check);

                // If one of the markers could not be found, assume that there are no more images and break the loop.
                if(link == null){
                    check = -1;
                    continue;
                }

                // Get the hashcode of the current image link
                byte[] hash = ARKArrayUtil.hexStringToBytes(IOTools.getFieldFromData(link, LINK_HASH_START, LINK_HASH_END, 0));

                try{
                    images.add(new X34Image(new URL(link), schema.query, hash, this.getID()));
                }catch (MalformedURLException e){
                    log.logEvent("Image link #" + (count + 1) + " is invalid, skipping.");
                }

                // Since this image was valid, update the start point and try for another image.
                check = page.indexOf(IMG_LINK_START, check + IMG_LINK_START.length());
                count ++;
            }

            log.logEvent("Got " + count + " image" + (count == 1 ? "" : "s") + " from page " + currentPage);

            currentPage ++;
        }while (currentPage != -1);

        log.logEvent("Page pull complete.");
        log.logEvent("Checking " + images.size() + " images against index...");

        // Loop through the new-image index if it has any entries, and see if each one is present in the index by checking
        // its hash ID against existing entries.
        ArrayList<X34Image> newImages = ProcessorUtils.checkIndex(index, images);

        for(X34Image i : newImages){
            log.logEvent("No hash match found for image " + ARKArrayUtil.byteArrayToHexString(i.hash) + ", marked as new.");
        }

        log.logEvent("Index check complete. " + newImages.size() + " image" + (newImages.size() == 1 ? "" : "s") + " found.");

        log.disassociate();
        return newImages;
    }

    @Override
    public boolean validateIndex(X34Index index) throws IOException
    {
        if(index == null || index.id == null) return false;
        if(index.entries.size() == 0) return true;

        String id = index.metadata.get("processor");

        // Validate index ownership
        if(id != null && !id.equals(this.getID())) throw new IllegalArgumentException("Provided index was created with another processor: " + id);
        if(id == null){
            X34Image img = index.entries.get(0);
            if(!img.source.toString().contains(".paheal.net")) throw new IllegalArgumentException("Provided index was not created with this processor!");
        }

        // Check image source data and integrity.
        ArrayList<X34Image> removed = new ArrayList<>();
        for(X34Image img : index.entries) {
            if(!img.source.toString().contains(".paheal.net")) continue;
            if(!img.verifyIntegrity()) removed.add(img);
        }

        index.entries.removeAll(removed);

        return removed.size() == 0;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getInformalName() {
        return INF;
    }

    @Override
    public String getFilenameFromURL(URL source) {
        String res = source.toString();
        return res.substring(res.lastIndexOf('/') + 1, res.length());
    }
}
