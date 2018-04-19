package X34.UI.JFX;

import X34.Core.*;
import X34.Core.IO.X34ConfigDelegator;
import X34.Core.IO.X34Config;
import X34.Core.IO.X34IndexDelegator;
import X34.Processors.X34ProcessorRegistry;
import X34.Processors.X34RetrievalProcessor;
import X34.UI.JFX.Managers.*;
import X34.UI.JFX.Util.*;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.*;
import core.system.ARKAppCompat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static X34.UI.JFX.Util.JFXConfigKeySet.*;

@SuppressWarnings({"FieldCanBeLocal"})
public class X34UI extends Application
{
    //
    // JFX BASE WINDOW
    //

    private Stage window;
    private Scene menu;
    private AnchorPane layout;

    //
    // MENU ELEMENTS
    //

    private MenuBar mainMenuBar;
    private Menu fileMenu;
    private Menu functionsMenu;
    private Menu windowMenu;
    private Menu helpMenu;


    private Menu fileMenuModeSwitch;
    private MenuItem fileMenuModeSelectSimple;
    private MenuItem fileMenuModeSelectAdvanced;
    private MenuItem fileMenuExit;

    private MenuItem functionsMenuStartRetrieval;
    private MenuItem functionsMenuStopRetrieval;

    private MenuItem windowMenuRuleManager;
    private MenuItem windowMenuOptionsManager;
    private MenuItem windowMenuFileManager;
    private MenuItem windowLogDisplayManager;

    //
    // MODE-LOCAL NODES
    //

    // DEAR GOD SO MANY ANNOTATIONS

    // Manual-mode controls

    @ModeLocal({MODE_SIMPLE})
    private TextField tagInputField;

    @ModeLocal({MODE_SIMPLE})
    private ListView<X34RetrievalProcessor> processorList;

    @ModeLocal({MODE_SIMPLE})
    private Button changeOutputDirectory;

    @ModeLocal({MODE_SIMPLE})
    private TextField outputDirectoryView;

    @ModeLocal({MODE_SIMPLE})
    private Label tagInputLabel;

    // Result caching controls

    @ModeLocal(identifier = "State", value = {STATE_RUNNING, STATE_FINISHED})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private ListView<RetrievalResultCache> results;

    @ModeLocal(identifier = "State", value = {STATE_RUNNING, STATE_FINISHED})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Button getResult;

    @ModeLocal(identifier = "State", value = {STATE_RUNNING, STATE_FINISHED})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Button resultDetails;

    @ModeLocal(identifier = "State", value = {STATE_RUNNING, STATE_FINISHED})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Button dropResult;

    // Status fields

    //@ModeLocal(identifier = "State", value = STATE_RUNNING)
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private VBox statusContainer;

    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Label masterStatus;

    @ModeLocal(identifier = "State", value = {STATE_BUSY, STATE_RUNNING})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private ProgressIndicator masterStateIndicator;

    private Label ruleStatus;
    private Label ruleProgressLabel;
    private ProgressBar ruleProgressIndicator;

    private Label processorStatus;
    private Label processorProgressLabel;
    private ProgressBar processorProgressIndicator;

    private Label pageStatus;
    private Label pageProgressLabel;
    private ProgressBar pageProgressIndicator;

    // Result state fields

    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private VBox resultDetailContainer;

    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Label resultTagLabel;

    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Label resultIndexSizeLabel;

    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Label resultNewCountLabel;

    // Download status fields

    @ModeLocal(identifier = "State", value = STATE_FINISHED)
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private VBox downloadStatusContainer;

    private Label downloadStatus;
    private Label downloadProgressLabel;
    private ProgressBar downloadProgressIndicator;

    // Auto-mode controls

    @ModeLocal({MODE_ADVANCED})
    private ListView<X34Rule> autoRuleList;

    @ModeLocal({MODE_ADVANCED})
    private Button editRuleListShortcut;

    @ModeLocal(identifier = "State", invert = true, value = {STATE_IDLE})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private ImageView ruleIndicatorArrow1;

    @ModeLocal(identifier = "State", invert = true, value = {STATE_IDLE})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private ImageView ruleIndicatorArrow2;

    // Common-mode controls

    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Button startRetrieval;

    @ModeLocal(identifier = "State", value = {STATE_RUNNING, STATE_BUSY})
    @ModeLocal({MODE_SIMPLE, MODE_ADVANCED})
    private Button stopRetrieval;

    //
    // MODE-COMMON NODES
    //

    private ImageView notificationSeparator;
    private Label info;

    //
    // INSTANCE VARIABLES
    //

    private X34Config config;
    private X34Core core;
    private XLoggerInterpreter log;
    private X34UIRuleManager ruleMgr;
    private X34UIFileManager fileMgr;
    private X34UIConfigManager configMgr;
    private X34UIConsoleLogManager logMgr;
    private X34UIResultDetailManager detailMgr;
    private UINotificationBannerControl notice;

    private ModeSwitchController modeControl;
    private ModeSwitchController stateControl;

    private Map<X34Rule, SimpleBooleanProperty> ruleListValues;
    private Map<X34RetrievalProcessor, SimpleBooleanProperty> processorListValues;
    private ArrayList<Task> registeredRetrievalTasks;

    private SimpleIntegerProperty state;
    private SimpleIntegerProperty mode;
    private SimpleIntegerProperty maxRuleCountProperty;
    private SimpleIntegerProperty ruleCountProperty;

    private File configFile = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\X34\\Config","JFXGeneralConfig.x34c");

    //
    // CONSTANTS
    //

    private static final int MODE_SIMPLE = 0;
    private static final int MODE_ADVANCED = 1;

    private static final int STATE_IDLE = -100;
    private static final int STATE_BUSY = -101;
    private static final int STATE_RUNNING = -102;
    private static final int STATE_FINISHED = -103;

