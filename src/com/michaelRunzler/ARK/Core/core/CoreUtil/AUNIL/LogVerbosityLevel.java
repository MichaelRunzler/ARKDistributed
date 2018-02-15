package core.CoreUtil.AUNIL;

/**
 * Represents system logging verbosity levels for events produced by the {@link XLoggerCore}.
 */
public enum LogVerbosityLevel {
    /**
     * Logs only basic information to the console.
     * Includes only {@link LogEventLevel#ERROR Error}, {@link LogEventLevel#CRITICAL Critical}, and {@link LogEventLevel#FATAL Fatal} event levels.
     */
    MINIMAL,
    /**
     * Logs all events to the console, with the exception of {@link LogEventLevel#DEBUG Debug} events.
     */
    STANDARD,
    /**
     * Logs all event levels to the console, effectively mirroring the output written to log files, if there are any.
     */
    DEBUG
}
