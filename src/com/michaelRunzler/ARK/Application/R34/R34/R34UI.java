package R34;

import com.sun.istack.internal.NotNull;
import core.UI.ARKInterfaceAlert;
import core.UI.ARKInterfaceDialogYN;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import core.system.ARKTransThreadTransport;
import core.system.ARKTransThreadTransportHandler;
import core.CoreUtil.RetrievalTools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import core.system.ARKTransThreadTransport.TransportType;

/**
 * The UI port of the ARK Rule 34 Dynamic Image Retrieval Utility.
 */
public class R34UI extends Application
{
    // GLOBAL VARIABLES
    // DO NOT TOUCH UNLESS YOU KNOW WHAT YOU ARE DOING
    // THEY DO THINGS, EVEN IF THEY LOOK STUPID

    // JavaFX window elements and nodes
    private Stage window;
    private Scene menu;
    private AnchorPane layout;

    private Button close;
    private Button retrieve;
    private Button selectDestDir;
    private Button options;
    private Button showLog;
    private Button stop;
    private Button help;
    private Button test;
    private TextField tagInput;
    private ChoiceBox<String> repoSelect;
    private CheckBox pushUpdates;
    private Label mainStatus;
    private Label ruleStatus;

    private ChoiceBox<String> modeSelect;

    private ListView<String> ruleView;
    private Button editRule;
    private Button newRule;
    private Button deleteRule;
    private Button moveUp;
    private Button moveDown;

    // Files: config, logging, directories, cache
    private final File MANUAL_CFG_FILE = new File(System.getenv("AppData") + "\\KAI\\ARK\\Config\\R34", "manual.vcss");
    private final File AUTO_CFG_FILE = new File(System.getenv("AppData") + "\\KAI\\ARK\\Config\\R34", "auto.vcss");
    private final File GENERAL_CFG_FILE = new File(System.getenv("AppData") + "\\KAI\\ARK\\Config\\R34", "global.vcss");
    private final File DATABASE = new File(System.getenv("AppData") + "\\KAI\\ARK\\R34", "database.xml");
    private File logFile = new File(System.getProperty("user.home") + "\\.R34Logs", "log.vcsl");
    private File outputDir = new File(System.getProperty("user.home") + "\\Desktop");
    private File index = new File(System.getenv("AppData") + "\\KAI\\ARK\\R34");

    // Global caching variables
    private long startTime = 0L;
    private int queuedModeSwitchChange = -1;
    private int totalDLCount = 0;
    private ArrayList<Rule> ruleList;
    private ArrayList<URL> loadedIndex;

    // Manager objects
    private R34OptionsManager optionsManager;
    private R34RuleManager ruleManager;
    private R34LoggingManager logger;
    private R34IntegratedHelpSectionManager helpManager;
    private ARKTransThreadTransportHandler handler;
    private R34RetrievalThreadExecutor executor;
    private R34ConnectionTestManager tester;
    private R34ConfigManager config;

    /**
     * Launches the application's core threads and initializes basic UI elements.
     * @throws Exception if the program throws an exception. Duh.
     */
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        // Alright, time to document this monster... oh, boy.
        // This is going to be a wild ride for anyone else reading this...
        // Comments are placed ABOVE the relevant method/loop/variable/object/lambda.
        //
        // A NOTE ON MANAGER CLASSES:
        // Manager classes are how much of the logic in this application (and others like it) is delegated.
        // A manager class will have the word 'manager' in its name, and will be an object class.
        // Classes that call manager functions, such as this one, are referred to as 'Supervisors'.
        // Following standard CS conventions, classes that call supervisor functions, such as the ARK Launcher,
        // are referred to as 'Hypervisors'.
        //
        // Now that the notes are out of the way, here goes...
        
        // Get the start time for the program for benchmarking purposes.
        startTime = System.currentTimeMillis();

        // INITIALIZE JAVAFX WINDOW
        window = new Stage();

        window.setResizable(false);
        window.setIconified(false);
        window.setMinHeight(400);
        window.setMinWidth(450);
        window.setX((Screen.getPrimary().getBounds().getWidth() / 2) - 400);
        window.setY((Screen.getPrimary().getBounds().getHeight() / 2) - 400);
        window.setTitle("ARK R34 Retrieval Engine");
        window.getIcons().add(new Image("R34/assets/r34-main.png"));

        window.setOnCloseRequest(e ->{
            e.consume();
            exitSystem();
        });

