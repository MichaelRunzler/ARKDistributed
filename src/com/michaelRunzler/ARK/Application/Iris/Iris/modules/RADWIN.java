package Iris.modules;

import Iris.util.AP;
import Iris.util.APStats;
import Iris.util.CPEStats;
import Iris.util.IPV4Address;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import core.system.ARKTransThreadTransport;
import core.system.ARKTransThreadTransportHandler;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Handles data query requests for RADWIN AP units.
 */
public class RADWIN
{
    /*
     * RADWIN OID INDEX FOR COMMON QUERIED DATA:
     *
     * All OID bindings are prefaced with the RFC 1213 standard .1.3.6.1.2.1 OID binding.
     *
     * PORT IDs (x is the command prefix for the port):
     * .2.2.1.x.1: the address for the Ethernet port on the unit.
     * .2.2.1.x.101: the address for the WLAN interface on the unit.
     *
     * PORT DATA COMMANDS (x is the port ID from above):
     * .2.2.1.2.x: the description for the port.
     * .2.2.1.6.x: the port's MAC address.
     * .2.2.1.8.x: the port's status. 1 is UP, 2 is DOWN, 3 is TEST, 4 is UNKNOWN, 5 is DORMANT, 6 is MISSING.
     * .2.2.1.10/16.x: the port's in/out octet count since last reset.
     *
     * GENERAL DATA COMMANDS:
     *
     */
    private static Snmp dispatcher;
    private ARKTransThreadTransportHandler handler;
    private TransportMapping udp;
    private final int DEFAULT_SNMP_PORT = 161;
    private final String DEFAULT_RADWIN_RW_COMMNAME = "netman";

    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    /**
     * Initializes this module and readies it for use.
     */
    public RADWIN()
    {
        handler = new ARKTransThreadTransportHandler(null);
        try {
            udp = new DefaultUdpTransportMapping();
            dispatcher = new Snmp(udp);
        } catch (IOException ignored) {
        }
    }

