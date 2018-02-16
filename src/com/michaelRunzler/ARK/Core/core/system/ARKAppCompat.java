package core.system;

import java.io.File;

/**
 * Provides access to application platform-compatibility utility methods and variables.
 */
public class ARKAppCompat
{
    /**
     * Lists a number of possible OS archetypes that may be used by this class to categorize specific constant values by platform.
     */
    public enum OS {
        WINDOWS,MAC,LINUX,ANDROID,IOS,UNIX,OTHER
    }

    public static final File DESKTOP_CACHE_ROOT = getOSSpecificAppCacheRoot();
    public static final File DESKTOP_DATA_ROOT = getOSSpecificAppPersistRoot();

    public static final String INDEX_FILE_EXTENSION     = ".vcsi";
    public static final String CONFIG_FILE_EXTENSION    = ".vcss";
    public static final String CLASS_FILE_EXTENSION     = ".axc";
    public static final String DATABASE_FILE_EXTENSION  = ".vcsd";
    public static final String LOG_FILE_EXTENSION       = ".vcsl";

    /**
     * Gets the generic type of OS that this JVM is running on from a list of possible choices. Specific architecture is
     * not determined, but some type information is determined, for example, it cannot tell which <i>version</i> of Windows
     * is running on a system, but it can tell the difference between Windows and other Unix-based OSes, such as Solaris.
     * Detection is done by calling {@link System#getProperty(String)} with {@code os.name} as the argument.
     * @return a value from the {@link OS} enum naming the current OS type, or {@code OS.OTHER} if the type could not be determined
     * @see OS for the list of possible return values from this method
     */
    public static OS getOSType()
    {
        String os = System.getProperty("os.name", "Unix/Other").toLowerCase();

        if(os.contains("win")) return OS.WINDOWS;
        else if(os.contains("mac")) return OS.MAC;
        else if(os.contains("linux")) return OS.LINUX;
        else if(os.contains("android")) return OS.ANDROID;
        else if(os.contains("unix") || os.contains("nix") || os.contains("sun")) return OS.UNIX;
        else return OS.OTHER;
    }

    /**
     * Gets the OS-specific equivalent of the ARK persistence directory (located under AppData on Windows) for the current platform.
     * @return a File representing the current OS-specific persistence root
     * @throws RuntimeException if this method is used on Android systems. Android has its own set of method calls through its
     * platform API that should be used to get filesystem data instead.
     */
    public static File getOSSpecificAppPersistRoot()
    {
        OS os = getOSType();
        String s;
        switch (os){
            case WINDOWS:
                s = System.getenv("AppData");
                break;
            case MAC:
                s = System.getProperty("user.home") + "\\Library\\";
                break;
            case LINUX:
                s = System.getProperty("user.home") + "\\.ark\\";
                break;
            case ANDROID:
                throw new RuntimeException("WARNING: Do not use this method to get Android filesystem roots, use the Android API instead.");
            case UNIX:
                s = System.getProperty("user.home") + "\\.ark\\";
                break;
            default:
                s = System.getenv("AppData").equals("null") ? System.getProperty("user.home") : System.getenv("AppData");
                break;
        }

        return new File(s + "\\ARK\\Persistent");
    }

    /**
     * Gets the OS-specific equivalent of the ARK cache directory (located under AppData on Windows) for the current platform.
     * @return a File representing the current OS-specific cache root
     * @throws RuntimeException if this method is used on Android systems. Android has its own set of method calls through its
     * platform API that should be used to get filesystem data instead.
     */
    public static File getOSSpecificAppCacheRoot()
    {
        OS os = getOSType();
        String s;
        switch (os){
            case WINDOWS:
                s = System.getenv("AppData");
                break;
            case MAC:
                s = System.getProperty("user.home") + "\\Library\\";
                break;
            case LINUX:
                s = System.getProperty("user.home") + "\\.ark\\";
                break;
            case ANDROID:
                throw new RuntimeException("WARNING: Do not use this method to get Android filesystem roots, use the Android API instead.");
            case UNIX:
                s = System.getProperty("user.home") + "\\.ark\\";
                break;
            default:
                s = System.getenv("AppData").equals("null") ? System.getProperty("user.home") : System.getenv("AppData");
                break;
        }

        return new File(s + "\\ARK\\Cache");
    }

    /**
     * Gets the OS-specific equivalent of the desktop on Windows for the current platform.
     * @return
     * @throws RuntimeException if this method is used on Android systems. Android has its own set of method calls through its
     * platform API that should be used to get filesystem data instead.
     */
    public static File getOSSpecificDesktopRoot()
    {
        OS os = getOSType();
        switch (os){
            case WINDOWS:
                return new File(System.getProperty("user.home") + "\\Desktop");
            case MAC:
                return new File(System.getProperty("user.home") + "\\Desktop");
            case LINUX:
                return new File(System.getProperty("user.home") + "\\Desktop");
            case ANDROID:
                throw new RuntimeException("WARNING: Do not use this method to get Android filesystem roots, use the Android API instead.");
            case UNIX:
                return new File(System.getProperty("user.home") + "\\");
            default:
                return new File(System.getProperty("user.home"));
        }
    }
}
