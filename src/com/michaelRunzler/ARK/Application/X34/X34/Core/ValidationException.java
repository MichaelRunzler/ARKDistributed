package X34.Core;

/**
 * Represents data-type formatting and validation exceptions, such as those encountered by data parsers and validators.
 */
public class ValidationException extends Exception
{
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException() {
        super();
    }
}
