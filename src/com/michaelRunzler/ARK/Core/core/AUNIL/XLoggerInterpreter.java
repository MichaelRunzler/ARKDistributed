package core.AUNIL;

import java.io.*;

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
    private long lastLogTime;

    /**
     * Internal use only. Used by the {@link XLoggerCore XLoggerCore} to register its own internal logger interpreter.
     * Does not auto-associate, must be associated manually.
     * @param assoc the {@link XLoggerCore XLoggerCore} object to associate with this object
     * @param friendlyName an optional 'friendly name' to use for this object. Providing null will set the friendly name to the class ID.
     */
    XLoggerInterpreter(XLoggerCore assoc, String friendlyName)
    {
        lastLogTime = System.currentTimeMillis();
        this.executor = assoc;
        this.classID = getCallerClass();
        this.friendlyName = friendlyName == null ? classID : friendlyName;
        this.implicitLevel = LogEventLevel.INFO;
    }

    /**
     * Instantiates a new copy of this object. Uses the caller's class ID as the friendly name, and uses the primary instance
     * of the globally accessible {@link XLoggerCore} object from the {@link XLoggerDelegator}.
     */
    public XLoggerInterpreter()
    {
        lastLogTime = System.currentTimeMillis();
        this.executor = XLoggerDelegator.getMainInstance();
        this.classID = getCallerClass();
        this.friendlyName = classID;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Instantiates a new copy of this object. Uses the primary instance of the globally accessible {@link XLoggerCore} object from the {@link XLoggerDelegator}.
     * @param friendlyName the 'friendly name' that the logger system should use instead of the class's actual class ID when
     *                     writing log entries
     */
    public XLoggerInterpreter(String friendlyName)
    {
        lastLogTime = System.currentTimeMillis();
        this.executor = XLoggerDelegator.getMainInstance();
        this.classID = getCallerClass();
        this.friendlyName = friendlyName;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Instantiates a new copy of this object. Uses the caller's class ID as the friendly name.
     * @param instanceID the global instance ID of the Core object that this object should use from the {@link XLoggerDelegator} class.
     *                   Useful if you have a large number (e.g > 20) threads running with logger objects; using multiple
     *                   instance IDs helps prevent slowdown and excessive memory usage by the logger objects.
     */
    public XLoggerInterpreter(short instanceID)
    {
        lastLogTime = System.currentTimeMillis();
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
     * @param instanceID the global instance ID of the Core object that this object should use from the {@link XLoggerDelegator} class.
     *                   Useful if you have a large number (e.g > 20) threads running with logger objects; using multiple
     *                   instance IDs helps prevent slowdown and excessive memory usage by the logger objects.
     */
    public XLoggerInterpreter(String friendlyName, short instanceID)
    {
        lastLogTime = System.currentTimeMillis();
        this.executor = instanceID < 0 ? XLoggerDelegator.getMainInstance() : XLoggerDelegator.getDynamicInstance(instanceID);
        this.classID = getCallerClass();
        this.friendlyName = friendlyName;
        this.implicitLevel = LogEventLevel.INFO;
        executor.associateInterpreter(this);
    }

    /**
     * Sets the implicit log event level of logged events from this object.
     * Avoid calling this every time you wish to use a different event level - instead, use
     * {@link #logEvent(LogEventLevel, String)} - this allows overriding of the implicit event level.
     * The default implicit event level for any given Interpreter object is {@link LogEventLevel#INFO}.
     * @param level the new implicit log event level for this object
     */
    public void setImplicitEventLevel(LogEventLevel level) {
        this.implicitLevel = level;
    }

    /**
     * De-registers this object from the {@link XLoggerCore Core} instance that it is associated with. Any successive calls to any logging functions
     * will be rejected until {@link XLoggerInterpreter#associate()} is called. It is recommended, although not required, to call this method on all
     * registered Interpreters before shutting down your application. The core will manually disassociate all associated interpreters
     * on application shutdown if they have not already disassociated.
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
     * 'Opts-out' of the {@link XLoggerCore core}'s master logfile. The master log file is a combination of all other active
     * logfiles, excluding those that have opted out. To reactivate master logging, call {@link XLoggerInterpreter#disassociate()},
     * followed by {@link XLoggerInterpreter#associate()}.
     */
    public void deactivateMasterLogging() {
        executor.optOutOfMasterLog(this);
    }

    /**
     * Changes the global {@link LogVerbosityLevel verbosity level} of this Interpreter's associated {@link XLoggerCore core} object.
     * By default, new {@link XLoggerCore}s are set to the {@link LogVerbosityLevel#DEBUG} verbosity level for compatibility reasons.
     * Verbosity level only affects events logged to the system console. Events logged to any
     * {@link XLoggerInterpreter Interpreter}'s logfile (or the master file) will be unaffected by the verbosity level set here.
     * @param level the verbosity level to change to
     * @throws SecurityException if the {@link XLoggerInterpreter Interpreter} requesting the change is not authorized
     * to execute such a change
     */
    public void changeLoggerVerbosity(LogVerbosityLevel level) throws SecurityException{
        executor.changeLogVerbosity(this, level);
    }

    /**
     * Logs the specified event to this object's associated {@link XLoggerCore} object.
     * Uses the specified event level instead of this object's implicit event level.
     * @param level the overridden log event level for this event
     * @param message the message to pass to the logger core
     * @see XLoggerInterpreter#logEvent(String) for the alternate version of this method
     */
    public void logEvent(LogEventLevel level, String message) {
        lastLogTime = System.currentTimeMillis();
        executor.logEvent(this, level, message);
    }

    /**
     * Logs the specified event to this object's associated {@link XLoggerCore} object.
     * Log event level is set to this object's implicit event level.
     * @param message the message to pass to the logger core
     * @see XLoggerInterpreter#logEvent(LogEventLevel, Exception) for the alternate version of this method
     */
    public void logEvent(String message){
        this.logEvent(implicitLevel, message);
    }

    /**
     * Logs the provided Exception to this object's associated {@link XLoggerCore} object.
     * Uses the specified event level instead of this object's implicit event level.
     * @param level the overridden log event level for this event
     * @param e the {@link Exception} to pass to the logger core
     * @see XLoggerInterpreter#logEvent(Exception) for the alternate version of this method
     */
    public void logEvent(LogEventLevel level, Exception e)
    {
        String str = "";

        if(e == null)
            str += "Null exception";
        else{
            StringWriter st = new StringWriter();
            PrintWriter pt = new PrintWriter(st);
            e.printStackTrace(pt);
            str = st.toString();
        }

        this.logEvent(level, str);
    }

    /**
     * Logs the provided Exception to this object's associated {@link XLoggerCore} object.
     * Log event level is set to {@link LogEventLevel#ERROR}.
     * @param e the {@link Exception} to pass to the logger core
     * @see XLoggerInterpreter#logEvent(LogEventLevel, Exception) for the alternate version of this method
     */
    public void logEvent(Exception e) {
        this.logEvent(LogEventLevel.ERROR, e);
    }

    /**
     * Requests to change the parent log directory of this object's associated {@link XLoggerCore} object.
     * @param newParent the new parent directory to attempt to switch to
     * @throws java.io.IOException if the change was denied for any reason. Possible reasons for denial include:
     * <ul>
     *     <li>This Interpreter has been disassociated with the {@link XLoggerCore Core} in question</li>
     *     <li>The {@link XLoggerCore Core} is unable to finalize ongoing write operations from this or other Interpreters in a timely manner</li>
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
     * Gets the time in milliseconds since the last call to {@link XLoggerInterpreter#logEvent(LogEventLevel, String) logEvent()} or any of its overloaded
     * variants. A <i>successful</i> event log is not required, simply a call to it. If no calls to these methods
     * have been made yet, returns the time since the constructor was called.
     * Time values are obtained via {@link System#currentTimeMillis()}. Calling this method does not reset the timer.
     * @return the time in milliseconds since the last event log call or constructor init call
     */
    public long getTimeSinceLastEvent() {
        return System.currentTimeMillis() - lastLogTime;
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