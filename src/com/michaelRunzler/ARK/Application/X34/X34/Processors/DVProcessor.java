package X34.Processors;

import X34.Core.*;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.ARKJsonParser.ARKJsonObject;
import core.CoreUtil.ARKJsonParser.ARKJsonParser;
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
import static X34.Core.X34CoreMetadataKeyMap.IS_CANCELLED;

public class DVProcessor extends X34RetrievalProcessor
{
    private final String ID = "DVRT";
    private final String INF = "DeviantArt";

    private final String PAGESRV_ROOT       = "https://www.deviantart.com/newest/?q=";
    private final String PAGESRV_PID_PREFIX = "&offset=";

    private final String AUTH_CLIENT_ID     = "7591";
    private final String AUTH_CLIENT_SECRET = "1e68d7fd21576d79a7a71f63dd21aa7f";
    private final String AUTH_CLIENT_PATH   = "https://www.deviantart.com/api/v1/oauth2/browse/newest?q=";
    private final String AUTH_UUID_PREFIX   = "&mature_content=true&limit=24&access_token=";
    private final String AUTH_TOKEN_REQUEST = "https://www.deviantart.com/oauth2/token?grant_type=client_credentials&client_id=" + AUTH_CLIENT_ID + "&client_secret=" + AUTH_CLIENT_SECRET;

    // legacy variables for old routine
    private final String IMG_LINK_START     = "data-super-full-img=\"";
    private final String IMG_LINK_END       = "\" data-super-full-width";
    private final String LINK_HASH_START    = "<img data-sigil=\"torpedo-img\" src=\"https://t00.deviantart.net/";
    private final String LINK_HASH_END      = "=";

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

        // Request access token from DeviantArt API backend
        log.logEvent("Authenticating with API backend. Client ID: " + AUTH_CLIENT_ID);
        String token = getAuthToken();
        if(token == null){
            // If we couldn't get an API access token, return with no images
            log.logEvent(LogEventLevel.ERROR, "API authentication request failed.");
            return null;
        }else{
            log.logEvent("Authentication token received: " + token);
        }

        ARKJsonObject json;
        String URLBase = AUTH_CLIENT_PATH + schema.query.replace(' ', '+').toLowerCase() + AUTH_UUID_PREFIX + token + PAGESRV_PID_PREFIX;
        ArrayList<X34Image> images = new ArrayList<>();

        int currentOffset = 0;
        int failed = 0;
        final int PAGE_OFFSET_DELTA = 20;
        int total = 0;
        boolean rtlTriggered = false;

        if(maxPageProperty != null) maxPageProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
        if(errorProperty != null) errorProperty.set(null);

