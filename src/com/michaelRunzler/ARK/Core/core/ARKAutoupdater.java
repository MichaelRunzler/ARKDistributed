package core;

import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.IOTools;
import core.UI.ARKInterfaceAlert;
import core.UI.ARKInterfaceDialogYN;
import com.sun.istack.internal.NotNull;
import javafx.application.Application;
import javafx.application.Platform;
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
import core.system.ARKTransThreadTransport;
import core.system.ARKTransThreadTransportHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
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
    private Service<String> updateTask;
    private static final String UPDATE_URL_INDEX = "https://raw.githubusercontent.com/MichaelRunzler/ARKDistributed/master/UpdateIndex.vcsi";

    private ARKTransThreadTransportHandler dispatcher;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        dispatcher = new ARKTransThreadTransportHandler(null);

        window = primaryStage;

        window.setResizable(false);
        window.setIconified(false);
        window.setMinHeight(200);
        window.setMinWidth(200);
        window.setTitle("ARK");
        window.getIcons().addAll(new Image("core/assets/launcher.png"));

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

        srcSelect = new FileChooser();
        srcSelect.setInitialDirectory(new File(System.getProperty("user.home")));
        srcSelect.setTitle("Select JAR to update");
        srcSelect.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable JAR file", "*.jar"));
        srcSelect.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

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

        update.setOnAction(e ->
        {
            updateTask = new Service<String>() {
                @Override
                protected Task<String> createTask() {
                    return new Task<String>() {
                        @Override
                        protected String call() throws Exception {
                            updateMTStatus("Status: Checking for Update");
                            return checkVersion();
                        }
                    };
                }
            };

            // Start the update check task.
            updateTask.restart();

            // If the update check failed, tell the user and return to the main menu.
            updateTask.setOnFailed(e1 ->{
                if(updateTask.getException() instanceof ARKTransThreadTransport){
                    ((ARKTransThreadTransport) updateTask.getException()).handleTransportPacket();
                }else{
                    updateTask.getException().printStackTrace();
                    new ARKInterfaceAlert("Warning", "Error while updating JAR file.", 100, 100).display();
                }
            });

            // If the update check succeeded, proceed to the next prompt and the download phase.
            updateTask.setOnSucceeded(e1 ->
            {
                // If for some reason the task finished without throwing an exception and has no value, return silently.
                if(updateTask.getValue() == null){
                    return;
                }

                // Check with the user to see what they would like to do with the new version.
                File downloadTarget = null;
                boolean doFileCheck = true;
                if(new ARKInterfaceDialogYN("Query", "Would you like to replace the existing JAR, or download " +
                        "the new version somewhere else?", "Replace", "Elsewhere", 200, 150).display())
                {
                    downloadTarget = currentTarget;
                    if (downloadTarget.exists() && !downloadTarget.delete()) {
                        new ARKInterfaceAlert("Warning", "Couldn't delete the existing JAR! Specify another location.", 120, 120).display();
                    }else{
                        doFileCheck = false;
                    }
                }

                // If the user wishes to download to another location, show the file select prompt.
                if(doFileCheck)
                {
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
                        new ARKInterfaceAlert("Notice", "Couldn't delete the existing JAR!", 100, 100).display();
                        return;
                    }

                    downloadTarget = f;
                }

                // Download the new version to the specified location.
                updateMTStatus("Status: Downloading Update");

                // Create the download task.
                final File downloadTargetFN = downloadTarget;
                final String source = updateTask.getValue();
                Service<Void> downloadTask = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                updateMTStatus("Status: Downloading Update");
                                IOTools.getFileFromURL(source, downloadTargetFN, true);
                                return null;
                            }
                        };
                    }
                };

                // Start the download task.
                downloadTask.restart();

                // If the download succeeds, notify the user.
                downloadTask.setOnSucceeded(e2 -> {
                    new ARKInterfaceAlert("Notice", "Update complete!", 100, 100).display();
                    updateMTStatus("Status: Complete");
                });

                // If the download fails, notify the user and log the exception to the console.
                downloadTask.setOnFailed(e2 -> {
                    new ARKInterfaceAlert("Notice", "Error while downloading new version!", 100, 100).display();
                    downloadTask.getException().printStackTrace();
                    updateMTStatus("Status: Complete");
                });

            });
        });
    }

    /**
     * Checks the version of the current target JAR file against the remote repo version.
     * @return the URI of the new version of the file, or null if an issue was encountered during checking
     * @throws ARKTransThreadTransport if an error occurred during the check
     */
    private String checkVersion() throws ARKTransThreadTransport
    {
        // Check to make sure that the current target is a valid JAR file.
        if(currentTarget == null || !currentTarget.exists() || !currentTarget.getName().contains(".jar")){
            dispatcher.dispatchTransThreadPacket("Select a valid target JAR file first!");
            return null;
        }

        // Inventory the JAR file to see what classes it contains in its root.
        ArrayList<String> classNames;

        // JAR inventory function sourced from GitHub user 'sigpwned'
        try {
            classNames = new ArrayList<>();
            ZipInputStream zip = new ZipInputStream(new FileInputStream(currentTarget));
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    // This ZipEntry represents a class. Now, what class does it represent?
                    String className = entry.getName().replace('/', '.'); // including ".class"
                    classNames.add(className.substring(0, className.length() - ".class".length()));
                }
            }
        }catch (IOException e1){
            dispatcher.dispatchTransThreadPacket("Could not load class list from JAR file!");
            return null;
        }

        // Set the identifier code based on what we find in the JAR.
        String appIdentifier = "";
        for(String s : classNames)
        {
            switch (s){
                case "R34UI":
                    appIdentifier = "R34UI";
                    break;
                case "RPlanner":
                    appIdentifier = "RP";
                    break;
                case "IPSearch":
                    appIdentifier = "IP";
                    break;
                case "com.michaelRunzler.ARK.application.retrieval.R34.R34UI":
                    dispatcher.dispatchTransThreadPacket("Selected JAR is from the legacy ARK IAF series, which is not supported at this time.");
                    return null;
            }

            if(!appIdentifier.isEmpty()){
                break;
            }
        }

        // If we couldn't determine what application is selected, tell the user and return.
        if(appIdentifier.isEmpty()){
            dispatcher.dispatchTransThreadPacket("Selected JAR file is not a supported ARK application!");
            return null;
        }

        // At this point, we have a valid application JAR, query GitHub for the update master index file.
        HashMap<String, String> masterIndexMap = new HashMap<>();

        String indexCache = null;
        try {
            indexCache = ARKArrayUtil.charArrayToString(ARKArrayUtil.byteToCharArray(IOTools.getBytesFromURL(UPDATE_URL_INDEX)));
        } catch (IOException e) {
            dispatcher.dispatchTransThreadPacket("Error while downloading master index from remote server!");
        }

        // Parse the cached index to get all of its code entries and corresponding URLs.
        Scanner parser = new Scanner(indexCache);
        parser.useDelimiter("\n");

        while(parser.hasNext()){
            String line = parser.next();
            masterIndexMap.put(line.substring(0, line.indexOf(">")), line.substring(line.indexOf(">") + 1, line.length()));
        }

        // If the master index does not contain an entry for the detected application JAR, tell the user and return.
        if(!masterIndexMap.containsKey(appIdentifier)){
            dispatcher.dispatchTransThreadPacket("Selected JAR is a valid ARK application, but does not support auto-update at this time.");
            return null;
        }

        // Check to see if the target file and the remote copy specified by the master index are the same size (byte-level accuracy).
        // If they are, they should be the same version, tell the user such. If not, proceed to the update process.
        try {
            if(IOTools.getRemoteFileSize(new URL(masterIndexMap.get(appIdentifier))) == currentTarget.length()){
                dispatcher.dispatchTransThreadPacket("Application is up to date!");
                return null;
            }
        } catch (IOException e) {
            dispatcher.dispatchTransThreadPacket("Could not check remote version!");
        }

        return masterIndexMap.get(appIdentifier);
    }

    /**
     * Update the status label from another thread. Avoids IllegalStateExceptions by using the JavaFX Delayed Concurrency
     * API to delegate the operation.
     * @param content the String to pass to the status label
     */
    private void updateMTStatus(String content){
        Platform.runLater(() -> {
            updateMTStatus(content);
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
