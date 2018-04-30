package X34.UI.JFX.Managers;

import X34.Core.IO.X34Config;
import X34.Core.IO.X34ConfigDelegator;
import X34.Core.IO.X34IndexDelegator;
import X34.UI.JFX.Util.JFXConfigKeySet;
import core.UI.ModeLocal.ModeLocal;
import core.UI.ModeLocal.ModeSwitchController;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.InterfaceDialogs.ARKInterfaceDialogYN;
import core.UI.ARKManagerBase;
import core.UI.NotificationBanner.UINotificationBannerControl;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Manages the files used by the X34UI application, including config, index, and log files, and allows the user to view,
 * open, move, and delete said files.
 */
public class X34UIFileManager extends ARKManagerBase
{
    //
    // CONSTANTS
    //

    public static final String TITLE = "File Management";
    public static final int DEFAULT_WIDTH = (int)(450 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(450 * JFXUtil.SCALE);

    //
    // JFX NODES
    //

    private TabPane typeSelector;

    private Label info;
    private Label fileListLabel;

    private TextField currentDir;
    private Button openDir;
    private Button changeDir;
    private Button close;
    private ImageView directorySeparator;
    private ImageView fileListSeparator;

    //
    // MODE-LOCAL NODES
    //

    @ModeLocal(invert = true, value = {})
    private ListView<File> fileList;

    @ModeLocal(invert = true, value = {})
    private Button delete;

    @ModeLocal(invert = true, value = {})
    private Button open;

    @ModeLocal(invert = true, value = {})
    private Label listFunctionsLabel;

    //
    // INSTANCE VARIABLES
    //

    private X34Config config;
    private XLoggerInterpreter log;

    private UINotificationBannerControl notice;
    private FileManagerTabProperty[] tabs;
    private ModeSwitchController modeControl;

    private boolean linkedSizeListeners;
    private SimpleIntegerProperty mode;
    private File linkedCurrentDir;

    public X34UIFileManager(double x, double y)
    {
        super(TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);

        window.initModality(Modality.APPLICATION_MODAL);
        window.getIcons().set(0, new Image("core/assets/options.png"));

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        window.setOnCloseRequest(e ->{
            e.consume();
            hide();
        });

        //
        // BASE OBJECT INIT
        //

        linkedSizeListeners = false;
        linkedCurrentDir = null;
        mode = new SimpleIntegerProperty(0);
        config = X34ConfigDelegator.getMainInstance();
        log = new XLoggerInterpreter();

        try {
            modeControl = new ModeSwitchController(this);
        } catch (ClassNotFoundException e) {
            log.logEvent(LogEventLevel.CRITICAL, "Error 14041: Unable to initialize mode-switch or state-switch controllers due to missing class(es).");
            log.logEvent(e);
        }

        mode.addListener((observable, oldValue, newValue) -> modeControl.switchMode(mode.get()));

        // Initialize tab list. Each tab object contains all necessary metadata to initialize and control its linked JFX Tab object.
        tabs = new FileManagerTabProperty[]{
                new FileManagerTabProperty("Configs", JFXConfigKeySet.KEY_CONFIG_FILE, ".x34c").setOnChange(param -> {
                    config.setTarget(param);
                    return null;
                }),
                new FileManagerTabProperty("Indexes", JFXConfigKeySet.KEY_INDEX_DIR, ".x34i").setOnChange(param -> {
                    X34IndexDelegator.getMainInstance().setParent(param);
                    return null;
                }),
                new FileManagerTabProperty("Log Files", JFXConfigKeySet.KEY_LOGGING_DIR, ".x34l").setOnChange(param -> {
                    try {
                        log.requestLoggerDirectoryChange(param);
                    } catch (IOException e) {
                        log.logEvent("Encountered an error while changing log directory.");
                        log.logEvent(e);
                    }
                    return null;
                })
        };

        //
        // NODE INIT
        //

        fileList = new ListView<>();
        currentDir = new TextField();
        typeSelector = new TabPane();

        close = new Button("Close");
        changeDir = new Button("Change");
        openDir = new Button("Open");
        delete = new Button("Delete");
        open = new Button("Open");

        info = new Label("");
        fileListLabel = new Label("- Relevant Files -");
        listFunctionsLabel = new Label("List Functions:");

        directorySeparator = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_horiz_256x4.png", 256, 4, false, true));
        fileListSeparator = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_vert_256x4.png", 4, 128, false, true));

        // Add all of the listed tabs to the selector node
        for(FileManagerTabProperty tab : tabs) {
            typeSelector.getTabs().add(new Tab(tab.name));
        }

        typeSelector.setSide(Side.TOP);
        typeSelector.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Add change listener for the type selector to enable bound element updates
        typeSelector.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.equals(newValue) || newValue.intValue() < 0 || newValue.intValue() >= typeSelector.getTabs().size()) return;
            mode.set(newValue.intValue());
            computeAvailableFiles();
        });

        currentDir.setEditable(false);
        openDir.setDefaultButton(true);

        info.setWrapText(true);
        notice = new UINotificationBannerControl(info);

        fileList.setEditable(false);
        fileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setElementTooltips();

        // Add type conversion to the cells in the file list
        fileList.setCellFactory(param -> new ListCell<File>() {
            @Override
            public void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                super.setText(item != null && !empty ? (item.isFile() ? item.getName() : "<DIR> " + item.getName()) : "");
            }
        });

        layout.getChildren().addAll(typeSelector, info, currentDir, changeDir, openDir, close, fileList, fileListLabel, open, delete, listFunctionsLabel, directorySeparator, fileListSeparator);

        //
        // NODE ACTIONS
        //

        close.setOnAction(e -> hide());

        openDir.setOnAction(e ->{
            if(mode.get() < 0 || linkedCurrentDir == null) return;

            if(linkedCurrentDir.exists()) {
                try {
                    Desktop.getDesktop().open(linkedCurrentDir.isFile() ? linkedCurrentDir.getParentFile() : linkedCurrentDir);
                } catch (IOException e1) {
                    log.logEvent(LogEventLevel.DEBUG, "Error while opening directory, see below for details.");
                    log.logEvent(e1);
                }
            }else notice.displayNotice("Unable to open directory; does not exist! Try resetting it using the 'Change' button.", UINotificationBannerControl.Severity.WARNING, 2500);        });

        changeDir.setOnAction(e ->{
            if(mode.get() < 0) return;

            DirectoryChooser selector = new DirectoryChooser();
            selector.setInitialDirectory(linkedCurrentDir);
            File f = selector.showDialog(window);
            if(f == null || !f.exists() || f.equals(linkedCurrentDir)) return;

            // Ask if the user wants to migrate existing files. If they do, copy any files matching the extension filter,
            // confirm copy, and delete the originals.
            if(new ARKInterfaceDialogYN("Query", "Do you want to move all relevant files from the old directory to the new one?", "Yes", "No").display())
            {
                File base = linkedCurrentDir.isFile() ? linkedCurrentDir.getParentFile() : linkedCurrentDir;
                File[] targets = base.listFiles((dir, name) -> {
                    for(String s : tabs[mode.get()].extensionFilters) if(name.contains(s)) return true;
                    return false;
                });

                int failed = 0;
                if(targets != null && targets.length > 0) {
                    for (int i = 0; i < targets.length; i++)
                    {
                        File target = targets[i];

                        log.logEvent("Copying file " + (i + 1) + " of " + targets.length + "...");
                        try {
                            Files.copy(Paths.get(target.toURI()), Paths.get(new File(f, target.getName()).toURI()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                            log.logEvent("Copy successful, deleting original...");
                            if(!target.delete()) log.logEvent(LogEventLevel.WARNING, "Unable to delete original file.");
                            else log.logEvent("Deletion successful.");
                        } catch (IOException e1) {
                            log.logEvent(LogEventLevel.DEBUG, "Failed to copy a file to its new directory, details below.");
                            log.logEvent(e1);
                            failed ++;
                        }
                    }
                    notice.displayNotice("Copied " + (targets.length - failed) + " of " + targets.length + " file(s). See log for details.", UINotificationBannerControl.Severity.INFO, 2000);
                }
            }

            // Store the new directory to the config, call the Callback in the corresponding tab, and force an update of the UI
            if(tabs[mode.get()].changeAction != null) tabs[mode.get()].changeAction.call(f);

            // If the previous 'directory' was actually a file reference (e.g to a single file used as a template),
            // use the same name as the previous file, but tagged onto the new directory
            if(linkedCurrentDir != null && linkedCurrentDir.isFile()) f = new File(f.getAbsolutePath(), linkedCurrentDir.getName());

            config.storeSetting(tabs[mode.get()].configKey, f);
            computeAvailableFiles();
            notice.displayNotice("Directory change committed!", UINotificationBannerControl.Severity.INFO, 1500);
        });

        delete.setOnAction(e ->{
            ObservableList<Integer> indices = fileList.getSelectionModel().getSelectedIndices();
            if(indices.size() == 0) return;
            if(!new ARKInterfaceDialogYN("Warning", "Deleting " + (indices.size() == 1 ? "this file" : "these files") + " is an irreversible action. Continue?", "Yes", "No").display()) return;

            int succ = 0;
            for(int i : indices) {
                if(fileList.getItems().get(i).delete()) succ ++;
            }

            notice.displayNotice("Successfully deleted " + (succ == indices.size() ? (indices.size() == 1 ? "the" : "all of the") : succ + " of " + indices.size()) + " selected file" + (indices.size() == 1 ? "" : "s") + ".", UINotificationBannerControl.Severity.INFO, 2500);

            // Force UI update
            computeAvailableFiles();
        });

        open.setOnAction(e ->{
            ObservableList<Integer> indices = fileList.getSelectionModel().getSelectedIndices();
            if(indices.size() == 0) return;

            for(int i : indices){
                try {
                    Desktop.getDesktop().open(fileList.getItems().get(i));
                } catch (IOException e1) {
                    log.logEvent(LogEventLevel.DEBUG, "Error while opening file or directory, see below for details.");
                    log.logEvent(e1);
                }
            }
        });
    }

    /**
     * Forces this Manager to re-compute its list of available files and their properties based on the current selected tab.
     * Called when {@link #display()} is called, usually not necessary to call externally.
     */
    public void computeAvailableFiles()
    {
        if(mode.get() < 0) return;

        // Change UI display mode if necessary
        modeControl.switchMode(mode.get());

        File f = config.getSettingOrDefault(tabs[mode.get()].configKey, (File)config.getDefaultSetting(tabs[mode.get()].configKey));

        currentDir.setText(f == null ? "" : f.isFile() ? f.getParentFile().getAbsolutePath() : f.getAbsolutePath());

        // Set file list contents from parent based on name filter
        FilenameFilter ff = (dir, name) -> {
            for(String s : tabs[mode.get()].extensionFilters) if(name.contains(s)) return true;
            return false;
        };

        fileList.getItems().clear();
        File[] list = f == null ? new File[0] : f.isFile() ? f.getParentFile().listFiles(ff) : f.listFiles(ff);
        fileList.getItems().addAll(list == null ? new File[0] : list);

        // Store reference to current file
        linkedCurrentDir = f;
    }

    /**
     * Re-positions all UI elements to their proper locations and sizes upon window display.
     */
    private void repositionElements()
    {
        // Only do this once, then lock it out to prevent unnecessary listener bindings
        if(!linkedSizeListeners){
            layout.widthProperty().addListener(e ->{
                repositionOnResize();
            });

            layout.heightProperty().addListener(e ->{
                repositionOnResize();
                fileListSeparator.setFitHeight(layout.getHeight() - directorySeparator.getLayoutY() - layout.getPadding().getBottom());
            });

            Platform.runLater(() ->{
                JFXUtil.bindAlignmentToNode(layout, fileList, fileListLabel, -10, Orientation.VERTICAL, JFXUtil.Alignment.CENTERED);

                // Set the padding in the layout to exclude the top menu bar, and update the menu bar's position to place it outside of those bounds.
                // Essentially, we are making it impossible for other nodes to clash with the menu bar.
                layout.setPadding(new Insets(layout.getPadding().getTop() + typeSelector.getHeight(), layout.getPadding().getRight(), layout.getPadding().getBottom(), layout.getPadding().getLeft()));
                AnchorPane.setTopAnchor(typeSelector, -1 * layout.getPadding().getTop());
                JFXUtil.bindAlignmentToNode(layout, open, listFunctionsLabel, -10, Orientation.HORIZONTAL, JFXUtil.Alignment.CENTERED);
            });

            linkedSizeListeners = true;
        }

        JFXUtil.setElementPositionInGrid(layout, info, 0, 0, 0, -1);
        JFXUtil.setElementPositionIgnorePadding(layout, typeSelector, 0, 0, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, close, 0, -1, -1, 0);

        JFXUtil.setElementPositionInGrid(layout, changeDir, 0, -1, 1, -1);
        JFXUtil.setElementPositionInGrid(layout, openDir, 0, -1, 2, -1);
        JFXUtil.setElementPositionInGrid(layout, currentDir, -1, -1, 1.5, -1);

        JFXUtil.setElementPositionInGrid(layout, directorySeparator, 0, 0, 3, -1);
        JFXUtil.setElementPositionInGrid(layout, fileListSeparator, 2, -1, -1, 0);

        JFXUtil.setElementPositionInGrid(layout, fileList, 2.5, 0, 4, 1);
        JFXUtil.setElementPositionInGrid(layout, delete, -1, 0, -1, 0);

        Platform.runLater(() ->{
            JFXUtil.setElementPosition(layout, open, -1, delete.getWidth(), -1, 0);
        });
    }

    /**
     * Called whenever the window or layout resizes on the X or Y axis.
     */
    private void repositionOnResize()
    {
        directorySeparator.setFitWidth(layout.getWidth() - layout.getPadding().getRight() - layout.getPadding().getLeft());
        directorySeparator.layoutYProperty().addListener(e -> fileListSeparator.setFitHeight(layout.getHeight() - directorySeparator.getLayoutY() - layout.getPadding().getBottom()));

        Platform.runLater(() ->{
            openDir.setPrefWidth(changeDir.getWidth());
            JFXUtil.setElementPosition(layout, currentDir, changeDir.getLayoutX() + changeDir.getWidth() - layout.getPadding().getLeft(), 0, -1, -1);
        });
    }

    private void setElementTooltips()
    {
        typeSelector.setTooltip(new Tooltip("Use this to select a file category to view or modify."));
        currentDir.setTooltip(new Tooltip("The currently set directory path for this file category."));
        fileList.setTooltip(new Tooltip("The list of all relevant files in the currently set directory for this category.\nThis will only update when tabs are changed or the window is closed and re-opened."));
        close.setTooltip(new Tooltip("Close this window. Changes will be saved."));
        openDir.setTooltip(new Tooltip("Open the currently set directory in the system file explorer."));
        changeDir.setTooltip(new Tooltip("Change the currently set directory."));
        delete.setTooltip(new Tooltip("Delete the currently selected file. WARNING: this action is permanent!"));
        open.setTooltip(new Tooltip("Open the currently selected file in its associated program."));
    }

    @Override
    public void display()
    {
        if(!window.isShowing()){
            computeAvailableFiles();
            repositionElements();
            notice.displayNotice("This is the notification area. System messages and other notifications will appear here.", UINotificationBannerControl.Severity.INFO, 5000);
            window.show();
        }
    }
}

/**
 * Data-storage class
 */
class FileManagerTabProperty
{
    String configKey;
    String[] extensionFilters;
    String name;
    Callback<File, Void> changeAction;

    FileManagerTabProperty(String name, String configKey, String... extensionFilters) {
        this.configKey = configKey;
        this.extensionFilters = extensionFilters;
        this.name = name;
    }

    FileManagerTabProperty setOnChange(Callback<File, Void> action) {
        this.changeAction = action;
        return this;
    }
}
