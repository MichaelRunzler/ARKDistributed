package X34.UI.JFX.Managers;

import X34.Core.IO.X34Config;
import X34.Core.IO.X34ConfigDelegator;
import X34.UI.JFX.Util.JFXConfigKeySet;
import core.UI.InterfaceDialogs.ARKInterfaceAlert;
import core.UI.InterfaceDialogs.ARKInterfaceDialog;
import core.UI.InterfaceDialogs.ARKInterfaceDialogYN;
import core.UI.InterfaceDialogs.ARKInterfaceFileChangeDialog;
import core.UI.ModeLocal.ModeLocal;
import core.UI.ModeLocal.ModeSwitchController;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.*;
import core.UI.NotificationBanner.UINotificationBannerControl;
import core.system.ARKAppCompat;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.io.*;
import java.util.HashMap;

public class X34UIConfigManager extends ARKManagerBase
{
    //
    // CONSTANTS
    //

    public static final String TITLE = "Config Management";
    public static final int DEFAULT_WIDTH = (int)(400 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(400 * JFXUtil.SCALE);

    private static final int MODE_SPECIAL = 114;

    //
    // MODE-LOCAL NODES
    //

    @ModeLocal({MODE_SPECIAL})
    private Button saveConfig;

    @ModeLocal({MODE_SPECIAL})
    private Button loadConfig;

    @ModeLocal({MODE_SPECIAL})
    private Button globalDefaults;

    @ModeLocal({MODE_SPECIAL})
    private Button returnToMain;

    @ModeLocal({MODE_SPECIAL})
    private Label additionalOptionsDesc;

    @ModeLocal(invert = true, value = {MODE_SPECIAL})
    private Button moreOptions;

    @ModeLocal(invert = true, value = {MODE_SPECIAL})
    private Button defaults;

    @ModeLocal(invert = true, value = {MODE_SPECIAL})
    private ScrollPane options;
    private VBox optionsContainer;

    //
    // TABBED NODES
    //

    // Mode-0
    private HBox globalOutputDirContainer;
    private Button globalOutputDirSelect;
    private TextField globalOutputDir;

    private CheckBox autoDownload;
    private CheckBox overwriteExisting;

    // Mode-1
    private CheckBox pushToIndex;
    private CheckBox fastRetrieval;

    // Mode-2
    private CheckBox logToFile;

    private HBox logLocationContainer;
    private Button logLocationSelect;
    private TextField logLocation;

    //
    // JFX NODES
    //

    private TabPane categorySelect;

    private Button close;
    private HBox configFunctionsContainer;
    private Button save;
    private Button revert;

    private Label info;

    private ImageView notificationSeparator;

    //
    // INSTANCE VARIABLES
    //

    private XLoggerInterpreter log;
    private X34Config config;

    private int cachedMode;
    private boolean linkedSizeListeners;
    private SimpleIntegerProperty mode;
    private ConfigManagerTabProperty[] tabs;
    private UINotificationBannerControl notice;
    private ModeSwitchController modeControl;

    public X34UIConfigManager(double x, double y)
    {
        super(TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);

        //
        // BASE OBJECT INIT
        //

        window.initModality(Modality.APPLICATION_MODAL);
        window.getIcons().set(0, new Image("core/assets/options.png"));

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        window.setOnCloseRequest(e -> requestClose(compareLiveAndCachedIndexes() || new ARKInterfaceDialogYN("Warning", "Save changes before exiting?", "Save", "Discard").display()));

        log = new XLoggerInterpreter();
        config = X34ConfigDelegator.getMainInstance();
        mode = new SimpleIntegerProperty(0);
        cachedMode = mode.get();
        try {
            modeControl = new ModeSwitchController(this);
        } catch (ClassNotFoundException e) {
            log.logEvent(LogEventLevel.CRITICAL, "Error 14041: Unable to initialize mode-switch controller due to missing class(es).");
            log.logEvent(e);
        }

        mode.addListener((observable, oldValue, newValue) -> modeControl.switchMode(newValue.intValue()));

        linkedSizeListeners = false;

        //
        // JFX NODE INIT
        //

        categorySelect = new TabPane();

        options = new ScrollPane();
        optionsContainer = new VBox();
        configFunctionsContainer = new HBox();

        options.setPadding(new Insets(10 * JFXUtil.SCALE, 10 * JFXUtil.SCALE, 10 * JFXUtil.SCALE, 10 * JFXUtil.SCALE));
        optionsContainer.setSpacing(10 * JFXUtil.SCALE);
        optionsContainer.setAlignment(Pos.TOP_LEFT);
        optionsContainer.setFillWidth(true);

        close = new Button("Close");
        save = new Button("Save Changes");
        revert = new Button("Discard Changes");
        defaults = new Button("Reset Tab to Defaults");
        moreOptions = new Button("More");

        globalDefaults = new Button("Reset to Global Defaults");
        saveConfig = new Button("Export config to file...");
        loadConfig = new Button("Import config from file...");
        returnToMain = new Button("Back");

        // TABBED NODE INIT

        // Mode-0
        autoDownload = new CheckBox("Auto-download");
        overwriteExisting = new CheckBox("Always overwrite existing");

        globalOutputDirSelect = new Button("Change Download Directory");
        globalOutputDir = new TextField();
        globalOutputDirContainer = new HBox(globalOutputDirSelect, globalOutputDir);

        globalOutputDirContainer.setFillHeight(false);
        globalOutputDirContainer.setSpacing(5 * JFXUtil.SCALE);
        globalOutputDirContainer.setAlignment(Pos.CENTER);

        autoDownload.setOnAction(e -> config.storeSetting(JFXConfigKeySet.KEY_AUTO_DOWNLOAD, autoDownload.isSelected()));
        overwriteExisting.setOnAction(e -> config.storeSetting(JFXConfigKeySet.KEY_OVERWRITE_EXISTING, overwriteExisting.isSelected()));

        globalOutputDirSelect.setOnAction(e -> {
            ARKInterfaceFileChangeDialog display = new ARKInterfaceFileChangeDialog("Download Location", "Change global image download destination");
            display.setChoiceMode(ARKInterfaceFileChangeDialog.ChoiceMode.DIR_SELECT);
            File current = (File)config.getDefaultSetting(JFXConfigKeySet.KEY_OUTPUT_DIR);
            display.setDefaultValue(current);
            display.setValue((File)config.getSetting(JFXConfigKeySet.KEY_OUTPUT_DIR));
            display.setInitialDirectory(ARKAppCompat.getOSSpecificDesktopRoot());

            File result = display.display();
            if(result == null || result == current) return;

            config.storeSetting(JFXConfigKeySet.KEY_OUTPUT_DIR, result);
            globalOutputDir.setText(result.getAbsolutePath());
        });
        globalOutputDir.setEditable(false);

        // Mode-1

        pushToIndex = new CheckBox("Push updates to index");
        pushToIndex.setOnAction(e -> config.storeSetting(JFXConfigKeySet.KEY_PUSH_TO_INDEX, pushToIndex.isSelected()));
        fastRetrieval = new CheckBox("Fast Retrieval"); //todo implement in processors/core
        fastRetrieval.setOnAction(e -> config.storeSetting(JFXConfigKeySet.KEY_FAST_RETRIEVAL, fastRetrieval.isSelected()));

        // Mode-2

        logToFile = new CheckBox("Log events to file");
        logToFile.setOnAction(e -> config.storeSetting(JFXConfigKeySet.KEY_DO_FILE_LOGGING, logToFile.isSelected()));
        logToFile.selectedProperty().addListener(e -> logLocationContainer.setDisable(!logToFile.isSelected()));

        logLocationSelect = new Button("Change Logfile Location");
        logLocation = new TextField();
        logLocationContainer = new HBox(logLocationSelect, logLocation);

        logLocationContainer.setFillHeight(false);
        logLocationContainer.setSpacing(5 * JFXUtil.SCALE);
        logLocationContainer.setAlignment(Pos.CENTER);

        logLocationSelect.setOnAction(e ->{
            ARKInterfaceFileChangeDialog display = new ARKInterfaceFileChangeDialog("Logfile Location", "Change system event logging location");
            display.setChoiceMode(ARKInterfaceFileChangeDialog.ChoiceMode.DIR_SELECT);
            File current = (File)config.getDefaultSetting(JFXConfigKeySet.KEY_LOGGING_DIR);
            display.setDefaultValue(current);
            display.setValue((File)config.getSetting(JFXConfigKeySet.KEY_LOGGING_DIR));
            display.setInitialDirectory(ARKAppCompat.getOSSpecificAppCacheRoot());

            File result = display.display();
            if(result == null || result == current) return;

            config.storeSetting(JFXConfigKeySet.KEY_LOGGING_DIR, result);
            logLocation.setText(result.getAbsolutePath());
        });
        logLocation.setEditable(false);

        // END TABBED NODES

        moreOptions.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_right_256px.png", 15));
        returnToMain.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_left_256px.png", 15));

        saveConfig.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_bounded_arrow_fill_left_256px.png", 15));
        loadConfig.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_bounded_arrow_fill_right_256px.png", 15));

        additionalOptionsDesc = new Label("- Advanced Configuration Tools -");
        info = new Label("");

        notificationSeparator = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_horiz_256x4.png"));

        configFunctionsContainer.getChildren().addAll(save, revert);
        configFunctionsContainer.setAlignment(Pos.CENTER);
        configFunctionsContainer.setSpacing(JFXUtil.SCALE * 5);
        configFunctionsContainer.setFillHeight(false);

        //
        // TABS
        //

        ConfigManagerTabProperty dnld = new ConfigManagerTabProperty("Downloads");
        ConfigManagerTabProperty ret = new ConfigManagerTabProperty("Retrieval");
        ConfigManagerTabProperty logs = new ConfigManagerTabProperty("Logging");

        dnld.addAction(JFXConfigKeySet.KEY_OUTPUT_DIR, param -> {
            globalOutputDir.setText(((File)param.data).getAbsolutePath());
            return null;
        }, globalOutputDirContainer);
        dnld.addAction(JFXConfigKeySet.KEY_AUTO_DOWNLOAD, param -> {
            autoDownload.setSelected((Boolean)param.data);
            return null;
        }, autoDownload);
        dnld.addAction(JFXConfigKeySet.KEY_OVERWRITE_EXISTING, param -> {
            overwriteExisting.setSelected((Boolean)param.data);
            return null;
        }, overwriteExisting);

        ret.addAction(JFXConfigKeySet.KEY_PUSH_TO_INDEX, param -> {
            pushToIndex.setSelected((Boolean)param.data);
            return null;
        }, pushToIndex);
        ret.addAction(JFXConfigKeySet.KEY_FAST_RETRIEVAL, param -> {
            fastRetrieval.setSelected((Boolean)param.data);
            return null;
        }, fastRetrieval);

        logs.addAction(JFXConfigKeySet.KEY_DO_FILE_LOGGING, param -> {
            logToFile.setSelected((Boolean)param.data);
            logLocationContainer.setDisable(!logToFile.isSelected());
            return null;
        }, logToFile);
        logs.addAction(JFXConfigKeySet.KEY_LOGGING_DIR, param -> {
            logLocation.setText(((File)param.data).getAbsolutePath());
            return null;
        }, logLocationContainer);

        tabs = new ConfigManagerTabProperty[]{dnld, ret, logs};

        for(ConfigManagerTabProperty tab : tabs) categorySelect.getTabs().add(new Tab(tab.name));

        //
        // JFX NODE CONFIG
        //

        options.setContent(optionsContainer);
        options.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        options.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        info.setWrapText(true);
        notice = new UINotificationBannerControl(info);

        globalDefaults.setGraphic(JFXUtil.generateGraphicFromResource("core/assets/warning.png", 15));
        defaults.setGraphic(JFXUtil.generateGraphicFromResource("core/assets/warning.png", 15));

        categorySelect.setSide(Side.LEFT);
        categorySelect.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        categorySelect.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.equals(newValue) || newValue.intValue() < 0 || newValue.intValue() >= categorySelect.getTabs().size()) return;
            mode.set(newValue.intValue());
            forceSettingsUpdate();
        });

        modeControl.addModeSwitchHook(mode -> {
            if(mode == -1 || mode == MODE_SPECIAL) return;

            ConfigManagerTabProperty tab = tabs[mode];

            // Clear the child list, then add the nodes in the tab in question if it has any
            optionsContainer.getChildren().clear();
            if(tab.dataPairs != null) for(KeyedActionCallback k : tab.dataPairs) optionsContainer.getChildren().add(k.boundNode);
        });

        setElementTooltips();

        layout.getChildren().addAll(categorySelect, close, configFunctionsContainer, defaults, globalDefaults, info, saveConfig, loadConfig, additionalOptionsDesc, notificationSeparator, returnToMain, options);

        //
        // NODE ACTIONS
        //

        close.setOnAction(e -> requestClose(compareLiveAndCachedIndexes() || new ARKInterfaceDialogYN("Warning", "Save changes before exiting?", "Save", "Discard").display()));

        save.setOnAction(e -> {
            if(config.getCache() == null || compareLiveAndCachedIndexes()){
                notice.displayNotice("No changes to save!", UINotificationBannerControl.Severity.INFO, 1000);
            }else {
                config.clearCache();
                notice.displayNotice("Settings changes saved!", UINotificationBannerControl.Severity.INFO, 1500);
                config.fillCache();
            }
        });

        revert.setOnAction(e ->{
            if(config.getCache() == null || compareLiveAndCachedIndexes()){
                notice.displayNotice("No changes to discard!", UINotificationBannerControl.Severity.INFO, 1000);
                return;
            }

            if(new ARKInterfaceDialogYN("Warning", "All unsaved changes will be lost! Continue?", "Yes", "No").display()){
                config.commitCache();
                notice.displayNotice("Settings changes discarded.", UINotificationBannerControl.Severity.INFO, 2000);
                config.fillCache();
                forceSettingsUpdate();
            }
        });

        globalDefaults.setOnAction(e ->{
            String res = new ARKInterfaceDialog("Warning", "ALL settings will be reset to their default values! ALL CONFIGURED RULES WILL ALSO BE CLEARED!\nEnter 'commit' if you are sure you want to do this:", "Confirm", "Cancel", "'commit'").display();
            if(res == null || !res.toLowerCase().equals("commit")){
                notice.displayNotice("Reset aborted.", UINotificationBannerControl.Severity.INFO, 1000);
            }else{
                config.loadAllDefaults();
                config.clearCache();
                notice.displayNotice("All settings reset to global defaults!", UINotificationBannerControl.Severity.WARNING, 4000);
                forceSettingsUpdate();
            }
        });

        defaults.setOnAction(e -> {
            if(!new ARKInterfaceDialogYN("Warning", "This will reset all settings in this tab to their original defaults! Proceed?", "Confirm", "Cancel").display()) return;
            ConfigManagerTabProperty tab = tabs[mode.get()];

            if(tab.dataPairs != null)
                for(KeyedActionCallback cb : tab.dataPairs)
                    config.loadDefault(cb.key);

            notice.displayNotice("All settings in this tab have been reset to defaults.", UINotificationBannerControl.Severity.INFO, 2500);
        });

        moreOptions.setOnAction(e ->{
            cachedMode = mode.get();
            categorySelect.setDisable(true);
            notice.displayNotice("This menu contains some powerful settings. Be careful!", UINotificationBannerControl.Severity.WARNING, 2000);
            mode.set(MODE_SPECIAL);
        });

        returnToMain.setOnAction(e ->{
            categorySelect.setDisable(false);
            mode.set(cachedMode);
        });

        saveConfig.setOnAction(e -> exportConfigToFile());

        loadConfig.setOnAction(e -> importConfigFromFile());
    }

    /**
     * Force this Manager to update its display state to reflect the current stored settings list.
     * Called when the {@link #display()} method is called, usually not necessary to call this externally.
     */
    public void forceSettingsUpdate()
    {
        // If the config does not have a stored cache (e.g if the Manager window is currently hidden), fill the cache from
        // the main settings index. If not (if the Manager window is open and editing), leave the cache as it is, and just
        // force an update of the displayed nodes.
        if(config.getCache() == null) config.fillCache();

        modeControl.switchMode(mode.get());

        // Check to see if the current mode is the advanced options menu mode. If it is, bypass setting updates, since
        // this menu does not have any actively linked nodes.
        if(mode.get() != MODE_SPECIAL){
            ConfigManagerTabProperty tab = tabs[mode.get()];
            // Call each of the action callbacks in the current tab. These will update any mode-local nodes in the tab.
            if(tab.dataPairs != null)
                for(KeyedActionCallback e : tab.dataPairs)
                    e.call();
        }
    }

    /**
     * Re-positions all UI elements to their proper locations and sizes upon window display.
     */
    private void repositionElements()
    {
        if(!linkedSizeListeners)
        {
            layout.widthProperty().addListener(e -> {
                repositionOnResize();
            });

            layout.heightProperty().addListener(e -> {
                repositionOnResize();
                categorySelect.setPrefHeight(layout.getHeight());
            });

            linkedSizeListeners = true;

            Platform.runLater(() ->{
                // Set the padding in the layout to exclude the left menu bar, and update the menu bar's position to place it outside of those bounds.
                // Essentially, we are making it impossible for other nodes to clash with the menu bar.
                layout.setPadding(new Insets(layout.getPadding().getTop(), layout.getPadding().getRight(), layout.getPadding().getBottom(), layout.getPadding().getLeft() + categorySelect.getWidth()));
                AnchorPane.setLeftAnchor(categorySelect, -1 * layout.getPadding().getLeft());

                // Manually call the resize method to ensure that the changes are passed along to all child nodes
                repositionOnResize();
            });
        }

        JFXUtil.setElementPositionInGrid(layout, close, -1, 0, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, moreOptions, -1, 0.75, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, configFunctionsContainer, 0, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, defaults, 0, -1, -1, 1);

        JFXUtil.setElementPositionInGrid(layout, returnToMain, -1, 0.75, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, additionalOptionsDesc, -1, -1, 1.25, -1);
        JFXUtil.setElementPositionInGrid(layout, saveConfig, 0, -1, 2, -1);
        JFXUtil.setElementPositionInGrid(layout, loadConfig, 0, -1, 3, -1);
        JFXUtil.setElementPositionInGrid(layout, globalDefaults, 0, -1, 4, -1);

        JFXUtil.setElementPositionInGrid(layout, info, 0, -1, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, notificationSeparator, 0, -1, 1, -1);

        JFXUtil.setElementPositionInGrid(layout, options, 0, 0, 1.5, 2);

        Platform.runLater(() ->{
            info.setMaxHeight(notificationSeparator.getLayoutY());
            JFXUtil.setElementPositionCentered(layout, additionalOptionsDesc, true, false);
        });
    }

    /**
     * Called whenever the window or layout resizes on the X or Y axis.
     */
    private void repositionOnResize()
    {
        info.setMaxWidth(layout.getWidth() - layout.getPadding().getLeft());
        notificationSeparator.setFitWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight());
        moreOptions.setLayoutX(close.getLayoutX() - (10 * JFXUtil.SCALE));
        returnToMain.setLayoutX(close.getLayoutX() - (10 * JFXUtil.SCALE));
    }

    private void setElementTooltips()
    {
        defaults.setTooltip(new Tooltip("Revert all options in this tab to their original default settings."));
        close.setTooltip(new Tooltip("Close this window. You will be asked to save changes if there are any."));
        save.setTooltip(new Tooltip("Save any changes."));
        revert.setTooltip(new Tooltip("Discard any changes."));
        moreOptions.setTooltip(new Tooltip("Show the advanced options menu, which contains more powerful (and risky!) options."));

        saveConfig.setTooltip(new Tooltip("Export the current set of configuration settings to an external file."));
        loadConfig.setTooltip(new Tooltip("Load previously exported settings from a file."));
        globalDefaults.setTooltip(new Tooltip("Reset ALL settings to their original defaults. THIS INCLUDES RULE PREFERENCES!"));
        returnToMain.setTooltip(new Tooltip("Go back to the standard configuration menu."));
    }

    private boolean compareLiveAndCachedIndexes()
    {
        // Check to see if the cache and the main index are identical by comparing their keysets and linked values.
        HashMap<String, Object> index = config.getAllSettings();
        HashMap<String, Object> cache = config.getCache();

        boolean identical = true;
        for(String s : index.keySet()) {
            if(!cache.containsKey(s) || !cache.get(s).equals(index.get(s))){
                identical = false;
                break;
            }
        }

        return identical;
    }

    private void exportConfigToFile()
    {
        FileChooser fc = new FileChooser();
        fc.setTitle("Specify Export Location");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ARK Exported Configuration File", "*.x34cx"));

        File f = fc.showSaveDialog(window);
        if(f == null) return;

        // Delete existing file if there is one
        if(f.exists() && !f.delete()){
            notice.displayNotice("Chosen file already exists, and could not be overwritten. Delete the file manually and try again!", UINotificationBannerControl.Severity.WARNING, 3000);
            return;
        }

        // Try to create a new destination file
        boolean succ = false;
        try {
            succ = f.createNewFile();
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));

            // Write the version number along with the exported file. This does two things: one, prevents users from copying
            // exported configuration files into the actual config directory, and two, helps prevent compatibility issues
            // between major revisions.
            os.writeLong(JFXConfigKeySet.MAJOR_CONFIG_VERSION);
            os.writeObject(config.getAllSettings());
            os.flush();
            os.close();
        } catch (IOException e) {
            log.logEvent(LogEventLevel.DEBUG, "Encountered I/O error while trying to create config export file. Details below.");
            log.logEvent(e);
            succ = false;
        }finally {
            if(!succ) notice.displayNotice("Could not create destination file! Try a different export location.", UINotificationBannerControl.Severity.WARNING, 3000);
            else notice.displayNotice("Successfully exported settings!", UINotificationBannerControl.Severity.INFO, 2000);
        }
    }

    private void importConfigFromFile()
    {
        FileChooser fc = new FileChooser();
        fc.setTitle("Specify Import Location");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ARK Exported Configuration File", "*.x34cx"));

        File f = fc.showOpenDialog(window);

        if(f == null || !f.exists()) return;

        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));

            long ver = is.readLong();
            if(ver != JFXConfigKeySet.MAJOR_CONFIG_VERSION){
                notice.displayNotice("Version mismatch! The imported file appears to be from an older (or newer) version of this program, and cannot be read.", UINotificationBannerControl.Severity.WARNING, 3000);
                return;
            }

            @SuppressWarnings("unchecked")
            HashMap<String, Object> importedConfig = (HashMap<String, Object>)is.readObject();

            // If the user wishes to import the new config, wipe the old config (excluding defaults) and import the new one, forcing
            // a UI element update after we have finished.
            if(importedConfig != null && new ARKInterfaceDialogYN("Notice", "Found a valid configuration file containing " +
                    importedConfig.size() + " valid settings! Overwrite the existing config with it?", "Yes", "No").display()){
                config.clearStorage();
                config.clearCache();
                config.storeMultipleSettings(importedConfig);
                forceSettingsUpdate();

                // Lock out the 'return' and 'defaults' buttons, preventing the user from making config changes until the next program restart.
                returnToMain.setOnAction(e -> new ARKInterfaceAlert("Warning", "Please restart the program before making any additional changes!").display());
                globalDefaults.setDisable(true);

                notice.displayNotice("New config imported! Please restart the program to commit changes.", UINotificationBannerControl.Severity.INFO, 10000);
            }
        } catch (Exception e) { // generic exception block due to the number of exceptions that can be thrown by the ReadObject method, and the lack of specific handling for them in this case
            log.logEvent(LogEventLevel.DEBUG, "Encountered I/O error while attempting to import configuration file, details below.");
            log.logEvent(e);
        }
    }

    private void requestClose(boolean save)
    {
        // If the user wishes to save changes, accept the live settings copy as the used version and hide. Otherwise,
        // commit the saved version of the config and hide.
        if(save){
            config.clearCache();
            hide();
        }else{
            config.commitCache();
            hide();
        }
    }

    @Override
    public void display()
    {
        repositionElements();
        forceSettingsUpdate();
        notice.displayNotice("This is the notification area. System messages and other notifications will appear here.", UINotificationBannerControl.Severity.INFO, 5000);
        super.display();
    }
}

