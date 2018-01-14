package X34.Core;

import java.awt.*;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;

public class X34Image implements Serializable
{
    private URL source;
    private String tag;
    public HashMap<String,String> metadata;
    private Image data;

    public X34Image(URL source, String tag, HashMap<String, String> metadata)
    {
        if(source == null) throw new IllegalArgumentException("Source cannot be null.");

        this.source = source;
        this.tag = tag == null ? "" : tag;
        this.metadata = metadata == null ? new HashMap<>() : metadata;
        data = null;
    }

    public URL getSource() {
        return source;
    }

    public String getTag() {
        return tag;
    }

    /**
     * Sets the image data associated with this object.
     * @param data the image data to associate with this object. Null values will be ignored.
     * @return this object to enable chain-calling from the constructor for convenience's sake
     */
    public X34Image setImageData(Image data)
    {
        this.data = data == null ? this.data : data;
        return this;
    }

    public Image getData() {
        return data;
    }
}