    /**
     * Gets data from an AP unit in the RADWIN family.
     * @param node the AP unit to query data from
     * @return a modified instance of the queried AP, or the original AP if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    public AP getData(AP node) throws ARKTransThreadTransport
    {
        if(node == null || node.getIP() == null) return null;

        APStats data;

        switch (node.getType())
        {
            case RADWIN:
                data = getV4Stats(node.getIP(), DEFAULT_RADWIN_RW_COMMNAME, DEFAULT_SNMP_PORT);
                break;
            default:return node;
        }
        node.setCPEList(data == null ? node.getCPEList() : data);
        return node;
    }

    /**
     * Gets statistics from a RADWIN AP unit running RADWIN OS on the tower network.
     * @param IP the IP address of the tower to parse
     * @param community the SNMP community string to use when querying the target
     * @param port the port on the target device to send the PDU to. Default port is usually UDP 161
     * @return the statistics of the queried AP, or null if a silent error occurred
     * @throws ARKTransThreadTransport if an unrecoverable error occurred while processing the query request
     */
    private APStats getV4Stats(IPV4Address IP, String community, int port) throws ARKTransThreadTransport
    {
        APStats retV = new APStats();

        final String WLAN_INT_STATUS_OID     = ".1.3.6.1.2.1.2.2.1.8.101";
        final String LAN_INT_STATUS_OID      = ".1.3.6.1.2.1.2.2.1.8.1";
        final String CLIENT_RADIO_COUNT_OID  = ".1.3.6.1.2.1.";//TODO
        final String CLIENT_RADIO_STATE_OID  = ".X";//TODO
        final String CLIENT_RADIO_NAME_OID   = ".X";//TODO
        final String CLIENT_RADIO_IP_OID     = ".X";//TODO
        final String CLIENT_RADIO_TXCAP_OID  = ".X";//TODO
        final String CLIENT_RADIO_RXCAP_OID  = ".X";//TODO
        final String CLIENT_RADIO_RXSIG_OID  = ".X";//TODO
        final String CLIENT_RADIO_TXSIG_OID  = ".X";//TODO
        final String CLIENT_RADIO_NOISE_OID  = ".X";//TODO
        final String CLIENT_RADIO_PING_OID   = ".X";//TODO
        final String CLIENT_RADIO_DIST_OID   = ".X";//TODO
        final String CLIENT_RADIO_UPTIME_OID = ".X";//TODO
        final String CLIENT_RADIO_LOC_OID    = ".X";//TODO

        int clientCount = 0;
        try {
            ArrayList<String> responses = sendSnmpV1OIDs(IP.toString(), community, port, PDU.GET, WLAN_INT_STATUS_OID, CLIENT_RADIO_COUNT_OID);
            if(responses == null || responses.get(0).equals("2")){
                return null;
            }
            clientCount = Integer.parseInt(responses.get(1));
        } catch (IOException | NumberFormatException e) {
            handler.dispatchTransThreadPacket(e.getMessage() == null ? e.toString() : e.getMessage());
        }

        for(int i = 0; i < clientCount; i++)
        {
            CPEStats unit = new CPEStats();

            String[] OIDs = new String[]{CLIENT_RADIO_STATE_OID, CLIENT_RADIO_NAME_OID, CLIENT_RADIO_IP_OID, CLIENT_RADIO_TXCAP_OID,
                    CLIENT_RADIO_RXCAP_OID, CLIENT_RADIO_TXSIG_OID, CLIENT_RADIO_RXSIG_OID, CLIENT_RADIO_NOISE_OID,
                    CLIENT_RADIO_PING_OID, CLIENT_RADIO_DIST_OID, CLIENT_RADIO_UPTIME_OID, CLIENT_RADIO_LOC_OID};

            for(int j = 0; j < OIDs.length; j++){
                OIDs[j] = OIDs[j].replace("X", "" + i);
            }

            try {
                ArrayList<String> responses = sendSnmpV1OIDs(IP.toString(), community, port, PDU.GET, OIDs);

                if(responses == null) return null;

                unit.status = responses.get(0);
                unit.name = responses.get(1) + " @ " + responses.get(11);
                unit.IP = IPV4Address.parseAddress(responses.get(2));
                unit.TXRXCapacity = responses.get(3) + "/" + responses.get(4);
                unit.TXSignal = Integer.parseInt(responses.get(5));
                unit.RXSignal = Integer.parseInt(responses.get(6));
                unit.noiseFloor = Integer.parseInt(responses.get(7));
                unit.latency = Integer.parseInt(responses.get(8));
                unit.distance = Double.parseDouble(responses.get(9));
                unit.uptime = responses.get(10);

                retV.addCPE(unit);
            } catch (IOException | NumberFormatException e) {
                handler.dispatchTransThreadPacket(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }

        return retV;
    }


    /**
     * Sends a PDU (with single or multiple OIDs) to an SNMP target using the v1 protocol and gets the response(s), if any.
     * @param address the IP or URL address of the SNMP target
     * @param community the community ID to provide with the OID. Can be either 'private' or 'public'
     * @param port the port on the target device to send the PDU to. Default port is usually UDP 161
     * @param type the PDU type. This is an integer ID defined by constants in the PDU class, such as PDU.GETNEXT
     * @param OIDs one or multiple Strings representing an OID or OIDs to send to the target, formatted in RFC 1213 standard format
     * @return the return value(s) as an ArrayList of Strings (may be empty if no responses were received), or null if a silent error occurred
     * @throws IOException if the request encountered an unrecoverable error
     */
    private ArrayList<String> sendSnmpV1OIDs(String address, String community, int port, int type, String... OIDs) throws IOException
    {
        if(OIDs == null || OIDs.length == 0){
            return null;
        }

        udp.listen();
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(GenericAddress.parse("udp:" + address + "/" + port));
        target.setRetries(1);
        target.setTimeout(10000);
        target.setVersion(SnmpConstants.version1);

        PDU pdu = new PDU();
        for(String s : OIDs){
            pdu.add(new VariableBinding(new OID(s)));
        }
        pdu.setType(type);

        ResponseEvent rs = dispatcher.get(pdu, target);
        udp.close();

        if(rs == null || rs.getResponse() == null){
            return null;
        }

        ArrayList<String> results = new ArrayList<>();

        for(VariableBinding v : rs.getResponse().getVariableBindings()){
            String str = v.toString();

            if(str.contains("=")){
                str = str.substring(str.indexOf("=") + 1, str.length());
                results.add(str);
            }
        }

        if(results.size() > 0){
            return results;
        }
        return null;
    }
}