/**
 * Container-type object.
 */
class ConfigManagerTabProperty
{
    String name;
    KeyedActionCallback[] dataPairs;

    ConfigManagerTabProperty(String name, KeyedActionCallback... data)
    {
        this.name = name;
        this.dataPairs = data;
    }

    /**
     * Returns itself to enable chain-calling.
     */
    ConfigManagerTabProperty addAction(String key, Callback<KeyedNodeBundle<?>, Void> action, Node n)
    {
        KeyedActionCallback[] temp = new KeyedActionCallback[dataPairs.length + 1];
        System.arraycopy(dataPairs, 0, temp, 0, dataPairs.length);
        temp[temp.length - 1] = new KeyedActionCallback(key, action, n);
        dataPairs = temp;

        return this;
    }
}

/**
 * Container-type object.
 */
class KeyedActionCallback
{
    String key;
    Callback<KeyedNodeBundle<?>, Void> action;
    Node boundNode;

    KeyedActionCallback(String key, Callback<KeyedNodeBundle<?>, Void> action, Node boundNode)
    {
        this.key = key;
        this.action = action;
        this.boundNode = boundNode;
    }

    void call(){
        action.call(new KeyedNodeBundle<>(X34ConfigDelegator.getMainInstance().getSettingOrDefault(key,
                X34ConfigDelegator.getMainInstance().getDefaultSetting(key)), boundNode));
    }
}

/**
 * Container-type object.
 */
class KeyedNodeBundle<T>
{
    Node node;
    T data;

    KeyedNodeBundle(T data, Node node)
    {
        this.node = node;
        this.data = data;
    }
}