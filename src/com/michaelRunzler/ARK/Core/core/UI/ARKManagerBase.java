package core.UI;

import com.sun.istack.internal.NotNull;
import core.system.ARKManagerInterface;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Provides a convenient base UI class for classes implementing the {@link ARKManagerInterface}.
 */
public abstract class ARKManagerBase implements ARKManagerInterface
{
    protected Stage window;
    protected Scene scene;
    protected AnchorPane layout;

    /**
     * Initializes a new instance of this class.
     * This class does not include any UI elements or functionality by default. Subclasses must
     * provide their own UI nodes and interactivity clauses.
     * By default, the window represented by this object will be resizable, start in the hidden state,
     * and use the icon defined by {@code info.png} icon in the Core Assets directory.
     * These properties can be changed by calling the respective methods or fields from subclasses, or overriding
     * their respective methods.
     * @param title the title of this Manager window
     * @param width the initial width of this Manager window
     * @param height the initial height of this Manager window
     * @param x the initial X coordinate of this Manager window
     * @param y the initial Y coordinate of this Manager window
     */
    protected ARKManagerBase(String title, int width, int height, double x, double y)
    {
        window = new Stage();
        window.setTitle(title);
        window.setResizable(true);
        window.setHeight(height);
        window.setWidth(width);
        window.setX(x);
        window.setY(y);
        window.getIcons().add(new Image("core/assets/info.png"));

        layout = new AnchorPane();
        layout.setPadding(new Insets(15, 15, 15, 15));

        scene = new Scene(layout, width, height);

        window.setOnCloseRequest(e -> hide());

        window.setScene(scene);
    }

    /**
     * Sets the icon for this Manager window. Clears any previous icons before setting the new icon.
     * For more advanced icon operations, use a direct reference to the {@link #window} field.
     * @param icon the new icon
     */
    protected void setIcon(@NotNull Image icon){
        window.getIcons().clear();
        window.getIcons().add(icon);
    }

    @Override
    public void display() {
        if(!window.isShowing()) {
            window.show();
        }
    }

    @Override
    public void hide() {
        if(window.isShowing()){
            window.hide();
        }
    }

    @Override
    public boolean getVisibilityState() {
        return window.isShowing();
    }
}
