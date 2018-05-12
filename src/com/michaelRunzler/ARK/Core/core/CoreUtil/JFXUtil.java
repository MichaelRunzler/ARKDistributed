package core.CoreUtil;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import java.util.regex.Pattern;

/**
 * Provides utility methods for JFX-based classes and applications.
 */
public class JFXUtil
{
    /**
     * Indicates a node alignment style. Used by several methods in this class.
     * Exact behavior of this enum's usage is dictated by its implementation, and is not enforced.
     * See the implementing method's documentation for more information.
     */
    public enum Alignment{
        /**
         * The node's alignment will use the negative-most coordinate of the target.
         */
        NEGATIVE,
        /**
         * The node's alignment will use the positive-most coordinate of the target.
         */
        POSITIVE,
        /**
         * The node's alignment will center to the target on the desired axis.
         */
        CENTERED,
        /**
         * The node's alignment will not be altered.
         */
        IGNORED
    }

    private static final double BASE_SCALE_SIZE = 16.0;

    /**
     * The inherited system UI scale, as calculated by generating a base UI element with a known size and measuring its actual size.
     */
    public static final double SCALE = Math.rint(new Text("").getLayoutBounds().getHeight()) / BASE_SCALE_SIZE;

    /**
     * The default spacing in pixels between UI nodes in a grid-type UI arrangement.
     * For example, this value is used by the {@link #setElementPositionInGrid(AnchorPane, Node, double, double, double, double)} method for spacing its nodes.
     * This value is auto-scaled using {@link #SCALE}.
     */
    public static final int DEFAULT_SPACING = (int)((BASE_SCALE_SIZE * 2) * SCALE);

    /**
     * The default spacing in pixels between UI nodes in a grid-type UI arrangement.
     * For example, this value is used by the {@link #setElementPositionInGrid(AnchorPane, Node, double, double, double, double)} method for spacing its nodes.
     * This value is not auto-scaled.
     */
    public static final int DEFAULT_SPACING_UNSCALED = (int)(BASE_SCALE_SIZE * 2);

