package Iris.util;

import Iris.modules.UniFi;
import Iris.modules.RADWIN;
import core.system.ARKTransThreadTransport;

/**
 * Stores information for an AP on the network.
 */
public class AP
{
    /**
     * Stores the OS type running on an AP unit.
     */
    public enum APType{
        AirOS3, AirOS6, AirOS8, AirOS8v2, RADWIN
    }

    private String name;
    private IPV4Address IP;
    private String user;
    private String password;
    private APType type;
    private APStats cpeList;

    /**
     * Constructs a new AP object with the specified parameters.
     * @param name the name of the AP unit
     * @param IP the IPv4 address of the AP unit
     * @param user the username to use when logging in to the AP
     * @param password the password to use when logging in to the AP
     * @param type the OS type running on the AP unit
     */
    public AP(String name, IPV4Address IP, String user, String password, APType type)
    {
        this.name = name;
        this.IP = IP;
        this.user = user;
        this.password = password;
        this.type = type;
        this.cpeList = new APStats();
    }

    /**
     * Constructs a new AP object with the specified parameters.
     * @param name the name of the AP unit
     * @param IP the IPv4 address of the AP unit
     * @param user the username to use when logging in to the AP
     * @param password the password to use when logging in to the AP
     * @param type the OS type running on the AP unit
     * @param CPEs the list of CPEs for the AP unit, encapsulated in an APStats object
     */
    public AP(String name, IPV4Address IP, String user, String password, APType type, APStats CPEs)
    {
        this.name = name;
        this.IP = IP;
        this.user = user;
        this.password = password;
        this.type = type;
        this.cpeList = CPEs;
    }

    public String getName() {
        return name;
    }

    public IPV4Address getIP() {
        return IP;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public APType getType() {
        return type;
    }

    public APStats getCPEList() {
        return this.cpeList;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIP(IPV4Address IP) {
        this.IP = IP;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setType(APType type) {
        this.type = type;
    }

    public void setCPEList(APStats cpeList) {
        this.cpeList = cpeList;
    }

    /**
     * Utility method, removes the need to externally call the necessary methods for stat-querying.
     * Queries live statistics from the physical AP unit represented by this object and updates this object's
     * internal CPE register with the results, if any.
     */
    public void queryLiveStats()
    {
        if(this.type == APType.RADWIN){
            try {
                this.cpeList = (new RADWIN().getData(this)).getCPEList();
            } catch (ARKTransThreadTransport e) {
                e.handleTransportPacketNoUI();
            }
        }else{
            try {
                this.cpeList = (new UniFi().getData(this)).getCPEList();
            } catch (ARKTransThreadTransport e) {
                e.handleTransportPacketNoUI();
            }
        }
    }

}
