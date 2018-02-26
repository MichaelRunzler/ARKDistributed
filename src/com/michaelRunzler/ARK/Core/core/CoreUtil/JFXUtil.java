package core.CoreUtil;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import javafx.collections.MapChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

/**
 * Provides utility methods for JFX-based classes and applications.
 */
public class JFXUtil
{
    /**
     * Indicates a node alignment style. Used by several methods in this class.
     */
    public enum Alignment{
        NEGATIVE, POSITIVE, CENTERED
    }

    private static final double BASE_SCALE_SIZE = 16.0;

    /**
     * The inherited system UI scale, as calculated by generating a base UI element with a known size and measuring its actual size.
     */
    public static final double SCALE = Math.rint(new Text("").getLayoutBounds().getHeight()) / BASE_SCALE_SIZE;

    /**
     * The default spacing in pixels between UI nodes in a grid-type UI arrangement.
     * For example, this value is used by the {@link #setElementPositionInGrid(AnchorPane, Node, int, int, int, int)} method for spacing its nodes.
     */
    public static final int DEFAULT_SPACING = (int)((BASE_SCALE_SIZE * 2) * SCALE);

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
    public static void setElementPositionInGrid(@NotNull AnchorPane layout, @NotNull Node element, int leftGridID, int rightGridID, int topGridID, int bottomGridID)
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
     * @param layout the {@link AnchorPane} in which the provided {@link Region} should be centered
     * @param element the {@link Region} to be centered
     * @param horizontal if this is {@code true}, the provided element will be horizontally centered
     * @param vertical if this is {@code true}, the provided element will be vertically centered
     */
    public static void setElementPositionCentered(@NotNull AnchorPane layout, @NotNull Region element, boolean horizontal, boolean vertical)
    {
        if(!horizontal && !vertical) return;

        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        double x = (layout.getWidth() / 2) - ((element.getWidth() <= 0 ? element.getPrefWidth() : element.getWidth()) / 2) - layout.getPadding().getLeft();
        double y = (layout.getHeight() / 2) - ((element.getHeight() <= 0 ? element.getPrefHeight() : element.getHeight() / 2)) - layout.getPadding().getBottom();

        if(horizontal) AnchorPane.setLeftAnchor(element, x);
        if(vertical) AnchorPane.setBottomAnchor(element, y);
    }

    /**
     * Aligns a {@link Region} with another {@link Region} in either the X or Y axis.
     * This alignment is not permanent, and must be updated if the target node moves or resizes for any reason.
     * Use {@link #bindAlignmentToNode(AnchorPane, Region, Region, double, Orientation, Alignment)} to enforce a constant dynamic alignment.
     * @param layout the * @param layout the {@link AnchorPane} in which the provided {@link Region} should be centered
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
    public static void alignToNode(@NotNull AnchorPane layout, @NotNull Region target, @NotNull Region element, @Nullable double margin, @NotNull Orientation axis, @NotNull Alignment edgeAlignment)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);
        if(!(layout.getChildren().contains(target))) throw new IllegalStateException("Target node must be in the same Layout as the candidate node.");

        if(axis == Orientation.VERTICAL)
        {
            if(margin >= 0) AnchorPane.setTopAnchor(element, target.getLayoutY() + target.getHeight() + margin);
            else AnchorPane.setTopAnchor(element, target.getLayoutY() + (margin == Integer.MIN_VALUE ? 0 : margin));

            if(edgeAlignment == JFXUtil.Alignment.CENTERED) AnchorPane.setLeftAnchor(element, target.getLayoutX() + (target.getWidth() / 2) - (element.getWidth() / 2) - layout.getPadding().getLeft());
            else if(edgeAlignment == JFXUtil.Alignment.NEGATIVE) AnchorPane.setLeftAnchor(element, target.getLayoutX() - layout.getPadding().getLeft());
            else if(edgeAlignment == JFXUtil.Alignment.POSITIVE) AnchorPane.setLeftAnchor(element, target.getLayoutX() + target.getWidth() - element.getWidth() - layout.getPadding().getLeft());
        }else if(axis == Orientation.HORIZONTAL)
        {
            if(margin >= 0) AnchorPane.setLeftAnchor(element, target.getLayoutX() + target.getWidth() + margin);
            else AnchorPane.setLeftAnchor(element, target.getLayoutX() - (margin == Integer.MIN_VALUE ? 0 : margin));

            if(edgeAlignment == JFXUtil.Alignment.CENTERED) AnchorPane.setTopAnchor(element, target.getLayoutY() + (target.getHeight() / 2) - (element.getHeight() / 2) - layout.getPadding().getTop());
            else if(edgeAlignment == JFXUtil.Alignment.NEGATIVE) AnchorPane.setTopAnchor(element, target.getLayoutY() - layout.getPadding().getTop());
            else if(edgeAlignment == JFXUtil.Alignment.POSITIVE) AnchorPane.setTopAnchor(element, target.getLayoutY() + target.getHeight() - element.getHeight() - layout.getPadding().getTop());
        }
    }

    /**
     * Aligns a {@link Region} with another {@link Region} in either the X or Y axis.
     * This alignment is permanent, and does not have to be updated if the target node moves or resizes for any reason.
     * Note that this binding is done by adding a listener to the target node's X and Y properties, and will be enforced for
     * the lifetime of said listener. Remove said listener or re-initialize the target or candidate node to remove said enforcement.
     * Use {@link #alignToNode(AnchorPane, Region, Region, double, Orientation, Alignment)} to set a temporary, non-dynamic alignment.
     * @param layout the * @param layout the {@link AnchorPane} in which the provided {@link Region} should be centered
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
    public static void bindAlignmentToNode(@NotNull AnchorPane layout, @NotNull Region target, @NotNull Region element, @Nullable double margin, @NotNull Orientation axis, @NotNull Alignment edgeAlignment)
    {
        alignToNode(layout, target, element, margin, axis, edgeAlignment);
        target.getProperties().addListener((MapChangeListener<Object, Object>) change -> alignToNode(layout, target, element, margin, axis, edgeAlignment));
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

    /**
     * Limits a {@link TextField} to numerical entry only (only the digits 0-9, no spaces or other separators).
     * Will also limit the total length of the digits in the field to the specified number, unless that number is 0 or less.
     * Auto-scales the provided {@link TextField} to the correct width for the input length limit specified if a limit
     * is being imposed.
     * @param node the {@link TextField} to limit input to
     * @param maxDigits the maximum number of digits in the input field at any given time. Will not impose a limit
     *                  if the provided value is 0 or less.
     */
    public static void limitTextFieldToNumerical(@NotNull TextField node, int maxDigits)
    {
        node.setPrefWidth((15 * maxDigits) * SCALE);
        node.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue.matches("\\d*") || newValue.length() > maxDigits){
                node.setText(oldValue);
            }
        });
    }

    /**
     * Generates a JFX {@link ImageView} object representing the specified image resource at the specified resolution.
     * Typically useful for setting the {@code graphic} properties of certain types of {@link Node}.
     * @param resourceID the relative JFX Resource URI of the image file to load
     * @param resolution the resolution that the image should be scaled to. If this is 0 or less, the image will be loaded
     *                   at its original resolution with no scaling.
     * @return the resultant {@link ImageView} object
     */
    public static ImageView generateGraphicFromResource(String resourceID, int resolution)
    {
        if(resolution > 0) {
            return new ImageView(new Image(resourceID,
                    resolution * JFXUtil.SCALE, resolution * JFXUtil.SCALE, true, true));
        }else{
            return new ImageView(new Image(resourceID));
        }
    }
}
