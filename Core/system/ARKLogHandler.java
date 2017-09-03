package system;

/**
 * Provides a global interface standard for all ARK application log handlers.
 */
public interface ARKLogHandler
{
    void logEvent(String event);
}
