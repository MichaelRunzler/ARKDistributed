package core.CoreUtil.AUNIL;

import core.system.ARKAppCompat;
import sun.misc.SharedSecrets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static core.CoreUtil.AUNIL.Callback.checkVerbosityLevel;

/**
 * Part of the ARK Unified Logging System, this class executes log operations passed to it via the {@link XLoggerDelegator}
 * and {@link XLoggerInterpreter} classes. Maintains a registry of associated Interpreters and their caller names.
 * Package-private, only accessible to its subclasses and associated classes/objects.
 */
public class XLoggerCore
{
    public final String LOG_FILE_EXTENSION = ".x34l";

    private File parent;
    private boolean fileWrite;

    // NOTE: In this map, 'null' in the value bucket is shorthand for 'this object doesn't support file write at this time.'
    // Objects with this type of value should be treated as though global file write is off.
    private static HashMap<XLoggerInterpreter, XLoggerFileWriteEntry> bridges;

    private boolean lockLogWrites;
    private ArrayList<XLoggerLogEntry> queue;
    private XLoggerInterpreter internal;
    private XLoggerInterpreter master;
    private DateFormat dateFormat;
    private LogVerbosityLevel verbosity;
    // Must be package-local to allow direct access from Interpreter classes
    XLoggerInputStream streamRegistry;

    /**
     * Constructs a new instance of this object. Sets its parent directory to a subdirectory of
     * {@link ARKAppCompat#DESKTOP_DATA_ROOT}, named 'AUNIL'.
     * Disk write is defaulted to 'on'.
     */
    XLoggerCore()
    {
        parent = new File(ARKAppCompat.DESKTOP_CACHE_ROOT.getAbsolutePath() + "\\AUNIL");
        fileWrite = true;
        lockLogWrites = false;
        queue = new ArrayList<>();
        bridges = new HashMap<>();
        streamRegistry = new XLoggerInputStream();
        internal = new XLoggerInterpreter(this, "Logger Core");
        master = new XLoggerInterpreter(this, "Master Logfile " + this.toString().substring(this.toString().lastIndexOf('@'), this.toString().length()));
        dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        verbosity = LogVerbosityLevel.DEBUG;
        internal.associate();
        master.associate();

        // Java 9 removed the Sun Misc package from the standard classpath. Check if it's present, and if not, issue a warning and disable
        // the shutdown hook.
        try {
            InternalError ex = null;
            int start = 2;
            int tries = 0;
            int maxTries = 10;
            do {
                try {
                    SharedSecrets.getJavaLangAccess().registerShutdownHook(start + tries, true, this::shutDown);
                } catch (InternalError e) {
                    ex = e;
                }
                tries ++;
            }while (ex != null && tries <= maxTries);

            if(tries == maxTries)
                internal.logEvent(LogEventLevel.CRITICAL, "Unable to register shutdown hook because something else is taking up all\r\n" +
                        "of the available hook slots. Please note that this is suboptimal,\r\n" +
                        "and may result in undefined behavior. Among other things, this logging system will be\r\n" +
                        "unable to automatically shut down its core loggers when the program exits, possibly resulting\r\n" +
                        "in log data loss.");
            else
                internal.logEvent(LogEventLevel.DEBUG, "JVM version looks good, shutdown hook registered.");
        }catch (NoClassDefFoundError e){
            internal.logEvent(LogEventLevel.CRITICAL, "You appear to be running a nonstandard JRE or JDK (possibly Java 9 or higher)\r\n" +
                                                              "without access to the Sun miscellaneous libraries. Please note that this is suboptimal,\r\n" +
                                                              "and may result in undefined behavior. Among other things, this logging system will be\r\n" +
                                                              "unable to automatically shut down its core loggers when the program exits, possibly resulting\r\n" +
                                                              "in log data loss. Please run this program with a standard JRE or JDK if possible.");
            internal.logEvent(LogEventLevel.CRITICAL, "Recommended Java versions are: JRE 8u113-8u151 x86/x64, JDK 8u58-8u151 x64");
        }

        internal.logEvent(LogEventLevel.DEBUG, "Initialization finished.");
        internal.logEvent(LogEventLevel.DEBUG, "File write enabled: " + fileWrite);
        internal.logEvent(LogEventLevel.DEBUG, "Parent directory: " + parent.getAbsolutePath());
        internal.logEvent(LogEventLevel.DEBUG, "Verbosity level: " + verbosity.name());
        internal.logEvent(LogEventLevel.DEBUG, "Available verbosity levels: " + Arrays.toString(LogVerbosityLevel.values()));
        internal.logEvent(LogEventLevel.DEBUG, "Available event severity levels: " + Arrays.toString(LogEventLevel.values()));
        internal.logEvent("Ready.");
    }

