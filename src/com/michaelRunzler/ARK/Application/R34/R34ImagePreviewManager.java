import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import system.ARKManagerInterface;

public class R34ImagePreviewManager implements ARKManagerInterface
{
    private Stage window;
    private Scene scene;
    private AnchorPane layout;
    private ImageView preview;

    /**
     * Creates a new instance of this Manager object
     * @param title the title of the window
     * @param x the x position of the window
     * @param y the y position of the window
     */
    public R34ImagePreviewManager(String title, int width, int height, double x, double y)
    {
        window = new Stage();
        window.setTitle(title);
        window.setResizable(false);
        window.setX(x);
        window.setY(y);
        window.setWidth(width);
        window.setHeight(height);
        window.getIcons().add(new Image("assets/info.png"));

        window.setOnCloseRequest(e -> hide());

        preview = new ImageView();

        preview.setPreserveRatio(true);
        preview.setFitHeight(width);
        preview.setFitWidth(height);

        layout = new AnchorPane(preview);
        scene = new Scene(layout, width, height);

        AnchorPane.setTopAnchor(preview, 0.0);
        AnchorPane.setLeftAnchor(preview, 0.0);

        window.setScene(scene);
    }

    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    public void display()
    {
        if(!window.isShowing()){
            window.show();
        }
    }

    /**
     * Hides this window if it is not already hidden.
     */
    public void hide()
    {
        if(window.isShowing()){
            window.hide();
        }
    }

    /**
     * Returns this Manager's visibility state.
     * @return true if this window is being displayed, false if otherwise
     */
    public boolean getVisibilityState()
    {
        return window.isShowing();
    }

    /**
     * Sets the image to be used by the preview manager.
     * @param image the image to preview
     */
    public void setImage(Image image){
        if(image != null) {
            preview.setImage(image);
            double width = preview.getBoundsInLocal().getWidth();
            double height = preview.getBoundsInLocal().getHeight();
            window.setWidth(width);
            window.setHeight(height);
            layout.setPrefWidth(width);
            layout.setPrefHeight(height); //todo fix not resizing window
        }
    }
}
