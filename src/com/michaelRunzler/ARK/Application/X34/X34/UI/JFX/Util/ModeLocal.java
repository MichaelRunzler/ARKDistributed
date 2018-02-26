package X34.UI.JFX.Util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Used to indicate that a specific field or variable (typically a {@link javafx.scene.Node} should be treated as mode-local.
 * If the GUI implementing the annotated element has multiple modes or display styles, this element should be included
 * in the mode ID(s) set in the annotation.
 */
@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ModeLocal {
    /**
     * The list of mode IDs that the annotated element should be included in.
     * It is up to the implementing reflection routine to determine if this list contains valid mode IDs for
     * the application, and deal with unexpected data.
     * @return the list of mode IDs that the annotated element should be included in
     */
    int[] value();

    /**
     * If {@code true}, the {@link #value()} list will be treated as a blacklist instead of a whitelist.
     * This means that all modes annotated in said list will <i>exclude</i> the annotated element instead of including it.
     * @return whether the {@link #value()} list should be used as a blacklist or whitelist
     */
    boolean invert() default false;
}
