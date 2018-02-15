package Bin2Pix;

// JESUS THIS IMPORT LIST IS A BIT TOO LONG
import Bin2Pix.Adapters.BMPAdapter;
import Bin2Pix.Adapters.JPEGAdapter;
import Bin2Pix.Adapters.PNGAdapter;
import Bin2Pix.Core.B2PCore;
import Bin2Pix.Core.ConversionException;
import Bin2Pix.Core.EncodingSchema;
import Bin2Pix.Core.ImageAdapter;
import com.sun.istack.internal.NotNull;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.UI.ARKInterfaceAlert;
import core.UI.ARKInterfaceDialogYN;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class B2PGUI extends Application
{
    // JavaFX window elements and nodes
    private Stage window;
    private Scene menu;
    private AnchorPane layout;

    private Button exit;
    private Button convert;
    private Button sourceSelect;
    private Button destSelect;
    private Button fileList;

    private TextField aspectRatioNum;
    private TextField aspectRatioDenom;
    private TextField MSRPref;
    private TextField lengthPref;

    private ChoiceBox<String> format;
    private CheckBox overwrite;
    private CheckBox advanced;

    private Label aspectRatioColon;
    private Label aspectRatioLabel;
    private Label progressLabel;
    private Label MSRPrefLabel;
    private Label lengthPrefLabel;

    private ProgressBar progress;
    private ProgressIndicator progressActiveIndicator;

    // Global variables and constants
    private HashMap<String, ImageAdapter> adapters;

    private final double BASE_SCALE_SIZE = 16.0;
    private final double SCALE = Math.rint(new Text("").getLayoutBounds().getHeight()) / BASE_SCALE_SIZE;
    private final double DEFAULT_SPACING = 32.0;
    private final int DEFAULT_DIALOG_SIZE = 80;

    private FileListViewer fileListViewer;

    private File lastSourceDir = null;
    private File lastDest = null;
    private ArrayList<File> sources = null;
    private boolean hasSeenWarning = false;

    private XLoggerInterpreter log;

    @Override
    public void start(Stage primaryStage)
    {
        log = new XLoggerInterpreter("Bin2Pix UI");

        log.logEvent("Initializing JFX...");

        // INITIALIZE JAVAFX WINDOW
        window = new Stage();

        window.setResizable(false);
        window.setIconified(false);
        window.setMinHeight(400);
        window.setMinWidth(400);
        window.setX((Screen.getPrimary().getBounds().getWidth() / 2) - 400);
        window.setY((Screen.getPrimary().getBounds().getHeight() / 2) - 400);
        window.setTitle("Bin2Pix Converter");
        window.getIcons().add(new Image("Bin2Pix/assets/main.png"));

        window.setOnCloseRequest(e ->{
            e.consume();
            exitSystem();
        });

        layout = new AnchorPane();
        layout.setPadding(new Insets(15, 15, 15, 15));
        menu = new Scene(layout, 400, 400);
        window.setScene(menu);
        window.show();

        log.logEvent("JFX init complete, adding adapters...");

        adapters = new HashMap<>();

        // add new adapters here
        adapters.put("BMP", new BMPAdapter());
        adapters.put("JPEG", new JPEGAdapter());
        adapters.put("PNG", new PNGAdapter());

        log.logEvent("Pre-init complete.");

        // run init methods
        preInit();
        setElementActions();
    }

    private void preInit()
    {
        log.logEvent("Starting JFX element configuration...");
        log.logEvent(LogEventLevel.DEBUG, "Scale factor: " + SCALE);

        // Init the file list viewer with some arbitrary window sizes, scaled to the user's global UI scale.
        // Size doesn't really matter too much here (insert dong joke), since the user can resize it anyway.
        fileListViewer = new FileListViewer((int) (150 * SCALE), (int) (250 * SCALE));

        // Initialize nodes
        exit = new Button("Exit");
        convert = new Button("Convert");
        sourceSelect = new Button("Select Source");
        destSelect = new Button("Select Destination");
        fileList = new Button("Source File List");

        aspectRatioNum = new TextField();
        aspectRatioDenom = new TextField();
        MSRPref = new TextField();
        lengthPref = new TextField();

        format = new ChoiceBox<>();
        overwrite = new CheckBox("Overwrite Existing Files");
        advanced = new CheckBox("Advanced Options");

        aspectRatioColon = new Label(":");
        aspectRatioLabel = new Label("Aspect Ratio:");
        progressLabel = new Label("Conversion Progress:");
        lengthPrefLabel = new Label("Pull Length (bytes): ");
        MSRPrefLabel = new Label("Pull Spacing (bytes): ");

        progress = new ProgressBar();

        // Set node properties
        progress.setPrefWidth(110 * SCALE);
        progress.setPrefHeight(20 * SCALE);
        progressActiveIndicator = new ProgressIndicator(-1);
        progressActiveIndicator.setPrefHeight(progress.getPrefHeight());
        progressActiveIndicator.setPrefWidth(progress.getPrefHeight());

        aspectRatioColon.setPrefWidth(10 * SCALE);
        aspectRatioNum.setPromptText("1");
        aspectRatioDenom.setPromptText("1");
        // Scale the separator colon to match the global UI scale
        aspectRatioColon.setFont(new Font(aspectRatioColon.getFont().getName(), aspectRatioColon.getFont().getSize() * SCALE));

        limitTextFieldToNumerical(aspectRatioNum, 2);
        limitTextFieldToNumerical(aspectRatioDenom, 2);
        limitTextFieldToNumerical(MSRPref, 3);
        limitTextFieldToNumerical(lengthPref, 3);

        format.getItems().add("-SELECT FORMAT-");

        format.getItems().addAll(adapters.keySet());
        format.getSelectionModel().select(0);

        MSRPref.setVisible(false);
        lengthPref.setVisible(false);
        MSRPref.setPromptText("0");
        lengthPref.setPromptText("1");
        MSRPrefLabel.setVisible(false);
        lengthPrefLabel.setVisible(false);

        progressActiveIndicator.setVisible(false);
        progress.setProgress(0.0);

        log.logEvent("Element config complete in " + log.getTimeSinceLastEvent() + "ms.");

        setElementPositions();
        setTooltips();
    }

    private void setElementPositions()
    {
        log.logEvent("Setting element positions...");

        // Some of these are called twice because one sets the position relative to the grid in some coordinates,
        // and the other sets the position as a non-grid number in other coordinates.
        setElementPositionInGrid(exit, 0, -1, -1, 0);
        setElementPositionInGrid(convert, -1, 0, -1, 0);
        setElementPositionInGrid(fileList, -1, 1, -1, 0);

        setElementPositionInGrid(sourceSelect, 0, -1, 0, -1);
        setElementPositionInGrid(destSelect, 0, -1, 1, -1);

        setElementPositionInGrid(overwrite, 0, -1, 2, -1);

        setElementPositionInGrid(aspectRatioLabel, 0, -1, 3, -1);
        setElementPositionInGrid(aspectRatioNum, 0, -1, 4, -1);
        setElementPositionInGrid(aspectRatioColon, -1, -1, 4, -1);
        setElementPositionInGrid(aspectRatioDenom, -1, -1, 4, -1);
        setElementPosition(aspectRatioColon, aspectRatioNum.getPrefWidth(), -1, -1, -1);
        setElementPosition(aspectRatioDenom, aspectRatioDenom.getPrefWidth() + aspectRatioColon.getPrefWidth(), -1, -1, -1);

        setElementPositionInGrid(progressLabel, 0, -1, -1, 3);
        setElementPositionInGrid(progress, 0, -1, -1, 2);
        setElementPositionInGrid(progressActiveIndicator, -1, -1, -1, 2);
        setElementPosition(progressActiveIndicator, progress.getPrefWidth() - (15 * SCALE), -1, -1, -1);

        setElementPositionInGrid(format, -1, 0, 0, -1);
        setElementPositionInGrid(advanced, -1, 0, 2, -1);
        setElementPositionInGrid(MSRPref, -1, 0, 3, -1);
        setElementPositionInGrid(lengthPref, -1, 0, 4, -1);
        setElementPositionInGrid(MSRPrefLabel, -1, -1, 3, -1);
        setElementPositionInGrid(lengthPrefLabel, -1, -1, 4, -1);
        setElementPosition(MSRPrefLabel, -1, MSRPref.getPrefWidth(), -1, -1);
        setElementPosition(lengthPrefLabel, -1, lengthPref.getPrefWidth(), -1, -1);

        log.logEvent("Position setup complete.");
    }

    private void setElementActions()
    {
        exit.setOnAction(e -> exitSystem());
        convert.setOnAction(e -> convert());

        sourceSelect.setOnAction(e -> {
            FileChooser src = new FileChooser();
            src.setInitialDirectory(lastSourceDir == null ? new File(System.getProperty("user.home")) : lastSourceDir);
            src.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            src.setTitle("Select Source File");

            List<File> f = src.showOpenMultipleDialog(window);

            if(f == null) return;

            // Check individual file size. We don't really care about TOTAL size, just individual size, so that we don't run out of memory.
            for(File x : f){
                if(x.length() > B2PCore.MAX_DATA_LENGTH){
                    new ARKInterfaceAlert("Warning", "One or more files exceed the maximum allowed file size of " + (B2PCore.MAX_DATA_LENGTH / 1048576L/*1 MB*/) + " MB.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.50 * SCALE)).display();
                    return;
                }
            }

            if(f.size() > 0){
                if(sources == null || sources.size() == 0){
                    sources = new ArrayList<>();
                    sources.addAll(f);
                }else{
                    if(new ARKInterfaceDialogYN("Query", "Replace current file list, or add these file(s) to it?",
                            "Add", "Replace", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.5 * SCALE)).display()) {
                        sources.addAll(f);
                    }else{
                        sources.clear();
                        sources.addAll(f);
                    }
                }
                lastSourceDir = f.get(0).getParentFile();
            }
        });

        destSelect.setOnAction(e ->{
            DirectoryChooser dest = new DirectoryChooser();
            dest.setInitialDirectory(lastDest == null ? new File(System.getProperty("user.home")) : lastDest.getParentFile());
            dest.setTitle("Select Destination Directory");

            File f = dest.showDialog(window);

            if(f != null && f.getParentFile().exists()) lastDest = f;
        });

        advanced.setOnAction(e ->{
            MSRPref.setVisible(advanced.isSelected());
            lengthPref.setVisible(advanced.isSelected());
            MSRPrefLabel.setVisible(advanced.isSelected());
            lengthPrefLabel.setVisible(advanced.isSelected());
        });

        overwrite.setOnAction(e -> hasSeenWarning = false);

        fileList.setOnAction(e ->{
            fileListViewer.updateFileList(sources == null ? new ArrayList<>() : sources);
            fileListViewer.display();
        });
        
        log.logEvent("Initialization complete. Program ready.");
    }

    private void convert()
    {
        // UI confirmation and state checking
        if(sources == null || sources.size() == 0){
            new ARKInterfaceAlert("Notice", "Select one or more source files to continue.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.25 * SCALE)).display();
            return;
        }else if(lastDest == null){
            new ARKInterfaceAlert("Notice", "Select a destination directory to continue.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.25 * SCALE)).display();
            return;
        }else if(format.getSelectionModel().getSelectedIndex() == 0){
            new ARKInterfaceAlert("Notice", "Select a format to continue.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * SCALE)).display();
            return;
        }else if(!hasSeenWarning && !new ARKInterfaceDialogYN("Notice", "Running this conversion will write a new image file to the target directory for each source file. The new files will be named in the format {(original filename)(.[format extension])}."
        + (overwrite.isSelected() ? " If any files with those names exist, they will be OVERWRITTEN." : "") + " Proceed?", "Yes", "No", (int)(DEFAULT_DIALOG_SIZE * 2.5 * SCALE), (int)(DEFAULT_DIALOG_SIZE * 2.25 * SCALE)).display()){
            return;
        }

        hasSeenWarning = true;

        log.logEvent("Starting conversion.");
        log.logEvent(LogEventLevel.DEBUG, "Source count: " + sources.size());
        log.logEvent(LogEventLevel.DEBUG, "Destination directory: " + lastDest);
        log.logEvent(LogEventLevel.DEBUG, "Format: " + format.getSelectionModel().getSelectedItem());
        log.logEvent(LogEventLevel.DEBUG, "Overwrite: " + (overwrite.isSelected() ? "ENABLED" : "DISABLED"));

        // Get necessary values from the input fields, or default if there are none
        int aspectX = aspectRatioNum.getText().length() == 0 ? 1 : Integer.parseInt(aspectRatioNum.getText()) == 0 ? 1 : Integer.parseInt(aspectRatioNum.getText());
        int aspectY = aspectRatioDenom.getText().length() == 0 ? 1 : Integer.parseInt(aspectRatioDenom.getText()) == 0 ? 1 : Integer.parseInt(aspectRatioDenom.getText());
        int msr = MSRPref.getText().length() == 0 ? 0 : Integer.parseInt(MSRPref.getText());
        int len = lengthPref.getText().length() == 0 ? 1 : Integer.parseInt(lengthPref.getText()) == 0 ? 1 : Integer.parseInt(lengthPref.getText());
        ImageAdapter adapter = adapters.get(format.getSelectionModel().getSelectedItem());
        String extension = adapter.getHandledExtensions()[0].substring(1, adapter.getHandledExtensions()[0].length());
        
        log.logEvent(LogEventLevel.DEBUG, "Aspect ratio: " + aspectX + ":" + aspectY);
        log.logEvent(LogEventLevel.DEBUG, "Mark-space ratio: " + msr);
        log.logEvent(LogEventLevel.DEBUG, "Sample length: " + len);

        Service conversionSvc = new Service() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Object call() throws Exception
                    {
                        XLoggerInterpreter xl = new XLoggerInterpreter("Bin2Pix Conversion Worker");
                        ArrayList<Exception> errors = new ArrayList<>();

                        // Iterate through the list of source files, and convert each one after checking the source and dest
                        for(int i = 0; i < sources.size(); i++)
                        {
                            xl.logEvent("Processing file " + (i + 1) + " of " + sources.size() + "...");
                            
                            File f = sources.get(i);
                            try {
                                File dest = new File(lastDest, f.getName() + extension);

                                if(dest.exists() && overwrite.isSelected()){
                                    if(!dest.delete()) throw new IOException("Could not delete existing file");
                                }else if(dest.exists() && !overwrite.isSelected()){
                                    throw new IOException("Destination file already exists");
                                }

                                B2PCore.B2PFile2File(f, dest, new EncodingSchema(msr, len, aspectX, aspectY, adapter));
                                xl.logEvent("Done processing with no errors.");
                            } catch (IOException | ConversionException e) {
                                errors.add(e);
                                xl.logEvent(LogEventLevel.ERROR, "Encountered non-fatal error, see below for details.");
                                xl.logEvent(LogEventLevel.ERROR, e);
                            } finally {
                                // garbage collect, because apparently Java can't automatically reclaim the memory used by the old arrays
                                System.gc();
                            }
                            updateProgress(i + 1, sources.size());
                        }
                        xl.logEvent("Batch complete with " + errors.size() + " errors.");
                        xl.disassociate();
                        return errors;
                    }
                };
            }
        };

        // Link the progress property of the service to allow for async progress updates
        conversionSvc.progressProperty().addListener((observable, oldValue, newValue) -> progress.setProgress(newValue.doubleValue()));

        conversionSvc.setOnSucceeded(e ->{
            ArrayList<Exception> result = conversionSvc.getValue() instanceof ArrayList ? (ArrayList<Exception>) conversionSvc.getValue() : null;
            handleResultFromConversion(result);
        });

        conversionSvc.setOnFailed(e ->{
            if(conversionSvc.getException() != null){
                new ARKInterfaceAlert("Warning", "Conversion encountered a fatal error. Please try again.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.5 * SCALE)).display();
                progress.setProgress(0.0);
                progressActiveIndicator.setVisible(false);
                conversionSvc.getException().printStackTrace();
                return;
            }

            ArrayList<Exception> result = conversionSvc.getValue() instanceof ArrayList ? (ArrayList<Exception>) conversionSvc.getValue() : null;
            handleResultFromConversion(result);
        });

        progressActiveIndicator.setVisible(true);
        
        log.logEvent("Starting conversion service...");

        conversionSvc.start();
    }

    // Pushed into a method because it would otherwise involve a lot of copied code and headaches later down the line.
    private void handleResultFromConversion(ArrayList<Exception> result)
    {
        progress.setProgress(0.0);
        progressActiveIndicator.setVisible(false);

        if(result == null || result.size() == 0){
            new ARKInterfaceAlert("Notice", "Conversion complete with no errors!", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.25 * SCALE)).display();
        }else{
            new ARKInterfaceAlert("Notice", "Conversion completed with errors, written to errors.txt in your user documents directory.", (int)(DEFAULT_DIALOG_SIZE * SCALE * 2), (int)(DEFAULT_DIALOG_SIZE * SCALE * 2)).display();

            try {
                log.logEvent("Writing error report...");
                File f = new File(System.getProperty("user.home") + "\\Documents", "errors.txt");
                if(f.exists()) f.delete();
                f.createNewFile();

                // Write exceptions to logfile if there are any.
                BufferedWriter wr = new BufferedWriter(new FileWriter(f));
                int i = 1;
                for(Exception e : result)
                {
                    wr.write("Exception #" + i + ":");
                    wr.newLine();
                    wr.newLine();

                    String str = "";
                    e.printStackTrace(new PrintStream(str));

                    wr.write(str);

                    wr.newLine();
                    i++;
                }

                wr.flush();
                wr.close();
            } catch (IOException e) {
                // If we can't even write the exceptions to the logfile, print them to the console instead.
                new ARKInterfaceAlert("Warning", "Error log write failed. Errors logged to console.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * SCALE)).display();
                e.printStackTrace();

                log.logEvent(LogEventLevel.CRITICAL, "Error stack from conversion is as follows:");
                log.logEvent(LogEventLevel.CRITICAL, "----------------------------");
                log.logEvent(LogEventLevel.CRITICAL, "");

                int i = 1;
                for(Exception e1 : result)
                {
                    log.logEvent(LogEventLevel.CRITICAL, "Exception #" + i + ":");
                    log.logEvent(LogEventLevel.CRITICAL, "");
                    log.logEvent(LogEventLevel.CRITICAL, e1);

                    i++;
                }
            }
            System.gc();
        }
    }

    private void setTooltips()
    {
        exit.setTooltip(new Tooltip("Exit the program."));
        convert.setTooltip(new Tooltip("Start the conversion process with current settings."));
        sourceSelect.setTooltip(new Tooltip("Select one or more source files for the conversion process."));
        destSelect.setTooltip(new Tooltip("Select the output directory for the results from the conversion process."));
        aspectRatioNum.setTooltip(new Tooltip("The horizontal component of the desired aspect ratio. Leave blank to default this setting."));
        aspectRatioDenom.setTooltip(new Tooltip("The vertical component of the desired aspect ratio. Leave blank to default this setting."));
        MSRPref.setTooltip(new Tooltip("Pull spacing, also known as MSR (mark-space ratio), is the spacing between each set of bytes that \n " +
                "are used for pixel color data. Bigger numbers will result in less pixels per byte."));
        lengthPref.setTooltip(new Tooltip("Pull length is the number of bytes in the source file used for each color value in each pixel of \n " +
                "the output image (R, G, and B). Bigger numbers will result in less pixels per byte, but smoother color transitions."));
        format.setTooltip(new Tooltip("Select the format for the output image(s). The output image(s) will be the same for any given format, \n " +
                "only the filesize and actual file format will differ."));
        overwrite.setTooltip(new Tooltip("If this is checked, any existing files that have the same names as any output image(s) will be \n " +
                "overwritten if the program has the proper permissions."));
        advanced.setTooltip(new Tooltip("Show some of the conversion engine's more advanced options."));
    }

    private void exitSystem(){
        log.logEvent("Shutting down...");
        log.disassociate();
        System.exit(0);
    }

    /*
     * UTILS
     */

    // autocompensates for scaling
    private void setElementPosition(@NotNull Node element, double left, double right, double top, double bottom)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        if(left >= 0) AnchorPane.setLeftAnchor(element, left * SCALE);
        if(right >= 0) AnchorPane.setRightAnchor(element, right * SCALE);
        if(top >= 0) AnchorPane.setTopAnchor(element, top * SCALE);
        if(bottom >= 0) AnchorPane.setBottomAnchor(element, bottom * SCALE);
    }

    // autocompensates for scaling
    private void setElementPositionInGrid(@NotNull Node element, int leftGridID, int rightGridID, int topGridID, int bottomGridID)
    {
        if(!layout.getChildren().contains(element)) layout.getChildren().add(element);

        if(leftGridID >= 0) AnchorPane.setLeftAnchor(element, (DEFAULT_SPACING * 2 * SCALE) * leftGridID);
        if(rightGridID >= 0) AnchorPane.setRightAnchor(element, (DEFAULT_SPACING * 2 * SCALE) * rightGridID);
        if(topGridID >= 0) AnchorPane.setTopAnchor(element, (DEFAULT_SPACING * SCALE) * topGridID);
        if(bottomGridID >= 0) AnchorPane.setBottomAnchor(element, (DEFAULT_SPACING * SCALE) * bottomGridID);
    }

    // numerical only, factors for scaling, digit width 15px
    private void limitTextFieldToNumerical(TextField node, int maxDigits)
    {
        node.setPrefWidth((15 * maxDigits) * SCALE);
        node.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue.matches("\\d*") || newValue.length() > maxDigits){
                node.setText(oldValue);
            }
        });
    }
}
