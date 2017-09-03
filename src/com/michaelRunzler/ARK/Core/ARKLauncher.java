import UI.ARKInterfaceDialogYN;
import system.ARKApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.ArrayList;

public class ARKLauncher extends Application
{
    private Stage window;
    private Scene menu;
    private AnchorPane layout;

    private Button launch;
    private ChoiceBox<String> programSelect;
    private ArrayList<ARKApplication> programs;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        window = primaryStage;

        window.setResizable(false);
        window.setIconified(false);
        window.setMinHeight(200);
        window.setMinWidth(200);
        window.setTitle("ARK");
        window.getIcons().addAll(new Image("assets/launcher.png"));

        window.setOnCloseRequest(e ->{
            e.consume();
            exitSystem();
        });

        launch = new Button("Launch");
        programSelect = new ChoiceBox<>();

        launch.setTooltip(new Tooltip("Launch the selected program"));
        programSelect.setTooltip(new Tooltip("The list of programs currently available to run"));

        programSelect.getItems().add("-Select Program-");
        programSelect.getSelectionModel().selectFirst();

        programs = new ArrayList<>();

        layout = new AnchorPane(launch, programSelect);
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 200, 200);
        window.setScene(menu);
        window.show();

        AnchorPane.setLeftAnchor(launch, 0.0);
        AnchorPane.setRightAnchor(launch, 0.0);
        AnchorPane.setBottomAnchor(launch, 0.0);

        AnchorPane.setLeftAnchor(programSelect, (window.getWidth() / 2) - 100);
        AnchorPane.setTopAnchor(programSelect, 0.0);

        launch.setOnAction(e ->
        {
            if(programSelect.getSelectionModel().getSelectedIndex() > 0)
            {
                Platform.runLater(() ->
                {
                    try {
                        programs.get(programSelect.getSelectionModel().getSelectedIndex() - 1).launch();
                        window.hide();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });
           }
        });

        
    }

    private void addProgram(ARKApplication cls, String name)
    {
        programSelect.getItems().add(name);
        programs.add(cls);
    }

    private void exitSystem()
    {
        if(new ARKInterfaceDialogYN("Query", "Are you sure you wish to exit?", "Yes", "No", 100, 100).display()){
            System.exit(0);
        }
    }
}
