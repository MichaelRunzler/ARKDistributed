package Bin2Pix.Adapters;

import Bin2Pix.Core.ConversionException;
import Bin2Pix.Core.EncodingSchema;
import Bin2Pix.Core.ImageAdapter;
import Bin2Pix.Core.Pixel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PNGAdapter implements ImageAdapter
{
    @Override
    public byte[] convert(Pixel[] data, EncodingSchema method) throws ConversionException
    {
        int resX = (int)Math.sqrt(data.length * ((double)method.hRatio / (double)method.vRatio));
        int resY = (int)Math.sqrt(data.length * ((double)method.vRatio / (double)method.hRatio));

        BufferedImage image = new BufferedImage(resX, resY, BufferedImage.TYPE_3BYTE_BGR);

        for(int y = 0; y < resY; y++) {
            for(int x = 0; x < resX; x++)
            {
                Pixel pix = data[(y * resX) + x];
                image.setRGB(x, y, new Color(pix.r + 128, pix.g + 128, pix.b + 128).getRGB());
            }
        }

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bs);
        } catch (IOException e) {
            throw new ConversionException("IO exception while computing output stream: " + (e.getMessage() == null ? "null" : e.getMessage()));
        }

        return bs.toByteArray();
    }

    @Override
    public String[] getHandledExtensions() {
        return new String[]{"*.png"};
    }
}
