package Iris.modules;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.ArrayList;

public class SNMPTest
{
    private static Snmp dispatcher;
    private static TransportMapping udp;
    private static final String CURRENT_TEST_OID   = ".1.3.6.1.2.1.2.2.1.8.101";
    private static final String CURRENT_TEST_OID_2 = ".1.3.6.1.2.1.2.2.1.6.101";
    private static final int DEFAULT_SNMP_PORT = 161;
    private static final String TEST_ADDRESS = "10.1.53.107";

    public static void main(String[] args)
    {
        try {
            udp = new DefaultUdpTransportMapping();
            dispatcher = new Snmp(udp);
        } catch (IOException e) {
            System.exit(404);
        }

        try {
            ArrayList<String> test1Response = sendSnmpV1OIDs(TEST_ADDRESS, "netman", DEFAULT_SNMP_PORT, PDU.GET, CURRENT_TEST_OID, CURRENT_TEST_OID_2);
            System.out.print("SendSNMPOID: ");
            for(String s : test1Response){
                System.out.print(s + " | ");
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("SNMPGet: " + snmpGet(TEST_ADDRESS, "netman", CURRENT_TEST_OID));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    private static ArrayList<String> sendSnmpV1OIDs(String address, String community, int port, int type, String... OIDs) throws IOException
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

    //todo temporary
    private static String snmpGet(String strAddress, String communityID, String strOID) throws IOException
    {
        String str = "";

        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        CommunityTarget comtarget = new CommunityTarget();
        comtarget.setCommunity(new OctetString(communityID));
        comtarget.setVersion(SnmpConstants.version1);
        comtarget.setAddress(new UdpAddress(strAddress + "/" + DEFAULT_SNMP_PORT));
        comtarget.setRetries(2);
        comtarget.setTimeout(5000);

        PDU pdu = new PDU();
        ResponseEvent response;
        Snmp snmp;

        pdu.add(new VariableBinding(new OID(strOID)));
        pdu.setType(PDU.GET);
        snmp = new Snmp(transport);

        response = snmp.get(pdu, comtarget);

        if(response != null) {
            if(response.getResponse().getErrorStatusText().equalsIgnoreCase("Success"))
            {
                PDU pduresponse = response.getResponse();
                str = pduresponse.getVariableBindings().firstElement().toString();

                if(str.contains("=")){
                    str = str.substring(str.indexOf("=") + 1, str.length());
                }
            }
        }else{
            throw new IOException("Request timed out!");
        }

        snmp.close();
        return str;
    }
}