    /**
     * Associates an {@link XLoggerInterpreter Interpreter} object with this logger core.
     * During association, the current parent directory is checked for integrity, and (if file write is enabled), the provided
     * Interpreter's target file is checked as well.
     * If file write is enabled, the core keeps a master logfile that is a combination of all other logfiles. Interpreters
     * can 'opt-out' of being logged to this file, but they must do this manually, and their preference will be lost if they
     * re-associate at any point.
     * @param caller the Interpreter object to associate with this object
     */
    synchronized void associateInterpreter(XLoggerInterpreter caller)
    {
        // If the permission check returns true, the caller must already be registered, so return immediately.
        if(checkCallerPermissions(caller)) return;

        /* If the registry size is 0, the internal Interpreter must have disassociated at some point due to disuse.
         * Either that, or the caller IS the internal Interpreter, and it's registering as part of the Core's startup.
         * If that's the case, register it as normal.
         * Otherwise, re-add it to the registry and bring it online before adding the new Interpreter.
         * Also associate the master interpreter and bring it online after adding the internal one.
         */
        if(bridges.keySet().size() == 0 && caller != internal) {
            internal.associate();
            master.disassociate();
        }

        internal.logEvent(LogEventLevel.DEBUG, "Interpreter " + caller.toString().substring(caller.toString().lastIndexOf("@") + 1) + " with informal name " + caller.friendlyName + " and class ID " + caller.classID + " is requesting association.");

        // If file write is disabled, add to registry with 'null' as the file write entry and return.
        if(!fileWrite){
            internal.logEvent(LogEventLevel.DEBUG, "Interpreter " + caller.toString().substring(caller.toString().lastIndexOf("@") + 1) + " with informal name " + caller.friendlyName + " and class ID " + caller.classID + " associated with core.");
            bridges.put(caller, null);
            return;
        }

        // The internal Interpreter is the first one to associate, so have the system check the parent directory if that's what we're dealing with.
        // Also initialize the master writer.
        if(caller == internal) checkParentDir();

        // Create the File reference and check it.
        File f = new File(parent, caller.friendlyName + LOG_FILE_EXTENSION);

        try {
            String value = checkChildFile(f, caller.friendlyName);
            if(value == null) throw new IOException("File creation failed");
            f = new File(parent, value);
        } catch (IOException e) {
            internal.logEvent(LogEventLevel.WARNING, "Interpreter " + caller.toString().substring(caller.toString().lastIndexOf("@") + 1) + " with informal name " + caller.friendlyName + " and class ID " + caller.classID + " associated, but file writing is offline due to an IO error, detailed below.");
            internal.logEvent(e);
            bridges.put(caller, null);
        }

        try {
            bridges.put(caller, new XLoggerFileWriteEntry(new BufferedWriter(new FileWriter(f)), f, true));
        } catch (IOException e) {
            internal.logEvent(LogEventLevel.WARNING, "Interpreter " + caller.toString().substring(caller.toString().lastIndexOf("@") + 1) + " with informal name " + caller.friendlyName + " and class ID " + caller.classID + " associated, but file writing is offline due to an IO error, detailed below.");
            internal.logEvent(e);
        }
        internal.logEvent(LogEventLevel.DEBUG, "Interpreter " + caller.toString().substring(caller.toString().lastIndexOf("@") + 1) + " with informal name " + caller.friendlyName + " and class ID " + caller.classID + " associated with core.");
    }

