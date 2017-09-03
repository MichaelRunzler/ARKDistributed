import UI.ARKInterfaceAlert;
import UI.ARKInterfaceDialogYN;
import com.sun.istack.internal.NotNull;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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
import util.ARKArrayUtil;
import util.RetrievalTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
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
    private Label status;

    private FileChooser srcSelect;

    private File currentTarget;
    private Service<Void> updateTask;
    private static final String UPDATE_URL_INDEX = "https://github.com/MichaelRunzler/ARKDistributed/blob/master/UpdateIndex.vcsi";

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
            if (updateTask != null && updateTask.isRunning()) {
                new ARKInterfaceAlert("Warning", "Update task is still running!", 100, 100).display();
                return;
            }
            System.exit(0);
        });

        currentTarget = null;

        selectSource = new Button("Browse...");
        update = new Button("Update");
        close = new Button("Close");
        srcDisplay = new TextField();
        srcDesc = new Label("Target JAR file:");
        desc = new Label("ARK Autoupdater Utility");
        status = new Label("Status: Ready");

        srcDisplay.setEditable(false);
        srcDisplay.setPromptText("Target JAR");
        srcSelect.setTitle("Select JAR to update");
        srcSelect.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable JAR file", "*.jar"));
        srcSelect.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

        srcSelect = new FileChooser();
        srcSelect.setInitialDirectory(new File(System.getProperty("user.home")));

        layout = new AnchorPane(selectSource, update, close, srcDisplay, srcDesc, desc, status);
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 200, 200);
        window.setScene(menu);
        window.show();

        setNodeAlignment(selectSource, -1, 0, -1, 80);
        setNodeAlignment(update, 20, 20, -1, 40);
        setNodeAlignment(close, 40, 40, -1, 0);
        setNodeAlignment(srcDisplay, 0, 80, -1, 80);
        setNodeAlignment(srcDesc, 5, -1, -1, 120);
        setNodeAlignment(desc, 5, -1, 0, -1);
        setNodeAlignment(status, 5, -1, 20, -1);

        close.setOnAction(e ->{
            if (updateTask != null && updateTask.isRunning()) {
                new ARKInterfaceAlert("Warning", "Update task is still running!", 100, 100).display();
                return;
            }
            System.exit(0);
        });

        selectSource.setOnAction(e ->{
            File f = srcSelect.showOpenDialog(window);

            if(f == null || !f.exists()) return;

            currentTarget = f;
            srcDisplay.setText(f.getName());
        });

        update.setOnAction(e ->{
            updateTask = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            status.setText("Status: Checking for Update");
                            runUpdate();
                            status.setText("Status: Complete");
                            return null;
                        }
                    };
                }
            };

            updateTask.restart();

            updateTask.setOnFailed(e1 ->{
                updateTask.getException().printStackTrace();
                new ARKInterfaceAlert("Warning", "Error while updating JAR file.", 100, 100).display();
            });

            updateTask.setOnSucceeded(e1 ->{
                new ARKInterfaceAlert("Notice", "Update complete!", 100, 100);
            });
        });
    }

    private void runUpdate() throws IOException
    {
        // Check to make sure that the current target is a valid JAR file.
        if(currentTarget == null || !currentTarget.exists() || !currentTarget.getName().contains(".jar")){
            new ARKInterfaceAlert("Notice", "Select a valid target JAR file first!", 100, 100).display();
            return;
        }

        // Inventory the JAR file to see what classes it contains in its root.
        ArrayList<String> classNames;

        // JAR inventory function sourced from GitHub user 'sigpwned'
        try {
            classNames = new ArrayList<>();
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

        // Set the identifier code based on what we find in the JAR.
        String appIdentifier = "";
        for(String s : classNames)
        {
            switch (s){
                case "R34UI.class":
                    appIdentifier = "R34UI";
                    break;
                case "RPlanner.class":
                    appIdentifier = "RP";
                    break;
                case "IPSearch.class":
                    appIdentifier = "IP";
                    break;
            }

            if(!appIdentifier.equals("")){
                break;
            }
        }

        // If we couldn't determine what application is selected, tell the user and return.
        if(appIdentifier.isEmpty()){
            new ARKInterfaceAlert("Warning", "Selected JAR file is not a supported ARK application!", 100, 100).display();
            return;
        }

        // At this point, we have a valid application JAR, query GitHub for the update master index file.
        HashMap<String, String> masterIndexMap = new HashMap<>();

        String indexCache = ARKArrayUtil.charArrayToString(ARKArrayUtil.byteToCharArray(RetrievalTools.getBytesFromURL(UPDATE_URL_INDEX)));

        // Parse the cached index to get all of its code entries and corresponding URLs.
        Scanner parser = new Scanner(indexCache);
        parser.useDelimiter("\n");

        while(parser.hasNext()){
            String line = parser.next();
            masterIndexMap.put(line.substring(0, line.indexOf(">") - 1), line.substring(line.indexOf(">"), line.length()));
        }

        // If the master index does not contain an entry for the detected application JAR, tell the user and return.
        if(!masterIndexMap.containsKey(appIdentifier)){
            new ARKInterfaceAlert("Notice", "Selected JAR is a valid ARK application, but does not support autoupdate at this time.", 150, 150).display();
            return;
        }

        // Check to see if the target file and the remote copy specified by the master index are the same size (byte-level accuracy).
        // If they are, they should be the same version, tell the user such. If not, proceed to the update process.
        if(RetrievalTools.getRemoteFileSize(new URL(masterIndexMap.get(appIdentifier))) == currentTarget.length()){
            new ARKInterfaceAlert("Notice", "Application is up to date!", 100, 100).display();
            return;
        }

        // Check with the user to see what they would like to do with the new version.
        File downloadTarget;
        if(new ARKInterfaceDialogYN("Query", "Would you like to replace the existing JAR, or download " +
                "the new version somewhere else?", "Replace", "Elsewhere", 150, 150).display())
        {
            downloadTarget = currentTarget;
            if (downloadTarget.exists() && !downloadTarget.delete()) {
                new ARKInterfaceAlert("Notice", "Couldn't delete the existing JAR! Specify another location.", 120, 120).display();
            }

        }else{
            FileChooser targetChooser = new FileChooser();
            targetChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            targetChooser.setTitle("Select Destination");
            targetChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable JAR file", "*.jar"));
            targetChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

            File f = targetChooser.showSaveDialog(window);
            if (f == null) {
                return;
            } else if (!f.getParentFile().exists()) {
                new ARKInterfaceAlert("Notice", "Specified save location is invalid!", 100, 100).display();
                return;
            }

            if (f.exists() && !f.delete()) {
                new ARKInterfaceAlert("Notice", "Couldn't delete the existing JAR!", 120, 120).display();
                return;
            }

            downloadTarget = f;
        }

        status.setText("Status: Downloading Update");

        RetrievalTools.getFileFromURL(masterIndexMap.get(appIdentifier), downloadTarget, true);

        new ARKInterfaceAlert("Notice", "Download complete!", 100, 100).display();
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
    private void setNodeAlignment(@NotNull Node n, int left, int right, int top, int bottom)
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
