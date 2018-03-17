package core.UI;

import core.CoreUtil.JFXUtil;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ARKInterfaceDialog
{

    private Stage window;
    private Label label;
    private Button yesButton, noButton;
    private TextField input;
    private AnchorPane layout;
    private Scene scene;

    public ARKInterfaceDialog(String title, String message, String yesText, String noText, String textBoxText, int width, int height)
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

        input = new TextField();
        input.setPromptText(textBoxText);

        yesButton.setDefaultButton(true);
        noButton.setCancelButton(true);

        layout = new AnchorPane();
        layout.getChildren().addAll(label, yesButton, noButton, input);
        layout.setPadding((new Insets(15, 15, 15, 15)));

        AnchorPane.setLeftAnchor(input, 10.0);
        AnchorPane.setRightAnchor(input, 10.0);
        AnchorPane.setBottomAnchor(yesButton, 0.0);
        AnchorPane.setRightAnchor(yesButton, 0.0);
        AnchorPane.setBottomAnchor(noButton, 0.0);
        AnchorPane.setLeftAnchor(noButton, 0.0);
        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);

        if(width > 0 && height > 0) {
            scene = new Scene(layout, width, height);
            AnchorPane.setBottomAnchor(input, height * 0.4);
        }
        else{
            double size = (message.length() > 25 ? Math.sqrt(message.length() / 25) * 80 : 80) * JFXUtil.SCALE;
            scene = new Scene(layout, size, size);

            AnchorPane.setBottomAnchor(input, (double)JFXUtil.DEFAULT_SPACING * 1.1);
        }
        window.setScene(scene);
    }

    public ARKInterfaceDialog(String title, String message, String yesText, String noText, String textBoxText)
    {
        this(title, message, yesText, noText, textBoxText, -1, -1);
    }

    private String answer;

    public String display()
    {
        window.setOnCloseRequest(e -> {
            answer = "";
            e.consume();
            window.close();
        });

        yesButton.setOnAction(e -> {
            answer = input.getText();
            window.close();
        });
        noButton.setOnAction(e -> {
            answer = "";
            window.close();
        });

        window.showAndWait();

        return answer;
    }

}
