package X34.UI.JFX.Util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for assigning class ownership for declared static constants in class headers.
 * No enforcement is made as to whether this can be used as a functional interface or not, since the
 * active retention policy makes no guarantee that this annotation will be visible at runtime.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface Owner
{
    Class value();
}
