package core.AUNIL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Part of the ARK Unified Logging System, this object provides the primary interface between the {@link XLoggerCore XLoggerCore} class and
 * callers. Any classes that wish to use the logging system must instantiate this object.
 *
 * It should be noted that this class does not perform any of the actual logging operations - it merely serves to forward
 * logging requests to the logger core, and identify caller classes to said core.
 */
public class XLoggerInterpreter
{
    private XLoggerCore executor;
    String friendlyName;
    String classID;
    private LogEventLevel implicitLevel;

    /**
     * Internal use only. Used by the {@link XLoggerCore XLoggerCore} to register its own internal logger interpreter.
     * @param assoc the {@link XLoggerCore XLoggerCore} object to associate with this object
     * @param friendlyName an optional 'friendly name' to use for this object. Providing null will set the friendly name to the class ID.
     */
    XLoggerInterpreter(XLoggerCore assoc, String friendlyName)
    {
        this.executor = assoc;
        this.classID = getCallerClass();
        this.friendlyName = friendlyName == null ? classID : friendlyName;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Instantiates a new copy of this object. Uses the caller's class ID as the friendly name, and uses the primary instance
     * of the globally accessible Core object from the Delegator.
     */
    public XLoggerInterpreter()
    {
        this.executor = XLoggerDelegator.getMainInstance();
        this.classID = getCallerClass();
        this.friendlyName = classID;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Instantiates a new copy of this object. Uses the primary instance of the globally accessible Core object from the Delegator.
     * @param friendlyName the 'friendly name' that the logger system should use instead of the class's actual class ID when
     *                     writing log entries
     */
    public XLoggerInterpreter(String friendlyName)
    {
        this.executor = XLoggerDelegator.getMainInstance();
        this.classID = getCallerClass();
        this.friendlyName = friendlyName;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Instantiates a new copy of this object. Uses the caller's class ID as the friendly name.
     * @param instanceID the global instance ID of the Core object that this object should use from the Delegator class.
     *                   Useful if you have a large number (e.g > 20) threads running with logger objects; using multiple
     *                   instance IDs helps prevent slowdown and excessive memory usage by the logger objects.
     */
    public XLoggerInterpreter(short instanceID)
    {
        this.executor = instanceID < 0 ? XLoggerDelegator.getMainInstance() : XLoggerDelegator.getDynamicInstance(instanceID);
        this.classID = getCallerClass();
        this.friendlyName = classID;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Instantiates a new copy of this object.
     * @param friendlyName the 'friendly name' that the logger system should use instead of the class's actual class ID when
     *                     writing log entries
     * @param instanceID the global instance ID of the Core object that this object should use from the Delegator class.
     *                   Useful if you have a large number (e.g > 20) threads running with logger objects; using multiple
     *                   instance IDs helps prevent slowdown and excessive memory usage by the logger objects.
     */
    public XLoggerInterpreter(String friendlyName, short instanceID)
    {
        this.executor = instanceID < 0 ? XLoggerDelegator.getMainInstance() : XLoggerDelegator.getDynamicInstance(instanceID);
        this.classID = getCallerClass();
        this.friendlyName = friendlyName;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Internal use only. Provides access to all internal fields during construction, allowing full control of parameters.
     * @see XLoggerInterpreter#XLoggerInterpreter(XLoggerCore, String)
     * @param classID the class ID to assign to this object
     * @param friendlyName the 'friendly name' to assign to this object
     * @param executor the {@link XLoggerCore Logger Core} object to assign to this object
     * @param level the implicit log event level to assign to this object
     */
    XLoggerInterpreter(String classID, String friendlyName, XLoggerCore executor, LogEventLevel level){
        this.executor = executor;
        this.classID = classID;
        this.friendlyName = friendlyName;
        this.implicitLevel = level;
        executor.associateInterpreter(this);
    }

    /**
     * Sets the implicit log event level of logged events from this Interpreter.
     * Avoid calling this every time you wish to use a different event level - instead, use
     * {@link #logEvent(LogEventLevel, String)} - this allows overriding of the implicit event level.
     * The default implicit event level for any given Interpreter object is {@link LogEventLevel#INFO}.
     * @param level the new implicit log event level for this Intepreter object
     */
    public void setImplicitEventLevel(LogEventLevel level) {
        this.implicitLevel = level;
    }

    /**
     * De-registers this object from the {@link XLoggerCore Core} instance that it is associated with. Any successive calls to any logging functions
     * will be rejected until {@link XLoggerInterpreter#associate()} is called. It is recommended to call this method on all
     * registered Interpreters before shutting down your application to preclude log data loss.
     */
    public void disassociate() {
        executor.disassociateInterpreter(this);
    }

    /**
     * Registers this object with its {@link XLoggerCore Core} instance. This is called in the constructor, so it is not
     * necessary to call it separately, although this will not cause any ill effects. Call {@link XLoggerInterpreter#disassociate()} to deregister.
     */
    public void associate() {
        executor.associateInterpreter(this);
    }

    /**
     * Logs the specified event to this Interpreter's associated Logger Core object.
     * Uses the specified event level instead of this object's implicit event level.
     * @param level the overridden log event level for this event
     * @param message the message to pass to the logger core
     */
    public void logEvent(LogEventLevel level, String message) {
        executor.logEvent(this, level, message);
    }

    /**
     * Logs the specified event to this Interpreter's associated Logger Core object.
     * Log event level is set to this object's implicit event level.
     * @param message the message to pass to the logger core
     */
    public void logEvent(String message){
        executor.logEvent(this, implicitLevel, message);
    }

    /**
     * Logs the provided Exception to this Interpreter's associated Logger Core object.
     * Uses the specified event level instead of this object's implicit event level.
     * @param level the overridden log event level for this event
     * @param e the Exception to pass to the logger core
     */
    public void logEvent(LogEventLevel level, Exception e)
    {
        String str = "";

        if(e == null)
            str += "Null exception";
        else
            try {e.printStackTrace(new PrintStream(str));} catch(FileNotFoundException ignored) {}

        executor.logEvent(this, level, str);
    }

    /**
     * Logs the provided Exception to this Interpreter's associated Logger Core object.
     * Log event level is set to this object's implicit event level.
     * @param e the Exception to pass to the logger core
     */
    public void logEvent(Exception e) {
        logEvent(implicitLevel, e);
    }

    /**
     * Requests to change the parent log directory of this Interpreter's associated Core object.
     * @param newParent
     * @throws java.io.IOException if the change was denied for any reason. Possible reasons for denial include:
     * <ul>
     *     <li>This Interpreter has been deassociated with the Core in question</li>
     *     <li>The Core is unable to finalize ongoing write operations from this or other Interpreters in a timely manner</li>
     *     <li>The new directory provided is invalid, or cannot be created properly</li>
     * </ul>
     */
    public void requestLoggerDirectoryChange(File newParent) throws IOException {
        executor.requestParentDirectoryChange(this, newParent);
    }

    /**
     * Gets the current logging parent directory. If a directory change is in progress, this may not return the new parent
     * until the change has completed.
     * @return the current log directory
     */
    public File getLogDirectory() {
        return executor.getParent();
    }

    /**
     * Internal use only. Returns the name of the class calling this method, excluding the current class.
     * Uses an {@link Exception Exception} to obtain the current stack trace, then pulls the {@link StackTraceElement StackTraceElement}
     * in index 2 to get the name of the calling class. Gets only the name of the class, not its filename or package path.
     * @return the name of the calling class
     */
    private String getCallerClass() {
        StackTraceElement str = new Exception().getStackTrace()[2];
        return str.getClassName().contains(".") ? str.getClassName().substring(str.getClassName().lastIndexOf('.') + 1) : str.getClassName();
    }
}