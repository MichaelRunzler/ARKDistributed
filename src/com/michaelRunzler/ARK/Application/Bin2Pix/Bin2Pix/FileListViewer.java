package Bin2Pix;

import core.system.ARKManagerInterface;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;

class FileListViewer implements ARKManagerInterface
{
    private Stage window;
    private Scene scene;
    private AnchorPane layout;
    private Button close;
    private ListView<String> list;

    private final double BASE_SCALE_SIZE = 16.0;
    private final double SCALE = Math.rint(new Text("").getLayoutBounds().getHeight()) / BASE_SCALE_SIZE;

    public FileListViewer(int width, int height)
    {
        window = new Stage();
        window.setTitle("");
        window.setResizable(true);
        window.setHeight(height);
        window.setWidth(width);
        window.getIcons().add(new Image("core/assets/info.png"));

        close = new Button("Close");
        list = new ListView<>();
        list.setEditable(false);

        layout = new AnchorPane(close, list);
        layout.setPadding(new Insets(15, 15, 15, 15));
        scene = new Scene(layout, width, height);

        AnchorPane.setLeftAnchor(close, 0.0);
        AnchorPane.setRightAnchor(close, 0.0);
        AnchorPane.setBottomAnchor(close, 0.0);

        AnchorPane.setLeftAnchor(list, 0.0);
        AnchorPane.setRightAnchor(list, 0.0);
        AnchorPane.setTopAnchor(list, 0.0);

        window.setOnCloseRequest(e -> {
            e.consume();
            hide();
        });

        close.setOnAction(e -> hide());

        scene.heightProperty().addListener((observable, oldValue, newValue) -> list.setPrefHeight(newValue.doubleValue() - (60 * SCALE)));

        window.setScene(scene);
    }

    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    public void display() {
        if (!window.isShowing()) {
            window.show();
        }
    }

    /**
     * Hides this window if it is not already hidden.
     */
    public void hide() {
        if (window.isShowing()) {
            window.hide();
        }
    }

    /**
     * Returns this Manager's visibility state.
     *
     * @return true if this window is being displayed, false if otherwise
     */
    public boolean getVisibilityState() {
        return window.isShowing();
    }

    public void updateFileList(ArrayList<File> list)
    {
        if(list != null){
            this.list.getItems().clear();
            for(File f : list) this.list.getItems().add(f.getName());
        }
    }
}