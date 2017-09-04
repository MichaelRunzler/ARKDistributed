package core.system;

import core.UI.ARKInterfaceAlert;
import core.UI.ARKInterfaceDialogYN;

import java.util.Scanner;

/**
 * A special subclass of exception report that is used as a trans-thread notification transport medium.
 */
public class ARKTransThreadTransport extends Exception
{

    /**
     * Used to determine message target types for this object.
     * Enumeration type summary:
     * <p>
     * ALERT: Should be used as a generic 'alert' or information dialog.
     * ERROR: Should be presented as a non-fatal error or problem report dialog.
     * QUERY: Should be presented as a true/false question dialog with action taken based on user input.
     * FATAL: Should be interpreted as a fatal, unrecoverable error being reported to the user or system for reporting purposes.
     */
    public enum TransportType {
        ALERT, ERROR, QUERY, FATAL
    }

    private TransportType type;
    private ARKLogHandler log;

    /**
     * Constructs a new transport object with the specified message. Type will be set to ALERT be default,
     * use other constructor to specify type manually.
     * @param message the cause or carrier data for this object
     */
    protected ARKTransThreadTransport(String message) {
        super(message);
        this.type = TransportType.ALERT;
        log = null;
    }

    /**
     * Creates a new transport object with the specified message and type.
     * @param message the cause or carrier for this object
     * @param type the intended message type
     */
    protected ARKTransThreadTransport(String message, TransportType type) {
        super(message);
        this.type = type;
        log = null;
    }

    /**
     * Gets the transport type of the message.
     * @return the intended display type of this transport object's payload
     */
    public TransportType getTransportType() {
        return type;
    }

    /**
     * Sets a system log handler for this transport packet.
     * @param handler a class implementing the ARKLogHandler interface to handle system event logging from this packet
     */
    public ARKTransThreadTransport setLogHandler(ARKLogHandler handler) {
        this.log = handler;
        return this;
    }

    /**
     * Handles the display of a transport packet object. Will throw a NullPointerException if no log handler has been set.
     * @return the result of a QUERY-type window if that is the set type, false if otherwise
     */
    public boolean handleTransportPacket()
    {
        double size = this.getMessage().length() > 25 ? Math.sqrt(this.getMessage().length() / 25) * 100 : 100;
        if (this.type == TransportType.ALERT) {
            if (log != null) log.logEvent(super.getMessage());
            new ARKInterfaceAlert("Notice", super.getMessage(), (int) size, (int) size).display();
        } else if (this.type == TransportType.ERROR) {
            if (log != null) log.logEvent("Warning: " + super.getMessage());
            new ARKInterfaceAlert("Warning", super.getMessage(), (int) size, (int) size).display();
        } else if (this.type == TransportType.QUERY) {
            return new ARKInterfaceDialogYN("Query", super.getMessage(), "Yes", "No", (int) size, (int) size).display();
        } else if (this.type == TransportType.FATAL) {
            if (log != null) log.logEvent("Error: " + super.getMessage());
            new ARKInterfaceAlert("Error", super.getMessage(), (int) size, (int) size).display();
        }
        return false;
    }

    /**
     * Handles the display of a transport packet object. Will throw a NullPointerException if no log handler has been set.
     * Does not use UI elements, prints output to STDOUT, suitable for use in command-line programs.
     * @return the result of a QUERY-type prompt if that is the set type, false if otherwise
     */
    public boolean handleTransportPacketNoUI()
    {
        if (this.type == TransportType.ALERT) {
            if (log != null) log.logEvent(super.getMessage());
            System.out.println("Notice: " + super.getMessage());
        } else if (this.type == TransportType.ERROR) {
            if (log != null) log.logEvent("Warning: " + super.getMessage());
            System.out.println("Warning: " + super.getMessage());
        } else if (this.type == TransportType.QUERY) {
            System.out.println("Query: " + super.getMessage() + " (Y/N)");
            Scanner in = new Scanner(System.in);
            if(in.hasNext() && in.next().equals("Y"))
                return true;
        } else if (this.type == TransportType.FATAL) {
            if (log != null) log.logEvent("Error: " + super.getMessage());
            System.out.println("Error: " + super.getMessage());
        }
        return false;
    }
}
