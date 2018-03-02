package X34.UI.JFX;

import X34.Core.IO.X34ConfigDelegator;
import X34.Core.IO.X34Config;
import X34.Core.X34Core;
import X34.Core.X34Rule;
import X34.UI.JFX.Managers.X34UIRuleManager;
import X34.UI.JFX.Util.ModeLocal;
import X34.UI.JFX.Util.ModeSwitchHook;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.LogVerbosityLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.ARKInterfaceDialogYN;
import core.system.ARKAppCompat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    //
    // JFX NODES
    //



    //
    // INSTANCE VARIABLES
    //

    private X34Config config;
    private X34Core core;
    private XLoggerInterpreter log;
    private X34UIRuleManager ruleMgr;

    private Map<Node, ModeLocal> annotatedNodes;
    private ArrayList<ModeSwitchHook> modeSwitchHooks;

    //
    // CONSTANTS
    //

    private final File configFile = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\X34","JFXGeneralConfig.x34c");

    private static final int MODE_SIMPLE = 0;
    private static final int MODE_ADVANCED = 1;

    //
    // CONFIG KEYS
    //

    private static final String WINDOW_MODE_KEY = "window_mode";
    private static final String RULE_LIST_KEY = "rules";

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
        //
        // CONFIG
        //

        log = new XLoggerInterpreter("X34-JFX UI");
        log.setImplicitEventLevel(LogEventLevel.DEBUG);
        log.changeLoggerVerbosity(LogVerbosityLevel.STANDARD);

        config = X34ConfigDelegator.getMainInstance();
        config.setTarget(configFile);
        core = new X34Core();
        ruleMgr = new X34UIRuleManager((Screen.getPrimary().getBounds().getWidth() / 2) - X34UIRuleManager.DEFAULT_WIDTH / 2, (Screen.getPrimary().getBounds().getHeight() / 2) - X34UIRuleManager.DEFAULT_HEIGHT / 2);

        // Try loading config. If it fails, assume that there is no valid config file, and load defaults instead.
        //todo add other config assignments
        try{
            log.logEvent("Loading configuration file...");
            config.loadConfigFromFile();
            log.logEvent("Configuration file loaded.");
            log.logEvent("Loaded " + config.getAllSettings().keySet().size() + " configuration settings.");
            config.getSettingOrStore(WINDOW_MODE_KEY, 0);

            // DEFAULTS
            config.setDefaultSetting(WINDOW_MODE_KEY, 0);
        }catch (IOException e){
            log.logEvent(LogEventLevel.WARNING, "Failed to load config file. Loading defaults.");
            config.loadAllDefaults();
        }

        //
        // INSTANCE VARIABLE INIT
        //

        modeSwitchHooks = new ArrayList<>();

        // Add hook for mode-switch UI menu graphic change.
        modeSwitchHooks.add(mode -> {
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
        });

        //
        // NODES
        //



        //
        // SUBCALLS
        //

        menuBarInit();
        nodePositioningInit();
        setActions();
        switchMode((int)config.getSetting(WINDOW_MODE_KEY));
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

        //
        // SHORTCUTS AND ACCELERATORS
        //

        setMenuItemKeyModifierDefaults(fileMenuExit, "X");

        setMenuItemKeyModifierDefaults(functionsMenuStartRetrieval, "S");
        setMenuItemKeyModifierDefaults(functionsMenuStopRetrieval, "R");

        setMenuItemKeyModifierDefaults(windowMenuFileManager, "F");
        setMenuItemKeyModifierDefaults(windowMenuOptionsManager, "O");
        setMenuItemKeyModifierDefaults(windowMenuRuleManager, "R");

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

        mainMenuBar.getMenus().addAll(fileMenu, functionsMenu, windowMenu, helpMenu);

        layout.getChildren().addAll(mainMenuBar);
    }

    private void nodePositioningInit()
    {
        // Bind the menu bar's width to the window width property to allow for automatic height adjustment.
        mainMenuBar.setPrefWidth(window.getWidth());
        window.widthProperty().addListener(e -> mainMenuBar.setPrefWidth(window.getWidth()));

        // Bind the menu bar's height to the layout padding, to ensure that no layout elements clash with the menu bar.
        mainMenuBar.layout();
        Platform.runLater(() -> layout.setPadding(new Insets(mainMenuBar.getHeight() + layout.getPadding().getTop(),
                layout.getPadding().getRight(), layout.getPadding().getBottom(), layout.getPadding().getLeft())));

        //todo align other elements
    }

    private void setActions()
    {
        fileMenuExit.setOnAction(e -> exit());

        fileMenuModeSelectSimple.setOnAction(e -> switchMode(MODE_SIMPLE));
        fileMenuModeSelectAdvanced.setOnAction(e -> switchMode(MODE_ADVANCED));

        functionsMenuStartRetrieval.setOnAction(e -> retrieve());

        windowMenuRuleManager.setOnAction(e -> {
            ruleMgr.setWorkingList(config.getSettingOrDefault(RULE_LIST_KEY, new ArrayList<>()));
            ruleMgr.display();
            config.storeSetting(RULE_LIST_KEY, ruleMgr.getCurrentRuleList());
        });

        //todo finish
    }

    private void retrieve()
    {
        switch ((int)config.getSetting(WINDOW_MODE_KEY)){
            case MODE_SIMPLE:
                //todo finish
                break;
            case MODE_ADVANCED:
                //todo finish
                break;
        }
    }

    private void exit(){
        try {
            config.writeStoredConfigToFile();
        } catch (IOException e) {
            if(!new ARKInterfaceDialogYN("Warning", "Unable to save configuration changes! Exit anyway?", "Exit", "Cancel").display())
                return;
        }
        System.exit(0);
    }

    //
    // UTILITIES
    //

    private static void setMenuItemKeyModifierDefaults(MenuItem node, String keyID)
    {
        if(node == null) return;
        node.setAccelerator(new KeyCharacterCombination(keyID, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
    }

    private void switchMode(int mode)
    {
        if(mode != MODE_SIMPLE && mode != MODE_ADVANCED) return;

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
                        if (o == null || !(o instanceof Node)) continue;
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

        config.storeSetting(WINDOW_MODE_KEY, mode);
    }
}
