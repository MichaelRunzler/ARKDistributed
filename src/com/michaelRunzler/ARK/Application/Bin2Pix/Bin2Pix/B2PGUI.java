package Bin2Pix;

import Bin2Pix.Adapters.BMPAdapter;
import Bin2Pix.Adapters.JPEGAdapter;
import Bin2Pix.Adapters.PNGAdapter;
import Bin2Pix.Core.B2PCore;
import Bin2Pix.Core.ConversionException;
import Bin2Pix.Core.EncodingSchema;
import Bin2Pix.Core.ImageAdapter;
import com.sun.istack.internal.NotNull;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    @Override
    public void start(Stage primaryStage) throws Exception
    {
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

        adapters = new HashMap<>();

        // add new adapters here
        adapters.put("BMP", new BMPAdapter());
        adapters.put("JPEG", new JPEGAdapter());
        adapters.put("PNG", new PNGAdapter());

        preInit();
        run();
    }

    private void preInit()
    {
        fileListViewer = new FileListViewer((int) (150 * SCALE), (int) (250 * SCALE));

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
        progress.setPrefWidth(110 * SCALE);
        progress.setPrefHeight(20 * SCALE);
        progressActiveIndicator = new ProgressIndicator(-1);
        progressActiveIndicator.setPrefHeight(progress.getPrefHeight());
        progressActiveIndicator.setPrefWidth(progress.getPrefHeight());

        aspectRatioColon.setPrefWidth(10 * SCALE);
        aspectRatioNum.setPromptText("1");
        aspectRatioDenom.setPromptText("1");
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

        setElementPositions();
        setTooltips();
    }

    private void setElementPositions()
    {
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
    }

    private void run()
    {
        exit.setOnAction(e -> exitSystem());
        convert.setOnAction(e -> convert());

        sourceSelect.setOnAction(e -> {
            FileChooser src = new FileChooser();
            src.setInitialDirectory(lastSourceDir == null ? new File(System.getProperty("user.home")) : lastSourceDir);
            src.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            src.setTitle("Select Source File");

            List<File> f = src.showOpenMultipleDialog(window);
            if(f != null && f.size() > 0){
                if(sources == null || sources.size() == 0){
                    sources = new ArrayList<>();
                    sources.addAll(f);
                }else{
                    if(new ARKInterfaceDialogYN("Query", "Replace current file list, or add these files to it?",
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
    }

    private void convert()
    {
        if(sources == null || sources.size() == 0){
            new ARKInterfaceAlert("Notice", "Select one or more source files to continue.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.25 * SCALE)).display();
            return;
        }else if(lastDest == null){
            new ARKInterfaceAlert("Notice", "Select a destination directory to continue.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.25 * SCALE)).display();
            return;
        }else if(format.getSelectionModel().getSelectedIndex() == 0){
            new ARKInterfaceAlert("Notice", "Select a format to continue.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * SCALE)).display();
            return;
        }else if(!hasSeenWarning && !new ARKInterfaceDialogYN("Notice", "Running this conversion will write a new image file to the target directory for each source file. The new file will be named in the format {(original filename)(.[format extension])}."
        + (overwrite.isSelected() ? " If any files with those names exist, they will be OVERWRITTEN." : "") + " Proceed?", "Yes", "No", (int)(DEFAULT_DIALOG_SIZE * 2.5 * SCALE), (int)(DEFAULT_DIALOG_SIZE * 2.25 * SCALE)).display()){
            return;
        }

        hasSeenWarning = true;

        int aspectX = aspectRatioNum.getText().length() == 0 ? 1 : Integer.parseInt(aspectRatioNum.getText()) == 0 ? 1 : Integer.parseInt(aspectRatioNum.getText());
        int aspectY = aspectRatioDenom.getText().length() == 0 ? 1 : Integer.parseInt(aspectRatioDenom.getText()) == 0 ? 1 : Integer.parseInt(aspectRatioDenom.getText());
        int msr = MSRPref.getText().length() == 0 ? 0 : Integer.parseInt(MSRPref.getText());
        int len = lengthPref.getText().length() == 0 ? 1 : Integer.parseInt(lengthPref.getText()) == 0 ? 1 : Integer.parseInt(lengthPref.getText());
        ImageAdapter adapter = adapters.get(format.getSelectionModel().getSelectedItem());
        String extension = adapter.getHandledExtensions()[0].substring(1, adapter.getHandledExtensions()[0].length());

        Service conversionSvc = new Service() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Object call() throws Exception
                    {
                        ArrayList<Exception> errors = new ArrayList<>();

                        for(int i = 0; i < sources.size(); i++)
                        {
                            File f = sources.get(i);
                            try {
                                File dest = new File(lastDest, f.getName() + extension);

                                if(dest.exists() && overwrite.isSelected()){
                                    if(!dest.delete()) throw new IOException("Could not delete existing file");
                                }else if(dest.exists() && !overwrite.isSelected()){
                                    throw new IOException("Destination file already exists");
                                }

                                B2PCore.B2PFile2File(f, dest, new EncodingSchema(msr, len, aspectX, aspectY, adapter));
                            } catch (IOException | ConversionException e) {
                                errors.add(e);
                            }
                            updateProgress(i + 1, sources.size());
                        }
                        return errors;
                    }
                };
            }
        };

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

        conversionSvc.start();
    }

    private void handleResultFromConversion(ArrayList<Exception> result)
    {
        progress.setProgress(0.0);
        progressActiveIndicator.setVisible(false);

        if(result == null || result.size() == 0){
            new ARKInterfaceAlert("Notice", "Conversion complete with no errors!", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * 1.25 * SCALE)).display();
        }else{
            new ARKInterfaceAlert("Notice", "Conversion completed with errors, written to errors.txt in your user documents directory.", (int)(DEFAULT_DIALOG_SIZE * SCALE * 2), (int)(DEFAULT_DIALOG_SIZE * SCALE * 2)).display();

            try {
                File f = new File(System.getProperty("user.home") + "\\Documents", "errors.txt");
                if(f.exists()) f.delete();
                f.createNewFile();

                BufferedWriter wr = new BufferedWriter(new FileWriter(f));
                int i = 1;
                for(Exception e : result)
                {
                    wr.write("Exception #" + i + ":");
                    wr.newLine();
                    wr.newLine();
                    wr.write(e.getMessage() == null ? "No message available" : e.getMessage());
                    wr.newLine();

                    Throwable th = e.getCause();
                    while (th != null){
                        wr.write(th.getMessage());
                        wr.newLine();
                        th = th.getCause();
                    }

                    wr.newLine();
                    i++;
                }

                wr.flush();
                wr.close();
            } catch (IOException e) {
                new ARKInterfaceAlert("Warning", "Error log write failed.", (int)(DEFAULT_DIALOG_SIZE * SCALE), (int)(DEFAULT_DIALOG_SIZE * SCALE)).display();
                e.printStackTrace();

                System.out.println("Error stack from conversion is as follows:");
                System.out.println("----------------------------");
                System.out.println();

                int i = 1;
                for(Exception e1 : result)
                {
                    System.out.println("Exception #" + i + ":");
                    System.out.println();
                    System.out.println(e1.getMessage() == null ? "No message available" : e1.getMessage());

                    Throwable th = e1.getCause();
                    while (th != null){
                        System.out.println(th.getMessage());
                        th = th.getCause();
                    }

                    i++;
                }
            }
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
        MSRPref.setTooltip(new Tooltip("Pull spacing, also known as MSR (mark-space ratio), is the spacing between each set of bytes that are used for pixel color data. Bigger numbers will result in less pixels per byte."));
        lengthPref.setTooltip(new Tooltip("Pull length is the number of bytes in the source file used for each color value in each pixel of the output image (R, G, and B). Bigger numbers will result in less pixels per byte, but smoother color transitions."));
        format.setTooltip(new Tooltip("Select the format for the output image(s). The output image(s) will be the same for any given format, only the filesize and actual file format will differ."));
        overwrite.setTooltip(new Tooltip("If this is checked, any existing files that have the same names as any output image(s) will be overwritten if the program has the proper permissions."));
        advanced.setTooltip(new Tooltip("Show some of the conversion engine's more advanced options."));
    }

    private void exitSystem(){
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