    private static final String STATUS_MASTER_BASE = "Status: ";
    private static final String STATUS_RULE_BASE = "Rule ";
    private static final String STATUS_PROC_BASE = "Repo ";
    private static final String STATUS_DOWNLOAD_BASE = "Download ";
    private static final String STATUS_PAGINATION_FULL = "Progress";

    private static final String RESULT_TAG_BASE = "Tag: ";
    private static final String RESULT_SIZE_BASE = "Index Size: ";
    private static final String RESULT_NEW_BASE = "New Images: ";

    //
    // PRIMARY METHODS
    //

    @Override
    public void start(Stage primaryStage)
    {
        // INITIALIZE JAVAFX WINDOW
        window = new Stage();

        window.setResizable(true);
        window.setIconified(false);
        window.setMinHeight(600 * JFXUtil.SCALE);
        window.setMinWidth(600 * JFXUtil.SCALE);
        window.setX((Screen.getPrimary().getBounds().getWidth() / 2) - (window.getMinWidth() / 2));
        window.setY((Screen.getPrimary().getBounds().getHeight() / 2) - (window.getMinHeight() / 2));
        window.setTitle("ARK Extensible Repository Manager");
        window.getIcons().add(new Image("X34/assets/x34-main.png"));

        window.setOnCloseRequest(e -> {
            e.consume();
            exit();
        });

        layout = new AnchorPane();
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 450, 400);
        window.setScene(menu);
        window.show();

