package core.CoreUtil.AUNIL;

/**
 * Relay-type class to relay log event data back to calling classes that have registered a stream listener with their linked
 * {@link XLoggerInterpreter} class instance. All logged data will be passed to this callback method. It is the responsibility
 * of the creating class to sort through the relayed data.
 */
public abstract class Callback
{
    // Only used by the stream registry class
    boolean doMultiThread;

    /**
     * Default constructor.
     * @param multiThreaded set this to {@code true} if this callback can be called from a daemon thread controlled by the
     *                      logging system, {@code false} if it should only be run on the main thread
     */
    public Callback(boolean multiThreaded){
        doMultiThread = multiThreaded;
    }

    /**
     * Called whenever a log entry is being written to the system console or a log file.
     * @param data the content of the log entry being written
     * @param IID the 'friendly name' of the {@link XLoggerInterpreter} that logged this entry
     * @param level the {@link LogEventLevel event level} of this entry
     * @param compiled a direct copy of the exact data written by the {@link XLoggerCore} to any active log files and/or
     *                 the system console
     */
    public abstract void call(String data, String IID, LogEventLevel level, String compiled);

    /**
     * Checks if an event should be logged to the console under the provided {@link LogVerbosityLevel verbosity level}.
     * @param level the {@link LogVerbosityLevel verbosity level} that this event should be filtered under
     * @param severity the {@link LogEventLevel event level} to check under the verbosity filter
     * @return {@code true} if the event should be allow to output to the console, {@code false} if otherwise
     */
    public static boolean checkVerbosityLevel(LogVerbosityLevel level, LogEventLevel severity)
    {
        switch (level)
        {
            case STANDARD:
                return severity != LogEventLevel.DEBUG;
            case MINIMAL:
                return severity != LogEventLevel.DEBUG && severity != LogEventLevel.INFO;
            case DEBUG:
                return true;
            default:
                return false;
        }
    }
}
