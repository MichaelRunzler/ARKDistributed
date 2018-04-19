package X34.UI.JFX.Managers;

import X34.Core.IO.X34Config;
import X34.Core.IO.X34ConfigDelegator;
import X34.UI.JFX.Util.JFXConfigKeySet;
import core.UI.ModeLocal;
import core.UI.ModeSwitchHook;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    // JFX NODES
    //

    private TabPane categorySelect;

    private Button close;
    private Button save;
    private Button revert;

    private Label info;

    private ImageView notificationSeparator;

    //
    // INSTANCE VARIABLES
    //

    private XLoggerInterpreter log;
    private X34Config config;

    private int mode;
    private int cachedMode;
    private boolean linkedSizeListeners;
    private ConfigManagerTabProperty[] tabs;
    private Map<Node, ModeLocal> annotatedNodes;
    private ArrayList<ModeSwitchHook> modeSwitchHooks;
    private UINotificationBannerControl notice;

    //todo: add following settings: global output dir, autodownload, overwrite state, index push,

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
        modeSwitchHooks = new ArrayList<>();
        mode = 0;
        cachedMode = mode;
        linkedSizeListeners = false;

        //
        // JFX NODE INIT
        //

        categorySelect = new TabPane();

        options = new ScrollPane();
        optionsContainer = new VBox();

        close = new Button("Close");
        save = new Button("Save Changes");
        revert = new Button("Discard Changes");
        defaults = new Button("Reset Tab to Defaults");
        moreOptions = new Button("More");

        globalDefaults = new Button("Reset to Global Defaults");
        saveConfig = new Button("Export config to file...");
        loadConfig = new Button("Import config from file...");
        returnToMain = new Button("Back");

        moreOptions.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_right_256px.png", 15));
        returnToMain.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_left_256px.png", 15));

        saveConfig.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_bounded_arrow_fill_left_256px.png", 15));
        loadConfig.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_bounded_arrow_fill_right_256px.png", 15));

        additionalOptionsDesc = new Label("- Advanced Configuration Tools -");
        info = new Label("");

        notificationSeparator = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_horiz_256x4.png"));

        //
        // TABS
        //

        //todo add actual tabs
        ConfigManagerTabProperty tab1 = new ConfigManagerTabProperty("Tab 1");
        ConfigManagerTabProperty tab2 = new ConfigManagerTabProperty("Tab 2");
        ConfigManagerTabProperty tab3 = new ConfigManagerTabProperty("Tab 3");

        tabs = new ConfigManagerTabProperty[]{tab1, tab2, tab3};

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
            mode = newValue.intValue();
            forceSettingsUpdate();
        });

        modeSwitchHooks.add(mode -> {
            if(mode == -1 || mode == MODE_SPECIAL) return;

            ConfigManagerTabProperty tab = tabs[mode];

            // Clear the child list, then add the nodes in the tab in question if it has any
            optionsContainer.getChildren().clear();
            if(tab.dataPairs != null) for(KeyedActionCallback k : tab.dataPairs) optionsContainer.getChildren().add(k.boundNode);
        });

        setElementTooltips();

        layout.getChildren().addAll(categorySelect, close, save, revert, defaults, globalDefaults, info, saveConfig, loadConfig, additionalOptionsDesc, notificationSeparator, returnToMain, options);

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
            }
        });

        globalDefaults.setOnAction(e ->{
            String res = new ARKInterfaceDialog("Warning", "ALL settings will be reset to their default values! ALL CONFIGURED RULES WILL ALSO BE CLEARED!\nEnter 'commit' if you are sure you want to do this:", "Confirm", "Cancel", "'commit'").display();
            if(res == null || !res.toLowerCase().equals("commit")){
                notice.displayNotice("Reset aborted.", UINotificationBannerControl.Severity.INFO, 1000);
            }else{
                config.loadAllDefaults();
                notice.displayNotice("All settings reset to global defaults!", UINotificationBannerControl.Severity.WARNING, 4000);
            }
        });

        defaults.setOnAction(e -> {
            if(!new ARKInterfaceDialogYN("Warning", "This will reset all settings in this tab to their original defaults! Proceed?", "Confirm", "Cancel").display()) return;
            ConfigManagerTabProperty tab = tabs[mode];

            if(tab.dataPairs != null)
                for(KeyedActionCallback cb : tab.dataPairs)
                    config.loadDefault(cb.key);

            notice.displayNotice("All settings in this tab have been reset to defaults.", UINotificationBannerControl.Severity.INFO, 2500);
        });

        moreOptions.setOnAction(e ->{
            cachedMode = mode;
            mode = MODE_SPECIAL;
            categorySelect.setDisable(true);
            notice.displayNotice("This menu contains some powerful settings. Be careful!", UINotificationBannerControl.Severity.WARNING, 2000);
            switchMode();
        });

        returnToMain.setOnAction(e ->{
            mode = cachedMode;
            categorySelect.setDisable(false);
            switchMode();
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

        switchMode();

        // Check to see if the current mode is the advanced options menu mode. If it is, bypass setting updates, since
        // this menu does not have any actively linked nodes.
        if(mode != MODE_SPECIAL){
            ConfigManagerTabProperty tab = tabs[mode];
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
        JFXUtil.setElementPositionInGrid(layout, moreOptions, 2.75, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, save, 1.5, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, revert, 0, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, defaults, 0, -1, -1, 1);

        JFXUtil.setElementPositionInGrid(layout, returnToMain, 2.75, -1, -1, 0);
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

    //todo convert to ModeSwitchController
    /**
     * Switches the UI's display mode to the appropriate mode for the currently selected tab.
     */
    private void switchMode()
    {
        // If a cached copy of the node list is present, use that instead of the full reflection routine.
        if(annotatedNodes != null)
        {
            // Iterate through the list of annotated nodes, setting each one's visibility to the correct state.
            for(Node n : annotatedNodes.keySet()){
                ModeLocal ml = annotatedNodes.get(n);
                boolean visible = ml.invert();
                for(int i : ml.value()){
                    if(i == mode){
                        visible = !visible;
                        break;
                    }
                }

                n.setVisible(visible);
            }
        }else{
            Field[] fields = this.getClass().getDeclaredFields();

            Map<Node, ModeLocal> elements = new HashMap<>();

            // Iterate through the list of this class's declared fields, checking each one for annotations.
            // Check to see if the field is annotated with ModeLocal. If it is, check its parameters to see if they
            // match the mode provided. If they do, check that the field is an instance of a Node. If it is,
            // set the node's visibility to the correct visibility for the invert settings and the mode ID.
            for (Field f : fields)
            {
                ModeLocal ml = f.getAnnotation(ModeLocal.class);
                if (ml != null)
                {
                    Object o;
                    try {
                        o = f.get(this);
                        if (!(o instanceof Node)) continue;
                    } catch (IllegalAccessException ignored) {
                        continue;
                    }

                    boolean visible = ml.invert();
                    for (int i : ml.value()) {
                        if (i == mode) {
                            visible = !visible;
                            break;
                        }
                    }

                    ((Node) o).setVisible(visible);
                    elements.put((Node) o, ml);
                }
            }

            // Cache the node list for future access, since this is a fairly CPU-intensive process, and the results
            // will always be the same within any given run, due to annotations being a compile-time feature.
            annotatedNodes = elements;
        }

        // Execute any mode-switch hooks that are present.
        if(modeSwitchHooks != null && modeSwitchHooks.size() > 0){
            for(ModeSwitchHook m : modeSwitchHooks){
                m.onModeSwitch(mode);
            }
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
        action.call(new KeyedNodeBundle<>(X34ConfigDelegator.getMainInstance().getSetting(key), boundNode));
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