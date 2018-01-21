package X34.Processors;

import core.CoreUtil.IOTools;

import java.io.IOException;

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
}
