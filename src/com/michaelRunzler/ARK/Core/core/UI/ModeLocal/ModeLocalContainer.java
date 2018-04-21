package core.UI.ModeLocal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Container annotation type for repeated annotations of type {@link ModeLocal}.
 */
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ModeLocalContainer{
    ModeLocal[] value();
}
