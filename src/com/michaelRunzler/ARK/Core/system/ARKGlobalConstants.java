package system;

import java.io.File;

public class ARKGlobalConstants
{
    public static File DESKTOP_CACHE_ROOT = new File(System.getenv("AppData") + "\\ARK\\Cache");
    public static File DESKTOP_DATA_ROOT = new File(System.getenv("AppData") + "\\ARK\\Persistent");

}
