package core.UI.InterfaceDialogs;

import core.CoreUtil.JFXUtil;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ARKInterfaceAlert
{
    private Stage window;
    private Label label;
    private Button closeButton;
    private AnchorPane layout;
    private Scene scene;

    public ARKInterfaceAlert(String title, String message, int width, int height)
    {
        window = new Stage();

        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setResizable(false);
        window.getIcons().addAll(new Image("core/assets/warning.png"));

        label = new Label();
        label.setText(message);
        label.setWrapText(true);

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> window.close());
        closeButton.setCancelButton(true);

        layout = new AnchorPane();
        layout.getChildren().addAll(label, closeButton);
        layout.setPadding((new Insets(15, 15, 15, 15)));

        JFXUtil.setElementPositionInGrid(layout, closeButton, 0, 0, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, label, 0, 0, 0, 1);

        if(width > 0 && height > 0)
            scene = new Scene(layout, width, height);
        else{
            double size = (message.length() > 25 ? Math.sqrt(message.length() / 25) * 75 : 75) * JFXUtil.SCALE;
            scene = new Scene(layout, size, size + JFXUtil.DEFAULT_SPACING);
        }
        window.setScene(scene);
    }

    public ARKInterfaceAlert(String title, String message)
    {
        this(title, message, -1, -1);
    }

    public void display()
    {
        window.setScene(scene);
        window.showAndWait();
    }

}
