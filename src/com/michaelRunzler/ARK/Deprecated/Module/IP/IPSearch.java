package com.michaelRunzler.ARK.Deprecated.Module.IP;

import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.IOTools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Searches for responding IPs on the specified IP domain.
 * Designed to be called from a command-line interface. Arguments are specified with the format "argumentName=value".
 * An example valid command line call is shown here:
 * java -jar IPSearch.jar IP=10.3.*.* count=4 timeout=4000 delay=100 threads=8
 *
 * This will search all IPs on the 10.3 block (default subnet) with a ping count of 2 per IP, a maximum wait time of
 * 1 second, and a delay between IP tests of 100 milliseconds.
 * The command list for this application is as follows:
 *
 * Argument 0: [String: IP] The IP block to scan. Asterisks may be used as wildcards. For example, to scan all IPs on the
 * 192.168 block, you would use: IP=192.168.*.*
 * Individual IPs may be specified as well, or *.*.*.* may be used to scan ALL IPs from 1.0.0.1 to 254.254.254.254.
 * (WARNING: this will take A LONG TIME - up to a few hours, as well as a lot of RAM.)
 * NOTE: Wildcards must be used to represent an entire segment, as opposed to a single digit. For example, you CANNOT use
 * the address '10.5*.0.1', but you CAN use '10.*.0.1'.
 * Argument 1 (optional): [int: count default 2] The number of tests to conduct on each IP address in the specified block.
 * Argument 2 (optional): [int: timeout default 1000] The maximum time to wait for a reply from each tested IP.
 * Argument 3 (optional): [int: delay default 0] The delay between testing each IP. 0 is no delay, -1 will wait for input
 * before proceeding to the next IP.
 * Argument 4 (optional): [int: threads default 4] The amount of concurrent threads to use while testing. Please note that
 * setting this to a larger number than your processor's number of cores may result in significantly increased RAM usage
 * with no significant performance improvement.
 * Argument 5 (optional): [File: log default %UserData%\Desktop\IPLog.txt] If present, specifies a location to log test
 * results to. The location should be a directory, under which a file called IPLog.txt will be written. If a file by that
 * name already exists, it will be deleted.
 */
public class IPSearch
{
    private static ArrayList<String> arguments;
    private static BufferedWriter logWriter;
    private static int totalIPs = 0;
    private static long benchmarkTimeStorage = 0;
    private static IPSearchThreadExecutor executor;

    private static long[] pingTimeList;
    private static long[] processTimeList;

