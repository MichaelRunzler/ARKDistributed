package Bin2Pix.Core;

/**
 * Classes that implement this interface must be able to output a valid bytestream for their specified format.
 */
public interface ImageAdapter{
    byte[] convert(Pixel[] data, EncodingSchema method) throws ConversionException;
    String[] getHandledExtensions();
}
