package core.CoreUtil;

import com.sun.istack.internal.NotNull;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

/**
 * Provides utility methods for JFX-based classes and applications.
 */
public class JFXUtil
{
    private static final double BASE_SCALE_SIZE = 16.0;

    /**
     * The inherited system UI scale, as calculated by generating a base UI element with a known size and measuring its actual size.
     */
    public static double SCALE = Math.rint(new Text("").getLayoutBounds().getHeight()) / BASE_SCALE_SIZE;

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
