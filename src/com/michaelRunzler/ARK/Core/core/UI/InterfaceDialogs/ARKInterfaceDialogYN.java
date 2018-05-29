package core.UI.InterfaceDialogs;

import core.CoreUtil.JFXUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ARKInterfaceDialogYN
{
    private Stage window;
    private Label label;
    private Button yesButton, noButton;
    private HBox buttonContainer;
    private AnchorPane layout;
    private Scene scene;

    public ARKInterfaceDialogYN(String title, String message, String yesText, String noText, int width, int height)
    {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setResizable(false);
        window.getIcons().addAll(new Image("core/assets/info.png"));

        buttonContainer = new HBox();

        label = new Label();
        label.setText(message);
        label.setWrapText(true);

        yesButton = new Button(yesText);
        noButton = new Button(noText);

        yesButton.setDefaultButton(true);
        noButton.setCancelButton(true);

        buttonContainer.setSpacing(10 * JFXUtil.SCALE);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setFillHeight(false);
        buttonContainer.getChildren().addAll(yesButton, noButton);

        layout = new AnchorPane();
        layout.getChildren().addAll(label, buttonContainer);
        layout.setPadding((new Insets(15, 15, 15, 15)));

        JFXUtil.setElementPositionInGrid(layout, label, 0, 0, 0, 1);
        JFXUtil.setElementPositionInGrid(layout, buttonContainer, 0, 0, -1, 0);

        if(width > 0 && height > 0)
            scene = new Scene(layout, width, height);
        else{
            double size = (message.length() > 25 ? Math.sqrt(message.length() / 25) * 75 : 75) * JFXUtil.SCALE;
            double buttonSize = (((yesText.length() + 2) * 6) + ((noText.length() + 2) * 6) + 40) * JFXUtil.SCALE;
            scene = new Scene(layout, size > buttonSize ? size : buttonSize, size + JFXUtil.DEFAULT_SPACING);
        }
        window.setScene(scene);
    }

    public ARKInterfaceDialogYN(String title, String message, String yesText, String noText)
    {
        this(title, message, yesText, noText, -1, -1);
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
