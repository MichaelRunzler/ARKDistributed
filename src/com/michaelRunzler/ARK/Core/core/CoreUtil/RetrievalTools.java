package core.CoreUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

/**
 * Provides tools for retrieving and manipulating remote resources from a server.
 * Includes utilities for loading and managing database files.
 */
public class RetrievalTools
{
    /**
     * Downloads a raw data file from a URL.
     * @param src the URL to load the file from. If it is a file, the file itself will be downloaded.
     *            If not, the page HTML or XML tree will be downloaded instead
     * @param dest the location to write the downloaded file
     * @param overwrite whether or not to overwrite an existing file in the specified download location
     * @throws IOException if there is a problem with the download or writing process
     * @throws IllegalArgumentException if the source URL or destination file are invalid or inaccessible
     */
    public static void getFileFromURL(String src, File dest, boolean overwrite) throws IOException, IllegalArgumentException
    {
        if(src == null || src.length() <= 0) {
            throw new IllegalArgumentException("Input URL must not be null or zero-length!");
        }

        if(dest != null) {
            if(!dest.exists()){
                dest.getParentFile().mkdirs();
                dest.createNewFile();
            }else{
                if(overwrite){
                    dest.delete();
                    dest.createNewFile();
                }
            }
        }else{
            throw new IllegalArgumentException("Output file must not be null!");
        }

        URL url = new URL(src);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(dest);

        fos.getChannel().transferFrom(rbc, 0, Integer.MAX_VALUE);
        fos.flush();
        fos.close();
    }