    /**
     * Sets the provided {@link Node}'s position inside of the provided {@link AnchorPane}.
     * Adds the provided {@link Node} to the {@link AnchorPane} if it is not already a member of said {@link AnchorPane}.
     * Automatically compensates for scaling (retrieved from {@link #SCALE}) when setting positions. For example,
     * if the native UI scale is 1.5, and the provided value for the 'left' argument is 20, the resultant actual spacing
     * between the left side of the {@link Node} and the left drawable canvas bound will be 30 pixels.
     * Specify any value less than 0 to skip aligning that vector and leave the alignment as it was.
     * Specifying values that clash, such as providing values for both 'left' and 'right', will cause the provided {@link Node}
     * to be resized to accommodate the specified values. This may cause undocumented behavior if the values are out-of-bounds of
     * the canvas size.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be repositioned
     * @param element the {@link Node} to be repositioned
     * @param left the distance in pixels from the left side of the drawable canvas area to the left edge of the provided {@link Node}
     * @param right the distance in pixels from the right side of the drawable canvas area to the right edge of the provided {@link Node}
     * @param top the distance in pixels from the top of the drawable canvas area to the top edge of the provided {@link Node}
     * @param bottom the distance in pixels from the bottom of the drawable canvas area to the bottom edge of the provided {@link Node}
     */
    public static void setElementPosition(@NotNull AnchorPane layout, @NotNull Node element, double left, double right, double top, double bottom)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        if(left >= 0) AnchorPane.setLeftAnchor(element, left * SCALE);
        if(right >= 0) AnchorPane.setRightAnchor(element, right * SCALE);
        if(top >= 0) AnchorPane.setTopAnchor(element, top * SCALE);
        if(bottom >= 0) AnchorPane.setBottomAnchor(element, bottom * SCALE);
    }

    /**
     * Sets the provided {@link Node}'s position inside of the provided {@link AnchorPane}.
     * Adds the provided {@link Node} to the {@link AnchorPane} if it is not already a member of said {@link AnchorPane}.
     * Automatically compensates for scaling (retrieved from {@link #SCALE}) when setting positions. For example,
     * if the native UI scale is 1.5, and the provided value for the 'left' argument is 20, the resultant actual spacing
     * between the left side of the {@link Node} and the left drawable canvas bound will be 30 pixels.
     * Specify any value less than 0 to skip aligning that vector and leave the alignment as it was.
     * Specifying values that clash, such as providing values for both 'left' and 'right', will cause the provided {@link Node}
     * to be resized to accommodate the specified values. This may cause undocumented behavior if the values are out-of-bounds of
     * the canvas size.
     * This variant of the positioning method ignores the padding of the specified {@link AnchorPane}, and sets the provided
     * {@link Node}'s position relative to the edge of the drawable canvas area, as opposed to the edge of the padding area.
     * If the provided {@link AnchorPane} has no padding set, or has all padding margins set to {@code 0}, the behavior of
     * this method will be identical to calling {@link #setElementPosition(AnchorPane, Node, double, double, double, double)}.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be repositioned
     * @param element the {@link Node} to be repositioned
     * @param left the distance in pixels from the left side of the drawable canvas area to the left edge of the provided {@link Node}
     * @param right the distance in pixels from the right side of the drawable canvas area to the right edge of the provided {@link Node}
     * @param top the distance in pixels from the top of the drawable canvas area to the top edge of the provided {@link Node}
     * @param bottom the distance in pixels from the bottom of the drawable canvas area to the bottom edge of the provided {@link Node}
     */
    public static void setElementPositionIgnorePadding(@NotNull AnchorPane layout, @NotNull Node element, double left, double right, double top, double bottom)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        if(left >= 0) AnchorPane.setLeftAnchor(element, (left * SCALE) - layout.getPadding().getLeft());
        if(right >= 0) AnchorPane.setRightAnchor(element, (right * SCALE) - layout.getPadding().getRight());
        if(top >= 0) AnchorPane.setTopAnchor(element, (top * SCALE) - layout.getPadding().getTop());
        if(bottom >= 0) AnchorPane.setBottomAnchor(element, (bottom * SCALE) - layout.getPadding().getBottom());
    }

    /**
     * Sets the provided {@link Node}'s position inside of the provided {@link AnchorPane}.
     * Position values are not in pixels, rather, they are in multiples of {@link #DEFAULT_SPACING}, essentially
     * turning the canvas of the provided {@link AnchorPane} into a grid with cell sizes equal to {@link #DEFAULT_SPACING}<sup>2</sup>.
     * This sizing system automatically scales with global UI scale. See {@link #SCALE} for more information.
     * Specify any value less than 0 to skip aligning that vector and leave the alignment as it was.
     * Specifying values that clash, such as providing values for both 'left' and 'right', will cause the provided {@link Node}
     * to be resized to accommodate the specified values. This may cause undocumented behavior if the values are out-of-bounds of
     * the canvas size.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be repositioned
     * @param element the {@link Node} to be repositioned
     * @param leftGridID the number of grid spaces between the left side of the drawable canvas area and the left edge of the provided {@link Node}
     * @param rightGridID the number of grid spaces between the right side of the drawable canvas area and the right edge of the provided {@link Node}
     * @param topGridID the number of grid spaces between the top of the drawable canvas area and the top edge of the provided {@link Node}
     * @param bottomGridID the number of grid spaces between the bottom of the drawable canvas area and the bottom edge of the provided {@link Node}
     */
    public static void setElementPositionInGrid(@NotNull AnchorPane layout, @NotNull Node element, double leftGridID, double rightGridID, double topGridID, double bottomGridID)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        if(leftGridID >= 0) AnchorPane.setLeftAnchor(element, (DEFAULT_SPACING * 2 * SCALE) * leftGridID);
        if(rightGridID >= 0) AnchorPane.setRightAnchor(element, (DEFAULT_SPACING * 2 * SCALE) * rightGridID);
        if(topGridID >= 0) AnchorPane.setTopAnchor(element, (DEFAULT_SPACING * SCALE) * topGridID);
        if(bottomGridID >= 0) AnchorPane.setBottomAnchor(element, (DEFAULT_SPACING * SCALE) * bottomGridID);
    }

    /**
     * Centers an element relative to the provided {@link AnchorPane}'s drawable canvas area.
     * The provided element must be a subclass of {@link Region} - in other words, it must expose {@link Region#getWidth()}
     * and {@link Region#getHeight()} methods to enable centering calculation.
     * The element may be centered in the horizontal or vertical axis - or both.
     * Centering is done by halving the width or height of the layout's canvas area, and subtracting half of the provided
     * element's width or height from it. The element's offset is then set to the result of that calculation in the
     * left (negative-X) and/or top (negative-Y) axes. Keep in mind that any pre-existing offset settings for the right or
     * bottom axes will be preserved.
     * Shorthand call, calls {@link #setElementPositionCentered(AnchorPane, Node, Rectangle2D, boolean, boolean)}.
     * @param layout the {@link AnchorPane} in which the provided {@link Region} should be centered
     * @param element the {@link Region} to be centered
     * @param horizontal if this is {@code true}, the provided element will be horizontally centered
     * @param vertical if this is {@code true}, the provided element will be vertically centered
     */
    public static void setElementPositionCentered(@NotNull AnchorPane layout, @NotNull Region element, boolean horizontal, boolean vertical)
    {
        setElementPositionCentered(layout, element, new Rectangle2D(element.getLayoutX(), element.getLayoutY(), getNormalizedWidth(element), getNormalizedHeight(element)), horizontal, vertical);
    }

    /**
     * Centers an element relative to the provided {@link AnchorPane}'s drawable canvas area.
     * The element may be centered in the horizontal or vertical axis - or both.
     * Centering is done by halving the width or height of the layout's canvas area, and subtracting half of the provided
     * bounds' width or height from it. The element's offset is then set to the result of that calculation in the
     * left (negative-X) and/or top (negative-Y) axes. Keep in mind that any pre-existing offset settings for the right or
     * bottom axes will be preserved.
     * Note that this method only uses the {@link Rectangle2D#width width} and {@link Rectangle2D#height height} properties of the
     * provided {@link Rectangle2D}s, not their {@link Rectangle2D#minX X} and {@link Rectangle2D#minY Y} properties. The X and Y
     * properties can safely be out-of-bounds or {@code null} with no ill effects.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be centered
     * @param element the {@link Node} to be centered
     * @param elementBounds the bounds of the provided element as a {@link Rectangle2D}
     * @param horizontal if this is {@code true}, the provided element will be horizontally centered
     * @param vertical if this is {@code true}, the provided element will be vertically centered
     */
    public static void setElementPositionCentered(@NotNull AnchorPane layout, @NotNull Node element, @NotNull Rectangle2D elementBounds, boolean horizontal, boolean vertical)
    {
        if(!horizontal && !vertical) return;

        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        double x = (layout.getWidth() / 2) - (elementBounds.getWidth() / 2) - layout.getPadding().getLeft();
        double y = (layout.getHeight() / 2) - (elementBounds.getHeight() / 2) - layout.getPadding().getBottom();

        if(horizontal) AnchorPane.setLeftAnchor(element, x);
        if(vertical) AnchorPane.setBottomAnchor(element, y);
    }

    /**
     * Aligns an element with another element along their centerlines in either the X or Y axis.
     * This does not enforce any kind of proximity between the target and the candidate, only alignment.
     * For example, the following method call would align the target and the candidate along their centerlines in the X (horizontal)
     * axis:
     * {@code centerToNode(layout, target, element, Orientation.HORIZONTAL)}
     * This would end up setting the candidate's centerline Y coordinate to be equal to the target's centerline Y coordinate, while
     * leaving both nodes' X coordinates unchanged.
     * Use {@link #alignToNode(AnchorPane, Region, Region, double, Orientation, Alignment)} to align
     * nodes in proximity with each other.
     * Shorthand call, calls {@link #centerToNode(AnchorPane, Node, Rectangle2D, Node, Rectangle2D, Orientation)}.
     * @param layout the {@link AnchorPane} in which the provided {@link Region} should be aligned
     * @param target the {@link Region} to center to
     * @param element the {@link Region} to be centered
     * @param axis the {@link Orientation} in which the nodes should be centered
     */
    public static void centerToNode(@NotNull AnchorPane layout, @NotNull Region target, @NotNull Region element, @NotNull Orientation axis)
    {
        centerToNode(layout, target, new Rectangle2D(target.getLayoutX(), target.getLayoutY(), getNormalizedWidth(target), getNormalizedHeight(target)),
                element, new Rectangle2D(element.getLayoutX(), element.getLayoutY(), getNormalizedWidth(element), getNormalizedHeight(element)), axis);
    }

    /**
     * Aligns an element with another element along their centerlines in either the X or Y axis.
     * This does not enforce any kind of proximity between the target and the candidate, only alignment.
     * For example, the following method call would align the target and the candidate along their centerlines in the X (horizontal)
     * axis:
     * {@code centerToNode(layout, target, new {@link Rectangle2D}(target.getX(), target.getY(), target.getPrefWidth(), target.getPrefHeight()),
     * element, new {@link Rectangle2D}(element.getX(), element.getY(), element.getPrefWidth(), element.getPrefHeight()), Orientation.HORIZONTAL)}
     * This would end up setting the candidate's centerline Y coordinate to be equal to the target's centerline Y coordinate, while
     * leaving both nodes' X coordinates unchanged.
     * Use {@link #alignToNode(AnchorPane, Node, Rectangle2D, Node, Rectangle2D, double, Orientation, Alignment)} to align
     * nodes in proximity with each other.
     * Note that this method only uses the {@link Rectangle2D#width width} and {@link Rectangle2D#height height} properties of the
     * provided {@link Rectangle2D}s, not their {@link Rectangle2D#minX X} and {@link Rectangle2D#minY Y} properties. The X and Y
     * properties can safely be out-of-bounds or {@code null} with no ill effects.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be aligned
     * @param target the {@link Node} to center to
     * @param targetBounds the bounds of the target node, given as a {@link Rectangle2D}
     * @param element the {@link Node} to be centered
     * @param elementBounds the bounds of the candidate node, given as a {@link Rectangle2D}
     * @param axis the {@link Orientation} in which the nodes should be centered
     */
    public static void centerToNode(@NotNull AnchorPane layout, @NotNull Node target, @NotNull Rectangle2D targetBounds,
                                    @NotNull Node element, @NotNull Rectangle2D elementBounds, @NotNull Orientation axis)
    {
        if(axis == Orientation.VERTICAL) {
            AnchorPane.setLeftAnchor(element, target.getLayoutX() + (targetBounds.getWidth() / 2) - (elementBounds.getWidth() / 2) - layout.getPadding().getLeft());
        }else if(axis == Orientation.HORIZONTAL) {
            AnchorPane.setTopAnchor(element, target.getLayoutY() + (targetBounds.getHeight() / 2) - (elementBounds.getHeight() / 2) - layout.getPadding().getTop());
        }
    }

    /**
     * Aligns a {@link Region} with another {@link Region} in either the X or Y axis.
     * This alignment is not permanent, and must be updated if the target node moves or resizes for any reason.
     * Shorthand call, calls {@link #alignToNode(AnchorPane, Node, Rectangle2D, Node, Rectangle2D, double, Orientation, Alignment)}
     * with the bounds of the provided {@link Region}s as arguments.
     * Use {@link #bindAlignmentToNode(AnchorPane, Region, Region, double, Orientation, Alignment)} to enforce a constant dynamic alignment.
     * @param layout the {@link AnchorPane} in which the provided {@link Region} should be centered
     * @param target the {@link Region} to align to
     * @param element the {@link Region} to be aligned
     * @param margin the margin (in pixels) that should be kept between the target and aligned element. This is automatically scaled
     *               by {@link #SCALE}. If this is positive, the candidate node will be aligned along the positive edge of the target -
     *               underneath it if the orientation is {@link Orientation#VERTICAL}, or to the right if it is {@link Orientation#HORIZONTAL}.
     *               Likewise, if it is negative, the candidate will be aligned along the negative edge - on top if the orientation is
     *               {@link Orientation#VERTICAL}, or to the left if it is {@link Orientation#HORIZONTAL}. If the desired margin is {@code 0},
     *               but you wish to align the node in the negative axis, provide {@link Integer#MIN_VALUE} instead, since the method cannot tell
     *               the difference between {@code 0} and {@code -0}.
     * @param axis the {@link Orientation} in which the nodes should be aligned
     * @param edgeAlignment where the candidate node should be positioned relative to the target node in the non-specified direction.
     *                      For example, if you wanted to align a candidate node below the target, with its left edge aligned with the target's left edge
     *                      and a 10-pixel scaled margin, your method call would end up looking something like this:
     *                          {@code alignToNode(layout, target, element, 10.0d, Orientation.VERTICAL, Alignment.NEGATIVE)}
     *                      Similarly, this aligns the candidate to the left of the target, centered along the vertical axis,
     *                      with a 5-pixel space:
     *                          {@code alignToNode(layout, target, element, -5.0d, Orientation.HORIZONTAL, Alignment.CENTERED)}
     */
    public static void alignToNode(@NotNull AnchorPane layout, @NotNull Region target, @NotNull Region element, @Nullable double margin, 
                                   @NotNull Orientation axis, @NotNull Alignment edgeAlignment)
    {
        alignToNode(layout, target, new Rectangle2D(target.getLayoutX(), target.getLayoutY(), getNormalizedWidth(target), getNormalizedHeight(target)),
                element, new Rectangle2D(element.getLayoutX(), element.getLayoutY(), getNormalizedWidth(element), getNormalizedHeight(element)), margin, axis, edgeAlignment);
    }

    /**
     * Aligns a {@link Node} with another {@link Node} in either the X or Y axis.
     * This alignment is not permanent, and must be updated if the target node moves or resizes for any reason.
     * Note that this method only uses the {@link Rectangle2D#width width} and {@link Rectangle2D#height height} properties of the
     * provided {@link Rectangle2D}s, not their {@link Rectangle2D#minX X} and {@link Rectangle2D#minY Y} properties. The X and Y
     * properties can safely be out-of-bounds or {@code null} with no ill effects.
     * Use {@link #bindAlignmentToNode(AnchorPane, Node, Rectangle2D, Node, Rectangle2D, double, Orientation, Alignment)} to enforce a constant dynamic alignment.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be centered
     * @param target the {@link Node} to align to
     * @param targetBounds the bounds of the target node, given as a {@link Rectangle2D}
     * @param element the {@link Node} to be aligned
     * @param elementBounds the bounds of the candidate node, given as a {@link Rectangle2D}
     * @param margin the margin (in pixels) that should be kept between the target and aligned element. This is automatically scaled
     *               by {@link #SCALE}. If this is positive, the candidate node will be aligned along the positive edge of the target -
     *               underneath it if the orientation is {@link Orientation#VERTICAL}, or to the right if it is {@link Orientation#HORIZONTAL}.
     *               Likewise, if it is negative, the candidate will be aligned along the negative edge - on top if the orientation is
     *               {@link Orientation#VERTICAL}, or to the left if it is {@link Orientation#HORIZONTAL}. If the desired margin is {@code 0},
     *               but you wish to align the node in the negative axis, provide {@link Integer#MIN_VALUE} instead, since the method cannot tell
     *               the difference between {@code 0} and {@code -0}.
     * @param axis the {@link Orientation} in which the nodes should be aligned
     * @param edgeAlignment where the candidate node should be positioned relative to the target node in the non-specified direction.
     *                      For example, if you wanted to align a candidate node below the target, with its left edge aligned with the target's left edge
     *                      and a 10-pixel scaled margin, your method call would end up looking something like this:
     *                          {@code alignToNode(layout, target, element, 10.0d, Orientation.VERTICAL, Alignment.NEGATIVE)}
     *                      Similarly, this aligns the candidate to the left of the target, centered along the vertical axis,
     *                      with a 5-pixel space:
     *                          {@code alignToNode(layout, target, element, -5.0d, Orientation.HORIZONTAL, Alignment.CENTERED)}
     */
    public static void alignToNode(@NotNull AnchorPane layout, @NotNull Node target, @NotNull Rectangle2D targetBounds, 
                                   @NotNull Node element, @NotNull Rectangle2D elementBounds, @Nullable double margin, 
                                   @NotNull Orientation axis, @NotNull Alignment edgeAlignment)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);
        if(!(layout.getChildren().contains(target))) throw new IllegalStateException("Target node must be in the same Layout as the candidate node.");

        if(axis == Orientation.VERTICAL)
        {
            if(margin >= 0) AnchorPane.setTopAnchor(element, target.getLayoutY() + targetBounds.getHeight() + margin - layout.getPadding().getTop());
            else AnchorPane.setTopAnchor(element, (target.getLayoutY() - elementBounds.getHeight() + (margin == Integer.MIN_VALUE ? 0 : margin) < 0 ? 0 : target.getLayoutY() - elementBounds.getHeight() + (margin == Integer.MIN_VALUE ? 0 : margin)) - layout.getPadding().getTop());

            if(edgeAlignment == JFXUtil.Alignment.CENTERED) AnchorPane.setLeftAnchor(element, target.getLayoutX() + (targetBounds.getWidth() / 2) - (elementBounds.getWidth() / 2) - layout.getPadding().getLeft());
            else if(edgeAlignment == JFXUtil.Alignment.NEGATIVE) AnchorPane.setLeftAnchor(element, target.getLayoutX() - layout.getPadding().getLeft());
            else if(edgeAlignment == JFXUtil.Alignment.POSITIVE) AnchorPane.setLeftAnchor(element, target.getLayoutX() + targetBounds.getWidth() - elementBounds.getWidth() - layout.getPadding().getLeft());
        }else if(axis == Orientation.HORIZONTAL)
        {
            if(margin >= 0) AnchorPane.setLeftAnchor(element, target.getLayoutX() + targetBounds.getWidth() + margin - layout.getPadding().getLeft());
            else AnchorPane.setLeftAnchor(element, (target.getLayoutX() - elementBounds.getWidth() + (margin == Integer.MIN_VALUE ? 0 : margin) < 0 ? 0 : target.getLayoutX() - elementBounds.getWidth() + (margin == Integer.MIN_VALUE ? 0 : margin)) - layout.getPadding().getLeft());

            if(edgeAlignment == JFXUtil.Alignment.CENTERED) AnchorPane.setTopAnchor(element, target.getLayoutY() + (targetBounds.getHeight() / 2) - (elementBounds.getHeight() / 2) - layout.getPadding().getTop());
            else if(edgeAlignment == JFXUtil.Alignment.NEGATIVE) AnchorPane.setTopAnchor(element, target.getLayoutY() - layout.getPadding().getTop());
            else if(edgeAlignment == JFXUtil.Alignment.POSITIVE) AnchorPane.setTopAnchor(element, target.getLayoutY() + targetBounds.getHeight() - elementBounds.getHeight() - layout.getPadding().getTop());
        }
    }

    /**
     * Aligns a {@link Region} with another {@link Region} in either the X or Y axis.
     * This alignment is permanent, and does not have to be updated if the target node moves or resizes for any reason.
     * Note that this binding is done by adding a listener to the target node's X and Y properties, and will be enforced for
     * the lifetime of said listener. Remove said listener or re-initialize the target or candidate node to remove said enforcement.
     * Use {@link #alignToNode(AnchorPane, Region, Region, double, Orientation, Alignment)} to set a temporary, non-dynamic alignment.
     * @param layout the {@link AnchorPane} in which the provided {@link Region} should be centered
     * @param target the {@link Region} to align to
     * @param element the {@link Region} to be aligned
     * @param margin the margin (in pixels) that should be kept between the target and aligned element. This is automatically scaled
     *               by {@link #SCALE}. If this is positive, the candidate node will be aligned along the positive edge of the target -
     *               underneath it if the orientation is {@link Orientation#VERTICAL}, or to the right if it is {@link Orientation#HORIZONTAL}.
     *               Likewise, if it is negative, the candidate will be aligned along the negative edge - on top if the orientation is
     *               {@link Orientation#VERTICAL}, or to the left if it is {@link Orientation#HORIZONTAL}. If the desired margin is {@code 0},
     *               but you wish to align the node in the negative axis, provide {@code null} instead, since the method cannot tell
     *               the difference between {@code 0} and {@code -0}.
     * @param axis the {@link Orientation} in which the nodes should be aligned
     * @param edgeAlignment where the candidate node should be positioned relative to the target node in the non-specified direction.
     *                      For example, if you wanted to align a candidate node below the target, with its left edge aligned with the target's left edge
     *                      and a 10-pixel scaled margin, your method call would end up looking something like this:
     *                          {@code alignToNode(layout, target, element, 10.0d, Orientation.VERTICAL, Alignment.NEGATIVE)}
     *                      Similarly, this aligns the candidate to the left of the target, centered along the vertical axis,
     *                      with a 5-pixel space:
     *                          {@code alignToNode(layout, target, element, -5.0d, Orientation.HORIZONTAL, Alignment.CENTERED)}
     */
    public static void bindAlignmentToNode(@NotNull AnchorPane layout, @NotNull Region target, @NotNull Region element,
                                           @Nullable double margin, @NotNull Orientation axis, @NotNull Alignment edgeAlignment)
    {
        alignToNode(layout, target, element, margin, axis, edgeAlignment);
        ChangeListener<Number> listener = (observable, oldValue, newValue) -> alignToNode(layout, target, element, margin, axis, edgeAlignment);

        target.widthProperty().addListener(listener);
        target.heightProperty().addListener(listener);
        target.layoutXProperty().addListener(listener);
        target.layoutYProperty().addListener(listener);
    }

    /**
     * Aligns a {@link Node} with another {@link Node} in either the X or Y axis.
     * This alignment is permanent, and does not have to be updated if the target node moves or resizes for any reason.
     * Note that this binding is done by adding a listener to the target node's X and Y properties, and will be enforced for
     * the lifetime of said listener. Remove said listener or re-initialize the target or candidate node to remove said enforcement.
     * Also note that this method only uses the {@link Rectangle2D#width width} and {@link Rectangle2D#height height} properties of the
     * provided {@link Rectangle2D}s, not their {@link Rectangle2D#minX X} and {@link Rectangle2D#minY Y} properties. The X and Y
     * properties can safely be out-of-bounds or {@code null} with no ill effects.
     * Use {@link #alignToNode(AnchorPane, Node, Rectangle2D, Node, Rectangle2D, double, Orientation, Alignment)} to set a temporary, non-dynamic alignment.
     * @param layout the {@link AnchorPane} in which the provided {@link Node} should be centered
     * @param target the {@link Node} to align to
     * @param targetBounds the bounds of the target, given as a {@link Rectangle2D}
     * @param element the {@link Node} to be aligned
     * @param elementBounds the bounds of the candidate, given as a {@link Rectangle2D}
     * @param margin the margin (in pixels) that should be kept between the target and aligned element. This is automatically scaled
     *               by {@link #SCALE}. If this is positive, the candidate node will be aligned along the positive edge of the target -
     *               underneath it if the orientation is {@link Orientation#VERTICAL}, or to the right if it is {@link Orientation#HORIZONTAL}.
     *               Likewise, if it is negative, the candidate will be aligned along the negative edge - on top if the orientation is
     *               {@link Orientation#VERTICAL}, or to the left if it is {@link Orientation#HORIZONTAL}. If the desired margin is {@code 0},
     *               but you wish to align the node in the negative axis, provide {@code null} instead, since the method cannot tell
     *               the difference between {@code 0} and {@code -0}.
     * @param axis the {@link Orientation} in which the nodes should be aligned
     * @param edgeAlignment where the candidate node should be positioned relative to the target node in the non-specified direction.
     *                      For example, if you wanted to align a candidate node below the target, with its left edge aligned with the target's left edge
     *                      and a 10-pixel scaled margin, your method call would end up looking something like this:
     *                          {@code alignToNode(layout, target, targetBounds, element, elementBounds, 10.0d, Orientation.VERTICAL, Alignment.NEGATIVE)}
     *                      Similarly, this aligns the candidate to the left of the target, centered along the vertical axis,
     *                      with a 5-pixel space:
     *                          {@code alignToNode(layout, target, targetBounds, element, elementBounds, -5.0d, Orientation.HORIZONTAL, Alignment.CENTERED)}
     */
    public static void bindAlignmentToNode(@NotNull AnchorPane layout, @NotNull Node target, @NotNull Rectangle2D targetBounds,
                                           @NotNull Node element, @NotNull Rectangle2D elementBounds, @Nullable double margin,
                                           @NotNull Orientation axis, @NotNull Alignment edgeAlignment)
    {
        alignToNode(layout, target, targetBounds, element, elementBounds, margin, axis, edgeAlignment);
        ChangeListener<Number> listener = (observable, oldValue, newValue) -> alignToNode(layout, target, targetBounds, element, elementBounds, margin, axis, edgeAlignment);

        target.layoutXProperty().addListener(listener);
        target.layoutYProperty().addListener(listener);
    }

    /**
     * Clears the positioning data of a specified {@link Node}. This will effectively make it behave as if it was never positioned -
     * that is, as if it had never had any positioning methods called on it. This is done by calling the position-set method
     * for each axis with {@code null} as the value. If the provided element is not a member of an {@link AnchorPane}, this
     * will not have any effect on its positioning outside of {@link AnchorPane}s.
     * @param element the {@link Node} to be repositioned
     */
    public static void resetElementPosition(@NotNull Node element)
    {
        AnchorPane.setLeftAnchor(element, null);
        AnchorPane.setRightAnchor(element, null);
        AnchorPane.setTopAnchor(element, null);
        AnchorPane.setBottomAnchor(element, null);
    }

    private static final Pattern p = Pattern.compile("\\d+(\\.\\d+)?");

    /**
     * Limits a {@link TextField} to numerical entry only (only the digits 0-9, no spaces or other separators).
     * Will also limit the total length of the digits in the field to the specified number, unless that number is 0 or less.
     * Auto-scales the provided {@link TextField} to the correct width for the input length limit specified if a limit
     * is being imposed.
     * Input to the field from any non-user source (i.e direct method calls, edit name generation via internal logic, etc.)
     * will also be subjected to these parameters.
     * If the field's content somehow includes an illegal character, the only allowed actions will be (1) clearing the field,
     * or (2) deleting one or more characters from the field's contents.
     * @param node the {@link TextField} to limit input to
     * @param maxDigits the maximum number of digits in the input field at any given time. Will not impose a limit
     *                  if the provided value is 0 or less.
     * @return the {@link ChangeListener} that was linked to the provided text field, to aid in delimiting the field later on
     * if desired.
     */
    public static ChangeListener<? extends String> limitTextFieldToNumerical(@NotNull TextField node, int maxDigits)
    {
        node.setPrefWidth((15 * maxDigits) * SCALE);
        ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            // Ignore change if the new value is shorter to prevent the field from locking up if there are already invalid
            // characters in it and the user tries to delete some.
            if(oldValue.length() > newValue.length()) return;

            if(!p.matcher(newValue).matches() || newValue.length() > maxDigits){
                node.setText(oldValue);
            }
        };

        node.textProperty().addListener(listener);
        return listener;
    }

    /**
     * Generates a JFX {@link ImageView} object representing the specified image resource at the specified resolution.
     * Typically useful for setting the {@code graphic} properties of certain types of {@link Node}.
     * Shorthand call, calls {@link #generateGraphicFromResource(String, int, int)} with {@param resolution} as the X and Y resolution.
     * @param resourceID the relative JFX Resource URI of the image file to load
     * @param resolution the resolution that the image should be scaled to. If this is 0 or less, the image will be loaded
     *                   at its original resolution with no scaling.
     * @return the resultant {@link ImageView} object
     */
    public static ImageView generateGraphicFromResource(String resourceID, int resolution)
    {
        return generateGraphicFromResource(resourceID, resolution, resolution);
    }

    /**
     * Generates a JFX {@link ImageView} object representing the specified image resource at the specified resolution.
     * Typically useful for setting the {@code graphic} properties of certain types of {@link Node}.
     * @param resourceID the relative JFX Resource URI of the image file to load
     * @param resX the resolution that the image should be scaled to in the X axis. If this is 0 or less, the image will be loaded
     *                   at its original resolution with no scaling.
     * @param resY the resolution that the image should be scaled to in the Y axis. If this is 0 or less, the image will be loaded
     *                   at its original resolution with no scaling.
     * @return the resultant {@link ImageView} object
     */
    public static ImageView generateGraphicFromResource(String resourceID, int resX, int resY)
    {
        if(resX > 0 && resY > 0) {
            return new ImageView(new Image(resourceID,
                    resX * JFXUtil.SCALE, resY * JFXUtil.SCALE, true, true));
        }else{
            return new ImageView(new Image(resourceID));
        }
    }

    /**
     * Gets the result of {@link Region#getWidth()} or {@link Region#getPrefWidth()}, whichever is valid.
     * If neither is valid, returns the result of {@link Region#getWidth()}.
     * @param n the {@link Region} to check coordinates for
     * @return the checked width value
     */
    public static double getNormalizedWidth(Region n)
    {
        return n.getWidth() <= 0 ? n.getPrefWidth() : n.getWidth();
    }

    /**
     * Gets the result of {@link Region#getHeight()} or {@link Region#getPrefHeight()}, whichever is valid.
     * If neither is valid, returns the result of {@link Region#getHeight()}.
     * @param n the {@link Region} to check coordinates for
     * @return the checked width value
     */
    public static double getNormalizedHeight(Region n)
    {
        return n.getHeight() <= 0 ? n.getPrefHeight() : n.getHeight();
    }
}
