package X34.Processors;

import java.net.URL;

/**
 * Contains noncritical metadata information for a {@link X34RetrievalProcessor} class.
 */
public abstract class ProcessorMetadataPacket
{
    /**
     * @return the list of supported repository sites for this processor
     */
    public abstract URL[] getSupportedSites();

    /**
     * @return a brief description of this processor
     */
    public abstract String getDescription();

    /**
     * @return the user(s) that created this processor plugin
     */
    public abstract String getAuthor();

    /**
     * @return a URL that links to the support or home page for this processor
     */
    public abstract URL getSupportURL();

    /**
     * @return this processor's copyright and attribution information
     */
    public abstract String getCopyrightInfo();
}
