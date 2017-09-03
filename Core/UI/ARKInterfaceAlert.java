package UI;

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
        window.getIcons().addAll(new Image("com/michaelRunzler/ARK/assets/warning.png"));

        label = new Label();
        label.setText(message);
        label.setWrapText(true);

        closeButton = new Button("Close");
        closeButton.setOnAction(e -> window.close());
        closeButton.setCancelButton(true);

        layout = new AnchorPane();
        layout.getChildren().addAll(label, closeButton);
        layout.setPadding((new Insets(15, 15, 15, 15)));

        AnchorPane.setBottomAnchor(closeButton, 0.0);
        AnchorPane.setLeftAnchor(closeButton, 0.0);
        AnchorPane.setRightAnchor(closeButton, 0.0);
        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);

        scene = new Scene(layout, width, height);
    }

    public void display()
    {
        window.setScene(scene);
        window.showAndWait();
    }

}