        systemInit();
    }

    private void systemInit()
    {
        state = new SimpleIntegerProperty();
        mode = new SimpleIntegerProperty();
        maxRuleCountProperty = new SimpleIntegerProperty();
        ruleCountProperty = new SimpleIntegerProperty();

        log = new XLoggerInterpreter("X34-JFX UI");
        log.setImplicitEventLevel(LogEventLevel.DEBUG);
        //todo re-add when done with debugging: log.changeLoggerVerbosity(LogVerbosityLevel.STANDARD);
        
        try {
            modeControl = new ModeSwitchController();
            stateControl = new ModeSwitchController("State");
        } catch (ClassNotFoundException e) {
            log.logEvent(LogEventLevel.FATAL, "Unable to initialize mode-switch or state-switch controllers due to missing class(es).");
            log.logEvent(e);
            System.exit(4041);
        }

        // Add automatic change listener for the internal state and mode properties
        state.addListener((observable, oldValue, newValue) -> {
            if(oldValue.equals(newValue)) return;
            stateControl.switchMode(newValue.intValue());
        });
        
        mode.addListener((observable, oldValue, newValue) -> {
            if(oldValue.equals(newValue)) return;
            mode.set(newValue.intValue());
        });

        //
        // CONFIG
        //

        config = X34ConfigDelegator.getMainInstance();
        config.setTarget(configFile);
        core = new X34Core();

        ruleMgr = new X34UIRuleManager((window.getX() - window.getWidth() / 2) - X34UIRuleManager.DEFAULT_WIDTH / 2,
                (window.getY() - window.getHeight() / 2) - X34UIRuleManager.DEFAULT_HEIGHT / 2);

        fileMgr = new X34UIFileManager((window.getX() - window.getWidth() / 2) - X34UIFileManager.DEFAULT_WIDTH / 2,
                (window.getY() - window.getHeight() / 2) - X34UIFileManager.DEFAULT_HEIGHT / 2);

        configMgr = new X34UIConfigManager((window.getX() - window.getWidth() / 2) - X34UIConfigManager.DEFAULT_WIDTH / 2,
                (window.getY() - window.getHeight() / 2) - X34UIConfigManager.DEFAULT_HEIGHT / 2);

        logMgr = new X34UIConsoleLogManager(100, 200, window.getX() + window.getWidth(), window.getY());

        detailMgr = new X34UIResultDetailManager((window.getX() - window.getWidth() / 2) - X34UIResultDetailManager.DEFAULT_WIDTH / 2,
                (window.getY() - window.getHeight() / 2) - X34UIResultDetailManager.DEFAULT_HEIGHT / 2);


        // Try loading config. If it fails, assume that there is no valid config file, and load defaults instead.

        int delayedMode = 0;

        // DEFAULTS
        config.setDefaultSetting(KEY_WINDOW_MODE, delayedMode);
        config.setDefaultSetting(KEY_CONFIG_FILE, configFile);
        config.setDefaultSetting(KEY_INDEX_DIR, X34IndexDelegator.getMainInstance().getParent());
        config.setDefaultSetting(KEY_LOGGING_DIR, log.getLogDirectory());
        config.setDefaultSetting(KEY_OUTPUT_DIR, ARKAppCompat.getOSSpecificDesktopRoot());

        try{
            // LOAD
            log.logEvent("Loading configuration file...");
            config.loadConfigFromFile();
            log.logEvent("Configuration file loaded.");
            log.logEvent("Loaded " + config.getAllSettings().keySet().size() + " configuration settings.");
        }catch (IOException e){
            // FALLBACK
            log.logEvent(LogEventLevel.WARNING, "Failed to load config file. Loading defaults.");
        }finally {
            // Ensure that any settings that were not present in the loaded index (ex. if versions have changed) are loaded
            // to their proper defaults. This will also cover cases where the index load failed, in which case this will act
            // as though we called loadAllDefaults().
            config.loadDefaultsIfMissing();
            try {
                config.writeStoredConfigToFile();
            } catch (IOException e1) {
                log.logEvent(LogEventLevel.DEBUG, "Encountered error while writing config settings to file, see below for details.");
                log.logEvent(e1);
            }

            // RETRIEVE
            delayedMode = config.getSettingOrStore(KEY_WINDOW_MODE, delayedMode);
            configFile = config.getSettingOrStore(KEY_CONFIG_FILE, configFile);
        }

        //
        // INSTANCE VARIABLE INIT
        //

        ruleListValues = new HashMap<>();
        processorListValues = new HashMap<>();
        registeredRetrievalTasks = new ArrayList<>();

        // Add hook for mode-switch UI menu graphic change.
        modeControl.addModeSwitchHook((mode -> {
            switch (mode){
                case MODE_SIMPLE:
                    log.logEvent("Switching to mode: SIMPLE/MANUAL");
                    fileMenuModeSelectSimple.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/menu/ic_check_200px.png", (int)(15 * JFXUtil.SCALE)));
                    fileMenuModeSelectAdvanced.setGraphic(null);
                    break;
                case MODE_ADVANCED:
                    log.logEvent("Switching to mode: ADVANCED/AUTO");
                    fileMenuModeSelectSimple.setGraphic(null);
                    fileMenuModeSelectAdvanced.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/menu/ic_check_200px.png", (int)(15 * JFXUtil.SCALE)));
                    break;
            }
        }));

        // Add hook for mode-switch disable on retrieval start.
        stateControl.addModeSwitchHook((mode -> {
            fileMenuModeSelectAdvanced.setDisable(mode == STATE_RUNNING || mode == STATE_BUSY);
            fileMenuModeSelectSimple.setDisable(mode == STATE_RUNNING || mode == STATE_BUSY);
        }));

        //
        // SUBCALLS
        //

        nodeInit();
        menuBarInit();
        nodePositioningInit();
        setActions();
        mode.set(delayedMode);
        state.set(STATE_IDLE);
    }

    private void nodeInit()
    {
        info = new Label("");
        notificationSeparator = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_taper_horiz_256x4.png"));
        info.setWrapText(true);
        notice = new UINotificationBannerControl(info);


        tagInputField = new TextField();
        tagInputLabel = new Label("Tag/Query: ");
        processorList = new ListView<>();
        changeOutputDirectory = new Button("Change...");
        outputDirectoryView = new TextField();

        outputDirectoryView.setText(config.getSettingOrDefault(KEY_OUTPUT_DIR, ARKAppCompat.getOSSpecificDesktopRoot()).getAbsolutePath());
        tagInputField.setPromptText("Tag or Query...");
        try {
            processorList.getItems().addAll(X34ProcessorRegistry.getAvailableProcessorObjects());
        } catch (ClassNotFoundException | IOException e) {
            log.logEvent(LogEventLevel.DEBUG, "Error retrieving processor list, details below.");
            log.logEvent(e);

            processorList.getItems().add(new X34RetrievalProcessor() {
                @Override
                public ArrayList<X34Image> process(X34Index index, X34Schema schema) {
                    return null;
                }

                @Override
                public boolean validateIndex(X34Index index) {
                    return false;
                }

                @Override
                public String getID() {
                    return null;
                }

                @Override
                public String getInformalName() {
                    return "No processors available!";
                }

                @Override
                public String getFilenameFromURL(URL source) {
                    return null;
                }
            });

            processorList.setDisable(true);
        }

        processorList.setEditable(false);

        // set up selection model for processor list
        StringConverter<X34RetrievalProcessor> scp = new StringConverter<X34RetrievalProcessor>() {
            @Override
            public String toString(X34RetrievalProcessor object) {
                return object == null ? "null" : object.getInformalName();
            }

            @Override
            public X34RetrievalProcessor fromString(String string) {
                return X34ProcessorRegistry.getProcessorForID(string);
            }
        };
        processorList.setCellFactory(param -> {
            CheckBoxListCell<X34RetrievalProcessor> cell = new CheckBoxListCell<>(param12 -> {
                if(processorListValues.containsKey(param12)){
                    return processorListValues.get(param12);
                }else{
                    SimpleBooleanProperty bp = new SimpleBooleanProperty();
                    bp.set(true);
                    bp.addListener((observable, oldValue, newValue) -> checkModeSpecificEnableState());

                    processorListValues.put(param12, bp);
                    return bp;
                }
            });

            cell.setConverter(scp);
            return cell;
        });

        outputDirectoryView.setPromptText("Output directory");
        outputDirectoryView.setEditable(false);

        // Add change listener to the tag field to ensure that the start button is in the correct state
        tagInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!oldValue.equals(newValue)) checkModeSpecificEnableState();
        });

        results = new ListView<>();
        getResult = new Button("Get");
        resultDetails = new Button("Details");
        dropResult = new Button("Discard");

        // Set up interpreter for results list
        results.setCellFactory(param -> new ListCell<RetrievalResultCache>(){
            @Override
            protected void updateItem(RetrievalResultCache item, boolean empty){
                super.updateItem(item, empty);

                if(empty || item == null || item.name == null) setText(null);
                else setText(item.name);
            }
        });

        // Disable the result list if there were no images from the retrieval process, or re-enable it if there are some
        results.itemsProperty().addListener((observable, oldValue, newValue) -> results.setDisable(newValue == null || newValue.size() == 0));

        results.setEditable(false);
        results.setDisable(true);
        getResult.setDisable(true);
        resultDetails.setDisable(true);
        dropResult.setDisable(true);

        // Add listeners for auto-hiding/showing the result statistic fields
        results.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.equals(newValue)) return;

            // If the value is out-of-bounds, hide the status fields. If it is in-bounds, show them.
            resultDetailContainer.setVisible(newValue.intValue() >= 0 && newValue.intValue() < results.getItems().size());
        });
        results.disabledProperty().addListener((observable, oldValue, newValue) -> resultDetailContainer.setVisible(!newValue));


        statusContainer = new VBox();

        masterStatus = new Label(STATUS_MASTER_BASE);
        masterStateIndicator = new ProgressIndicator();

        ruleStatus = new Label(STATUS_RULE_BASE);
        ruleProgressLabel = new Label("");
        ruleProgressIndicator = new ProgressBar();

        processorStatus = new Label(STATUS_PROC_BASE);
        processorProgressLabel = new Label("");
        processorProgressIndicator = new ProgressBar();

        pageStatus = new Label(STATUS_PAGINATION_FULL);
        pageProgressLabel = new Label("");
        pageProgressIndicator = new ProgressBar();

        masterStateIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        masterStateIndicator.setVisible(false);

        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.getChildren().addAll(masterStatus, masterStateIndicator, ruleStatus, ruleProgressLabel, ruleProgressIndicator,
                processorStatus, processorProgressLabel, processorProgressIndicator, pageStatus, pageProgressLabel, pageProgressIndicator);
        statusContainer.setVisible(false);

        // Add listeners and value handlers for the telemetry returned by the core class during retrieval
        state.addListener((observable, oldValue, newValue) -> {
            switch (newValue.intValue()){
                case STATE_IDLE:
                    masterStatus.setText(STATUS_MASTER_BASE + "Idle");
                    masterStateIndicator.setVisible(false);
                    break;
                case STATE_BUSY:
                    masterStatus.setText(STATUS_MASTER_BASE + "Busy");
                    masterStateIndicator.setVisible(true);
                    break;
                case STATE_RUNNING:
                    masterStatus.setText(STATUS_MASTER_BASE + "Running");
                    masterStateIndicator.setVisible(true);
                    break;
                case STATE_FINISHED:
                    masterStatus.setText(STATUS_MASTER_BASE + "Finished!");
                    masterStateIndicator.setVisible(false);
                    break;
            }
        });

        core.processorCountProperty().addListener((observable, oldValue, newValue) -> {
            processorProgressLabel.setText((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : newValue.intValue() + " / ")
                    + (core.maxProcessorCountProperty().get() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : core.maxProcessorCountProperty().get()));

            processorProgressIndicator.setProgress((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ||
                    core.maxProcessorCountProperty().get() == X34Core.INVALID_INT_PROPERTY_VALUE) ? ProgressIndicator.INDETERMINATE_PROGRESS :
                    (double)newValue.intValue() / (double)core.maxProcessorCountProperty().get());

            processorStatus.setText(core.currentProcessorProperty().get() == null ? "" : core.currentProcessorProperty().get());
        });

        core.paginationProperty().addListener((observable, oldValue, newValue) -> {
            pageProgressLabel.setText((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : newValue.intValue() + " / ")
                    + (core.maxPaginationProperty().get() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : core.maxPaginationProperty().get()));

            pageProgressIndicator.setProgress((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ||
                    core.maxPaginationProperty().get() == X34Core.INVALID_INT_PROPERTY_VALUE) ? ProgressIndicator.INDETERMINATE_PROGRESS :
                    (double)newValue.intValue() / (double)core.maxPaginationProperty().get());
        });

        ruleCountProperty.addListener((observable, oldValue, newValue) -> {
            ruleProgressLabel.setText((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : newValue.intValue() + " / ")
                    + (maxRuleCountProperty.get() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : maxRuleCountProperty.get()));

            ruleProgressIndicator.setProgress((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ||
                    maxRuleCountProperty.get() == X34Core.INVALID_INT_PROPERTY_VALUE) ? ProgressIndicator.INDETERMINATE_PROGRESS :
                    (double)newValue.intValue() / (double)maxRuleCountProperty.get());

            ruleStatus.setText(core.currentQueryProperty().get() == null ? "" : core.currentQueryProperty().get());
        });


        downloadStatusContainer = new VBox();

        downloadStatus = new Label(STATUS_DOWNLOAD_BASE);
        downloadProgressLabel = new Label("");
        downloadProgressIndicator = new ProgressBar();

        downloadStatusContainer.getChildren().addAll(downloadStatus, downloadProgressLabel, downloadProgressIndicator);

        core.downloadProgressProperty().addListener((observable, oldValue, newValue) -> {
            downloadProgressLabel.setText((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : newValue.intValue() + " / ")
                    + (core.maxDownloadProgressProperty().get() == X34Core.INVALID_INT_PROPERTY_VALUE ? "" : core.maxDownloadProgressProperty().get()));

            downloadProgressIndicator.setProgress((newValue.intValue() == X34Core.INVALID_INT_PROPERTY_VALUE ||
                    core.maxDownloadProgressProperty().get() == X34Core.INVALID_INT_PROPERTY_VALUE) ? ProgressIndicator.INDETERMINATE_PROGRESS :
                    (double)newValue.intValue() / (double)core.maxDownloadProgressProperty().get());

            downloadStatus.setText(core.currentDownloadProperty().get() == null ? "" : core.currentDownloadProperty().get());
        });


        resultDetailContainer = new VBox();

        resultTagLabel = new Label(RESULT_TAG_BASE);
        resultIndexSizeLabel = new Label(RESULT_SIZE_BASE);
        resultNewCountLabel = new Label(RESULT_NEW_BASE);

        resultDetailContainer.getChildren().addAll(resultTagLabel, resultIndexSizeLabel, resultNewCountLabel);
        resultDetailContainer.setVisible(false);


        autoRuleList = new ListView<>();
        editRuleListShortcut = new Button("Edit Rule List...");
        ruleIndicatorArrow1 = JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_right_256px.png", 25);
        ruleIndicatorArrow2 = JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_right_256px.png", 25);

        autoRuleList.setEditable(false);

        // set up selection model for rule list
        final StringConverter<X34Rule> sc = new StringConverter<X34Rule>() {
            @Override
            public String toString(X34Rule object) {
                return object == null ? "null" : object.query;
            }

            @Override
            public X34Rule fromString(String string) {
                X34Rule foundObject = null;
                ArrayList<X34Rule> ruleList = config.getSettingOrDefault(KEY_RULE_LIST, new ArrayList<>());

                for(X34Rule r : ruleList)
                    if (r.query.equals(string)) foundObject = r;

                if(foundObject != null){
                    return new X34Rule(string, foundObject.getMetaData(), foundObject.getProcessorList());
                }else{
                    try {
                        return new X34Rule(string, null, X34ProcessorRegistry.getAvailableProcessorIDs()[0]);
                    } catch (ClassNotFoundException | IOException e) {
                        return null;
                    }
                }
            }
        };
        autoRuleList.setCellFactory(param -> {
            CheckBoxListCell<X34Rule> cell = new CheckBoxListCell<>(param1 -> {
                if(ruleListValues.containsKey(param1)){
                    ruleListValues.get(param1).set(param1 == null || param1.getMetaData() == null || !param1.getMetaData().containsKey("disabled"));
                    return ruleListValues.get(param1);
                }else{
                    SimpleBooleanProperty bp = new SimpleBooleanProperty();

                    // Auto-update the selection state based on the master disable tag set by the Rule Manager
                    bp.set(param1 == null || param1.getMetaData() == null || !param1.getMetaData().containsKey("disabled"));
                    bp.addListener((observable, oldValue, newValue) -> checkModeSpecificEnableState());

                    // Set up change-implementation listener for the checkbox state
                    bp.addListener((observable, oldValue, newValue) -> {
                        if(param1 == null) return;
                        if(param1.getMetaData() == null) param1.setMetaData(new HashMap<>());

                        if(newValue) param1.getMetaData().remove("disabled");
                        else param1.getMetaData().put("disabled", null);
                    });
                    ruleListValues.put(param1, bp);
                    return bp;
                }
            });

            cell.setConverter(sc);
            return cell;
        });

        autoRuleList.getItems().addAll(config.getSettingOrDefault(KEY_RULE_LIST, new ArrayList<>()));


        startRetrieval = new Button("Start Retrieval");
        stopRetrieval = new Button("Stop Retrieval");

        stopRetrieval.setVisible(false);

        // oh boy here we go...
        layout.getChildren().addAll(tagInputField, tagInputLabel, processorList, changeOutputDirectory, outputDirectoryView, results, getResult, resultDetails,
                dropResult, statusContainer, resultDetailContainer, autoRuleList, editRuleListShortcut, ruleIndicatorArrow1, ruleIndicatorArrow2,
                startRetrieval, stopRetrieval, downloadStatusContainer);
    }

    private void menuBarInit()
    {
        //
        // INSTANTIATION
        //

        mainMenuBar = new MenuBar();

        fileMenu = new Menu();
        functionsMenu = new Menu();
        windowMenu = new Menu();
        helpMenu = new Menu();

        fileMenuModeSwitch = new Menu();
        fileMenuModeSelectSimple = new MenuItem();
        fileMenuModeSelectAdvanced = new MenuItem();
        fileMenuExit = new MenuItem();

        functionsMenuStartRetrieval = new MenuItem();
        functionsMenuStopRetrieval = new MenuItem();

        windowMenuRuleManager = new MenuItem();
        windowMenuOptionsManager = new MenuItem();
        windowMenuFileManager = new MenuItem();
        windowLogDisplayManager = new MenuItem();

        //
        // TEXT AND APPEARANCE
        //

        fileMenu.setText("File");
        functionsMenu.setText("Functions");
        windowMenu.setText("Windows");
        helpMenu.setText("Help");

        fileMenuModeSwitch.setText("Change Mode...");
        fileMenuModeSelectSimple.setText("Simple");
        fileMenuModeSelectAdvanced.setText("Advanced");
        fileMenuExit.setText("Exit...");

        functionsMenuStartRetrieval.setText("Start Retrieval...");
        functionsMenuStopRetrieval.setText("Stop Retrieval");

        windowMenuFileManager.setText("Show File Manager...");
        windowMenuOptionsManager.setText("Show Settings Menu...");
        windowMenuRuleManager.setText("Show Rule Manager...");
        windowLogDisplayManager.setText("Show/Hide System Log");

        //
        // SHORTCUTS AND ACCELERATORS
        //

        setMenuItemKeyModifierDefaults(fileMenuExit, "X");

        setMenuItemKeyModifierDefaults(functionsMenuStartRetrieval, "S");
        setMenuItemKeyModifierDefaults(functionsMenuStopRetrieval, "R");

        setMenuItemKeyModifierDefaults(windowMenuFileManager, "F");
        setMenuItemKeyModifierDefaults(windowMenuOptionsManager, "O");
        setMenuItemKeyModifierDefaults(windowMenuRuleManager, "R");
        setMenuItemKeyModifierDefaults(windowLogDisplayManager, "L");

        //
        // ASSEMBLY
        //

        fileMenuModeSwitch.getItems().addAll(fileMenuModeSelectSimple, fileMenuModeSelectAdvanced);
        fileMenu.getItems().addAll(fileMenuModeSwitch, fileMenuExit);

        functionsMenu.getItems().addAll(functionsMenuStartRetrieval);
        functionsMenu.getItems().addAll(functionsMenuStopRetrieval);

        windowMenu.getItems().addAll(windowMenuFileManager);
        windowMenu.getItems().addAll(windowMenuOptionsManager);
        windowMenu.getItems().addAll(windowMenuRuleManager);
        windowMenu.getItems().addAll(windowLogDisplayManager);

        mainMenuBar.getMenus().addAll(fileMenu, functionsMenu, windowMenu, helpMenu);

        layout.getChildren().addAll(mainMenuBar);
    }

    private void nodePositioningInit()
    {
        // Bind the menu bar's width to the window width property to allow for automatic height adjustment.
        mainMenuBar.setPrefWidth(window.getWidth());

        layout.widthProperty().addListener(e -> {
            onWindowWSizeChange();
            onWindowSizeChange();
        });

        layout.heightProperty().addListener(e ->{
            onWindowHSizeChange();
            onWindowSizeChange();
        });

        mainMenuBar.layout();

        JFXUtil.setElementPositionInGrid(layout, info, 0, -1, 0, -1);

        // Manual-mode

        JFXUtil.setElementPositionInGrid(layout, notificationSeparator, 0, -1, 1.25, -1);
        JFXUtil.setElementPositionInGrid(layout, tagInputLabel, 0, -1, 2, -1);
        JFXUtil.setElementPositionInGrid(layout, changeOutputDirectory, 0, -1, 3, -1);
        JFXUtil.setElementPositionInGrid(layout, processorList, 0, -1, 4, -1);

        Platform.runLater(() ->{
            JFXUtil.bindAlignmentToNode(layout, changeOutputDirectory, outputDirectoryView, 10, Orientation.HORIZONTAL, JFXUtil.Alignment.CENTERED);
            JFXUtil.bindAlignmentToNode(layout, tagInputLabel, tagInputField, 10, Orientation.HORIZONTAL, JFXUtil.Alignment.CENTERED);

            // Bind the menu bar's height to the layout padding, to ensure that no layout elements clash with the menu bar.
            layout.setPadding(new Insets(mainMenuBar.getHeight() + layout.getPadding().getTop(),
                    layout.getPadding().getRight(), layout.getPadding().getBottom(), layout.getPadding().getLeft()));
            AnchorPane.setTopAnchor(mainMenuBar, -1 * layout.getPadding().getTop());

            // Bind the tag field's label to the width of the output directory change button to ensure equal spacing
            // Never let it be said that ARK Software is not an equal-spacing employer
            tagInputLabel.prefWidthProperty().bind(changeOutputDirectory.widthProperty());
        });

        // Auto-mode

        JFXUtil.setElementPositionInGrid(layout, autoRuleList, 0, -1, 2, -1);

        Platform.runLater(() ->{

        });

        // Common-mode

        JFXUtil.setElementPositionInGrid(layout, startRetrieval, -1, -1, -1, 4);
        JFXUtil.setElementPositionInGrid(layout, stopRetrieval, -1, -1, -1, 3);

        JFXUtil.setElementPositionInGrid(layout, statusContainer, -1, -1, 2, -1);

        Platform.runLater(() ->{
            results.heightProperty().addListener((observable, oldValue, newValue) -> JFXUtil.alignToNode(layout, results, resultDetailContainer,
                    10, Orientation.VERTICAL, JFXUtil.Alignment.CENTERED));

            // Manual call to size-change listeners to ensure resizing of all relevant elements even if the layout is not resized.
            onWindowHSizeChange();
            onWindowWSizeChange();
            onWindowSizeChange();
        });
    }

    // on height change only
    private void onWindowHSizeChange()
    {

    }

    // on width change only
    private void onWindowWSizeChange()
    {
        mainMenuBar.setPrefWidth(layout.getWidth());
        info.setPrefWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight());
        notificationSeparator.setFitWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight());

        JFXUtil.setElementPositionCentered(layout, startRetrieval, true, false);
        JFXUtil.setElementPositionCentered(layout, stopRetrieval, true, false);

        JFXUtil.setElementPositionCentered(layout, statusContainer, true, false);
    }

    // on both height and width change, exclusive with both other methods preferably
    private void onWindowSizeChange()
    {
        processorList.setPrefSize((layout.getWidth() - layout.getPadding().getRight() - layout.getPadding().getLeft()) * 0.3,
                layout.getHeight() - layout.getPadding().getTop() - layout.getPadding().getBottom() - (JFXUtil.DEFAULT_SPACING * 10));

        autoRuleList.setPrefSize((layout.getWidth() - layout.getPadding().getRight() - layout.getPadding().getLeft()) * 0.3,
                layout.getHeight() - layout.getPadding().getTop() - layout.getPadding().getBottom() - (JFXUtil.DEFAULT_SPACING * 8));

        JFXUtil.alignToNode(layout, autoRuleList, new Rectangle2D(autoRuleList.getLayoutX(), autoRuleList.getLayoutY(), autoRuleList.getWidth(), autoRuleList.getHeight()),
                ruleIndicatorArrow1, new Rectangle2D(ruleIndicatorArrow1.getX(), ruleIndicatorArrow1.getY(), ruleIndicatorArrow1.getFitWidth(), ruleIndicatorArrow1.getFitHeight()),
                5 * JFXUtil.SCALE, Orientation.HORIZONTAL, JFXUtil.Alignment.CENTERED);

        JFXUtil.alignToNode(layout, results, new Rectangle2D(results.getLayoutX(), results.getLayoutY(), results.getWidth(), results.getHeight()),
                ruleIndicatorArrow2, new Rectangle2D(ruleIndicatorArrow2.getX(), ruleIndicatorArrow2.getY(), ruleIndicatorArrow2.getFitWidth(), ruleIndicatorArrow2.getFitHeight()),
                -5 * JFXUtil.SCALE, Orientation.HORIZONTAL, JFXUtil.Alignment.CENTERED);
    }

    private void setActions()
    {
        // Menu options

        fileMenuExit.setOnAction(e -> exit());

        fileMenuModeSelectSimple.setOnAction(e -> mode.set(MODE_SIMPLE));
        fileMenuModeSelectAdvanced.setOnAction(e -> mode.set(MODE_ADVANCED));

        functionsMenuStartRetrieval.setOnAction(e -> retrieve());

        windowMenuRuleManager.setOnAction(e -> {
            ruleMgr.setWorkingList(config.getSettingOrDefault(KEY_RULE_LIST, new ArrayList<>()));
            ruleMgr.display();
            config.storeSetting(KEY_RULE_LIST, ruleMgr.getCurrentRuleList());
            autoRuleList.getItems().clear();
            autoRuleList.getItems().addAll(config.getSettingOrDefault(KEY_RULE_LIST, new ArrayList<>()));
        });
        windowMenuFileManager.setOnAction(e -> {
            fileMgr.display();
            // Update output directory display since it may have changed
            outputDirectoryView.setText(config.getSettingOrDefault(KEY_OUTPUT_DIR, ARKAppCompat.getOSSpecificDesktopRoot()).getAbsolutePath());
        });
        windowMenuOptionsManager.setOnAction(e -> configMgr.display());
        windowLogDisplayManager.setOnAction(e -> {
            if(logMgr.getVisibilityState()) logMgr.hide();
            else logMgr.display();
        });

        // Manual-mode

        changeOutputDirectory.setOnAction(e ->{
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Output Directory...");

            File f = chooser.showDialog(window);
            if(f != null && f.exists()) config.storeSetting(KEY_OUTPUT_DIR, f);
        });

        // Auto-mode



        // Common-mode

        startRetrieval.setOnAction(e -> retrieve());
        stopRetrieval.setOnAction(e ->{
            // Cancel the core's task and notify all pending and running tasks in the registration index
            core.cancelRetrieval();
            for(Task t : registeredRetrievalTasks) t.cancel();
        });

        getResult.setOnAction(e -> {
            int index = results.getSelectionModel().getSelectedIndex();
            if(index > 0 && index < results.getItems().size()) downloadSelectedItem(index);
        });
        dropResult.setOnAction(e -> {
            int index = results.getSelectionModel().getSelectedIndex();
            if(index < 0 || index >= results.getItems().size()) return;
            if(new ARKInterfaceDialogYN("Warning", "This result will no longer be available for download! Continue?", "Yes", "No").display())
                results.getItems().remove(index);
        });
        resultDetails.setOnAction(e ->{
            int index = results.getSelectionModel().getSelectedIndex();
            if(index > 0 && index < results.getItems().size()) {
                // Cannot be done inline due to potential ConcurrentModificationException in the list
                RetrievalResultCache temp = detailMgr.editResultEntry(results.getItems().get(index));
                results.getItems().set(index, temp);
            }
        });
    }

    private void retrieve()
    {
        registeredRetrievalTasks.clear();

        switch ((int)config.getSetting(KEY_WINDOW_MODE))
        {
            case MODE_SIMPLE:
                ArrayList<String> processors = new ArrayList<>();
                for(X34RetrievalProcessor rp : processorListValues.keySet()) if(processorListValues.get(rp).get()) processors.add(rp.getID());

                final X34Rule rule = new X34Rule(tagInputField.getText(), null, processors.toArray(new String[0]));

                Task<ArrayList<X34Image>> retrievalTask = new Task<ArrayList<X34Image>>() {
                    @Override
                    protected ArrayList<X34Image> call() throws Exception {
                        return core.retrieve(rule);
                    }
                };

                retrievalTask.setOnSucceeded(e ->{
                    notice.displayNotice("Retrieval complete! See the Results List for details.", UINotificationBannerControl.Severity.INFO, 4000);

                    ArrayList<X34Image> result = retrievalTask.getValue();

                    results.getItems().clear();
                    registeredRetrievalTasks.remove(retrievalTask);
                    ruleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
                    maxRuleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);

                    if(result == null || result.size() == 0){
                        state.set(STATE_IDLE);
                    }else {
                        results.getItems().add(new RetrievalResultCache(rule, result));
                        state.set(STATE_FINISHED);
                    }
                });

                retrievalTask.setOnFailed(e ->{
                    Throwable e1 = retrievalTask.getException();
                    new ARKInterfaceAlert("Warning", "Encountered internal validation error, see log for details.").display();
                    log.logEvent(LogEventLevel.DEBUG, "Rule/schema validation error, see below for details.");
                    if(e1 instanceof Exception) log.logEvent((Exception)e1);
                    registeredRetrievalTasks.remove(retrievalTask);
                    ruleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
                    maxRuleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
                    state.set(STATE_IDLE);
                });

                retrievalTask.setOnCancelled(e ->{
                    notice.displayNotice("Retrieval cancelled.", UINotificationBannerControl.Severity.INFO, 2500);
                    registeredRetrievalTasks.remove(retrievalTask);
                    ruleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
                    maxRuleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
                    state.set(STATE_IDLE);
                });

                ruleCountProperty.set(0);
                maxRuleCountProperty.set(1);

                Thread executor = new Thread(retrievalTask);
                executor.setDaemon(true);
                state.set(STATE_RUNNING);
                registeredRetrievalTasks.add(retrievalTask);
                executor.start();
                break;

            case MODE_ADVANCED:
                state.set(STATE_RUNNING);
                maxRuleCountProperty.set(autoRuleList.getItems().size());
                final Iterator<X34Rule> iterator = autoRuleList.getItems().iterator();
                runMTRetrieval(iterator);
                break;
        }
    }

    private void downloadSelectedItem(int index)
    {
        RetrievalResultCache result = results.getItems().get(index);
        HashMap<X34Rule, File> dirMap = config.getSettingOrDefault(KEY_RULEDIR_MAP, new HashMap<>());

        try {
            core.writeImagesToFile(result.results, dirMap.get(result.sourceRule) == null ?
                    config.getSettingOrDefault(KEY_OUTPUT_DIR, ARKAppCompat.getOSSpecificDesktopRoot()) : dirMap.get(result.sourceRule),
                    config.getSettingOrDefault(KEY_OVERWRITE_EXISTING, false), true);
            notice.displayNotice("Result(s) successfully written to file!", UINotificationBannerControl.Severity.INFO, 2500);
        } catch (IOException e) {
            notice.displayNotice("Error while writing one or more output files, see log for details.", UINotificationBannerControl.Severity.WARNING, 2500);
            log.logEvent(LogEventLevel.DEBUG, "I/O error during file write, see below for details.");
            log.logEvent(e);
        }
    }

    // Chain-called from subthreads. Will chain until it runs out of rules.
    private void runMTRetrieval(final Iterator<X34Rule> ruleSource)
    {
        // If the rule source has no more entries, we are done. Alert the user, set the final state, and return without chaining further.
        if(!ruleSource.hasNext()){
            if(results.getItems() == null || results.getItems().size() == 0){
                state.set(STATE_IDLE);
                Platform.runLater(() -> notice.displayNotice("No results available from retrieval. Results list hidden.", UINotificationBannerControl.Severity.WARNING, 4000));
            }else{
                state.set(STATE_FINISHED);
                Platform.runLater(() -> notice.displayNotice("All retrieval processes complete. See results list for more information.", UINotificationBannerControl.Severity.INFO, 4000));
            }

            ruleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
            maxRuleCountProperty.set(X34Core.INVALID_INT_PROPERTY_VALUE);
            return;
        }

        final X34Rule rule = ruleSource.next();

        ruleCountProperty.set(autoRuleList.getItems().indexOf(rule) + 1);

        Task<ArrayList<X34Image>> autoRetrievalTask = new Task<ArrayList<X34Image>>() {
            @Override
            protected ArrayList<X34Image> call() throws Exception {
                return core.retrieve(rule);
            }
        };

        // If the task succeeds, store the results and chain to the next task.
        autoRetrievalTask.setOnSucceeded(e ->{
            ArrayList<X34Image> result = autoRetrievalTask.getValue();

            if(result != null && result.size() != 0) results.getItems().addAll(new RetrievalResultCache(rule, result));
            registeredRetrievalTasks.remove(autoRetrievalTask);
            runMTRetrieval(ruleSource);
        });

        // If the task fails, it's due to a validation problem. Alert the user, and chain to the next task.
        autoRetrievalTask.setOnFailed(e ->{
            Throwable e1 = autoRetrievalTask.getException();
            Platform.runLater(() -> notice.displayNotice("Encountered internal validation error, see log for details.", UINotificationBannerControl.Severity.WARNING, 2500));
            log.logEvent(LogEventLevel.DEBUG, "Rule/schema validation error, see below for details.");
            if(e1 instanceof Exception) log.logEvent((Exception)e1);
            registeredRetrievalTasks.remove(autoRetrievalTask);
            runMTRetrieval(ruleSource);
        });

        // If the task is cancelled, do not chain to the next process and just flag results if any.
        autoRetrievalTask.setOnCancelled(e ->{
            notice.displayNotice("Retrieval cancelled.", UINotificationBannerControl.Severity.INFO, 2500);

            if(results.getItems() == null || results.getItems().size() == 0){
                state.set(STATE_IDLE);
                Platform.runLater(() -> notice.displayNotice("No results available from retrieval. Results list hidden.", UINotificationBannerControl.Severity.WARNING, 4000));
            }else{
                state.set(STATE_FINISHED);
                Platform.runLater(() -> notice.displayNotice("All retrieval processes complete. See results list for more information.", UINotificationBannerControl.Severity.INFO, 4000));
            }

            registeredRetrievalTasks.remove(autoRetrievalTask);
        });

        Thread t = new Thread(autoRetrievalTask);
        t.setDaemon(true);
        registeredRetrievalTasks.add(autoRetrievalTask);
        t.start();
    }

    private void exit()
    {
        // If there are uncommitted results, warn the user before exiting, as they will not be saved on exit.
        if(results.getItems().size() > 0 && new ARKInterfaceDialogYN("Warning", "Uncommitted results will be discarded and become unavailable " +
                "for download in future! Continue with exit?", "Exit", "Cancel").display()) return;

        try {
            // If the user has (intentionally or unintentionally) deleted the config file, do NOT save settings, as this would make
            // deleting the file a moot action.
            if(config.getTarget().exists()) config.writeStoredConfigToFile();
        } catch (IOException e) {
            if(!new ARKInterfaceDialogYN("Warning", "Unable to save configuration changes! Exit anyway?", "Exit", "Cancel").display())
                return;
        }
        System.exit(0);
    }

    //
    // UTILITIES
    //

    private void checkModeSpecificEnableState()
    {
        if(modeControl.getCurrentMode() == MODE_SIMPLE) startRetrieval.setDisable(checkManualStartEnableState());
        else if(modeControl.getCurrentMode() == MODE_ADVANCED) startRetrieval.setDisable(checkAutoStartEnableState());
    }

    private boolean checkManualStartEnableState()
    {
        boolean isSelected = false;
        for(SimpleBooleanProperty bp : processorListValues.values()){
            if(bp.getValue()){
                isSelected = true;
                break;
            }
        }

        return isSelected && tagInputField.getText().length() > 0;
    }

    private boolean checkAutoStartEnableState()
    {
        boolean isSelected = false;
        for(SimpleBooleanProperty bp : ruleListValues.values()){
            if(bp.getValue()){
                isSelected = true;
                break;
            }
        }

        return isSelected;
    }

    private static void setMenuItemKeyModifierDefaults(MenuItem node, String keyID)
    {
        if(node == null) return;
        node.setAccelerator(new KeyCharacterCombination(keyID, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
    }
}
