package Bin2Pix.Core;

import Bin2Pix.Adapters.PNGAdapter;

/**
 * Stores encoding scheme data for converting hex data to pixel data.
 */
public class EncodingSchema
{
    public static final EncodingSchema DEFAULT_SCHEMA = new EncodingSchema(0, 1, 1, 1, new PNGAdapter());

    public int MSR; // markspace ratio, the space between byte clusters used for pixels
    public int length; // pickup length, the number of bytes to be used for each value in the pixel
    public int hRatio; // horizontal ratio component of the aspect ratio, for example 16
    public int vRatio; // vertical ratio component of the aspect ratio, for example 9
    public ImageAdapter adapter; // the file format adapter to use for the conversion

    public EncodingSchema(int MSR, int length, int hRatio, int vRatio, ImageAdapter adapter)
    {
        this.MSR = MSR;
        this.length = length;
        this.hRatio = hRatio;
        this.vRatio = vRatio;
        this.adapter = adapter;
    }
}