    /**
     * gotta do a ping test, y'know
     * @param src the URL to do a ping test on
     * @return the results of the ping test
     * whoa whoa whoa, what do you mean, ping test?
     */
    public static int pingTestURL(String src) throws IOException, IllegalArgumentException
    {
        if(src == null || src.length() <= 0) {
            throw new IllegalArgumentException("Input URL must not be null or zero-length!");
        }

        if(src.contains("http://") || src.contains("https://")){
            src = src.substring(src.indexOf("://") + 3, src.length());
        }

        if(src.charAt(src.length() - 1) == '/'){
            src = src.substring(0, src.length() - 1);
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder processBuilder = new ProcessBuilder("ping", isWindows? "-n" : "-c", "1", src);
        processBuilder.redirectErrorStream(true);

        Process proc = processBuilder.start();
        InputStream is = proc.getInputStream();

        try {
             proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] b = new byte[is.available()];
        is.read(b);

        String output = ARKArrayUtil.charArrayToString(ARKArrayUtil.byteToCharArray(b));
        if(output.contains("time="))
            return Integer.parseInt(output.substring(output.indexOf("time=") + 5, output.indexOf("TTL=") - 3));
        else
            return -1;
    }

    /**
     * Simplification method. Downloads a file from a URL using getFileFromURL, then reads the file to a buffer using
     * loadDataBytesFromFile and deletes the file.
     * @param src the URL to download the file from
     * @return the contents of the specified URL as a byte array
     * @throws IOException if there is an error writing the file or reading the URL
     * @throws IllegalArgumentException if the provided URL is zero-length or invalid
     */
    public static byte[] getBytesFromURL(String src) throws IOException, IllegalArgumentException
    {
        File dest = new File(System.getenv("AppData") + "\\KAI\\com.michaelRunzler.ARK\\", "temp.xml");
        getFileFromURL(src, dest, true);
        byte[] buffer = loadDataBytesFromFile(dest);
        dest.delete();
        return buffer;
    }

    /**
     * Loads all raw data from a file and formats it into a Unicode string.
     * @param src the file to load
     * @return the contents of the file as a Unicode string
     * @throws IOException if the file is invalid or too large to load
     * @throws IllegalArgumentException if the file does not exist or is null
     */
    public static String loadDataFromFile(File src) throws IOException, IllegalArgumentException
    {
        if(src == null || !src.exists()){
            throw new IllegalArgumentException("Target file must exist!");
        }else if(src.length() > Integer.MAX_VALUE){
            throw new IOException("File size exceeds maximum String character limit of " + Integer.MAX_VALUE + ".");
        }

        char[] buffer = new char[(int)src.length()];
        FileReader reader = new FileReader(src);
        reader.read(buffer);
        reader.close();
        return new String(buffer);
    }

    /**
     * Loads all raw data from a file and interprets it into a character array.
     * @param src the file to load
     * @return the contents of the file as a character array
     * @throws IOException if the file is invalid or too large to load
     * @throws IllegalArgumentException if the file does not exist or is null
     */
    public static char[] loadDataCharsFromFile(File src) throws IOException, IllegalArgumentException
    {
        if(src == null || !src.exists()){
            throw new IllegalArgumentException("Target file must exist!");
        }else if(src.length() > Integer.MAX_VALUE){
            throw new IOException("File size exceeds maximum array size limit of " + Integer.MAX_VALUE + ".");
        }

        char[] buffer = new char[(int)src.length()];
        FileReader reader = new FileReader(src);
        reader.read(buffer);
        reader.close();
        return buffer;
    }

    /**
     * Loads all raw data from a file as unmodified byte data
     * @param src the file to load
     * @return the contents of the file as an array of bytes
     * @throws IOException if the file is invalid or too large to load
     * @throws IllegalArgumentException if the file does not exist or is null
     */
    public static byte[] loadDataBytesFromFile(File src) throws IOException, IllegalArgumentException
    {
        if(src == null || !src.exists()){
            throw new IllegalArgumentException("Target file must exist!");
        }else if(src.length() > Integer.MAX_VALUE){
            throw new IOException("File size exceeds maximum array size limit of " + Integer.MAX_VALUE + ".");
        }

        byte[] buffer = new byte[(int)src.length()];
        FileInputStream reader = new FileInputStream(src);
        reader.read(buffer);
        reader.close();
        return buffer;
    }

    /**
     * Gets a section of text between two marks in a stream of data.
     * @param data the data to parse
     * @param startMark the starting mark to search for
     * @param endMark the ending mark to search for
     * @param pos the index in the data to begin searching from
     * @return the data between the starting and ending marks, or null if none was found
     * @throws IllegalArgumentException if any arguments are invalid or null
     */
    public static String getFieldFromData(String data, String startMark, String endMark, int pos) throws IllegalArgumentException
    {
        if(data == null || startMark == null || endMark == null){
            throw new IllegalArgumentException("All input data must not be null!");
        }else if(pos < 0 || pos >= data.length()){
            throw new IllegalArgumentException("Starting position must be positive and less that the data's length!");
        }else if(!data.contains(startMark) || !data.contains(endMark)){
            return null;
        }

        int initialIDX = data.indexOf(startMark, pos) + startMark.length();
        int endIDX = data.indexOf(endMark, initialIDX);

        if(endIDX == -1)
            return null;

        return data.substring(initialIDX, endIDX);
    }

    /**
     * Gets a section of text between a mark and the end of a stream of data.
     * @param data the data to parse
     * @param startMark the starting mark to search for
     * @param pos the index in the data to begin searching from
     * @return the data between the starting and ending marks, or null if none was found
     * @throws IllegalArgumentException if any arguments are invalid or null
     */
    public static String getFieldFromData(String data, String startMark, int pos) throws IllegalArgumentException
    {
        if(data == null || startMark == null){
            throw new IllegalArgumentException("All input data must not be null!");
        }else if(pos < 0 || pos >= data.length()){
            throw new IllegalArgumentException("Starting position must be positive and less that the data's length!");
        }else if(!data.contains(startMark)){
            return null;
        }

        int initialIDX = data.indexOf(startMark, pos) + startMark.length();
        int endIDX = data.length();

        if(endIDX == -1)
            return null;

        return data.substring(initialIDX, endIDX);
    }

    /**
     * Gets the result from submitting a POST-mode HTML form stored at a URL.
     * Modified version of a solution by Stack Overflow user 'Eng.Fouad'.
     * @param url the URL containing the form to submit
     * @param params a Map containing the list of paired parameter names and values to be submitted
     * @return the page that results from submitting the form
     * @throws IOException if an IO error is encountered while submitting the form or getting the result
     */
    public static String getDataFromSubmittedHttpForm(String url, final HashMap<String, String> params) throws IOException
    {
        if(url.length() == 0){
            throw new IllegalArgumentException("URL is zero-length!");
        }
        if(params.size() == 0){
            throw new IllegalArgumentException("Parameter list is empty! Use getFileFromURL instead!");
        }

        URL target = new URL(url);
        URLConnection con = target.openConnection();
        con.setDoOutput(true);

        PrintWriter wr = new PrintWriter(con.getOutputStream(), true);

        StringBuilder parameters = new StringBuilder();
        String[] keys;
        keys = params.keySet().toArray(new String[0]);
        for(int i = 0; i < keys.length; i++){
            parameters.append(keys[i]).append('=').append(URLEncoder.encode(params.get(keys[i]), "UTF-8"));
            if(i < keys.length - 1){
                parameters.append("&");
            }
        }

        wr.print(parameters);
        wr.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) result.append(line);
        br.close();

        return result.toString();
    }

    /**
     * Gets the size of a remote URL, be it a file or a page, by sending it an HTTP HEAD request for its content-length attribute.
     * Sourced from the Stack Overflow user 'user1723178'.
     * @param url the URL to check
     * @return the size of the URL's content in bytes
     */
    public static int getRemoteFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }
}
