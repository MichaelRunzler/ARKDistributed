package core.system;

/**
 * Handles the dispatch of TransThreadTransport objects and their log handlers.
 */
public class ARKTransThreadTransportHandler
{
    ARKLogHandler logger;

    /**
     * Constructs a new Trans-Thread Transport Handler object.
     * @param log an object implementing the com.michaelRunzler.ARK Log Handler interface to handle event logging
     */
    public ARKTransThreadTransportHandler(ARKLogHandler log){
        logger = log;
    }

    /**
     * Dispatches (throws) a new Trans-Thread Packet with the type ALERT.
     * @param message the cause or carrier data for this object
     * @throws ARKTransThreadTransport
     */
    public void dispatchTransThreadPacket(String message) throws ARKTransThreadTransport {
        throw new ARKTransThreadTransport(message).setLogHandler(logger);
    }

    /**
     * Dispatches (throws) a new Trans-Thread Packet with a user-specified type.
     * @param message the cause or carrier data for this object
     * @param type the intended message type
     * @throws ARKTransThreadTransport
     */
    public void dispatchTransThreadPacket(String message, ARKTransThreadTransport.TransportType type) throws ARKTransThreadTransport {
        throw new ARKTransThreadTransport(message, type).setLogHandler(logger);
    }
}