    /**
     * Disassociates an {@link XLoggerInterpreter Interpreter} object from this logger core. If all registered Interpreters
     * have deregistered, this object's internal Interpreter will deregister as well.
     * @param caller the Interpreter to deregister
     */
    synchronized void disassociateInterpreter(XLoggerInterpreter caller)
    {
        // Check caller permissions.
        if(!checkCallerPermissions(caller)){
            internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + caller.friendlyName + "\" attempted disassociation while already disassociated.");
            return;
        }

        internal.logEvent(LogEventLevel.DEBUG, "Interpreter \"" + caller.friendlyName + "\" is requesting disassociation.");

        // Close the Interpreter's writer if it has one.
        XLoggerFileWriteEntry xf = bridges.get(caller);
        try {
            if(xf != null) {
                xf.writer.flush();
                xf.writer.close();
            }
        } catch (IOException e) {
            internal.logEvent(LogEventLevel.ERROR, "Interpreter \"" + caller.friendlyName + "\" encountered an IO error while disassociating. Details below.");
            internal.logEvent(e);
        }

        bridges.remove(caller);

        // Even if the object requesting disassociation was the internal Interpreter, it's not null, it's just disassociated,
        // and it will pass the security check for event logging even if disassociated.
        internal.logEvent(LogEventLevel.DEBUG, "Interpreter \"" + caller.friendlyName + "\" disassociated.");

