package X34.Processors;

import X34.Core.X34Image;
import X34.Core.X34Index;
import core.CoreUtil.IOTools;

import java.io.IOException;
import java.util.ArrayList;

public class ProcessorUtils
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
}
