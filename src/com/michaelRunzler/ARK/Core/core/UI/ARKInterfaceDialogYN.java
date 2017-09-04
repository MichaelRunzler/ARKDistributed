package core.UI;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ARKInterfaceDialogYN
{

    private Stage window;
    private Label label;
    private Button yesButton, noButton;
    private AnchorPane layout;
    private Scene scene;

    public ARKInterfaceDialogYN(String title, String message, String yesText, String noText, int width, int height)
    {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setResizable(false);
        window.getIcons().addAll(new Image("core/assets/info.png"));

        label = new Label();
        label.setText(message);
        label.setWrapText(true);

        yesButton = new Button(yesText);
        noButton = new Button(noText);

        yesButton.setDefaultButton(true);
        noButton.setCancelButton(true);

        layout = new AnchorPane();
        layout.getChildren().addAll(label, yesButton, noButton);
        layout.setPadding((new Insets(15, 15, 15, 15)));

        AnchorPane.setBottomAnchor(yesButton, 0.0);
        AnchorPane.setRightAnchor(yesButton, 0.0);
        AnchorPane.setBottomAnchor(noButton, 0.0);
        AnchorPane.setLeftAnchor(noButton, 0.0);
        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);

        scene = new Scene(layout, width, height);
        window.setScene(scene);
    }

    boolean answer;

    public boolean display()
    {
        window.setOnCloseRequest(e -> {
            answer = false;
            e.consume();
            window.close();
        });

        yesButton.setOnAction(e -> {
            answer = true;
            window.close();
        });
        noButton.setOnAction(e -> {
            answer = false;
            window.close();
        });

        window.showAndWait();

        return answer;
    }

}
