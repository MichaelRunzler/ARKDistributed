package X34.Processors;

import X34.Core.X34Image;
import X34.Core.X34Index;
import X34.Core.X34Schema;
import core.AUNIL.LogEventLevel;
import core.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.IOTools;

import javax.xml.bind.ValidationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Pulls image data from <a href="http://rule34.xxx/">Rule 34</a>.
 */
public class R34XProcessor extends X34RetrievalProcessor
{
    private final String ID = "R34X";
    private final String INF = "Rule 34";

    final String PAGESRV_ROOT       = "https://rule34.xxx/index.php?page=dapi&s=post&q=index&limit=100&tags=";
    final String PAGESRV_PID_PREFIX = "&pid=";
    final String IMG_LINK_START     = "file_url=\"";
    final String IMG_LINK_END       = "\" parent_id=";
    final String LINK_HASH_START    = "/images/";
    final String LINK_HASH_SEPARATOR= "/";
    final String LINK_HASH_END      = ".";

    /**
     * Zero-arg constructor to allow calling from the {@link X34ProcessorRegistry} class.
     */
    public R34XProcessor()
    {
    }

    @Override
    public ArrayList<X34Image> process(X34Index index, X34Schema schema) throws ValidationException
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
        String URLBase = PAGESRV_ROOT + schema.query.replace(' ', '_').toLowerCase().trim() + PAGESRV_PID_PREFIX;
        ArrayList<X34Image> images = new ArrayList<>();

        int currentPage = 1;
        int failed = 0;

        // Loop until we run out of pages.
        do{
            // If we have failed more than 10 pages in a row, assume some kind of critical network error and break loop.
            if(failed > 10){
                log.logEvent(LogEventLevel.CRITICAL, "Failed 10 pages in a row, assuming critical network error and aborting.");
                currentPage = -1;
                continue;
            }

            // Get pagedata from URL. Skip page if the read fails.
            try{
                page = ProcessorUtils.tryDataTransfer(URLBase + (currentPage - 1), 5);
            }catch (IOException e){
                log.logEvent(LogEventLevel.ERROR, "Encountered I/O error during page read, skipping page. Exception details below.");
                log.logEvent(e);
                currentPage ++;
                failed ++;
                continue;
            }

            // If the page length is 0, assume that the read failed and skip this page.
            // If the page contains the end-of-pages marker, assume that we have hit the end of the valid page range
            // or that the tag wasn't valid in the first place, and stop the loop.
            if(page.length() == 0){
                log.logEvent(LogEventLevel.ERROR, "Pulled data with length of 0, assuming I/O error and skipping page.");
                currentPage ++;
                failed ++;
                continue;
            }else if(!page.contains(IMG_LINK_START)){
                if(currentPage > 1) log.logEvent("End of valid entries.");
                else log.logEvent(LogEventLevel.WARNING, "Tag appears to be invalid (reason: first page returned no-images warning)");
                currentPage = -1;
                continue;
            }

            log.logEvent("Getting images from page " + currentPage);

            // Find image section start point
            int check = page.indexOf(IMG_LINK_START);
            int count = 0;
            // Loop until we run out of image links in the page.
            while (check > -1)
            {
                // Get image link
                String link = IOTools.getFieldFromData(page, IMG_LINK_START, IMG_LINK_END, check);

                // If one of the markers could not be found, assume that there are no more images and break the loop.
                if(link == null){
                    check = -1;
                    continue;
                }

                // Get the hashcode of the current image link
                byte[] hash = ARKArrayUtil.hexStringToBytes(IOTools.getFieldFromData(link, LINK_HASH_SEPARATOR, LINK_HASH_END, link.indexOf(LINK_HASH_START) + LINK_HASH_START.length()));

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
    public boolean validateIndex(X34Index index) throws IOException, ValidationException {
        return false;
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
