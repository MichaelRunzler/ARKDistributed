package core.CoreUtil;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Provides tools for retrieving and manipulating remote resources from a server.
 * Includes utilities for loading and managing database files.
 */
public class IOTools
{
    /**
     * Downloads a raw data file from a URL.
     * @param src the URL to load the file from. If it is a file, the file itself will be downloaded.
     *            If not, the page HTML or XML tree will be downloaded instead
     * @param dest the location to write the downloaded file
     * @param overwrite whether or not to overwrite an existing file in the specified download location
     * @throws IOException if there is a problem with the download or writing process, or if it takes more than 5 seconds
     * to open the file read channel
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

        // Must be final one-element arrays for cross-thread access
        final IOException[] thrown = new IOException[1];
        final ReadableByteChannel[] rbc = new ReadableByteChannel[1];

        // Dispatch retrieval operations to a thread to allow for timeout support
        Thread t = new Thread(() -> {
            try {
                URL url = new URL(src);
                rbc[0] = Channels.newChannel(url.openStream());
            }catch (IOException e){
                thrown[0] = e;
            }
        });

        t.start();

        // Default is 10 seconds (10,000 ms)
        final long MAX_WAIT_TIME = 5000;
        long time = System.currentTimeMillis();

        // Wait at most MAX_WAIT_TIME milliseconds for the retrieval thread to finish
        try {
            t.join(MAX_WAIT_TIME);
        } catch (InterruptedException ignored) {}

        // Check if the thread actually finished before it joined
        if(System.currentTimeMillis() - time >= MAX_WAIT_TIME){
            // If it didn't (it timed out), throw an exception.
            throw new IOException("Remote file read timed out");
        }else{
            // Throw any exceptions that occurred during execution
            if(thrown[0] != null) throw thrown[0];

            // If no exceptions occurred, begin the file transfer
            FileOutputStream fos = new FileOutputStream(dest);

            fos.getChannel().transferFrom(rbc[0], 0, Integer.MAX_VALUE);
            fos.flush();
            fos.close();
        }
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

    /**
     * Gets the complete list of files and directories (collectively, 'children') in a given directory. This includes the
     * children of all subdirectories. Will run until the entire directory structure has been inventoried, or an error has occurred.
     * Note that the nature of this method may result in excessively long processing times on directories with complex substructures,
     * such as OS directories or caches. The tool will track its own recursion, and will stop searching sublevels when it
     * has recurred 255 times to avoid infinite recursion scenarios (such as subdirectories which are symlinks to parent
     * directories). This method returns noncritical status in the last file in the returned array, which corresponds to an exception
     * (critical errors will be thrown as normal exceptions instead. Critical errors are problems which prevent further progress).
     * This exception can be recovered by passing the File object through the recoverFileTreeErrorState() method, which will
     * throw the original exception.
     * @param src the directory to use as the root for the search
     * @return an array of File objects corresponding to the children of the searched directory. The last entry in the file
     * list will correspond to the noncritical error status of the search process, and will be null if no errors occurred.
     * @throws IOException if an unrecoverable error is encountered while searching the directory
     */
    public static File[] getFileTree(File src) throws IOException
    {
        // Check access, existence, and directory validity.
        if(src == null || !src.exists() || !src.isDirectory() || !src.canRead()) throw new IllegalArgumentException("Supplied file must be a valid directory");

        ArrayList<File> list = new ArrayList<>();

        // Stores the current directory level. The higher this number is, the deeper we are in the directory tree.
        // If this value exceeds 255, we assume that we have run into an infinite recursion problem, and terminate the
        // loop, marking the error as such.
        int cycle = 0;
        // Stores the current list of files at (level -1)
        ArrayList<File> currentList = new ArrayList<>();
        // Stores the current list of searchable directories at (level -1).
        ArrayList<File> currentDirList = new ArrayList<>();
        // Start the cycle by adding the source directory to the search list.
        currentDirList.add(src);

        // Run the cycle, checking each directory level for subdirectories, and searching them if present.
        while(cycle < 256)
        {
            // Search the current list of directories.
            for(File f : currentDirList) {
                File[] curr = f.listFiles();
                // If there were any files under that directory, add them to the list.
                if(curr != null && curr.length > 0) currentList.addAll(Arrays.asList(curr));
            }

            // Reset the directory list cache to prepare for refilling.
            currentDirList.clear();

            // If the queried list is empty, we're done.
            if(currentList.size() == 0) break;

            // Add all subdirectories to the directory cache, and all files and directories to the main list.
            for(File f : currentList){
                if(f.isDirectory()) currentDirList.add(f);
                // We WOULD normally check to see if the file already exists in the list, and avoid adding it (raising a flag
                // in the process). However, this is EXTREMELY resource-intensive for larger file lists (> 10,000 entries).
                list.add(f);
            }

            // If there were no directories in the list, we're done, since the files have already been tracked.
            if(currentDirList.size() == 0) break;

            // Reset the file list cache for the next run.
            currentList.clear();
            cycle ++;
        }

        // Add noncritical error condition information to the end of the list.
        if(cycle >= 256){
            list.add(new File("invalid://><%Warning while parsing: possible infinite recursion detected."));
        }else{
            list.add(null);
        }

        File[] output = new File[1];
        return list.toArray(output);
    }

    /**
     * Recovers an error report from the result of a call to the getFileTree() method.
     * Will throw the relevant exception if there is one found. If no valid exception is found, or the input is null,
     * no action will be taken.
     * @param f the 'exception report' to check
     * @throws Exception if the report is valid, this will be the original exception from the method call
     */
    public static void recoverFileTreeErrorState(@Nullable File f) throws Exception
    {
        final String IDENTIFIER = "invalid://";
        final String DELIMITER = "><%";

        if(f == null || !f.getAbsolutePath().startsWith(IDENTIFIER)) return;

        String ex = f.getAbsolutePath();
        ex = ex.substring(ex.indexOf(DELIMITER) + DELIMITER.length(), ex.length());

        throw new Exception(ex);
    }
}