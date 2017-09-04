package R34;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import core.system.ARKManagerInterface;

/**
 * Provides an interface for managing a short multi-page help manual for the retrieval program.
 */
public class R34IntegratedHelpSectionManager implements ARKManagerInterface
{

    private Stage window;
    private Scene scene;
    private AnchorPane layout;
    private Button close;
    private Button prev;
    private Button next;
    private Label help;
    private Label help2;

    private int page;

    /**
     * Creates a new Manager object of this type.
     * @param title the title of the window
     */
    public R34IntegratedHelpSectionManager(String title)
    {
        page = 1;

        window = new Stage();
        window.setTitle(title);
        window.setResizable(false);
        window.setHeight(400);
        window.setWidth(500);
        window.getIcons().add(new Image("core/assets/info.png"));

        close = new Button("Close");
        prev = new Button("Previous Page");
        next = new Button("Next Page");

        help = new Label("Help (Page 1) \n" +
                         "\n" +
                         "Welcome to the Automated Image Retrieval Utility! The following information \n" +
                         "will help you get started with the program. \n" +
                         "\n" +
                         "1. Mode Select: This is used to set the program's operating mode.\n" +
                         "Manual mode allows you to pull images from a specific repo and tag\n" +
                         "each time, if you want to see what is available or only have one tag\n" +
                         "you want to search. Auto mode uses a configurable list of 'Rules', \n" +
                         "each of which has a separate set of settings. In Auto mode, when \n" +
                         "retrieval is initiated, all Rules in the list are pulled in succession.");

        help2 = new Label("Help (Page 2) \n" +
                          "\n" +
                          "2. Log Window: This is the live view of all system activity. Any\n" +
                          "action taken by the retrieval system will show up here as a log entry.\n" +
                          "Actions will also be logged to a file on disk, this is configurable \n" +
                          "in Options.\n" +
                          "\n" +
                          "3. Options Window: This window allows the user to change various\n" +
                          "system options. Options include system index hashing, live log\n" +
                          "trimming, file logging, and other such settings.");

        layout = new AnchorPane(close, next, help);
        layout.setPadding(new Insets(15, 15, 15, 15));
        scene = new Scene(layout, 500, 500);

        AnchorPane.setLeftAnchor(close, (window.getWidth() / 2) - 40);
        AnchorPane.setBottomAnchor(close, 0.0);

        AnchorPane.setLeftAnchor(prev, 0.0);
        AnchorPane.setBottomAnchor(prev, 0.0);

        AnchorPane.setRightAnchor(next, 0.0);
        AnchorPane.setBottomAnchor(next, 0.0);

        AnchorPane.setLeftAnchor(help, 0.0);
        AnchorPane.setTopAnchor(help, 0.0);

        AnchorPane.setLeftAnchor(help2, 0.0);
        AnchorPane.setTopAnchor(help2, 0.0);

        close.setOnAction(e -> window.close());

        next.setOnAction(e ->
        {
            page ++;

            if(page == 2){
                layout.getChildren().remove(help);
                layout.getChildren().add(help2);
                layout.getChildren().add(prev);
            }
        });

        prev.setOnAction(e ->
        {
            page --;

            if(page == 1){
                layout.getChildren().remove(help2);
                layout.getChildren().add(help);
                layout.getChildren().remove(prev);
            }
        });

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


}
