import UI.ARKInterfaceAlert;
import com.sun.istack.internal.NotNull;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ARKAutoupdater extends Application
{
    private Stage window;
    private Scene menu;
    private AnchorPane layout;

    private Button selectSource;
    private Button update;
    private Button close;
    private TextField srcDisplay;
    private Label srcDesc;
    private Label desc;

    private FileChooser srcSelect;

    private File currentTarget;
    private static final String UPDATE_URL_INDEX = "https://github.com/MichaelRunzler/ARKDistributed/";

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

        });

        currentTarget = null;

        selectSource = new Button("Browse...");
        update = new Button("Update");
        close = new Button("Close");
        srcDisplay = new TextField();
        srcDesc = new Label("Target JAR file:");
        desc = new Label("ARK Autoupdater Utility");

        srcDisplay.setEditable(false);
        srcDisplay.setPromptText("Target JAR");

        srcSelect = new FileChooser();

        layout = new AnchorPane(selectSource, update, close, srcDisplay, srcDesc, desc);
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 200, 200);
        window.setScene(menu);
        window.show();

        setNodeAlignment(selectSource, -1, 0, -1, 80);
        setNodeAlignment(update, 20, 20, -1, 40);
        setNodeAlignment(close, 40, 40, -1, 0);
        setNodeAlignment(srcDisplay, 0, 80, -1, 80);
        setNodeAlignment(srcDesc, 30, -1, -1, 120);
        setNodeAlignment(desc, 10, -1, 0, -1);

        close.setOnAction(e ->{

        });

        selectSource.setOnAction(e ->{
            File f = srcSelect.showOpenDialog(window);

            if(f == null || !f.exists()) return;

            currentTarget = f;
            srcDisplay.setText(f.getName());
        });

        update.setOnAction(e ->{
            if(currentTarget == null || !currentTarget.exists() || !currentTarget.getName().contains(".jar")){
                new ARKInterfaceAlert("Notice", "Select a valid target JAR file first!", 100, 100).display();
                return;
            }

            ArrayList<String> classNames = null;

            // JAR inventory function sourced from GitHub user 'sigpwned'
            try {
                classNames = new ArrayList<String>();
                ZipInputStream zip = new ZipInputStream(new FileInputStream("/path/to/jar/file.jar"));
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        // This ZipEntry represents a class. Now, what class does it represent?
                        String className = entry.getName().replace('/', '.'); // including ".class"
                        classNames.add(className.substring(0, className.length() - ".class".length()));
                    }
                }
            }catch (IOException e1){
                new ARKInterfaceAlert("Warning", "Could not load class list from JAR file!", 100, 100).display();
                return;
            }

            for(String s : classNames){
                switch (s)
                {
                    case "R34UI.class":
                        break;
                    case "RPlanner.class":
                        break;
                }
            }
        });
    }

    /**
     * Sets the alignment of a JavaFX Node in the layout pane. Utility method to simplify code.
     * Set an offset number to -1 to leave that axis at default (the equivalent of not calling the
     * method on that axis for this node).
     * @param n the node to set positioning for
     * @param left the left offset
     * @param right the right offset
     * @param top the top offset
     * @param bottom you get the idea, right?
     */
    public void setNodeAlignment(@NotNull Node n, int left, int right, int top, int bottom)
    {
        if(left >= 0)
            AnchorPane.setLeftAnchor(n, (double)left);
        if(right >= 0)
            AnchorPane.setRightAnchor(n, (double)right);
        if(top >= 0)
            AnchorPane.setTopAnchor(n, (double)top);
        if(bottom >= 0)
            AnchorPane.setBottomAnchor(n, (double)bottom);
    }
}
