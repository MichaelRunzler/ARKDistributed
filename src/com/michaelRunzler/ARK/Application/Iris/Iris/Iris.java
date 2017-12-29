package Iris;

import Iris.util.AP;
import Iris.util.IPV4Address;
import com.sun.istack.internal.NotNull;
import core.CoreUtil.IOTools;
import core.system.ARKTransThreadTransportHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Runs a statistical analysis on the tower network and reports the results.
 * Argument table is as follows:
 *
 * Arg 0: [File: list] The list file to load. This file contains AP IP addresses and other configuration information
 * in a special format detailed below.
 * Arg 1: [null: WTF] Logs extracted statistical data to a file located in %UserHome%\Documents\Iris in addition to the
 * console output.
 *
 * Each AP's configuration information should be listed in the file on a new line, following the format:
 * towername,towerIP,username,password,OStype
 * where OStype is one of the following: 3, 6, 8, 8v2, RADWIN. For example:
 * Telegraph AP-1,127.0.0.1,admin,password,6
 * Each entry of this type should be on a separate line (or followed by a newline character [\n]).
 */
public class Iris
{
    private static File resultFile;
    private static File listFile;
    private static BufferedWriter resultWriter;
    private static ArrayList<String> arguments;

    private static ARKTransThreadTransportHandler handler;
    private static boolean writeToFile;

    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    /**
     * Runs the main program through the JVM.
     * @param args arguments from the JVM console
     */
    public static void main(String[] args)
    {
        ArrayList<AP> APList = new ArrayList<>();
        handler = new ARKTransThreadTransportHandler(null);

        arguments = new ArrayList<>();

        if(args != null){
            if(args.length > 0){
                Collections.addAll(arguments, args);
            }else{
                System.out.println("No arguments!");
                exit();
            }
        }else{
            System.out.println("Argument list is null!");
            exit();
        }

        String list = getArgument("list");
        
        writeToFile = iterateArgumentListContentWildCard("WTF") > -1;

        if(list != null) {
            listFile = new File(list);
        }else{
            System.out.println("Invalid path or filename for list file!");
            System.exit(404);
        }

        if(!listFile.exists()){
            System.out.println("List file does not exist!");
            System.exit(404);
        }

        try {
            String listData = IOTools.loadDataFromFile(listFile);
            listData = listData.replace("\r", "");

            do{
                String sN = listData.substring(0, listData.indexOf(','));
                listData = listData.substring(listData.indexOf(',') + 1, listData.length());
                String sI = listData.substring(0, listData.indexOf(','));
                listData = listData.substring(listData.indexOf(',') + 1, listData.length());
                String sU = listData.substring(0, listData.indexOf(','));
                listData = listData.substring(listData.indexOf(',') + 1, listData.length());
                String sP = listData.substring(0, listData.indexOf(','));
                listData = listData.substring(listData.indexOf(',') + 1, listData.length());
                String sT;
                if(listData.contains("\n")) {
                    sT = listData.substring(0, listData.indexOf('\n'));
                    listData = listData.substring(listData.indexOf('\n') + 1, listData.length());
                }else{
                    sT = listData;
                }

                AP.APType type;
                switch (sT){
                    case "3": type = AP.APType.AirOS3;
                        break;
                    case "6": type = AP.APType.AirOS6;
                        break;
                    case "8": type = AP.APType.AirOS8;
                        break;
                    case "8v2": type = AP.APType.AirOS8v2;
                        break;
                    case "RADWIN" : type = AP.APType.RADWIN;
                        break;
                    default: type = AP.APType.AirOS6;
                }
                APList.add(new AP(sN, IPV4Address.parseAddress(sI), sU, sP, type));
            }while(listData.contains("\n"));
        } catch (IOException e) {
            System.out.println("Error loading list file!");
            System.exit(403);
        } catch (StringIndexOutOfBoundsException e){
            System.out.println("List file is empty or incorrectly formatted!");
            System.exit(400);
        }

        resultFile = new File(System.getProperty("user.home") + "\\Documents\\Iris", "results-"
                + Long.toHexString(System.currentTimeMillis()).toUpperCase() + ".rtf");

        if(writeToFile) {
            try {
                if (!resultFile.exists()) {
                    resultFile.getParentFile().mkdirs();
                } else {
                    resultFile.delete();
                }

                resultFile.createNewFile();
                resultWriter = new BufferedWriter(new FileWriter(resultFile));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Couldn't create the system result file! The program will progress without logging results to file.");
            }
        }

        //todo rewrite
        /*
        for(APStats AP : APs)
        {
            logData("AP detail for " + AP.getName() + ": ");
            logData("IP Address: " + AP.getIP());
            logData("Total Station Count: " + AP.getCPECount());
            logData("");

            for(CPEStats CPE : AP.getCPEs())
            {
                logData("Client name: " + CPE.name);
                logData("IP Address: " + CPE.IP);
                logData("TX Signal: " + CPE.TXSignal + " dBm");
                logData("RX Signal: " + CPE.RXSignal + " dBm");
                logData("Noise: " + CPE.noiseFloor + " dBm");
                logData("Latency: " + CPE.latency + " ms");
                logData("Distance: " + CPE.distance + " miles");
                logData("TX/RX Capacity: " + CPE.TXRXCapacity + " Mb/s");
                logData("CCQ: " + CPE.CCQ + "%");
                logData("Connection Uptime: " + CPE.uptime);
                logData("");
            }

        }
        */
    }

    private static double evaluateRiskAssessment()
    {
        return Double.MAX_VALUE;
    }

    /**
     * Prints the supplied data to the console log, and writes it to the output file if set to do so.
     * @param data the data to write to console output and/or file
     */
    private static void logData(@NotNull String data)
    {
        if(writeToFile && resultWriter != null){
            try {
                resultWriter.write(data + "\n");
                resultWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error writing to log file! No further logs of these ");
            }
        }
        System.out.println(data);
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
            return str.substring(str.indexOf('=') + 1, str.length());
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
     * Performs shutdown tasks and exits the program.
     */
    private static void exit()
    {
        System.out.println("Exiting program...");
        System.out.println("Closing logfile writer...");
        if(resultWriter != null) {
            try {
                resultWriter.close();
            } catch (IOException e) {
                if (e.getMessage() != null) {
                    System.out.println(e.getMessage());
                } else {
                    System.out.println(e.toString());
                }
            }
        }
        System.exit(0);
    }
}
