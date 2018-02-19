package X34.UI;

import X34.Core.IO.X34ConfigDelegator;
import X34.Core.IO.X34Config;
import core.CoreUtil.JFXUtil;
import core.UI.ARKInterfaceDialogYN;
import core.system.ARKAppCompat;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
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
    private MenuItem fileMenuModeSelectManual;
    private MenuItem fileMenuModeSelectAuto;
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
    private final File configFile = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\X34",
            "JFXGeneralConfig.x34c");

    //
    // CONSTANTS
    //

    private static final int MODE_MANUAL = 0;
    private static final int MODE_AUTOMATIC = 1;

    //
    // CONFIG KEYS
    //

    private static final String WINDOW_MODE_KEY = "window_mode";

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
        menuBarInit();
        nodePositioningInit();
        setActions();
    }

    private void systemInit()
    {
        config = X34ConfigDelegator.getMainInstance();
        config.setTarget(configFile);

        // Try loading config. If it fails, assume that there is no valid config file, and load defaults instead.
        try{
            config.loadConfigFromFile();
            config.getSettingOrStore(WINDOW_MODE_KEY, 0);
            //todo add other config assignments
        }catch (IOException e){
            config.loadAllDefaults();
        }
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
        fileMenuModeSelectManual = new MenuItem();
        fileMenuModeSelectAuto = new MenuItem();
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
        fileMenuModeSelectManual.setText("Manual");
        //todo temporary, remove when dynamic symbol updates are in place
        fileMenuModeSelectManual.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/menu/ic_check_200px.png", 20));
        fileMenuModeSelectAuto.setGraphic(JFXUtil.generateGraphicFromResource("X34/assets/GUI/menu/ic_check_200px.png", 20));
        fileMenuModeSelectAuto.setText("Automatic");
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

        fileMenuModeSwitch.getItems().addAll(fileMenuModeSelectManual, fileMenuModeSelectAuto);
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
    }

    private void switchMode(int mode)
    {
        switch (mode)
        {
            case MODE_MANUAL:

                break;
            case MODE_AUTOMATIC:

                break;
        }
    }

    private void setActions()
    {
        fileMenuExit.setOnAction(e -> exit());

        fileMenuModeSelectManual.setOnAction(e -> switchMode(MODE_MANUAL));
        fileMenuModeSelectAuto.setOnAction(e -> switchMode(MODE_AUTOMATIC));

        functionsMenuStartRetrieval.setOnAction(e -> retrieve());
    }

    private void exit(){
        //todo finish
        try {
            config.writeStoredConfigToFile();
        } catch (IOException e) {
            if(!new ARKInterfaceDialogYN("Warning", "Unable to save configuration changes! Exit anyway?", "Exit", "Cancel").display())
                return;
        }
        System.exit(0);
    }

    private void retrieve()
    {
        switch ((int)config.getSetting(WINDOW_MODE_KEY)){
            case MODE_MANUAL:
                //todo finish
                break;
            case MODE_AUTOMATIC:
                //todo finish
                break;
            default:
                return;
        }
    }

    //
    // UTILITIES
    //

    private static void setMenuItemKeyModifierDefaults(MenuItem node, String keyID)
    {
        if(node == null) return;
        node.setAccelerator(new KeyCharacterCombination(keyID, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
    }
}