        layout = new AnchorPane();
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 450, 400);
        window.setScene(menu);
        window.show();
        
        // END JAVAFX INIT

        // Delegates initialization operations for variables, basic UI elements, and system configuration settings.
        sysPreInit();

        // Aligns UI elements, sets listeners and executors, and sets UI actions.
        sysInit();
    }

    /**
     * Initializes all UI elements and sets preliminary properties for objects.
     */
    private void sysPreInit()
    {
        // Set a default uncaught exception handler for the JavaFX core code.
        // If any exception occurs inside of the JavaFX core that is not managed by an internal catch block,
        // it will be caught by this code here and handled appropriately.
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if(e instanceof IllegalStateException)
                // This sometimes occurs if a subthread attempts to access a UI elements, such as a ListPane or other
                // such object. This is a nonfatal exception, and has no impact on operations, apart from logging an
                // annoying text block to the console. We catch the exception, and notify the dev console.
                System.out.println("FYI, caught ISEx from JFX core.");
        });

        // Initialize the system log file.
        try {
            logFile.getParentFile().mkdirs();
            if(logFile.exists()){
                logFile.delete();
            }
            logFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize system manager classes. These are submodules that handle complex tasks such as configuration management,
        // rule editing, and thread delegation.
        ruleList = new ArrayList<>();
        optionsManager = new R34OptionsManager("Options", 300, 400, window.getX() + 100, window.getY() + 100);
        ruleManager = new R34RuleManager("Rule Management", window.getX() + 100, window.getY() + 100);
        helpManager = new R34IntegratedHelpSectionManager("Program Help");
        executor = new R34RetrievalThreadExecutor();
        logger = new R34LoggingManager("System Event Log", 300, 400, window.getX() + window.getWidth() + 20,
                window.getY(), logFile);
        logger.updateLogOptions(false, true, true, logFile);
        logger.display();

        tester = new R34ConnectionTestManager("Connection Test");
        
        handler = new ARKTransThreadTransportHandler(logger);

        config = new R34ConfigManager(GENERAL_CFG_FILE, MANUAL_CFG_FILE, AUTO_CFG_FILE, logger);

        optionsManager.setCfgLocations(GENERAL_CFG_FILE, MANUAL_CFG_FILE, AUTO_CFG_FILE);

        // JAVAFX WINDOW ELEMENT OBJECT INIT
        close = new Button("Close");
        retrieve = new Button("Start Retrieval");
        selectDestDir = new Button("Select Destination");
        options = new Button("Options...");
        showLog = new Button("Show/Hide Log");
        stop = new Button("Stop Retrieval");
        help = new Button("Help");
        test = new Button("Test Connection");
        tagInput = new TextField();
        repoSelect = new ChoiceBox<>();
        pushUpdates = new CheckBox("Push to Index");
        mainStatus = new Label("Ready!");
        ruleStatus = new Label("");

        modeSelect = new ChoiceBox<>();

        ruleView = new ListView<>();
        editRule = new Button("Edit Rule");
        newRule = new Button("New Rule");
        deleteRule = new Button("Delete Rule");
        moveUp = new Button("Move Rule Up");
        moveDown = new Button("Move Rule Down");

        // END JAVAFX WINDOW ELEMENT OBJECT INIT

        // JAVAFX WINDOW ELEMENT TOOLTIP INIT
        tagInput.setPromptText("Tag...");
        repoSelect.getItems().addAll( "Both", "R34 Paheal", "R34");
        modeSelect.getItems().addAll("-Select Mode-", "Manual", "Automated");
        ruleView.setPrefSize(200, 300);

        logger.logEvent("Starting...");

        close.setTooltip(new Tooltip("Close the program and save configuration"));
        retrieve.setTooltip(new Tooltip("Start the retrieval process with current settings"));
        selectDestDir.setTooltip(new Tooltip("Select a new destination for manual-mode image downloads"));
        options.setTooltip(new Tooltip("Configure global program options, such as logging and indexing"));
        showLog.setTooltip(new Tooltip("Show or hide the system log view"));
        stop.setTooltip(new Tooltip("Stop an in-progress retrieval"));
        tagInput.setTooltip(new Tooltip("The tag to search for in the current repo (underscores are optional)"));
        repoSelect.setTooltip(new Tooltip("Which repo to use - Paheal, legacy R34, or both"));
        pushUpdates.setTooltip(new Tooltip("Uncheck this if you do not want new image records pushed to the stored index file"));
        modeSelect.setTooltip(new Tooltip("Switch the retrieval mode that the program is using, see Help for more information"));
        editRule.setTooltip(new Tooltip("Change the settings of the currently selected rule"));
        newRule.setTooltip(new Tooltip("Create a new rule and specify its settings"));
        deleteRule.setTooltip(new Tooltip("Delete the currently selected rule (will ask for confirmation first)"));
        moveUp.setTooltip(new Tooltip("Move the selected rule up in the list"));
        moveDown.setTooltip(new Tooltip("Move the selected rule down in the list"));
        help.setTooltip(new Tooltip("Show the Help window"));

        // END JAVAFX WINDOW ELEMENT TOOLTIP INIT

        logger.logEvent("Loading configuration...");

        // Tell the Config Manager to try and load the config. All operations are dealt with in that class, see its documentation
        // for more details. Errors (for the most part) are also dealt with there. The IO exception is thrown if the manager
        // runs into a problem it can't handle, so it tells its supervisor about it.
        try {
            config.readConfig();
        } catch (IOException e) {
            e.printStackTrace();
            logger.logEvent("Config reader encountered an IO error!\nConfig options may be set to defaults.");
        }

        // Get the UI mode from the Config Manager. If it is different from the current mode, queue it.
        // This has to be done because the UI doesn't deal with immediate mode switches very well, so we have to wait
        // to switch UI mode until AFTER the system has finished loading.
        queuedModeSwitchChange = config.getOpmode();
        modeSelect.getSelectionModel().select(queuedModeSwitchChange);

        // Update the Options Manager's list with the options from the Config Manager.
        logFile = config.getLogFile();
        optionsManager.loadOptions(config.isDoMD5(), config.isDoFileLog(), config.isDoLogTrim(), config.isDoSkipDLPrompt(), logFile, config.isDoPreview());
        logger.updateLogOptions(config.isDoLogTrim(), config.isDoFileLog(), config.isDoPreview(), config.getLogFile());
        outputDir = config.getOutputDir();

        // Update the UI with the options from the Config Manager.
        repoSelect.getSelectionModel().select(config.getRepoM());
        tagInput.setText(config.getTagM());
        pushUpdates.setSelected(config.isPushM());

        ruleList = config.getRuleList();
        for(Rule r : ruleList) {
            ruleView.getItems().add(r.name);
        }

        logger.logEvent("Configuration phase complete!");
    }

    /**
     * Displays the UI and manages user input logic for all main UI elements.
     */
    private void sysInit()
    {
        logger.logEvent("Pre-init phase complete!");
        // Add JavaFX UI elements to the UI's node list.
        layout.getChildren().addAll(close, retrieve, selectDestDir, options, showLog, tagInput, repoSelect, pushUpdates, modeSelect,
                ruleView, editRule, newRule, deleteRule, moveUp, moveDown, stop, help, test, mainStatus, ruleStatus);

        // Set the nodes in the list to the proper positions.
        setNodeAlignment(close, 0, -1, -1, 0);
        setNodeAlignment(retrieve, -1, 0, -1, 0);
        setNodeAlignment(selectDestDir, 160, -1, 0, -1);
        setNodeAlignment(options, 60, -1, -1, 0);
        setNodeAlignment(showLog, 140, -1, -1, 0);
        setNodeAlignment(help, 250,  -1, -1, 0);
        setNodeAlignment(test, -1, 0, 0, -1);
        setNodeAlignment(tagInput, 0, -1, 80, -1);
        setNodeAlignment(repoSelect, 0, -1, 40, -1);
        setNodeAlignment(pushUpdates, 0, -1, 120, -1);
        setNodeAlignment(modeSelect, 0, -1, 0, -1);
        setNodeAlignment(ruleView, 0, -1, 40, -1);
        setNodeAlignment(newRule, 220, -1, 40, -1);
        setNodeAlignment(editRule, 220, -1, 80, -1);
        setNodeAlignment(deleteRule, 220, -1, 120, -1);
        setNodeAlignment(moveUp, 220, -1, 160, -1);
        setNodeAlignment(moveDown, 220, -1, 200, -1);
        setNodeAlignment(stop, -1,0,-1,40);
        setNodeAlignment(ruleStatus, -1, 0, -1, 80);
        setNodeAlignment(mainStatus, -1, 0, -1, 100);

        logger.logEvent("Init phase complete!");

        // Self-explanatory.
        close.setOnAction(e -> exitSystem());

        // Self-explanatory.
        modeSelect.setOnAction(e -> switchMode(modeSelect.getSelectionModel().getSelectedIndex()));

        // Open the Options Manager.
        options.setOnAction(e -> {
            optionsManager.display();
            logFile = optionsManager.getLoggingLocation();
            //Once it finishes and closes, check to see if file logging is enabled, and send an update to the Logging Manager.
            if(!optionsManager.getDoFileLogging()){
                logger.logEvent("File logging disabled!");
                logger.updateLogOptions(optionsManager.getDoLogTrimming(), optionsManager.getDoFileLogging(), optionsManager.getDoPreview(), optionsManager.getLoggingLocation());
            }else{
                logger.updateLogOptions(optionsManager.getDoLogTrimming(), optionsManager.getDoFileLogging(), optionsManager.getDoPreview(), optionsManager.getLoggingLocation());
                logger.logEvent("File logging enabled!");
            }
        });

        // Self-explanatory.
        showLog.setOnAction(e ->{
            if(logger.getVisibilityState()){
                logger.hide();
            }else{
                logger.display();
            }
        });

        // The big one. This fires off the main sequence of events that manage the actual checking and downloading
        // part of the program. Sub-delegates out to five submethods and a thread executor, so be warned: this is nasty.
        retrieve.setOnAction(e -> {
            int mode = modeSelect.getSelectionModel().getSelectedIndex();
            // If the mode is incorrect (mode-0), or if one of the input fields does not validate properly, exit before doing anything.
            if(mode == 0 || (mode == 1 && tagInput.getText().length() <= 0) || (mode == 2 && ruleList.size() <= 0)){
                return;
            }

            // Check to make sure that the user has not already started a retrieval process.
            if(executor.verifyNoThreadExecution()){
                executor.clearThreadExecutionStack();
            }else{
                new ARKInterfaceAlert("Warning", "One or more background threads still appear to be executing!", 150,150).display();
                return;
            }

            // Check mode and start the appropriate retrieval sequence.
            switch(mode){
                case 1:
                    // In this case, we just want to run one sequence, so we start the standard retrieval method.
                    // The method will autodetect which mode it is in, so we don't have to call the executor to start.
                    runRetrieval(tagInput.getText(), repoSelect.getSelectionModel().getSelectedIndex(), pushUpdates.isSelected(), mode, outputDir);
                    break;
                case 2:
                    // In this case, we want to run multiple sequential threaded retrieval sequences, so we start each
                    // rule in the list in order, then call the executor to start executing the threads.
                    for (Rule r : ruleList) {
                        runRetrieval(r.tag, r.repo, r.push, mode, r.dest);
                    }
                    mainStatus.setText("Running rule " + (executor.getStackCounterPosition() + 1) + " of " + (executor.getStackSize() == 0 ? 1 : executor.getStackSize()) + "...");
                    executor.executeNextThread();
                    break;
            }
        });

        // Allow the user to change the output directory for image downloads.
        selectDestDir.setOnAction(e -> {
            DirectoryChooser outputCh = new DirectoryChooser();
            outputCh.setTitle("Select Output Directory");
            outputCh.setInitialDirectory(outputDir); // 'Show' the user the current image directory.
            File f = outputCh.showDialog(window);
            // If the user cancels, the dialog returns null. In that case, leave the destination as it is.
            outputDir = f == null ? outputDir : f;
        });

        // Open the R34.Rule Manager.
        editRule.setOnAction(e -> {
            int index = ruleView.getSelectionModel().getSelectedIndex();
            if(index >= 0){
                logger.logEvent("Starting edit on rule #" + (index + 1) + "...");
                ruleManager.showEditRuleUI(ruleList.get(index));

                // Get the edited rule from the R34.Rule Manager once it finishes and closes.
                Rule tmp = ruleManager.getCurrentRule();

                // Check to see if the user cancelled. If they didn't, and the rule is valid, pass it to the R34.Rule List.
                if(tmp != null) {
                    ruleList.set(index, tmp);
                    ruleView.getItems().set(index, tmp.name);
                    logger.logEvent("Rule edit complete.");
                }else{
                    logger.logEvent("Rule edit cancelled.");
                }
            }
        });

        // Same as above, but don't pass the R34.Rule Manager an existing rule. Validation and export are the same.
        newRule.setOnAction(e ->{
            ruleManager.showNewRuleUI();
            Rule tmp = ruleManager.getCurrentRule();
            if(tmp != null){
                ruleView.getItems().add(tmp.name);
                ruleList.add(tmp);
                logger.logEvent("New rule #" + ruleList.size() + " added.");
            }
        });

        // Check to see if the user has a rule selected in the list. If they do, ask for confirmation, and then delete it.
        deleteRule.setOnAction(e ->{
            int index = ruleView.getSelectionModel().getSelectedIndex();
            if(index >= 0 && new ARKInterfaceDialogYN("Warning!", "Are you sure you want to delete this rule?",
                    "Yes", "No", 150, 120).display())
            {
                ruleList.remove(index);
                ruleView.getItems().remove(index);

                logger.logEvent("Rule #" + (index + 1) + " deleted!");
            }
        });

        // Move a rule up in the rule list.
        moveUp.setOnAction(e ->{
            int index = ruleView.getSelectionModel().getSelectedIndex();
            // Check to make sure that the selected rule is valid and does not already occupy the top spot in the list.
            if(index > 0)
            {
                // Copy the rule that occupies the target spot and cache it, then move the new rule up one.
                Rule r = ruleList.get(index - 1);
                ruleList.set(index - 1, ruleList.get(index));
                ruleList.set(index, r);

                // Put the cached rule in the old rule's spot (down one), and then make sure the list is labelled correctly.
                String s = ruleView.getItems().get(index - 1);
                ruleView.getItems().set(index - 1, ruleView.getItems().get(index));
                ruleView.getItems().set(index, s);
                ruleView.getSelectionModel().select(index - 1);

                logger.logEvent("Move complete.");
            }
        });

        // Move a rule down in the rule list. Same as above, but in reverse. Move and validation operations are the same.
        moveDown.setOnAction(e ->{
            int index = ruleView.getSelectionModel().getSelectedIndex();
            if(index >= 0 && index < ruleList.size() - 1)
            {
                Rule r = ruleList.get(index + 1);
                ruleList.set(index + 1, ruleList.get(index));
                ruleList.set(index, r);

                String s = ruleView.getItems().get(index + 1);
                ruleView.getItems().set(index + 1, ruleView.getItems().get(index));
                ruleView.getItems().set(index, s);
                ruleView.getSelectionModel().select(index + 1);

                logger.logEvent("Move complete.");
            }
        });

        // Self-explanatory.
        help.setOnAction(e ->{
            if(helpManager.getVisibilityState()){
                helpManager.hide();
            }else{
                helpManager.display();
            }
        });

        // Self-explanatory.
        test.setOnAction(e ->{
            tester.testConnection();
            tester.display();
        });

        // The system has finished loading by this point, so we can switch to the actual UI mode specified by the
        // Config Manager in pre-init.
        // If the mode is mode-0 (default), we can ignore it, since the UI loads in mode-0.
        if(queuedModeSwitchChange >= 0)
            switchMode(queuedModeSwitchChange);

        // Dump some minimal configuration data to the log for troubleshooting purposes.
        logger.logEvent("Post-init phase complete!");
        logger.logEvent("Output directory set to:\n" + outputDir.getAbsolutePath());
        logger.logEvent("Log output directory set to:\n" + logFile.getAbsolutePath());
        logger.logEvent("Load complete in " + (System.currentTimeMillis() - startTime) + "ms.");
        logger.logEvent("Program ready.");
    }

    /**
     * Runs a retrieval operation, including initial load and post-pull cleanup operations.
     * Uses two background threads to execute retrieval operations by default.
     * Can use as many worker threads as there are virtual threads in the stack if the multi-threading option flag is set.
     * @param tag the tag to retrieve
     * @param repo the repo to pull from
     * @param push whether or not to push index changes to the file on disk
     */
    private void runRetrieval(String tag, int repo, boolean push, int mode, File dest)
    {
        // Set the UI so that the user cannot start another retrieval sequence while this one is running.
        stop.setVisible(true);
        modeSelect.setDisable(true);
        retrieve.setDisable(true);
        logger.showBottomOfLog();

        // The Service object that performs the retrieval. This is required because of how the JavaFX UI works - any
        // long-running process will lock up the UI if it is on the main thread, so we delegate it like this.
        Service<ArrayList<Integer>> retrievalSVC = new Service<ArrayList<Integer>>() {
            @Override
            protected Task<ArrayList<Integer>> createTask() {
                return new Task<ArrayList<Integer>>() {
                    @Override
                    protected ArrayList<Integer> call() throws Exception {
                        // Set the stop button to kill this process and tell the user if it stopped successfully.
                        stop.setOnAction(e1 ->{
                            mainStatus.setText("Ready!");
                            pushMTUpdateToStatusField("");
                            // If this is running through the Executor, cancel its stack. Else, tell this Service to cancel itself.
                            if(mode == 2)executor.cancelStackExecution(); else cancel();
                            pushPostRetrievalUIUpdate();
                            logger.logEvent("Retrieval stopped by user!");
                        });
                        return retrieveGlobal(repo, tag);
                    }
                };
            }
        };

        // Tell the user which thread we are running in the stack if we are running in mode-1 (or mode-2 with SMT),
        // or add the thread to the executor stack if we are running mode-2 with no SMT.
        if(modeSelect.getSelectionModel().getSelectedIndex() == 1 ||
                modeSelect.getSelectionModel().getSelectedIndex() == 2 && optionsManager.getDoMultiThreading()) {
            mainStatus.setText("Running rule " + (executor.getStackCounterPosition() + 1) + " of " + (executor.getStackSize() == 0 ? 1 : executor.getStackSize()) + "...");
            retrievalSVC.restart();
        }else{
            executor.registerThreadForExecution(retrievalSVC);
        }

        // If the main retrieval thread fails for some reason:
        retrievalSVC.setOnFailed(e ->{
            // Handle any exception the service may have thrown to the main thread.
            Throwable e1 = retrievalSVC.getException();
            if(e1 instanceof ARKTransThreadTransport){
                ((ARKTransThreadTransport)e1).handleTransportPacket();
            }

            // Check to see if there are any more threads to execute. If there are, keep going. If not, tell the user, and reset
            // the UI to its standby state.
            if(mode == 2 && !optionsManager.getDoMultiThreading()){
                if(executor.checkThreadStackCounter()){
                    new ARKInterfaceAlert("Notice", "All retrieval threads have finished with " + totalDLCount + " new images!", 125, 125).display();
                    totalDLCount = 0;
                    mainStatus.setText("Ready!");
                    pushMTUpdateToStatusField("");
                    pushPostRetrievalUIUpdate();
                }else{
                    mainStatus.setText("Running rule " + (executor.getStackCounterPosition() + 1) + " of " + (executor.getStackSize() == 0 ? 1 : executor.getStackSize()) + "...");
                    executor.executeNextThread();
                }
            }
        });

        // If the service has not reported any fatal errors:
        retrievalSVC.setOnSucceeded(e ->{
            // Get the list of new image indices from the service's buffer.
            ArrayList<Integer> values = retrievalSVC.getValue();

            // Check to see if the user wants to download new images, if there are any. If the skip-download flag is
            // set, we try to download without notifying the user (assuming there is anything to download, that is).
            boolean pull = false;
            if(values.size() > 0) {
                logger.logEvent("Looks like you're in luck!");
                pull = optionsManager.getDoSkipDLPrompt() || new ARKInterfaceDialogYN("Query", values.size()
                       + " image(s) available from tag "+ tag +"! Download?", "Yes!", "No...", 150, 150).display();
            }else{
                logger.logEvent("Looks like you're out of \nluck (and fap material)! \nBetter luck next time, eh?");
            }

            logger.showBottomOfLog();

            // Spawn another subthread that manages downloading and cleanup operations.
            // The Final variable is necessary because of how lambdas work with external variables - they must be final
            // or effectively final, so we ensure that the value when the lambda is initialized will remain constant.
            final boolean pullF = pull;
            Service<Void> postRetrievalSvc = new Service<Void>() {
                @Override
                protected Task<Void> createTask() {
                    return new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            retrievalCleanup(values, pullF, push, dest, isCancelled());
                            return null;
                        }
                    };
                }
            };

            // Disable the Stop button, as this has no effect on the post-retrieval service for some reason.
            stop.setDisable(true); //temporary
            stop.setOnAction(e1 ->{
                mainStatus.setText("Ready!");
                pushMTUpdateToStatusField("");
                postRetrievalSvc.cancel(); //fixme does not cancel subthread for some reason
                pushPostRetrievalUIUpdate();
                logger.logEvent("Retrieval stopped by user!");
            });

            // Start the post-retrieval thread.
            postRetrievalSvc.restart();

            // If the post-retrieval thread failed:
            postRetrievalSvc.setOnFailed(e1 ->{
                // Same thing as if the main thread failed - check to see if there are any more threads, notify the user,
                // and handle any exceptions that were cached by the subthread.
                if(mode == 2 && !optionsManager.getDoMultiThreading()){
                    if(executor.checkThreadStackCounter()){
                        new ARKInterfaceAlert("Notice", "All retrieval threads have finished with " + totalDLCount + " new images!", 125, 125).display();
                        totalDLCount = 0;
                        mainStatus.setText("Ready!");
                        pushMTUpdateToStatusField("");
                        pushPostRetrievalUIUpdate();
                    }else{
                        mainStatus.setText("Running rule " + (executor.getStackCounterPosition() + 1) + " of " + (executor.getStackSize() == 0 ? 1 : executor.getStackSize()) + "...");
                        executor.executeNextThread();
                    }
                }
                stop.setDisable(false); //temporary
                Throwable e2 = postRetrievalSvc.getException();
                if(e2 instanceof ARKTransThreadTransport){
                    ((ARKTransThreadTransport)e2).handleTransportPacket();
                }
            });

            // If the post-retrieval thread reported no fatal errors:
            postRetrievalSvc.setOnSucceeded(e1 ->{
                // Same thing as if the thread succeeded, but we send a different notice to the user and don't handle exceptions.
                if(mode == 2 && !optionsManager.getDoMultiThreading()){
                    if(executor.checkThreadStackCounter()){
                        new ARKInterfaceAlert("Notice", "All retrieval threads have finished with " + totalDLCount + " new images!", 125, 125).display();
                        totalDLCount = 0;
                        mainStatus.setText("Ready!");
                        pushMTUpdateToStatusField("");
                        pushPostRetrievalUIUpdate();
                    }else{
                        mainStatus.setText("Running rule " + (executor.getStackCounterPosition() + 1) + " of " + (executor.getStackSize() == 0 ? 1 : executor.getStackSize()) + "...");
                        executor.executeNextThread();
                    }
                }
                stop.setDisable(false); //temporary
                logger.logEvent("Retrieval complete!");
            });
        });
    }

    /**
     * Sets UI elements to their proper state when the retrieval routine finishes.
     */
    private void pushPostRetrievalUIUpdate()
    {
        // Self-explanatory.
        stop.setVisible(false);
        modeSelect.setDisable(false);
        retrieve.setDisable(false);
        stop.setOnAction(e2 ->{});
    }

    /**
     * Loads an index file and prepares the system to run a retrieval routine.
     */
    private void retrieveInit()
    {
        // Checks index file state and makes sure that the program is ready to run the actual retrieval sequence.
        logger.logEvent("Checking a few things in AppData. \nHold on a moment.");
        if(index.exists()){
            logger.logEvent("Found an index file! \nLoading it now...");

            // Make sure the index file is valid, and read it if it has any entries.
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(index));
                loadedIndex = (ArrayList<URL>) ois.readObject();
                ois.close();

                logger.logEvent("Loaded the index! *fist pump*");
            } catch (EOFException e){
                logger.logEvent("Existing index file is empty! \nWe'll write changes to it later.");
                loadedIndex = new ArrayList<>();
            } catch (IOException e) {
                logger.logEvent(e);
                logger.logEvent("Couldn't load from the index file. \nMaybe it's corrupt? \nTry deleting it and try again!");
                exitSystem();
            } catch (ClassNotFoundException e){
                logger.logEvent(e);
                logger.logEvent("OK, what did you do to your Java class libraries? \nOne of the core classes is missing!\n" +
                        "Try re-downloading this software or \nyour JRE/JDK and try again!");
                exitSystem();
            }
        }else{
            // If the index doesn't exist, write one and make sure it can accept a write stream.
            logger.logEvent("Just letting you know, I didn't find an \nindex file in the default directory.\n" +
                    "If that's okay, you don't \nneed to worry about this message.\n" +
                    "If not, you may want to check \nyour index storage directory.");
            index.getParentFile().mkdirs();
            try {
                index.createNewFile();
                loadedIndex = new ArrayList<>();
                logger.logEvent("Created the new index file!");
            } catch (IOException e) {
                logger.logEvent(e);
                logger.logEvent("Couldn't create the index file! \nTry checking your index directory.");
                exitSystem();
            }
        }

        // Delete any old database files before proceeding.
        if(DATABASE.exists() && DATABASE.delete()){
            logger.logEvent("Deleted old database file!");
        }
    }

    /**
     * Determines what type of retrieval routine to run and executes all pre-run operations.
     * @throws ARKTransThreadTransport if a problem is encountered during operation
     */
    private ArrayList<Integer> retrieveGlobal(int type, String tag) throws ARKTransThreadTransport
    {
        // Called from the runRetrieval method, checks the supplied tag and calls the appropriate method for its type.
        if(tag.length() > 0){
            if(outputDir.exists())
            {
                // Replace any unwanted characters in the tag with properly formatted underscores.
                tag = tag.replace(' ', '_');
                String str = tag.replace('?', '_');
                str = str.replace('!', '_');
                str = str.replace('<', '_');
                str = str.replace('>', '_');
                str = str.replace('*', '_');
                str = str.replace('|', '_');
                str = str.replace('/', '_');
                str = str.replace('\\', '_');
                str = str.replace(':', '_');

                // Assign the index variable to the currently querying tag so that the retrieveInit method can check it.
                index = new File(System.getenv("AppData") + "\\KAI\\ARK\\R34", "index-" + str + ".vcsi");
                retrieveInit();
                ArrayList<Integer> newImageIDs = null;

                // Check the retrieval type and run the appropriate retrieval routine.
                if(type == 0){
                    ArrayList<Integer> ni1 = retrieveR34P(tag);
                    ArrayList<Integer> ni2 = retrieveR34(tag);

                    newImageIDs = new ArrayList<>();
                    if(ni1 != null)
                        newImageIDs.addAll(ni1);
                    if(ni2 != null)
                        newImageIDs.addAll(ni2);
                }else if(type == 1){
                    newImageIDs = retrieveR34P(tag);
                }else if(type == 2){
                    newImageIDs = retrieveR34(tag);
                }

                // If all executed methods return NULL instead of an actual ArrayList (empty or not), they must have errored.
                // In that case, let the main method know and return.
                if(newImageIDs == null){
                    handler.dispatchTransThreadPacket("Retrieval encountered an error.", TransportType.ERROR);
                }

                logger.logEvent("OK, now that that's over with, here's the report:");
                logger.logEvent("");
                logger.logEvent("Tag searched: " + tag);
                logger.logEvent("Search type: " + type);
                logger.logEvent("Total images in index: " + loadedIndex.size());
                logger.logEvent("New images scanned to index: " + newImageIDs.size());
                logger.logEvent("");
                return newImageIDs;
            }else{
                handler.dispatchTransThreadPacket("Output directory does not exist.", TransportType.FATAL);
            }
        }else{
            handler.dispatchTransThreadPacket("Tag cannot be zero-length.", TransportType.FATAL);
        }
        return null;
    }

    /**
     * Handles new image processing and download operations.
     * @param newImageIDs the list of new image IDs from retrieveGlobal
     * @throws ARKTransThreadTransport if a problem is encountered during operation
     */
    private void retrievalCleanup(ArrayList<Integer> newImageIDs, boolean pull, boolean push, File outputDir, Boolean interrupt) throws ARKTransThreadTransport
    {
        int newImageCount = newImageIDs.size();
        // Check to see if the method should actually download images, or just update the index and clean up.
        boolean dirSkipFlag = true;
        // Check to make sure that the output directory exists. If it doesn't, try
        // to create it. If we can't, go to the download skip notification and set the directory skip flag.
        if(pull && (outputDir.exists() || (dirSkipFlag = outputDir.mkdirs()))) {
            logger.logEvent("OK, downloading the images now. \nJust a minute (or more, if your internet is crap)...");

            int k = 0;
            for (int i = newImageCount - 1; i >= 0; i--)
            {
                if(interrupt)
                    handler.dispatchTransThreadPacket("Download halted by user!");

                int j = newImageIDs.get(i);
                logger.logEvent("Downloading image " + (k + 1) + " of " + newImageCount + "...");
                pushMTUpdateToStatusField("Downloading: " + (k + 1) + " of " + newImageCount + "...");
                try {
                    String str = loadedIndex.get(j).getPath();
                    File imgF = new File(outputDir, str.substring(str.lastIndexOf("/") + 1, str.length()));

                    if (imgF.getAbsolutePath().length() >= 255) {
                        logger.logEvent("The combined file descriptor length of your \nimage is too long! Attempting to truncate...");
                        int pathLen = imgF.getParentFile().getAbsolutePath().length();

                        if (pathLen >= 250) {
                            logger.logEvent("The path to your download directory is too long! \nPlease choose another directory and restart.");
                            logger.logEvent("Skipped image URL:" + loadedIndex.get(j).getPath());
                            handler.dispatchTransThreadPacket("The path to your download directory is too long! Please choose another directory.", TransportType.ERROR);
                        } else {
                            imgF = new File(imgF.getParentFile(), imgF.getName().substring(imgF.getName().length() - (255 - pathLen), imgF.getName().length()));
                            logger.logEvent("Truncated to " + imgF.getName() + " sucessfully!");
                        }
                    }

                    if (!imgF.exists()) {
                        BufferedImage img;
                        img = ImageIO.read(loadedIndex.get(j).getPath().contains("https:") ?
                                new URL(loadedIndex.get(j).getPath().replace("https:", "http:")) : loadedIndex.get(j));

                        ImageIO.write(img, str.substring(str.lastIndexOf('.') + 1, str.length()), imgF);
                        logger.logEvent("Download successful!");
                        totalDLCount ++;
                        logger.logEventWithLink("<Select to preview, press 'Enter' to open>", imgF);
                    } else {
                        logger.logEvent("Image already exists! Skipping.");
                        totalDLCount ++;
                    }
                } catch (IOException e) {
                    logger.logEvent(e);
                    logger.logEvent("Failed to download image due to some sort of \nIO error! Trying the rest anyway...");
                    logger.logEvent("Skipped image URL:" + loadedIndex.get(j).getPath());
                } catch (IllegalArgumentException e) {
                    logger.logEvent(e);
                    logger.logEvent("This image is in a format that is not supported!");
                    logger.logEvent("Skipped image URL:" + loadedIndex.get(j).getPath());
                }
                k++;
            }
            logger.logEvent("Looks like that's all of 'em! Enjoy!");
        }else{
            // If we had to skip downloading because of directory problems, notify the user.
            if(!dirSkipFlag){
                logger.logEvent("Couldn't create download directory!");
                new ARKInterfaceAlert("Warning", "Couldn't create output directory! Try changing it to another folder, and try again.", 150, 150).display();
            }
            logger.logEvent("Skipping download.");
        }

        pushMTUpdateToStatusField("Updating index...");
        logger.logEvent("Don't go yet, Senpai! \nCleaning up a few things \n(just like you will have to later...)");
        logger.logEvent("Updating local index file...");

        // Check to see if we need to push changes to the index file.
        if(push)
        {
            // Delete the existing file, and write a new one with the contents we have in cache.
            try {
                if (index.exists()) {
                    index.delete();
                }
                index.createNewFile();

                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(index));
                oos.writeObject(loadedIndex);
                oos.flush();
                oos.close();
                logger.logEvent("Updated!");
            } catch (IOException e) {
                logger.logEvent(e);
                logger.logEvent("Couldn't write index to disk! \nCheck your index directory and try again!");
                handler.dispatchTransThreadPacket("Couldn't write index to disk!", TransportType.ERROR);
            }
        }else{
            logger.logEvent("Index update skipped \nas per configuration settings.");
        }

        logger.logEvent("Deleting local database copy...");

        // Try to delete the now-unused database file. If something still has a lock on it, leave it, and it'll get taken
        // care of on the next run.
        if(DATABASE.delete()){
            logger.logEvent("Deleted!");
        }else{
            logger.logEvent("Couldn't get it! \nYou can try deleting it manually, \nor I'll do it on the next program run. \nYour choice!");
        }

        logger.logEvent("All done! \nSee you next time, ya pervert! \\(^w^)/");
        pushMTUpdateToStatusField("Done!");
    }

    /**
     * Retrieves images from the R34.Rule 34 Paheal network.
     * @param tag the tag to search
     * @return the list of new image IDs in the master index
     * @throws ARKTransThreadTransport if a problem is encountered during operation
     */
    private ArrayList<Integer> retrieveR34P(String tag) throws ARKTransThreadTransport
    {
        // Global try/catch blocks are usually a bad idea, granted. But in this case, we need to catch exceptions at a lot
        // of different points in the code, and they are all dealt with in the same way - with a trans-thread transport
        // notification to their supervisor thread and a termination - so it helps with code complexity if we make it global.
        try {
            logger.logEvent("Starting retrieval \nfrom Paheal repository...");
            logger.logEvent("Getting page count...");

            // INITIALIZATION PHASE:
            // We first try to get the initial page for the tag. If it fails, we try four more times and continue if one
            // of them succeeds. If not, we throw the exception we got to the main try/catch block and deal with it there.
            IOException thrownByRead = null;
            int tryCounter = 0;
            do {
                try {
                    RetrievalTools.getFileFromURL("http://rule34.paheal.net/post/list/" + tag + "/1", DATABASE, true);
                } catch(ClosedByInterruptException e){
                    return null;
                } catch (IOException e){
                    logger.logEvent("Failed to obtain page number! Trying again...");
                    thrownByRead = e;
                    tryCounter ++;
                }
            } while (thrownByRead != null && tryCounter < 4);
            if(tryCounter >= 4){
                throw thrownByRead;
            }else{
                tryCounter = 0;
                thrownByRead = null;
            }

            ArrayList<URL> images = new ArrayList<>();
            ArrayList<Integer> indexes = new ArrayList<>();

            // PAGE LOCATOR PHASE:
            // Load the downloaded file from its cache on disk.
            String db = RetrievalTools.loadDataFromFile(DATABASE);

            int lastPageID = 1;

            // Try to get the last page number from the initial page.
            // If it fails, the number must be longer than we expected. Try shifting the end-mark right one and try again.
            // If the number is more than 4 digits, stop and throw the exception we got.
            if(db.contains("Last</a>")) {
                NumberFormatException thrown;
                int counter = 5;
                do{
                    try{
                        lastPageID = Integer.parseInt(db.substring(db.indexOf("Last</a>") - counter, db.indexOf("Last</a>") - 2));
                        thrown = null;
                    }catch(NumberFormatException e){
                        thrown = e;
                        counter --;
                    }
                }while(thrown != null && counter > 3);
                if(thrown != null){
                    throw thrown;
                }
            }

            int i = 1;
            logger.logEvent("Got page count as " + lastPageID);

            // RETRIEVAL PHASE:
            do{
                // PARSE SUBPHASE:
                // If we are on a multiple of 25 pages, wait for 4 seconds to avoid being marked as a DDoS client.
                if(lastPageID >= 25 && (double)i % 25.0 == 0){
                    logger.logEvent("Sleeping for 4s to avoid \noverloading the server...");
                    Thread.sleep(4000);
                    logger.logEvent("Done, continuing.");
                }

                logger.logEvent("Pulling image URLs \nfrom page " + i + " of " + lastPageID + "...");
                pushMTUpdateToStatusField("Pulling: page " + i + " of " + lastPageID + "...");

                // here there be dragons:
                int lastChar = 0;
                int parityCheck = 0;
                int lc2 = 0;
                int newCount = 0;

                // Load the list of image URLs from the downloaded page.
                while(parityCheck > -1){
                    lastChar = db.indexOf("/_images/", lastChar);
                    parityCheck = lastChar;
                    lastChar = lastChar + 9;
                    lc2 = db.indexOf(">Image Only", lc2) + 11;
                    if(parityCheck > -1){
                        images.add(new URL("http://rule34-data-" + db.substring(lastChar - 23, lc2 - 12)));
                        newCount ++;
                    }
                }

                logger.logEvent("Pull complete! Pulled " + newCount + " images.");

                // Increment page counter
                i++;

                // CLEANUP SUBPHASE:
                DATABASE.delete();

                // If we are NOT on the last page:
                if(i <= lastPageID) {
                    DATABASE.createNewFile();

                    // RESET SUBPHASE:
                    // Same as the initial page read.
                    do {
                        try {
                            RetrievalTools.getFileFromURL("http://rule34.paheal.net/post/list/" + tag + "/" + i, DATABASE, true);
                        } catch(ClosedByInterruptException e){
                            return null;
                        }  catch (IOException e){
                            logger.logEvent("Failed to obtain page data! Trying again...");
                            thrownByRead = e;
                            tryCounter ++;
                        }
                    } while (thrownByRead != null && tryCounter < 4);

                    if(tryCounter >= 4){
                        throw thrownByRead;
                    }else{
                        tryCounter = 0;
                        thrownByRead = null;
                    }
                    db = RetrievalTools.loadDataFromFile(DATABASE);
                }

                // Keep looping this code until we have gone through every page.
            }while(i <= lastPageID);

            // CHECK PHASE:
            // Check the loaded index to see how many out of the pulled URLs are actually new.
            logger.logEvent("Querying local image index...");
            for (URL url : images) {
                if (!loadedIndex.contains(url)) {
                    loadedIndex.add(url);
                    indexes.add(loadedIndex.indexOf(url));
                }
            }

            logger.logEvent("Retrieval complete with " + indexes.size() + " new image(s)!");
            pushMTUpdateToStatusField("Done!");

            return indexes;
        } catch (IOException e) {
            // If an IO error occurred, and it's not a result of the executor telling this thread to stop running,
            // notify the user of it and tell the supervisor thread.
            if(!(e instanceof ClosedByInterruptException)) {
                logger.logEvent(e);
                logger.logEvent("IO exception while running \ninitial database read!");
                handler.dispatchTransThreadPacket("IO exception while running initial database read for rule " + tag + "!", TransportType.FATAL);
            }
            // If something else has told this thread to stop forcibly, log it to the console for debugging purposes.
        } catch (InterruptedException e) {
            e.printStackTrace();
            // If we couldn't read the page number properly, notify the user and tell the supervisor thread.
        } catch (NumberFormatException e){
            logger.logEvent(e);
            logger.logEvent("Problem parsing page number!");
            handler.dispatchTransThreadPacket("Number parse error while running initial database read for rule " + tag + "!", TransportType.ERROR);
        }
        // If somehow the program gets here without triggering a catch block, return null. This should NEVER happen,
        // but I'm sure you've all heard THAT one before.
        return null;
    }

    /**
     * Retrieves images from the original R34.Rule 34 network.
     * @param tag the tag to search
     * @return the list of new image IDs in the master index
     * @throws ARKTransThreadTransport if a problem is encountered during operation
     */
    private ArrayList<Integer> retrieveR34(String tag) throws ARKTransThreadTransport
    {
        try {
            logger.logEvent("Starting retrieval \nfrom legacy repository...");
            logger.logEvent("Getting total post count...");

            // INITIALIZATION PHASE:
            // We first try to get the initial page for the tag. If it fails, we try four more times and continue if one
            // of them succeeds. If not, we throw the exception we got to the main try/catch block and deal with it there.
            IOException thrownByRead = null;
            int tryCounter = 0;
            do {
                try {
                    RetrievalTools.getFileFromURL("https://rule34.xxx/index.php?page=dapi&s=post&q=index&limit=100&tags=" + tag + "&pid=0", DATABASE, true);
                } catch(ClosedByInterruptException e){
                    return null;
                } catch (IOException e){
                    logger.logEvent("Failed to obtain page number! Trying again...");
                    thrownByRead = e;
                    tryCounter ++;
                }
            } while (thrownByRead != null && tryCounter < 4);
            if(tryCounter >= 4){
                throw thrownByRead;
            }else{
                tryCounter = 0;
                thrownByRead = null;
            }
            ArrayList<URL> images = new ArrayList<>();
            ArrayList<Integer> indexes = new ArrayList<>();

            // PAGE LOCATOR PHASE:
            // Load the downloaded file from its cache on disk.
            String db = RetrievalTools.loadDataFromFile(DATABASE);

            // Try to get the last page number from the initial page.
            int postCount = Integer.parseInt(RetrievalTools.getFieldFromData(db, "<posts count=\"", "\" offset=", 0));
            // Extrapolate the last page number by the amount of posts @ 100 per page.
            int lastPageID = (int)Math.ceil((double)postCount / 100);
            int i = 1;
            logger.logEvent("Got total post count as " + postCount);
            logger.logEvent("Will query a total of " + lastPageID + " time(s).");

            // RETRIEVAL PHASE:
            do{
                // PARSE SUBPHASE:
                logger.logEvent("Pulling image URLs \nfrom page " + i + " of " + lastPageID + "...");
                pushMTUpdateToStatusField("Pulling: page " + i + " of " + lastPageID + "...");

                // here there be dragons (again):
                int lastChar = 0;
                int lc2 = 0;
                int parityCheck = 0;
                int newCount = 0;

                // Load the list of image URLs from the downloaded page.
                while(parityCheck > -1){
                    lastChar = db.indexOf("file_url=\"", lastChar);
                    parityCheck = lastChar;
                    lastChar = lastChar + 10;
                    lc2 = db.indexOf("\" parent_id=", lc2) + 12;
                    if(parityCheck > -1){
                        images.add(new URL("http:" + db.substring(lastChar, lc2 - 12)));
                        newCount ++;
                    }
                }

                logger.logEvent("Pull complete! Pulled " + newCount + " images.");

                // Increment page counter
                i++;

                // CLEANUP SUBPHASE:
                DATABASE.delete();

                // If we are NOT on the last page:
                if(i <= lastPageID) {
                    DATABASE.createNewFile();

                    // RESET SUBPHASE
                    // Same as the initial page read.
                    do {
                        try {
                            RetrievalTools.getFileFromURL("https://rule34.xxx/index.php?page=dapi&s=post&q=index&limit=100&tags=" + tag + "&pid=" + (i - 1), DATABASE, true);
                        } catch(ClosedByInterruptException e){
                            return null;
                        } catch (IOException e){
                            logger.logEvent("Failed to obtain page data! Trying again...");
                            thrownByRead = e;
                            tryCounter ++;
                        }
                    } while (thrownByRead != null && tryCounter < 4);

                    if(tryCounter >= 4){
                        throw thrownByRead;
                    }else{
                        tryCounter = 0;
                        thrownByRead = null;
                    }
                    db = RetrievalTools.loadDataFromFile(DATABASE);
                }

            }while(i <= lastPageID);

            // CHECK PHASE:
            // Check the loaded index to see how many out of the pulled URLs are actually new.
            logger.logEvent("Querying local image index...");
            for (URL url : images) {
                if (!loadedIndex.contains(url) && !loadedIndex.contains(new URL(url.toString().replace("https:", "http:")))) {
                    loadedIndex.add(url);
                    indexes.add(loadedIndex.indexOf(url));
                }
            }

            logger.logEvent("Retrieval complete with " + indexes.size() + " new images!");
            pushMTUpdateToStatusField("Done!");

            return indexes;
        } catch (IOException e) {
            // If an IO error occurred, and it's not a result of the executor telling this thread to stop running,
            // notify the user of it and tell the supervisor thread.
            if(!(e instanceof ClosedByInterruptException)) {
                logger.logEvent(e);
                logger.logEvent("IO exception while running \ninitial database read!");
                handler.dispatchTransThreadPacket("IO exception while running initial database read for rule " + tag + "!", TransportType.FATAL);
            }
            // If we couldn't read the page number properly, notify the user and tell the supervisor thread.
        } catch (NumberFormatException e){
            logger.logEvent(e);
            logger.logEvent("Problem parsing page number!");
            handler.dispatchTransThreadPacket("Number parse error while running initial database read for rule " + tag + "!", TransportType.ERROR);
        }
        // If somehow the program gets here without triggering a catch block, return null. This should NEVER happen,
        // but I'm sure you've all heard THAT one before.
        return null;
    }

    /**
     * Switches the program's operational mode depending on user input.
     * @param mode the mode ID to switch to: 0 is NONE, 1 is MANUAL, and 2 is AUTOMATIC
     */
    private void switchMode(int mode)
    {
        // Mostly self-explanatory. Switches node visibility based on mode.
        switch(mode)
        {
            case 0:
                for(Node n : layout.getChildren()){
                    n.setVisible(false);
                }
                close.setVisible(true);
                options.setVisible(true);
                showLog.setVisible(true);
                modeSelect.setVisible(true);
                break;
            case 1:
                for(Node n : layout.getChildren()){
                    n.setVisible(false);
                }
                close.setVisible(true);
                retrieve.setVisible(true);
                selectDestDir.setVisible(true);
                options.setVisible(true);
                showLog.setVisible(true);
                modeSelect.setVisible(true);
                tagInput.setVisible(true);
                repoSelect.setVisible(true);
                pushUpdates.setVisible(true);
                help.setVisible(true);
                test.setVisible(true);
                mainStatus.setVisible(true);
                ruleStatus.setVisible(true);
                break;
            case 2:
                for(Node n : layout.getChildren()){
                    n.setVisible(false);
                }
                close.setVisible(true);
                retrieve.setVisible(true);
                options.setVisible(true);
                showLog.setVisible(true);
                modeSelect.setVisible(true);
                ruleView.setVisible(true);
                editRule.setVisible(true);
                newRule.setVisible(true);
                deleteRule.setVisible(true);
                moveUp.setVisible(true);
                moveDown.setVisible(true);
                help.setVisible(true);
                test.setVisible(true);
                mainStatus.setVisible(true);
                ruleStatus.setVisible(true);
                break;
        }
    }

    /**
     * Utility method: pushes an update to the rule status field from a background thread.
     */
    private void pushMTUpdateToStatusField(String update) {
        Platform.runLater(() -> ruleStatus.setText(update));
    }

    /**
     * Updates the Config Manager with the latest config options and flags.
     * Does NOT write any changes to disk.
     */
    private void updateConfigOptions()
    {
        // Pushes configuration options from the Options Manager to the cache in the Config Manager to prepare for file writing.
        config.setRuleList(ruleList);

        config.setRepoM(repoSelect.getSelectionModel().getSelectedIndex());
        config.setTagM(tagInput.getText());
        config.setPushM(pushUpdates.isSelected());

        config.setOpmode(modeSelect.getSelectionModel().getSelectedIndex());
        config.setOutputDir(outputDir);
        config.setDoMD5(optionsManager.getDoMD5());
        config.setDoFileLog(optionsManager.getDoFileLogging());
        config.setDoLogTrim(optionsManager.getDoLogTrimming());
        config.setDoMT(optionsManager.getDoMultiThreading());
        config.setDoSkipDLPrompt(optionsManager.getDoSkipDLPrompt());
        config.setLogFile(logFile);
        config.setDoPreview(optionsManager.getDoPreview());
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

    /**
     * Exits the program after asking for user confirmation.
     */
    private void exitSystem()
    {
        // Confirms exit with the user.
        if(new ARKInterfaceDialogYN("Query", "Are you sure you want to exit?", "Yes", "No", 125, 125).display()){
            logger.logEvent("Saving configuration...");
            try {
                // Makes sure that the Config Manager has the same set of options as the Options Manager, then write
                // the Config Manager's cache to disk.
                updateConfigOptions();
                config.writeConfig();
            } catch (IOException e) {
                logger.logEvent("WARNING: unable to save one or more config options!");
                logger.logEvent("Verify directory integrity and config file existence!");
                new ARKInterfaceAlert("Warning", "Unable to save one or more configs!", 100, 100).display();
                if(!new ARKInterfaceDialogYN("Query", "Proceed with exit anyway?", "Yes", "No", 100, 100).display()){
                    return;
                }
                e.printStackTrace();
            }
            logger.logEvent("Closing logfile writers...");
            try {
                logger.closeLog();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // We're all done, so go ahead and send a termination signal to the main threads.
            System.exit(0);
        }
    }
    
    // FROM THE PROGRAMMER RESPONSIBLE FOR THIS MESS
    //
    // To anyone reading this:
    // First, I pity you - you have the solemn and unenviable
    // task of interpreting my code ahead of you. I wish you luck. (NOTE: Since writing this, I have added comments,
    // for whatever good they do...)
    // Secondly, I have officially lost faith in humanity.
    // Seeing dakimakura art of Sagiri from Eromanga-sensei killed any
    // hope of us ever achieving a higher calling as a species.
    // This is coming from a lolicon who wrote THIS PROGRAM,
    // mind you. Just let that sink in for a moment.
    // .
    // .
    // .
    // Good. Shout out to @GrenadeSmasher69 for delivering the final
    // straw that broke the loli's back. Have a good life, whoever happens
    // to be reading this comment.
    //
    // P.S. If this is the FBI, CIA, or NSA reading this, hello!
    // How are things going? Is this seriously the best use of your
    // time?
    //
    // P.P.S. I wish IntelliJ would allow you to collapse comment blocks...
    // EDIT: Apparently it lets you collapse end-of-line comments, but not delimited comments.
    // Who knew.
    //
    // P.P.P.S Shoutouts to Simpleflips

}
