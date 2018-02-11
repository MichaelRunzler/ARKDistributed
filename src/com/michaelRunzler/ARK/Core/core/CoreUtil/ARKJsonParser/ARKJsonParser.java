package core.CoreUtil.ARKJsonParser;

import core.CoreUtil.IOTools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Provides utility methods for loading {@link ARKJsonObject}s from external sources.
 */
public class ARKJsonParser
{
    public static final int INDENT_SPACING_COUNT = 4;
    public static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Parses an {@link ARKJsonObject} from a source text file.
     * @param source the {@link File} to load JSON data from
     * @return the result of parsing data from the source file, or null if the file could not be parsed as JSON (or was empty)
     * @throws IOException if an unrecoverable error was encountered during the file load or parsing process
     */
    public static ARKJsonObject loadFromFile(File source) throws IOException {
        return new ARKJsonObject(formatJSONStr(IOTools.loadDataFromFile(source), INDENT_SPACING_COUNT));
    }

    /**
     * Parses the response data from a GET request to the provided {@link URL} into a {@link ARKJsonObject}.
     * @param source the {@link URL} to retrieve JSON data from
     * @return the result of parsing the server's response data, or null if the data could not be parsed as JSON (or was zero-length)
     * @throws IOException if the server returned an HTTP error code, or if any other unrecoverable error was encountered during the retrieval or parsing process
     */
    public static ARKJsonObject loadFromURL(URL source) throws IOException {
        return loadFromURL(source.toString());
    }

    /**
     * Parses the response data from a GET request to the provided String representation of a URL into a {@link ARKJsonObject}.
     * @param source the String representation of a URL to retrieve JSON data from
     * @return the result of parsing the server's response data, or null if the data could not be parsed as JSON (or was zero-length)
     * @throws IOException if the server returned an HTTP error code, or if any other unrecoverable error was encountered during the retrieval or parsing process
     */
    public static ARKJsonObject loadFromURL(String source) throws IOException {
        return new ARKJsonObject(formatJSONStr(IOTools.getStringFromURL(source), INDENT_SPACING_COUNT));
    }

    /**
     * Formats a raw JSON string to obey standard JSON formatting requirements.
     * Sourced from Stack Overflow user 'Janardhan' with minor modifications.
     * @param json_str the JSON string to format
     * @param indent_width the number of spaces to use as the indent spacing.
     * @return the formatted version of the input JSON string
     */
    public static String formatJSONStr(final String json_str, final int indent_width)
    {
        final char[] chars = json_str.toCharArray();
        final String newline = LINE_SEPARATOR;

        StringBuilder ret = new StringBuilder();
        boolean begin_quotes = false;

        for (int i = 0, indent = 0; i < chars.length; i++)
        {
            char c = chars[i];

            if (c == '\"') {
                ret.append(c);
                begin_quotes = !begin_quotes;
                continue;
            }

            if (!begin_quotes) {
                switch (c) {
                    case '{':
                    case '[':
                        ret.append(c).append(newline).append(String.format("%" + (indent += indent_width) + "s", ""));
                        continue;
                    case '}':
                    case ']':
                        ret.append(newline).append((indent -= indent_width) > 0 ? String.format("%" + indent + "s", "") : "").append(c);
                        continue;
                    case ':':
                        ret.append(c).append(" ");
                        continue;
                    case ',':
                        ret.append(c).append(newline).append(indent > 0 ? String.format("%" + indent + "s", "") : "");
                        continue;
                    default:
                        if (Character.isWhitespace(c)) continue;
                }
            }

            ret.append(c).append(c == '\\' ? "" + chars[++i] : "");
        }

        // Remove backslash-escaped forward-slash characters
        return ret.toString().replace("\\/", "/");
    }

    /**
     * Removes the surrounding quotes from a JSON string value. If the value does not have quotes, no changes will be made.
     * @param value the value to de-quote
     * @return the de-quoted version of the input string, or the unaltered input string if the input was null or did not have quotes
     */
    public static String deQuoteJSONValue(String value)
    {
        if(value == null || value.isEmpty()) return value;

        if(value.charAt(0) == '"') value = value.substring(1, value.length());
        if(value.charAt(value.length() - 1) == '"') value = value.substring(0, value.length() - 1);
        return value;
    }
}