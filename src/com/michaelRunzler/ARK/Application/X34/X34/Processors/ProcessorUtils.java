package X34.Processors;

import X34.Core.X34Image;
import X34.Core.X34Index;
import com.sun.istack.internal.NotNull;
import core.CoreUtil.IOTools;

import java.io.IOException;
import java.util.ArrayList;

class ProcessorUtils
{
    /**
     * Cyclically tries getting data from a URL until it succeeds or exceeds the try count (default: 5).
     * @param url the URL to try getting data from
     * @param maxTries the maximum try count before giving up on transfer
     * @return the String representation of the data at the specified URL
     * @throws IOException if the try count is exceeded, this will be the exception thrown on the last try
     */
    static String tryDataTransfer(String url, int maxTries) throws IOException
    {
        int tries = 0;
        IOException thrown;
        do {
            try {
                return IOTools.getStringFromURL(url);
            } catch (IOException e) {
                thrown = e;
                tries ++;
            }
        }while (tries < maxTries);
        throw thrown;
    }

    /**
     * Checks the provided list of {@link X34Image images} against the provided {@link X34Index index}, and returns a list
     * of images that do not exist in the index, if there are any. Comparison is done by checking the hash of each image
     * against the hashes of the entries in the index. If no match is found, the image is added to both the provided index
     * and the list to be returned.
     * @param index the index to check images against. Images that do not exist in the index will be added to it. If this is
     *              null or contains no images, all provided images will be assumed to be new, and the result will be equal
     *              to the image list provided.
     * @param imageList the list of images to check against the index. If this is null or zero-length, the method will simply
     *                  return an empty array. Null entries in this list will be skipped.
     * @return the subset of images from the input image list that were not present in the provided index, if there were any
     */
    static ArrayList<X34Image> checkIndex(X34Index index, ArrayList<X34Image> imageList)
    {
        if(imageList == null || imageList.size() == 0) return new ArrayList<>();
        if(index == null || index.entries == null || index.entries.size() == 0){
            if(index != null)
                if(index.entries == null) index.entries = new ArrayList<>(imageList);
                else index.entries.addAll(imageList);
            return new ArrayList<>(imageList);
        }

        ArrayList<X34Image> newImages = new ArrayList<>();
        for (X34Image x : imageList)
        {
            // See if any images with an identical hash exist in the index already.
            int hashID = index.getEntryByHash(x.hash);
            if(hashID > -1){
                // If we found an existing image with a hash match, make sure its URL is current.
                X34Image curr = index.entries.get(hashID);
                if(!(curr.source == x.source) && x.hash != null) curr.source = x.source;
            }else{
                // If we didn't find a hash match, mark the image as new.
                index.entries.add(x);
                newImages.add(x);
            }
        }

        return newImages;
    }

    static void handleHttpError(@NotNull IOException e, HttpErrorHandler... handlers)
    {
        // Get code from the provided event and check its range.
        short code = getHttpErrorCode(e);
        if(code == -1 || handlers == null || handlers.length == 0) return;

        // Provided that both the code and handler stack are valid, iterate through and activate any handlers whose codes match.
        for(HttpErrorHandler handler : handlers) {
            if(handler.getCode() == code) handler.handleError();
        }
    }

    /**
     * Extracts the HTTP error code from an {@link IOException} that contains such a code.
     * Will return the 3-digit HTTP error code as a {@code short}, or {@code -1} if none could be located.
     * WIll never return a value larger than {@code 600}, or less than {@code 100} (with the exception of {@code -1} for invalid values).
     * @param e the {@link IOException} to extract the error code from
     * @return the 3-digit HTTP error code contained within the provided exception as a {@code short}, or {@code -1} if no valid code was found
     */
    static short getHttpErrorCode(@NotNull IOException e)
    {
        if(e.getMessage() != null && e.getMessage().contains("Server returned HTTP response code: ")){
            try{
                short retV = Short.parseShort(IOTools.getFieldFromData(e.getMessage(), "code: ", " for URL", 0));
                if(retV > 600 || retV < 100) return -1;
                else return retV;
            }catch (NumberFormatException e1){
                return -1;
            }
        }else return -1;
    }
}

/**
 * Handles an HTTP error code using the provided {@link HttpErrorActionEvent} handler.
 * Primarily useful as a container-type object.
 */
class HttpErrorHandler
{
    private short code;
    private HttpErrorActionEvent e;

    /**
     * Constructs a new instance of this object.
     * @param code the 3-digit HTTP error code to indicate which type of error should be handled by this object.
     * @param event the {@link HttpErrorActionEvent} handler to activate when this object's {@link #handleError()} or {@link #handleError(short)}
     *              method is called.
     */
    HttpErrorHandler(short code, HttpErrorActionEvent event){
        this.code = code;
        this.e = event;
    }

    /**
     * Handles an HTTP error code. Checks the provided code against this object's stored code, and returns without taking
     * action if they do not match.
     * @param code the HTTP error code to check against this object's internal code
     */
    void handleError(short code){
        if(code == this.code) this.handleError();
    }

    /**
     * Triggers this object's internal handler object.
     * It is up to the calling class to determine when to call this method - no internal checks are done prior to calling
     * the contained handler's {@link HttpErrorActionEvent#action(short)} method.
     */
    void handleError(){
        this.e.action(code);
    }

    /**
     * Gets this object's stored HTTP error code.
     * @return a short representing this object's contained HTTP error code
     */
    short getCode() {
        return code;
    }
}

/**
 * A generic handler-type abstract class designed to handle HTTP errors delegated to it by the {@link HttpErrorHandler} class.
 * Takes a single argument to its method, a short representing the HTTP code that it is being called to handle.
 */
abstract class HttpErrorActionEvent{
    public abstract void action(short code);
}
