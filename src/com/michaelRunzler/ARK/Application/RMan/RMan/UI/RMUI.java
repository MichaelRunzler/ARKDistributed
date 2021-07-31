package RMan.UI;

import RMan.Core.Types.Card;
import RMan.Core.Types.Component;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class RMUI extends Application
{
    // JavaFX window elements and nodes
    private Stage window;
    private Scene menu;
    private AnchorPane layout;

    private ListView<Card> cards;
    private ListView<Component> components;

    @Override
    public void start(Stage primaryStage)
    {
        window = new Stage();

        window.setResizable(true);
        window.setIconified(false);
        window.setMinHeight(300);
        window.setMinWidth(300);
        window.setX((Screen.getPrimary().getBounds().getWidth() / 2) - 300);
        window.setY((Screen.getPrimary().getBounds().getHeight() / 2) - 30);
        window.setTitle("Recipe Card Manager");
        window.getIcons().add(new Image("RMan/assets/main.png"));

        window.setOnCloseRequest(e ->{
            e.consume();
            shutDown();
        });

        layout = new AnchorPane();
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 300, 300);
        window.setScene(menu);
        window.show();

        
    }

    private void shutDown() //todo necessary?
    {

    }
}