        // If the registry only contains the internal Interpreter, it means that all others have disassociated.
        // In that case, log this and disassociate the internal and master Interpreters as well.
        if(bridges.keySet().size() == 1 && bridges.keySet().contains(internal)){
            internal.logEvent("No registered Interpreters remain. Shutting down logger core until further notice.");
            master.disassociate();
            internal.disassociate();
        }else if(bridges.keySet().size() == 0){
            // If the registry is empty, it means that the internal interpreter has just disassociated. Log as such.
            internal.logEvent("All Interpreters now disassociated. Core will reactivate if any Interpreters re-associate.");
        }
    }

    /**
     * Requests that the logger's parent directory be changed to the specified file.
     * @param caller the {@link XLoggerInterpreter Interpreter} that is requesting the change
     * @param newParent the new parent file
     * @throws IOException if the system encounters an unrecoverable error while attempting to change the parent directory
     */
    void requestParentDirectoryChange(XLoggerInterpreter caller, File newParent) throws IOException
    {
        // Check permissions and argument validity.
        if(!checkCallerPermissions(caller)) throw new IOException("Interpreter is not registered with this Core");
        if(newParent == null) throw new IOException("New parent file is invalid");
        if(lockLogWrites) throw new IOException("Parent directory change already in progress");

        // Return if the caller is trying to set the parent file to itself - saves time that way.
        if(newParent.getAbsolutePath().equals(parent.getAbsolutePath())) return;

        // Try to make the directory structure leading to the new parent. If it fails, throw an IOException.
        if(!newParent.exists() && !newParent.mkdirs()){
            throw new IOException("New parent directory creation failed");
        }else{
            // If the new parent created successfully, lock the log stack and start re-initializing the registry.
            internal.logEvent("Interpreter \"" + caller.friendlyName + "\" initiated directory change.");
            internal.logEvent(LogEventLevel.DEBUG, "Old directory: " + parent.getAbsolutePath());
            internal.logEvent(LogEventLevel.DEBUG, "New directory: " + newParent.getAbsolutePath());
            internal.logEvent(LogEventLevel.DEBUG, "Write stack LOCKED!");
            lockLogWrites = true;
        }

        this.parent = newParent;

        // If the parent directory check fails, file writing is automatically disabled anyway, so just return.
        if(!checkParentDir()) return;

        internal.logEvent("Re-registering writers...");

        // Reset each writer's file writer entry to the new parent dir, and check its file. If the file check fails,
        // set its file writer entry's contents to null.
        for(XLoggerInterpreter xl : bridges.keySet())
        {
            internal.logEvent(LogEventLevel.DEBUG, "Re-associating interpreter \"" + xl.friendlyName + "\"...");
            XLoggerFileWriteEntry xf = bridges.get(xl);

            // Check if the specified writer has file write enabled. If it doesn't, skip it.
            if(xf == null || xf.writer == null){
                internal.logEvent(LogEventLevel.DEBUG, "Interpreter " + xl.friendlyName + " has writing disabled, skipping.");
                continue;
            }

            // If file write is enabled, close the writer before changing file.
            if(fileWrite) xf.writer.close();

            // Change file target.
            File f = new File(parent, xf.target.getName());

            // If file write is enabled, check the child file. If not, use the default filename for this Interpreter and continue.
            String value = fileWrite ? checkChildFile(f, xl.friendlyName) : caller.friendlyName;
            if(value == null){
                // If the file check fails, set writer and target to null to signify write-disable for this Interpreter and continue.
                bridges.get(xl).writer = null;
                bridges.get(xl).target = null;
                internal.logEvent(LogEventLevel.WARNING, "Re-association complete, but an IO error is preventing file writing from enabling for this interpreter.");
            }else{
                // If the check succeeds or file write is disabled, set the new filename and writer and continue.
                bridges.get(xl).target = new File(parent, value);
                bridges.get(xl).writer = new BufferedWriter(new FileWriter(bridges.get(xl).target));
                internal.logEvent("Re-association successful, file logging online.");
            }
        }

        // Unlock the log stack.
        lockLogWrites = false;
        internal.logEvent(LogEventLevel.DEBUG, "Write stack UNLOCKED!");
        internal.logEvent("Directory change complete.");
    }

    /**
     * Tells the core to stop logging data from the provided {@link XLoggerInterpreter interpreter} to the master log. Standard data logging to the console
     * and the interpreter's own logfile will not change.
     * @param caller the {@link XLoggerInterpreter} that is requesting the opt-out operation
     */
    void optOutOfMasterLog(XLoggerInterpreter caller)
    {
        if(!checkCallerPermissions(caller)){
            internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + caller.friendlyName + "\" attempted master write opt-out while disassociated.");
            return;
        }

        // Turn master log write off if it is not already.
        XLoggerFileWriteEntry fw = bridges.get(caller);
        if(fw != null && fw.writer != null && fw.writeToMaster){
            internal.logEvent(LogEventLevel.DEBUG, "Interpreter \"" + caller.friendlyName + "\" opted-out of master logging.");
            fw.writeToMaster = false;
        }
    }

    /**
     * Requests that this object's log verbosity level be changed to the specified level.
     * By default, new {@link XLoggerCore}s are set to the {@link LogVerbosityLevel#DEBUG} verbosity level for compatibility reasons.
     * Verbosity level only affects events logged to the system console. Events logged to any
     * {@link XLoggerInterpreter Interpreter}'s logfile (or the master file) will be unaffected by the verbosity level set here.
     * @param caller the {@link XLoggerInterpreter} that is requesting the verbosity change
     * @param level the verbosity level to change to
     * @throws SecurityException if the {@link XLoggerInterpreter Interpreter} requesting the change is not authorized
     * to execute such a change
     */
    synchronized void changeLogVerbosity(XLoggerInterpreter caller, LogVerbosityLevel level) throws SecurityException
    {
        if(level == verbosity) return;

        if(!checkCallerPermissions(caller)){
            internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + caller.friendlyName + "\" attempted to change verbosity settings while disassociated.");
            throw new SecurityException("Caller does not have permission to perform this operation.");
        }

        internal.logEvent(LogEventLevel.DEBUG, "Verbosity changed from " + verbosity.name() + " to " + level.name());

        verbosity = level;
    }

    /**
     * Gets the currently set verbosity level.
     * @return the current {@link LogVerbosityLevel}
     */
    LogVerbosityLevel getVerbosityLevel(){
        return verbosity;
    }

    /**
     * Gets whether or not global filesystem event processing is active. Note that this is the <i>global</i> setting,
     * and does not take into account individual {@link XLoggerInterpreter interpreters'} settings.
     * @param caller the {@link XLoggerInterpreter} that is requesting this information
     * @return {@code true} if global file logging is enabled, {@code false} if otherwise
     */
    boolean fileLoggingEnabled(XLoggerInterpreter caller)
    {
        if(!checkCallerPermissions(caller)){
            internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + caller.friendlyName + "\" attempted to check metadata while disassociated.");
            throw new SecurityException("Caller does not have permission to perform this operation.");
        }else return this.fileWrite;
    }

    /**
     * Enables or disables global filesystem event processing (whether or not active {@link XLoggerInterpreter interpreters} will
     * log their events to their associated log file on disk).
     * @param caller the {@link XLoggerInterpreter} that is requesting this change
     * @param doFileLogging the desired enable state for global file logging
     */
    synchronized void setGlobalFileLoggingEnable(XLoggerInterpreter caller, boolean doFileLogging)
    {
        if(!checkCallerPermissions(caller)){
            internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + caller.friendlyName + "\" attempted to change file logging state while disassociated.");
            throw new SecurityException("Caller does not have permission to perform this operation.");
        }else {
            this.fileWrite = doFileLogging;
            internal.logEvent(LogEventLevel.DEBUG, "Interpreter \"" + caller.friendlyName + "\" changed file write status to " + (doFileLogging ? "ENABLED" : "DISABLED"));
            internal.logEvent(LogEventLevel.INFO, "File event logging " + (doFileLogging ? "enabled" : "disabled") + ".");
        }
    }

    /**
     * Logs an event to the system console, and, if file logging is enabled, to its own event log file inside the parent
     * directory set by this object. Checks its file before logging anything to it.
     * @param caller the {@link XLoggerInterpreter Interpreter} that is requesting the logging operation
     * @param level the {@link LogEventLevel event level} that this event should be assigned, i.e its severity
     * @param message the message to log
     */
    synchronized void logEvent(XLoggerInterpreter caller, LogEventLevel level, String message)
    {
        // Check that the caller is associated. If it is not, log it and return without doing anything else.
        if(!checkCallerPermissions(caller) && caller != internal){
            if(internal != null) internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + caller.friendlyName + "\" attempted event log while disassociated.");
            return;
        }

        // Check if the write stack is locked.
        if(lockLogWrites && message != null) {
            // If it's locked, add the event to the queue and return.
            queue.add(new XLoggerLogEntry(caller, level, message));
            return;
        }else if(!lockLogWrites && message != null){
            // If it's not, check the queue to see if it has been emptied yet.
            if(queue.size() > 0){
                // If it has, write all of the stored messages to the log, empty the queue, and flag it as such.
                for(XLoggerLogEntry l : queue){
                    logEventInternal(l.caller, l.level, l.message);
                }
                queue.clear();
                // Once done clearing the log, continue.
            }
        }else{
            // If the message is invalid, say so in the log and return.
            internal.logEvent(LogEventLevel.WARNING, "Interpreter \"" + (caller == null ? "null" : caller.friendlyName) + "\" attempted event log with invalid data.");
            return;
        }

        // Delegate to the actual event log. If the write stack is not locked, there are no queued entries, and the message is valid,
        // this will be the only line that is called.
        logEventInternal(caller, level, message);
    }

    /**
     * Internal use only. Private version of {@link XLoggerCore#logEvent(XLoggerInterpreter, LogEventLevel, String) logEvent} that skips all directory and file checks.
     * See that method's JavaDoc for information on operation and arguments.
     * @param caller see other method
     * @param level see other method
     * @param message see other method
     * @see XLoggerCore#logEvent(XLoggerInterpreter, LogEventLevel, String)
     */
    private void logEventInternal(XLoggerInterpreter caller, LogEventLevel level, String message)
    {
        // Compile the output data.
        String compiled = "(" + dateFormat.format(System.currentTimeMillis()) + ") <" + level.name() + "> [" + caller.friendlyName + "]: " + message;

        // Check file write status.
        if(fileWrite) {
            // If file write is enabled, check the bridge registry for the caller. If it's null, or somehow disassociated,
            // just log to the system console. Otherwise, write to file and continue.
            try {
                if(bridges.get(caller) != null && bridges.get(caller).writer != null) {
                    BufferedWriter br = bridges.get(caller).writer;
                    br.write(compiled);
                    br.newLine();
                    br.flush();

                    // If the master-write flag is set on this caller, write a copy of the compiled data to the master logfile.
                    if(bridges.get(caller).writeToMaster && bridges.get(master) != null && bridges.get(master).writer != null){
                        BufferedWriter mr = bridges.get(master).writer;
                        mr.write(compiled);
                        mr.newLine();
                        mr.flush();
                    }
                }
            } catch (IOException e) {
                internal.logEvent(LogEventLevel.ERROR, "Interpreter \"" + caller.friendlyName + "\" encountered an IO error, detailed below");
                internal.logEvent(e);
            }
        }

        // Write the message to the system console if verbosity settings allow it.
        if(checkVerbosityLevel(verbosity, level)) System.out.println(compiled);

        // Relay a copy of the uncompiled log data to the stream registry manager and let it call any callback classes
        // Doing this last, since the callbacks could take some time, even with multithreading
        streamRegistry.logEventToStream(message, caller.friendlyName, level, compiled);
    }

    /**
     * Gets a copy (<i>NOT a reference</i>) of the current parent directory.
     * @return a File representing a copy of the parent directory File
     */
    File getParent() {
        return new File(parent.getAbsolutePath());
    }

    /**
     * Forces a shutdown of this logger core object and all associated writers and interpreters. Acts as though all remaining
     * interpreters disassociated simultaneously. Locks write access while running, so that any remaining threads that may
     * be attempting to log during this period will stall until the shutdown completes. Called when the JVM shuts down with
     * an exit code of 0.
     */
    private synchronized void shutDown()
    {
        if(bridges.isEmpty()) return;

        internal.logEvent(LogEventLevel.DEBUG, "Forced shutdown initiated by core.");
        // Lock the write stack.
        lockLogWrites = true;

        // Force disassociation on all associated interpreters.
        // This will also cause the core writers to shut down through the disassociate method's internal hooks.
        for(XLoggerInterpreter x : bridges.keySet()){
            x.disassociate();
        }
    }

    /**
     * Checks if the provided Interpreter object has permission to execute operations on this Core object, in other words,
     * is it currently valid and registered?
     * @param caller the Interpreter object to check permissions for
     * @return true if the provided Interpreter is valid and registered, false otherwise
     */
    private boolean checkCallerPermissions(XLoggerInterpreter caller) {
        return caller != null && (caller.classID != null && bridges.containsKey(caller));
    }

    /**
     * Internal use only. Checks the current parent directory for integrity. Disables file writes if it cannot create the
     * parent directory.
     */
    private boolean checkParentDir()
    {
        if(!parent.exists()) {
            if (!parent.mkdirs()) {
                internal.logEvent(LogEventLevel.CRITICAL, "Unable to initialize logging directory, file logging disabled.");
                fileWrite = false;
                return false;
            }else{
                internal.logEvent("File logging parent directory initialized, logging online.");
            }
        }
        return true;
    }

    /**
     * Checks a specified child file for validity. Cyclically changes the name of the file up to 10 times if there is an
     * existing file with the same name that cannot be deleted.
     * @param f the file to check
     * @param name the name of the original file, minus extension
     * @return the name of the new file. If the creation failed, the result will be null.
     * @throws IOException if the system encountered an IO error while creating the new file
     */
    private String checkChildFile(File f, String name) throws IOException
    {
        if(f.exists() && !f.delete()){
            int tries = 0;
            while (!f.createNewFile() && tries < 10){
                f = new File(parent, name + "_" + tries + LOG_FILE_EXTENSION);
                tries ++;
            }
            if(tries < 10) return f.getName();
            else return null;
        }
        return f.createNewFile() ? f.getName() : null;
    }
}