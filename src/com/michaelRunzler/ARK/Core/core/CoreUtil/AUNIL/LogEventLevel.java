package core.CoreUtil.AUNIL;

/**
 * Represents severity levels for log events written by the {@link XLoggerCore} object.
 * Note that the severity of these event levels is only meant as an additional degree of clarity
 * as to the importance of the event in question, and does not affect how they are processed by
 * the {@link XLoggerCore Core} (with the exception of {@link LogVerbosityLevel verbosity settings}).
 */
public enum LogEventLevel {
    /**
     * An event that contains data only useful to a developer or advanced user for debugging purposes.
     */
    DEBUG,
    /**
     * An event that contains general information about program state or progress updates.
     */
    INFO,
    /**
     * An event that warns of a potential non-critical problem with the program.
     */
    WARNING,
    /**
     * An event that indicates a serious (but recoverable) error in the program,
     * perhaps something that should be reported to the developer.
     */
    ERROR,
    /**
     * An event that indicates a severe (usually unrecoverable) error in the program,
     * one that might cause undesirable operation or further malfunctions.
     */
    CRITICAL,
    /**
     * An event that indicates an error so severe that the program must shut down or cease operation
     * immediately to prevent data corruption or further severe problems.
     */
    FATAL
}
