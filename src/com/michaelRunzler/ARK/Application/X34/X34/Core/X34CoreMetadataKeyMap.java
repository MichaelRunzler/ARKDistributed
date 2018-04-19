package X34.Core;

import X34.UI.JFX.Util.Owner;

/**
 * This class contains all officially-supported metadata flags that should be read by subclasses of {@link X34.Processors.X34RetrievalProcessor}
 * during retrieval. These flags are not <i>required</i> to be supported by all subclasses, and backwards-compatibility will
 * be respected by calling classes, but it is recommended for all subclasses to be able to read and deal with data under
 * these metadata tags to ensure full functionality.
 */
public class X34CoreMetadataKeyMap
{
    /**
     * TYPE: {@code boolean}
     * FUNCTION: Whether the processor should push changes to the index.
     * RECOMMENDED ABSENCE BEHAVIOR: Assume to be {@code true}.
     * MUTABILITY: None, no need to modify
     * NOTE: This is usually dealt with at the core-level, not the processor-level. Processors should act as though this
     *       is always {@code true}, but additional behavior is not prohibited.
     * @since primary version 3.2, class version 1.0
     */
    @Owner(X34Core.class)
    public static final String PUSH_TO_INDEX = "index_push";

    /**
     * TYPE: {@code boolean}
     * FUNCTION: Whether the processor should attempt to push telemetry data to provided telemetry properties, if it is able.
     * RECOMMENDED ABSENCE BEHAVIOR: Assume to be {@code false}.
     * MUTABILITY: None, no need to modify
     * @since primary version 3.2, class version 1.0
     */
    @Owner(X34Core.class)
    public static final String TELEMETRY_CAPABLE = "has_telemetry";

    /**
     * TYPE: {@link javafx.beans.property.SimpleIntegerProperty}
     * FUNCTION: A linked property provided by a caller class, used to relay page-state data to said caller, specifically,
     *           the maximum estimated number of pages that the processor expects to have to iterate through.
     * RECOMMENDED ABSENCE BEHAVIOR: None.
     * MUTABILITY: Changes should be pushed to the wrapped value of the Property, where they should be dealt with by the caller.
     *             If no maximum page value is available, the convention is to set the wrapped value to {@link X34Core#INVALID_INT_PROPERTY_VALUE},
     *             or {@code -1} if that value is not accessible.
     * @since primary version 3.2, class version 1.0
     */
    @Owner(X34Core.class)
    public static final String TELEM_MAX_PAGE_PROP = "max_pagination_property";

    /**
     * TYPE: {@link javafx.beans.property.SimpleIntegerProperty}
     * FUNCTION: A linked property provided by a caller class, used to relay page-state data to said caller, specifically,
     *           the current page that the processor is working on.
     * RECOMMENDED ABSENCE BEHAVIOR: None.
     * MUTABILITY: Changes should be pushed to the wrapped value of the Property, where they should be dealt with by the caller.
     *             If no maximum page value is available, the convention is to set the wrapped value to {@link X34Core#INVALID_INT_PROPERTY_VALUE},
     *             or {@code -1} if that value is not accessible.
     * @since primary version 3.2, class version 1.0
     */
    @Owner(X34Core.class)
    public static final String TELEM_CURR_PAGE_PROP = "pagination_property";

    /**
     * TYPE: {@link javafx.beans.property.SimpleStringProperty}
     * FUNCTION: A linked property provided by a caller class, used to relay error telemetry data to said caller, specifically,
     *           a generated state indicator to be interpreted and displayed by a UI frontend in cases where a noncritical error
     *           has occurred. This property is typically noncritical, as most error-handling is taken care of by the {@link core.CoreUtil.AUNIL.XLoggerCore} class.
     * RECOMMENDED ABSENCE BEHAVIOR: Errors should be directed to the active {@link core.CoreUtil.AUNIL.XLoggerInterpreter} instead.
     * MUTABILITY: Changes should be pushed to the wrapped value of the Property, where they should be dealt with by the caller.
     *             If no value is available, the convention is to set the wrapped value to {@code null}.
     * @since primary version 3.2, class version 1.0
     */
    @Owner(X34Core.class)
    public static final String TELEM_ERROR_STATE_PROP = "current_error_property";

    /**
     * TYPE: {@link javafx.beans.property.SimpleBooleanProperty}
     * FUNCTION: If this retrieval procedure has been cancelled by its calling class. This condition should be checked every
     *           time the processor iterates through a logic loop, and the loop should be terminated early and partial results
     *           should be returned if it is {@code true}.
     * RECOMMENDED ABSENCE BEHAVIOR: Assume to be {@code false}.
     * MUTABILITY: This property should be treated as read-only by the processor itself. Its wrapped value should only be changed by
     *             the calling class, if at all.
     * @since primary version 3.2, class version 1.0
     */
    @Owner(X34Core.class)
    public static final String IS_CANCELLED = "is_retrieval_cancelled";
}
