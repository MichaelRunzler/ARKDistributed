package Bin2Pix;

import Bin2Pix.Adapters.PNGAdapter;
import Bin2Pix.Core.B2PCore;
import Bin2Pix.Core.ConversionException;
import Bin2Pix.Core.EncodingSchema;

import java.io.File;
import java.io.IOException;

public class B2P
{
    public static void main(String[] args)
    {
         if(args.length < 1){
             System.out.println("No arguments");
             System.exit(1);
         }

         File src = new File(args[0]);

        try {
            File dest = new File(args.length < 2 ? System.getProperty("user.home") + "\\Desktop\\image.png" : args[1]);
            B2PCore.B2PFile2File(src, dest, new EncodingSchema(0, 1, 16, 9, new PNGAdapter()));
        } catch (IOException | ConversionException e) {
            e.printStackTrace();
        }
    }

    private static void test()
    {
        File src = new File("D:\\Libraries\\Downloads", "nepgear_splash_by_weresdrim-dbt4bpl.bmp");
        try {
            File dest = new File("D:\\Libraries\\Downloads", "image.png");
            B2PCore.B2PFile2File(src, dest, new EncodingSchema(0, 1, 16, 9, new PNGAdapter()));
        } catch (IOException | ConversionException e) {
            e.printStackTrace();
        }
    }
}
