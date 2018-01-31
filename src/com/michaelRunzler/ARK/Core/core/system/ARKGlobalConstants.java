package core.system;

import java.io.File;

public class ARKGlobalConstants
{
    /**
     * Lists a number of possible OS archetypes that may be used by this class to categorize specific constant values by platform.
     */
    public enum OS {
        WINDOWS,MAC,LINUX,ANDROID,IOS,UNIX,OTHER
    }

    public static File DESKTOP_CACHE_ROOT = new File(System.getenv("AppData") + "\\ARK\\Cache");
    public static File DESKTOP_DATA_ROOT = new File(System.getenv("AppData") + "\\ARK\\Persistent");

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
}
