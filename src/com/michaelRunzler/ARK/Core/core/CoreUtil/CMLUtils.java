package core.CoreUtil;

/**
 * Provides common utilities for command-line mode applications.
 */
public class CMLUtils
{
    /**
     * Gets a command-line argument value from a provided argument array.
     * @param args the array to retrieve the argument from
     * @param key the argument name to search for
     * @param separator the separator between the argument name and its value, usually {@code '='}.
     * @return the value of the searched-for argument, or null if none was found
     */
    public static String getArgument(String[] args, String key, char separator)
    {
        int index = ARKArrayUtil.containsString(args, key);
        if(index < 0) return null;

        String str = args[index];

        if(str.indexOf(separator) + 1 == str.length()) return "";

        return str.substring(str.indexOf(separator) + 1, str.length());
    }

    /**
     * Gets a command-line argument value from a provided argument array.
     * Equivalent to calling {@link CMLUtils#getArgument(String[], String, char)} with {@code '='} as the separator character.
     * @param args the array to retrieve the argument from
     * @param key the argument name to search for
     * @return the value of the searched-for argument, or null if none was found
     */
    public static String getArgument(String[] args, String key) {
        return getArgument(args, key, '=');
    }
}
