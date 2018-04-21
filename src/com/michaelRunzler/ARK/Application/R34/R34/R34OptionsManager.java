package R34;

import core.UI.InterfaceDialogs.ARKInterfaceAlert;
import core.UI.InterfaceDialogs.ARKInterfaceDialogYN;
import com.sun.istack.internal.NotNull;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import core.system.ARKManagerInterface;

import java.io.File;

public class R34OptionsManager implements ARKManagerInterface
{
    private Stage window;
    private Scene scene;
    private AnchorPane layout;

    private Button close;
    private Button defaults;
    private CheckBox doMD5;
    private CheckBox doFileLogging;
    private CheckBox doLogTrimming;
    private CheckBox doSkipDLPrompt;
    //private CheckBox doMultiThread;
    private CheckBox doPreview;
    private Button exitNoCfg;
    private Button deleteCfgs;
    private Button changeLogLoc;

    private DirectoryChooser logCh;

    private File cfgG;
    private File cfgM;
    private File cfgA;
    private File log;

    boolean hasViewedWarning;

    public R34OptionsManager(String title, int width, int height, double x, double y)
    {
        hasViewedWarning = false;

        window = new Stage();
        window.setTitle(title);
        window.setResizable(false);
        window.setHeight(height);
        window.setWidth(width);
        window.setX(x);
        window.setY(y);
        window.getIcons().add(new Image("core/assets/options.png"));
        window.initModality(Modality.APPLICATION_MODAL);

        window.setOnCloseRequest(e -> hide());

        close = new Button("Save & Close");
        defaults = new Button("Reset to Defaults");
        doMD5 = new CheckBox("Use MD5 hash verification");
        doFileLogging = new CheckBox("Log to file");
        doLogTrimming = new CheckBox("Trim live log to 100 entries");
        doSkipDLPrompt = new CheckBox("Auto-download ALL new images");
        //doMultiThread = new CheckBox("Multi-threaded retrieval");
        doPreview = new CheckBox("Preview downloads on click");
        exitNoCfg = new Button("Discard Config Changes");
        deleteCfgs = new Button("Delete Config Files");
        changeLogLoc = new Button("Change Log Location");

        layout = new AnchorPane(close, defaults, doMD5, doFileLogging, doLogTrimming/*, doMultiThread*/,
                doSkipDLPrompt, doPreview, exitNoCfg, deleteCfgs, changeLogLoc);
        layout.setPadding(new Insets(15, 15, 15, 15));

        logCh = new DirectoryChooser();

        logCh.setTitle("Select Logging Location");
        logCh.setInitialDirectory(new File(System.getProperty("user.home") + "\\.R34Logs"));

        scene = new Scene(layout, width, height);

        setNodeAlignment(close, 0, -1, -1, 0);
        setNodeAlignment(defaults, -1, 0, -1, 0);
        setNodeAlignment(doMD5, 0, -1, 0, -1);
        setNodeAlignment(doFileLogging, 0, -1, 30, -1);
        setNodeAlignment(doLogTrimming, 0, -1, 60, -1);
        setNodeAlignment(doPreview, 0, -1, 90, -1);
        setNodeAlignment(doSkipDLPrompt, 0, -1, 120, -1);
        //setNodeAlignment(doMultiThread, 0, -1, 90, -1);
        setNodeAlignment(exitNoCfg, 0, -1, 160, -1);
        setNodeAlignment(deleteCfgs, 0, -1, 200, -1);
        setNodeAlignment(changeLogLoc, 0, -1, 240, -1);

        //doMultiThread.setDisable(true);
        //doMultiThread.setTooltip(new Tooltip("Disabled for now! Fix in progress."));

        close.setOnAction(e -> hide());

        defaults.setOnAction(e -> setDefaults());

        doLogTrimming.setOnAction(e ->{
            if(!doLogTrimming.isSelected())
                new ARKInterfaceAlert("Warning", "Disabling log trimming may cause performance issues!", 150, 150).display();
        });

        doMD5.setOnAction(e ->{
            if(doMD5.isSelected())
                new ARKInterfaceAlert("Warning", "MD5 hash support is currently experimental!", 150, 150).display();
        });

        /*
        doMultiThread.setOnAction(e ->{
            if(doMultiThread.isSelected()){
                new ARKInterfaceAlert("Warning", "Multi-threading will increase retrieval speed, but make a mess of your log!", 150, 150).display();
            }
        });
        */

        exitNoCfg.setOnAction(e ->{
            if(new ARKInterfaceDialogYN("Warning", "This will discard all config changes (including rules!) since the last launch and exit. Are you sure?",
                    "Proceed", "No", 180, 180).display()){
                System.exit(101);
            }
        });

        deleteCfgs.setOnAction(e ->{
            if(!hasViewedWarning){
                new ARKInterfaceAlert("Warning", "This button deletes all config files (including rule lists!). This should be used with caution. Press it again to proceed.", 200, 180).display();
                hasViewedWarning = true;
            }else if(new ARKInterfaceDialogYN("Warning", "This will delete ALL config files! Are you sure?",
                    "Proceed", "Cancel", 170, 120).display()){
                if(cfgG.delete() & cfgM.delete() & cfgA.delete()){
                    new ARKInterfaceAlert("Notice", "Configs deleted. Will attempt to re-create on next normal exit. Use 'Discard Config Changes' to skip re-creation.", 180, 180).display();
                }else{
                    new ARKInterfaceAlert("Notice", "Couldn't delete all config files. You may need to manually delete some files, usually located in AppData\\Roaming\\KAI\\ARK\\Config.", 200, 200).display();
                }
            }
        });

        changeLogLoc.setOnAction(e ->{
            File f = logCh.showDialog(window);
            log = f == null ? log : (new File(f, "R34log.vcsl"));
        });

        window.setScene(scene);
    }

    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    public void display()
    {
        if(!window.isShowing()) {
            window.showAndWait();
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

    public boolean getDoMD5() {
        return doMD5.isSelected();
    }

    public boolean getDoFileLogging() {
        return doFileLogging.isSelected();
    }

    public boolean getDoLogTrimming() {
        return doLogTrimming.isSelected();
    }

    public boolean getDoMultiThreading() {
        return false;
    }

    public File getLoggingLocation(){
        return log;
    }

    public boolean getDoPreview(){
        return doPreview.isSelected();
    }

    public boolean getDoSkipDLPrompt(){
        return doSkipDLPrompt.isSelected();
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

    /**
     * Loads a set of config options into the options manager.
     * @param doMD5
     * @param doFileLogging
     * @param doLogTrimming
     */
    public void loadOptions(boolean doMD5, boolean doFileLogging, boolean doLogTrimming, boolean doSkipDLPrompt, File logDir, boolean doPreview)
    {
        this.doMD5.setSelected(doMD5);
        this.doFileLogging.setSelected(doFileLogging);
        this.doLogTrimming.setSelected(doLogTrimming);
        //this.doMultiThread.setSelected(doMT);
        this.doSkipDLPrompt.setSelected(doSkipDLPrompt);
        this.doPreview.setSelected(doPreview);
        this.log = logDir;
    }

    /**
     * Sets configuration options to defaults.
     */
    public void setDefaults()
    {
        doMD5.setSelected(false);
        doFileLogging.setSelected(true);
        doLogTrimming.setSelected(true);
        doSkipDLPrompt.setSelected(false);
        //doMultiThread.setSelected(false);
        doPreview.setSelected(true);
        log = new File(System.getProperty("user.home") + "\\.R34Logs", "R34log.vcsl");
    }

    /**
     * Updates the internal config file registry with new config files.
     * @param G the Global config file's location
     * @param M the Manual-Mode config file's location
     * @param A the Auto-Mode config file's location
     */
    void setCfgLocations(File G, File M, File A)
    {
        this.cfgG = G;
        this.cfgM = M;
        this.cfgA = A;
    }

}