        // Loop until we run out of pages
        do{
            if(pageProperty != null) pageProperty.set(currentOffset);
            if(cancelledProperty != null && cancelledProperty.get()){
                log.logEvent(LogEventLevel.WARNING, "Retrieval process cancellation request received, stopping.");
                return null;
            }

            // If we have failed more than 10 pages in a row, assume some kind of critical network error and break loop.
            if(failed > 10){
                log.logEvent(LogEventLevel.CRITICAL, "Failed 10 pages in a row, assuming critical network error and aborting.");
                if(errorProperty != null) errorProperty.set("Critical network error");
                currentOffset = -1;
                continue;
            }

            // Get JSON data from URL. Skip page if the read fails.
            try{
                json = ARKJsonParser.loadFromURL(URLBase + currentOffset);
            }catch (FileNotFoundException e){
                // Force the JSON data to null, forcing the end-of-page handler, since the server has told us that this is the case.
                json = null;
            } catch (IOException e) {
                // Detect error code and take action accordingly.
                switch (ProcessorUtils.getHttpErrorCode(e))
                {
                    case 400:
                    case 401:
                        // If the server gives an auth error, try getting a new token.
                        log.logEvent(LogEventLevel.WARNING, "API authentication token expired, getting new one...");
                        String newToken = getAuthToken();
                        if(newToken == null){
                            // If we couldn't get an API access token, return with no images
                            log.logEvent(LogEventLevel.ERROR, "API authentication request failed.");
                            if(errorProperty != null) errorProperty.set("API auth request failed");
                            return null;
                        }
                        // Refresh base URL with the new token and keep going.
                        URLBase = AUTH_CLIENT_PATH + schema.query.replace(' ', '+').toLowerCase() + AUTH_UUID_PREFIX + newToken + PAGESRV_PID_PREFIX;
                        log.logEvent("New authentication token received: " + newToken);
                        continue;
                    case 403:
                    case 429:
                        // The server has triggered its rate-limiting, wait for about 10s to let it catch up.
                        log.logEvent(LogEventLevel.WARNING, "Adaptive API rate-limiting triggered. Waiting for 10s.");
                        try{Thread.sleep(10000);}catch(InterruptedException e1){continue;}
                        log.logEvent("Resuming retrieval.");
                        // If the rate-limit detection has been tripped twice in a row, assume that we have run across some kind of severe limit, log it as an error.
                        if(rtlTriggered) failed++;
                        else rtlTriggered = true;
                        continue;
                    default:
                        // Otherwise, the error is probably something we can't deal with, error out.
                        log.logEvent(LogEventLevel.ERROR, "Encountered I/O error during page read, skipping page. Exception details below.");
                        log.logEvent(e);
                        currentOffset += PAGE_OFFSET_DELTA;
                        failed ++;
                        continue;
                }
            }

            // If the JSON data is null, assume that the read failed and skip this page.
            // If the data does not contain a valid result array, assume that we have hit the end of the valid page range
            // or that the tag wasn't valid in the first place, and stop the loop.
            if(json == null){
                log.logEvent(LogEventLevel.ERROR, "Pulled invalid JSON data, assuming I/O error and skipping page.");
                currentOffset += PAGE_OFFSET_DELTA;
                failed ++;
                continue;
            }else if(json.getArrayByName("results") == null || json.getArrayByName("results").getSubElements().length == 0){
                if(currentOffset >= 1) log.logEvent("End of valid entries.");
                else{
                    log.logEvent(LogEventLevel.WARNING, "Tag appears to be invalid (reason: first page returned no-images warning)");
                    if(errorProperty != null) errorProperty.set("Invalid tag");
                }
                currentOffset = -1;
                continue;
            }

            // Get result array elements.
            ARKJsonElement[] results = json.getArrayByName("results").getSubElements();

            // Try to get the total estimated number of images from JSON.
            if(total == 0){
                try {
                    total = Integer.parseInt(json.getElementByName("estimated_total").getDeQuotedValue());
                    if(maxPageProperty != null) maxPageProperty.set(total);
                }catch (NumberFormatException | NullPointerException ignored){}
            }

            log.logEvent("Getting images " + currentOffset + " to " + (currentOffset + results.length) + " of " + (total == 0 ? "an unknown number" : total) + "...");

            int count = 0;
            for(ARKJsonElement result : results)
            {
                // Get 'content' sub-element from result. This will be absent from all results that are journal entries, application entries,
                // and other things like that.
                ARKJsonElement content = result.getSubElementByName("content");
                if(content == null || !content.hasSubElements()) continue;

                // Get full-size source link from content sub-element. It shouldn't ever be null or zero-length, but check anyway.
                String link = content.getSubElementByName("src").getDeQuotedValue();
                if(link == null || link.isEmpty()) continue;

                // Get deviation hash ID from result. Remove hyphens, and convert it to hex bytes.
                String DID = result.getSubElementByName("deviationid").getDeQuotedValue();
                byte[] hash = ARKArrayUtil.hexStringToBytes(DID.replace("-", ""));

                try{
                    images.add(new X34Image(new URL(link), schema.query, hash, this.getID()));
                }catch (MalformedURLException e){
                    log.logEvent("Image link #" + (count + 1) + " is invalid, skipping.");
                }

                count ++;
            }

            log.logEvent("Got " + count + " image" + (count == 1 ? "" : "s") + " from offset ID " + currentOffset);

            // Get offset from JSON. If it cannot be read for some reason, assume that there are no more images and break the loop.
            try {
                currentOffset = Integer.parseInt(json.getElementByName("next_offset").getValue());
            }catch (NumberFormatException | NullPointerException e){
                log.logEvent("End of valid entries.");
                currentOffset = -1;
                continue;
            }

            // Reset the rate-limit detection flag since we got a page successfully.
            rtlTriggered = false;

            // Sleep for 2s to avoid server overload.
            try{Thread.sleep(2000);}catch(InterruptedException ignored){}
        }while (currentOffset != -1);

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

