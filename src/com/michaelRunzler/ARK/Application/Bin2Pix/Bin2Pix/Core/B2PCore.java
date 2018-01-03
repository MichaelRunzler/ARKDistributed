package Bin2Pix.Core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Actually converts shit from binary to image data.
 */
public class B2PCore
{
    // 300 MB in bytes
    public static final long MAX_DATA_LENGTH = 314572800L;

    public static byte[] B2PFromFile(File src, EncodingSchema method) throws IOException, ConversionException
    {
        if(src == null || !src.exists() || !src.canRead()){
            throw new IOException("Target file is invalid or nonexistent");
        }else if(src.length() >= (long)Integer.MAX_VALUE){
            throw new IllegalArgumentException("Target file is too large (> 2.14 GB)");
        }

        FileInputStream is = new FileInputStream(src);
        byte[] buffer = new byte[(int)src.length()];
        is.read(buffer);
        is.close();

        return B2P(buffer, method);
    }

    public static byte[] B2P(byte[] data, EncodingSchema method) throws ConversionException
    {
        if(method == null || method.adapter == null || method.length <= 0 || (double)method.hRatio / (double)method.vRatio <= 0.0){
            throw new IllegalArgumentException("Provided schema is invalid");
        }else if(data == null || data.length <= method.length){
            throw new IllegalArgumentException("Provided data array is invalid");
        }else if(data.length > MAX_DATA_LENGTH){
            throw new IllegalArgumentException("Input data is larger than allowed length");
        }
        
        int perPixelBytes = (method.length * 3) + method.MSR;
        
        Pixel[] pixels = new Pixel[data.length / perPixelBytes];

        if(data.length % perPixelBytes != 0){
            byte[] temp = new byte[data.length];
            System.arraycopy(data, 0, temp, 0, data.length);
            data = new byte[temp.length - (temp.length % perPixelBytes)];
            System.arraycopy(temp, 0, data, 0, data.length);
        }

        int i = 0;
        for(int j = 0; j < data.length; j += perPixelBytes)
        {
            int r = 0;
            for(int k = j; k < j + method.length; k++){
                r += (data[k] + 128);
            }
            r = (r / method.length) - 128;

            int g = 0;
            for(int k = j + method.length; k < j + method.length * 2; k++){
                g += (data[k] + 128);
            }
            g = (g / method.length) - 128;

            int b = 0;
            for(int k = j + (method.length * 2); k < j + method.length * 3; k++){
                b += (data[k] + 128);
            }
            b = (b / method.length) - 128;
            
            pixels[i] = new Pixel((byte)r, (byte)g, (byte)b, (byte)0xFF);
            i++;
        }

        return method.adapter.convert(pixels, method);
    }

    public static void B2PFile2File(File src, File dest, EncodingSchema method) throws IOException, ConversionException
    {
        if(dest == null || dest.exists()){
            throw new IllegalArgumentException("Destination file is null or invalid");
        }

        byte[] data = B2PFromFile(src, method);

        if(!dest.getParentFile().exists()){
            dest.getParentFile().mkdirs();
        }
        dest.createNewFile();

        FileOutputStream fos = new FileOutputStream(dest);

        fos.write(data);
        fos.flush();
        fos.close();
    }
}

