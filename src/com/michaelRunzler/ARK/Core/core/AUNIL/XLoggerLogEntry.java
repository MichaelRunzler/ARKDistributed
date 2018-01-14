package core.AUNIL;

/**
 * Data container for log data stored by the {@link XLoggerCore XLoggerCore} class.
 */
public class XLoggerLogEntry
{
    String message;
    LogEventLevel level;
    XLoggerInterpreter caller;

    XLoggerLogEntry(XLoggerInterpreter caller, LogEventLevel level, String message){
        this.caller = caller;
        this.level = level;
        this.message = message;
    }
}
