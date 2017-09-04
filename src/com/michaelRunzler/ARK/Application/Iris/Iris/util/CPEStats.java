package Iris.util;

import java.util.HashMap;

/**
 * Stores statistics from a CPE unit in an easy-to-use container.
 * All fields are public, call a field to modify.
 */
public class CPEStats
{
    public String name;
    public IPV4Address IP;
    public int TXSignal;
    public int RXSignal;
    public int noiseFloor;
    public int latency;
    public double distance;
    public String TXRXCapacity;
    public double CCQ;
    public String uptime;
    public String status;
    public double riskProbability;

    /**
     * Constructs a new CPEStats object with the specified statistical data.
     * @param name the friendly name of the unit
     * @param IP the IPv4 address of the unit
     * @param TXSignal the transmit signal strength of the unit in dBm
     * @param RXSignal the receive signal strength of the unit in dBm
     * @param noiseFloor the noise floor of the unit in dBm
     * @param latency the connection latency of the unit in milliseconds
     * @param distance the distance from the AP to the CPE in miles
     * @param TXRXCapacity the transmit/receive capacity of the AP-CPE link in megabits/second
     * @param CCQ the average packet loss rate over the AP-CPE link in % successful
     * @param uptime the time since the unit last lost connection to the AP
     * @param riskProbability the probability that the unit will experience connection problems (0.0 is best, 100.0 is worst)
     */
    public CPEStats(String name, IPV4Address IP, int TXSignal, int RXSignal, int noiseFloor, int latency, double distance,
                    String TXRXCapacity, double CCQ, String uptime, String status, double riskProbability) {
        this.name = name;
        this.IP = IP;
        this.TXSignal = TXSignal;
        this.RXSignal = RXSignal;
        this.noiseFloor = noiseFloor;
        this.latency = latency;
        this.distance = distance;
        this.TXRXCapacity = TXRXCapacity;
        this.CCQ = CCQ;
        this.uptime = uptime;
        this.status = status;
        this.riskProbability = riskProbability;
    }

    /**
     * Constructs a new CPEStats object with default values in all fields.
     */
    public CPEStats()
    {
        name = "Unnamed";
        IP = new IPV4Address();
        TXSignal = 0;
        RXSignal = 0;
        noiseFloor = 0;
        latency = 0;
        distance = 0.0;
        TXRXCapacity = "0/0";
        CCQ = 0.0;
        uptime = "00:00:00:00";
        status = "Offline";
        riskProbability = 100.0;
    }


    /**
     * Converts a HashMap(String, String) containing the CPE's statistics into a CPEStats object.
     */
    public CPEStats(HashMap<String, String> stats)
    {
        this.name = stats.get("Name");
        this.IP = IPV4Address.parseAddress(stats.get("IP"));
        this.TXSignal = stats.get("TX Signal").equals("-") ? 0 : Integer.parseInt(stats.get("TX Signal"));
        this.RXSignal = stats.get("RX Signal").equals("-") ? 0 : Integer.parseInt(stats.get("RX Signal"));
        this.noiseFloor = stats.get("Noise").equals("-") ? 0 : Integer.parseInt(stats.get("Noise"));
        this.latency = Integer.parseInt(stats.get("Latency"));
        this.distance = stats.get("Distance").equals("-") ? 0.0 : Double.parseDouble(stats.get("Distance"));
        this.TXRXCapacity = stats.get("TX/RX");
        this.CCQ = Double.parseDouble(stats.get("CCQ"));
        this.uptime = stats.get("Uptime");
        this.status = stats.get("Status");
        this.riskProbability = Double.parseDouble(stats.get("Risk"));
    }
}