    private String getAuthToken()
    {
        try {
            ARKJsonObject auth = new ARKJsonObject(ProcessorUtils.tryDataTransfer(AUTH_TOKEN_REQUEST, 2));
            auth.parse();
            ARKJsonElement key = auth.getElementByName("access_token");
            return key.getDeQuotedValue();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Older version of the retrieval sequence for DeviantArt. Uses the basic page-grab sequence instead of the DeviantArt API.
     * @param index see {@link X34RetrievalProcessor#process(X34Index, X34Schema)}
     * @param schema see {@link X34RetrievalProcessor#process(X34Index, X34Schema)}
     * @return see {@link X34RetrievalProcessor#process(X34Index, X34Schema)}
     * @throws ValidationException if the provided {@link X34Schema} failed to pass validation
     * @see X34RetrievalProcessor#process(X34Index, X34Schema)
     * @deprecated in favor of the newer API-driven process, detailed in {@link #process(X34Index, X34Schema)}.
     */
    @Deprecated
    private ArrayList<X34Image> retrieveNoAuthAPI(X34Index index, X34Schema schema) throws ValidationException
    {
        if(!schema.validate()) throw new ValidationException("Schema failed to pass validation");
        XLoggerInterpreter log = new XLoggerInterpreter(this.INF + " RT Module");

        log.logEvent("Starting retrieval...");
        log.logEvent(LogEventLevel.DEBUG, "----DEBUG INFO DUMP----");
        log.logEvent(LogEventLevel.DEBUG, "Index ID: " + index.id);
        log.logEvent(LogEventLevel.DEBUG, "Index length: " + index.entries.size());
        log.logEvent(LogEventLevel.DEBUG, "Schema query: " + schema.query);
        log.logEvent(LogEventLevel.DEBUG, "Processor ID: " + this.getID());

        String page;
        String URLBase = PAGESRV_ROOT + schema.query.replace(' ', '+').toLowerCase() + PAGESRV_PID_PREFIX;
        ArrayList<X34Image> images = new ArrayList<>();

        int currentOffset = 0;
        int failed = 0;
        final int PAGE_OFFSET_DELTA = 20;

        // Loop until we run out of pages
        do{
            // If we have failed more than 10 pages in a row, assume some kind of critical network error and break loop.
            if(failed > 10){
                log.logEvent(LogEventLevel.CRITICAL, "Failed 10 pages in a row, assuming critical network error and aborting.");
                currentOffset = -1;
                continue;
            }

            // Get pagedata from URL. Skip page if the read fails.
            try{
                page = ProcessorUtils.tryDataTransfer(URLBase + currentOffset, 5);
            }catch (FileNotFoundException e){
                // Force the page to length-0, forcing the end-of-page handler, since the server has told us that this is the case.
                page = "";
            } catch (IOException e){
                log.logEvent(LogEventLevel.ERROR, "Encountered I/O error during page read, skipping page. Exception details below.");
                log.logEvent(e);
                currentOffset += PAGE_OFFSET_DELTA;
                failed ++;
                continue;
            }

            // If the page length is 0, assume that the read failed and skip this page.
            // If the page does not contain the image link marker, assume that we have hit the end of the valid page range
            // or that the tag wasn't valid in the first place, and stop the loop.
            if(page.length() == 0){
                log.logEvent(LogEventLevel.ERROR, "Pulled data with length of 0, assuming I/O error and skipping page.");
                currentOffset += PAGE_OFFSET_DELTA;
                failed ++;
                continue;
            }else if(!page.contains(IMG_LINK_START)){
                if(currentOffset >= 1) log.logEvent("End of valid entries.");
                else log.logEvent(LogEventLevel.WARNING, "Tag appears to be invalid (reason: first page returned no-images warning)");
                currentOffset = -1;
                continue;
            }

            log.logEvent("Getting images from offset ID " + currentOffset);

            // Find first image link, guaranteed not to be -1, since we checked that earlier.
            int check = page.indexOf(IMG_LINK_START);
            int count = 0;
            // Loop until we run out of image links in the page.
            while(check > -1)
            {
                // Get image link
                String link = IOTools.getFieldFromData(page, IMG_LINK_START, IMG_LINK_END, check);

                // If one of the markers could not be found, assume that there are no more images and break the loop.
                if(link == null){
                    check = -1;
                    continue;
                }

                // Get image UUID from the page (it's not in the link - it's in the link-src tag below it). If we got a valid
                // substring, remove any dashes or underscores from it so that the conversion method can deal with it.
                String hashB36 = IOTools.getFieldFromData(page, LINK_HASH_START, LINK_HASH_END, check);
                if(hashB36 != null) hashB36 = hashB36.replace("-", "").replace("_", "");

                // Convert the base-36 UUID into a usable base-16 hash ID.
                byte[] hash = ARKArrayUtil.hexStringToBytes(ARKArrayUtil.base36ToHexString(hashB36));

                // Since this image was valid, update the start point and try for another image.
                check = page.indexOf(IMG_LINK_START, check + IMG_LINK_START.length());

                try{
                    images.add(new X34Image(new URL(link), schema.query, hash, this.getID()));
                }catch (MalformedURLException e){
                    log.logEvent("Image link #" + (count + 1) + " is invalid, skipping.");
                }

                count ++;
            }

            log.logEvent("Got " + count + " image" + (count == 1 ? "" : "s") + " from offset ID " + currentOffset);

            currentOffset += count;
        }while (currentOffset != -1);

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
            if(!img.source.toString().contains(".deviantart.net")) throw new IllegalArgumentException("Provided index was not created with this processor!");
        }

        // Check image source data and integrity.
        ArrayList<X34Image> removed = new ArrayList<>();
        for(X34Image img : index.entries) {
            if(!img.source.toString().contains(".deviantart.net")) continue;
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