    public static void main(String[] args)
    {
        String block = "";
        int testCount;
        int timeout;
        int delay;
        int threadCount;
        File logTarget = null;

        System.out.println("Starting...");
        benchmarkTime("", "");

        //test for no-arg scenario
        if(args == null || args.length == 0){
            System.out.println("No arguments!");
            System.exit(10);
        }

        arguments = new ArrayList<>();
        Collections.addAll(arguments, args);

        //check for the IP argument
        if(iterateArgumentListContentWildCard("com/michaelRunzler/ARK/Deprecated/Module/IP") < 0){
            System.out.println("Missing argument: IP block");
            System.exit(11);
        }else{
            block = getArgument("com/michaelRunzler/ARK/Deprecated/Module/IP");
        }

        if(iterateArgumentListContentWildCard("log") >= 0){
            logTarget = getArgument("log").equals("log")
                    ? new File(System.getProperty("user.home") + "\\Desktop", "IPLog.txt")
                    : new File(getArgument("log"), "IPLog.txt");

            if(!logTarget.exists()){
                logTarget.getParentFile().mkdirs();
            }
            try {
                logWriter = new BufferedWriter(new FileWriter(logTarget));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //get optional arguments, set to default if not present
        //one-line try-catches FTW
        try{testCount = Integer.parseInt(getArgument("count"));}catch (NumberFormatException e){testCount = 2;}

        try{timeout = Integer.parseInt(getArgument("timeout"));}catch (NumberFormatException e){timeout = 1000;}

        try{delay = Integer.parseInt(getArgument("delay"));}catch (NumberFormatException e){delay = 0;}

        try{threadCount = Integer.parseInt(getArgument("threads"));}catch (NumberFormatException e){threadCount = 4;}

        executor = new IPSearchThreadExecutor(threadCount);

        benchmarkTime("Init took ", " ms");

        //validate IP
        System.out.println("Validating IP block...");
        if(!validateIPAddress(block)){
            System.out.println("Supplied IP block is not valid.");
            System.exit(12);
        }else{
            benchmarkTime("Block is valid. Validation took ", " ms");
        }

        System.out.println("Beginning IP test.");

        //test IPs
        ArrayList<String> result = null;
        try {
            result = testIPBlock(block, testCount, timeout, delay);
        } catch (IOException e) {
            System.out.println("Error while testing address in block!");
            System.out.println(e.getMessage() == null ? e.toString() : e.getMessage());
        }

        System.out.println("");
        System.out.println("Test completed.");
        benchmarkTime("Testing took ", " ms");
        System.out.println("");

        if(logTarget != null) {
            try {
                logTarget.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        long avgPingTime;
        long tempT = 0;
        for(long l : pingTimeList){
            tempT += l;
        }

        avgPingTime = (tempT / pingTimeList.length);

        long avgProcTime;
        long tempP = 0;
        for(long l : processTimeList){
            tempP += l;
        }

        avgProcTime = (tempP / processTimeList.length);

        //report results
        if(result != null)
        {
            logResult("Test time (UET+): " + System.currentTimeMillis());
            logResult("Test results are as follows:");
            logResult("");
            logResult("Tested IP block: " + block);
            logResult("Total number of tested IPs in block: " + totalIPs);
            logResult("Average process time: " + avgProcTime);
            logResult("Average ping time: " + avgPingTime);
            logResult("Number of responding IPs: " + result.size());
            if(result.size() > 0) {
                logResult("The following is a list of all IP addresses that responded to the test:");
                logResult("");
                for (String s : result) {
                    logResult(s);
                }
            }
            logResult("");
            logResult("End of result log.");
        }else{
            logResult("No results to list!");
        }

        System.out.println("Testing complete.");
        if(logWriter != null) {
            try {
                logWriter.flush();
                logWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    /**
     * Tests a sequence of IPs on a specified IP block.
     * @param block a String representing a validated IP block
     * @param count the number of ICMP packets to send to each IP on the block
     * @param timeout the time in milliseconds to wait for a response from each tested IP
     * @param delay the delay between testing each IP block
     * @return an ArrayList containing the IPs on the block that responded to the test, or an empty ArrayList if none responded
     */
    private static ArrayList<String> testIPBlock(String block, int count, int timeout, int delay) throws IOException
    {
        ArrayList<String> results = new ArrayList<>();
        String[] SrcSegments = getAddressSegmentsFromBlock(block);
        int[] segments = new int[4];
        boolean noWC = true;

        pingTimeList = new long[255];
        processTimeList = new long[255];

        int limit1;
        int limit2;
        int limit3;
        int limit4;

        if(SrcSegments[0].equals("*") || SrcSegments[0].equals("0")){
            segments[0] = 1;
            limit1 = 254;
            noWC = false;
        }else{
            segments[0] = Integer.parseInt(SrcSegments[0]);
            limit1 = segments[0];
        }
        if(SrcSegments[1].equals("*")){
            segments[1] = 0;
            limit2 = 254;
            noWC = false;
        }else{
            segments[1] = Integer.parseInt(SrcSegments[1]);
            limit2 = segments[1];
        }
        if(SrcSegments[2].equals("*")){
            segments[2] = 0;
            limit3 = 254;
            noWC = false;
        }else{
            segments[2] = Integer.parseInt(SrcSegments[2]);
            limit3 = segments[2];
        }
        if(SrcSegments[3].equals("*") || SrcSegments[3].equals("0")){
            segments[3] = 1;
            limit4 = 254;
            noWC = false;
        }else{
            segments[3] = Integer.parseInt(SrcSegments[3]);
            limit4 = segments[3];
        }

        //todo multithread
        if(noWC){
            if(IOTools.pingTestURL(block) >= 0){
                results.add(block);
                return results;
            }
        }else{
            for(int i = segments[0]; i <= limit1; i++){
                for(int j = segments[1]; j <= limit2; j++){
                    for(int k = segments[2]; k <= limit3; k++){
                        for(int l = segments[3]; l <= limit4; l++)
                        {
                            String IP = i + "." + j + "." + k + "." + l;
                            executor.addThreadToStack(() -> {
                                long startTime = System.currentTimeMillis();
                                totalIPs ++;
                                System.out.print("Testing IP " + IP + "... ");
                                for(int m = 0; m < count; m++)
                                {
                                    // Commented to remove reliance on library classes
                                    /*
                                    long startPTime = System.currentTimeMillis();
                                    try {
                                        IcmpPingRequest rq = IcmpPingUtil.createIcmpPingRequest();
                                        rq.setHost(IP);
                                        rq.setTimeout(timeout);
                                        IcmpPingResponse response = IcmpPingUtil.executePingRequest(rq);
                                        if(response.getSuccessFlag()) {
                                            System.out.print("com/michaelRunzler/ARK/Deprecated/Module/IP " + IP + " responded.\n");
                                            results.add(IP);
                                            break;
                                        }else{
                                            System.out.print("com/michaelRunzler/ARK/Deprecated/Module/IP " + IP + " did not respond.\n");
                                        }
                                    }catch(RuntimeException ignored){}
                                    pingTimeList[totalIPs - 1] = System.currentTimeMillis() - startPTime;
                                    */
                                }
                                if(delay > 0) try{System.out.println("Waiting " + delay + "ms...");Thread.sleep(delay);}catch(Exception ignored){}
                                processTimeList[totalIPs - 1] = System.currentTimeMillis() - startTime;
                            });
                        }
                    }
                }
            }
            executor.startStackExecution();
            // Wait for completion of stack execution before returning.
            executor.waitForStackCompletion(100);
        }
        return results;
    }

    /**
     * Gets the content for a specified argument tag in the argument list.
     * @param tag the argument tag to search for, ex. 'repo='
     * @return the content of the searched tag, or null if the tag was not found in the argument list
     */
    private static String getArgument(String tag)
    {
        if(iterateArgumentListContentWildCard(tag) > -1){
            String str = arguments.get(iterateArgumentListContentWildCard(tag));
            try {
                return str.substring(str.indexOf('=') + 1, str.length());
            }catch (StringIndexOutOfBoundsException e){
                return null;
            }
        }
        return null;
    }

    /**
     * Iterates through the contents of the internal argument list, searching for an entry that contains the given string.
     * @param content a String to search for in the argument list. Note that any given array entry only has to <u>contain</u> the value,
     *                not equal it.
     * @return the index of the array cell containing the given string
     */
    private static int iterateArgumentListContentWildCard(String content)
    {
        for(int i = 0; i < arguments.size(); i++){
            String str = arguments.get(i);

            if(str.contains(content)){
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks that a given String is a valid representation of an IPv4 address in dot-decimal notation.
     * Addresses may be complete, or they may be a wildcarded version with asterisks representing wildcard address segments.
     * Wildcarded addresses must still be complete (4 address segments separated with decimal points), or they will fail
     * the validation check. For example, the strings '192.*.*.10' and '*.*.*.*' will pass, while strings '192.*.10' and
     * '*.*' will fail.
     * @param IP a candidate String to be validated
     * @return true if the address is valid, false if otherwise
     */
    public static boolean validateIPAddress(String IP)
    {
        if(IP == null || IP.length() > 15 || IP.length() < 7 || !IP.contains(".")){
            return false;
        }

        try {
            String seg1 = IP.substring(0, IP.indexOf('.'));
            IP = IP.substring(IP.indexOf('.') + 1, IP.length());

            String seg2 = IP.substring(0, IP.indexOf('.'));
            IP = IP.substring(IP.indexOf('.') + 1, IP.length());

            String seg3 = IP.substring(0, IP.indexOf('.'));
            IP = IP.substring(IP.indexOf('.') + 1, IP.length());

            return validateIPAddressSegment(seg1) && validateIPAddressSegment(seg2) &&
                    validateIPAddressSegment(seg3) && validateIPAddressSegment(IP);
        }catch (NullPointerException | StringIndexOutOfBoundsException e){
            return false;
        }
    }

    /**
     * Checks whether a String is a valid segment in an IP address. Valid String values are:
     * - An integer value between 0 and 254 (inclusive)
     * - A single asterisk
     * @param segment the String to check for validity
     * @return true if the segment is valid, false if otherwise
     */
    private static boolean validateIPAddressSegment(String segment)
    {
        if(segment != null && segment.length() > 0 && segment.length() <= 3)
        {
            if (segment.equals("*")) {
                return true;
            } else {
                try {
                    int value1 = Integer.parseInt(segment);
                    return value1 < 255 && value1 >= 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }else{
            return false;
        }
    }

    /**
     * Splits a String that represents a valid IP block into its four address segments.
     * See the @see {@link #validateIPAddress(String) IP Validation} and {@link #validateIPAddressSegment(String) IP Segment Validation} methods for more information about IP validation.
     * @param block a String representing a valid IP address block
     * @return a String array containing all four segments of the IP address, or null if the input address is not valid
     */
    public static String[] getAddressSegmentsFromBlock(String block)
    {
        String[] segments = new String[4];

        try {
            segments[0] = block.substring(0, block.indexOf('.'));
            block = block.substring(block.indexOf('.') + 1, block.length());

            segments[1] = block.substring(0, block.indexOf('.'));
            block = block.substring(block.indexOf('.') + 1, block.length());

            segments[2] = block.substring(0, block.indexOf('.'));
            block = block.substring(block.indexOf('.') + 1, block.length());

            segments[3] = block;
        }catch(NullPointerException | StringIndexOutOfBoundsException e){
            return null;
        }
        return segments;
    }

    /**
     * Logs a String to both the console log and the previously set log file.
     * If there was a problem creating the log file, or logging is disabled, this will only lgo to the console window.
     * @param data the String to log
     */
    private static void logResult(String data)
    {
        System.out.println(data);
        if(logWriter != null) {
            try {
                logWriter.write(data);
                logWriter.newLine();
                logWriter.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage() == null ? e.toString() : e.getMessage());
                logWriter = null;
            }
        }
    }

    /**
     * Tests a validated IP block.
     * @param IP the IP to do a ping test on
     * @return the results of the ping test
     */
    @Deprecated
    public static int pingTestIP(String IP, int timeout, int count) throws IOException, IllegalArgumentException
    {
        if(IP == null || IP.length() <= 0) {
            throw new IllegalArgumentException("Input URL must not be null or zero-length!");
        }

        ProcessBuilder processBuilder = new ProcessBuilder("ping", "-n", "" + count, "-w", "" + timeout, IP);
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
        if(output.contains("time=")) {
            String temp = output.substring(output.indexOf("time=") + 5, output.indexOf("TTL=") - 3);
            return temp.contains("<") ? 1 : Integer.parseInt(temp);
        } else return -1;
    }

    /**
     * Gives the time since this method was last called in milliseconds and prints to the console.
     * Messages can be provided to prepend and append to the time report.
     * Provide the literal ""  for both arguments to simply print the time.
     * @param prepend the message to prepend to the time report
     * @param append the message to append to the time report
     */
    private static void benchmarkTime(String prepend, String append)
    {
        long temp = System.currentTimeMillis();
        if(benchmarkTimeStorage > 0){
            System.out.println(prepend + (temp - benchmarkTimeStorage) + append);
        }
        benchmarkTimeStorage = temp;
    }
}

