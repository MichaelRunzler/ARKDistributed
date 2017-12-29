package Iris.modules;

import Iris.util.AP;
import Iris.util.APStats;
import Iris.util.CPEStats;
import Iris.util.IPV4Address;
import core.CoreUtil.IOTools;
import core.system.ARKTransThreadTransport;
import core.system.ARKTransThreadTransportHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Handles data query requests for Ubiquiti Networks AP units.
 */
public class UniFi
{
    private ARKTransThreadTransportHandler handler;

    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    /**
     * Initializes this module and readies it for use.
     */
    public UniFi() {
        handler = new ARKTransThreadTransportHandler(null);
    }

    /**
     * Gets data from an AP unit in the Ubiquiti family.
     * @param node the AP unit to query data from
     * @return a modified instance of the queried AP, or the original AP if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    public AP getData(AP node) throws ARKTransThreadTransport
    {
        if(node == null) return null;

        APStats data;
        switch (node.getType())
        {
            case AirOS3:
                data = getAOS3Data(node.getIP(), node.getUser(), node.getPassword());
                break;
            case AirOS6:
                data = getAOS6Data(node.getIP(), node.getUser(), node.getPassword());
                break;
            case AirOS8:
                data = getAOS8Data(node.getIP(), node.getUser(), node.getPassword());
                break;
            case AirOS8v2:
                data = getAOS8v2Data(node.getIP(), node.getUser(), node.getPassword());
                break;
            default:return node;
        }
        node.setCPEList(data == null ? node.getCPEList() : data);
        return node;
    }

    /**
     * Gets data from a Ubiquiti AP unit running AirOS 3.
     * @param IP the IP address of the target AP
     * @param user the username to log in with
     * @param password the password to log in with
     * @return the statistics of the queried AP, or null if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    private APStats getAOS3Data(IPV4Address IP, String user, String password) throws ARKTransThreadTransport
    {
        APStats values = new APStats();
        String data = "";

        //load raw data from AP station list table
        try {
            //get table from AP
            HashMap<String, String> params = new HashMap<>();
            params.put("password", password);
            params.put("username", user);

            data = IOTools.getDataFromSubmittedHttpForm("https://" + IP.toString() + "/login.cgi?uri=/stalist.cgi", params);
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while logging into AP");
        }

        //TODO TEMPORARY
        try {
            data = IOTools.loadDataFromFile(new File(System.getProperty("user.home") + "\\Desktop\\ARK", "Associated Stations.html"));
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while writing database file.");
        }

        //todo magic goes here

        return values;
    }

    /**
     * Gets data from a Ubiquiti AP unit running AirOS 4, 5, 6, or 7.
     * @param IP the IP address of the target AP
     * @param user the username to log in with
     * @param password the password to log in with
     * @return the statistics of the queried AP, or null if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    private APStats getAOS6Data(IPV4Address IP, String user, String password) throws ARKTransThreadTransport
    {
        APStats values = new APStats();
        String data = "";

        //load raw data from AP station list table
        try {
            //get table from AP
            HashMap<String, String> params = new HashMap<>();
            params.put("password", password);
            params.put("username", user);

            data = IOTools.getDataFromSubmittedHttpForm("https://" + IP.toString() + "/login.cgi?uri=/stalist.cgi", params);
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while logging into AP");
        }

        //TODO TEMPORARY
        try {
            data = IOTools.loadDataFromFile(new File(System.getProperty("user.home") + "\\Desktop\\ARK", "Associated Stations.html"));
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while writing database file.");
        }

        //trim everything before the table itself off
        data = IOTools.getFieldFromData(data, "<tbody>", "</tbody>", data.indexOf("table id=\"sta_list\""));

        //while we still have more rows to parse:
        while(data.contains("<tr role=\"row\""))
        {
            HashMap<String, String> rowStats = new HashMap<>();
            //trim down to the header of the first row
            int nextValue = data.indexOf("</td>") + "</td>".length();
            data = data.substring(nextValue, data.length());

            //get the fields from the table
            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("Name", IOTools.getFieldFromData(data, "<td>", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("TX Signal", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("RX Signal", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("Noise", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("Latency", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("Distance", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("TX/RX", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("CCQ", IOTools.getFieldFromData(data, "<td class=\" centered\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("Uptime", IOTools.getFieldFromData(data, "<td class=\" uptime\">", "</td>", 0));
            data = data.substring(nextValue, data.length());

            nextValue = data.indexOf("</td>") + "</td>".length();
            rowStats.put("IP", IOTools.getFieldFromData(data, "<a href=\"http://", "/\" target=\"", 0));
            data = data.substring(nextValue, data.length());

            //trim the rest of this row off in preparation for the next row
            data = data.substring(data.indexOf("</tr>"), data.length());

            //store the row's stats
            values.addCPE(new CPEStats(rowStats));
        }

        return values;
    }

    /**
     * Gets data from a Ubiquiti AP unit running AirOS 8.
     * @param IP the IP address of the target AP
     * @param user the username to log in with
     * @param password the password to log in with
     * @return the statistics of the queried AP, or null if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    private APStats getAOS8Data(IPV4Address IP, String user, String password) throws ARKTransThreadTransport
    {
        APStats values = new APStats();
        String data = "";

        //load raw data from AP station list table
        try {
            //get table from AP
            HashMap<String, String> params = new HashMap<>();
            params.put("password", password);
            params.put("username", user);

            data = IOTools.getDataFromSubmittedHttpForm("https://" + IP.toString() + "/login.cgi?uri=/stalist.cgi", params);
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while logging into AP");
        }

        //TODO TEMPORARY
        try {
            data = IOTools.loadDataFromFile(new File(System.getProperty("user.home") + "\\Desktop\\ARK", "Associated Stations.html"));
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while writing database file.");
        }

        //todo magic goes here

        return values;
    }

    /**
     * Gets data from a Ubiquiti AP unit running AirOS 8 gen. 2.
     * @param IP the IP address of the target AP
     * @param user the username to log in with
     * @param password the password to log in with
     * @return the statistics of the queried AP, or null if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    private APStats getAOS8v2Data(IPV4Address IP, String user, String password) throws ARKTransThreadTransport
    {
        APStats values = new APStats();
        String data = "";

        //load raw data from AP station list table
        try {
            //get table from AP
            HashMap<String, String> params = new HashMap<>();
            params.put("password", password);
            params.put("username", user);

            data = IOTools.getDataFromSubmittedHttpForm("https://" + IP.toString() + "/login.cgi?uri=/stalist.cgi", params);
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while logging into AP");
        }

        //TODO TEMPORARY
        try {
            data = IOTools.loadDataFromFile(new File(System.getProperty("user.home") + "\\Desktop\\ARK", "Associated Stations.html"));
        } catch (IOException e) {
            handler.dispatchTransThreadPacket("IO error while writing database file.");
        }

        //todo magic goes here

        return values;
    }
}
