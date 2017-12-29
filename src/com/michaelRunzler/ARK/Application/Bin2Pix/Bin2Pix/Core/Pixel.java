package Bin2Pix.Core;

/**
 * Stores pixel value data.
 */
public class Pixel
{
    public byte r;
    public byte g;
    public byte b;
    public byte a;

    public Pixel(byte r, byte g, byte b, byte a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
