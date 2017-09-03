import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import system.ARKLogHandler;
import system.ARKManagerInterface;

import java.awt.*;
import java.io.*;
import java.util.HashMap;

/**
 * Manages logging operations for the R34 UI class.
 */
public class R34LoggingManager implements ARKManagerInterface, ARKLogHandler
{
    private Stage window;
    private Scene scene;
    private AnchorPane layout;
    private ListView<String> list;
    private BufferedWriter logFileWriter;
    private boolean doLogTrim;
    private boolean doFileLogging;
    private boolean doPreview;
    private long lastLogTime = 0;
    private File logRefFile;

    private HashMap<Integer, File> linkMap;

    private R34ImagePreviewManager manager;

    /**
     * Creates a new Manager object of this type.
     * @param title the title of the window
     * @param width the width of the window
     * @param height the height of the window
     * @param x the horizontal position of the window
     * @param y the vertical position of the window
     */
    public R34LoggingManager(String title, int width, int height, double x, double y, File log)
    {
        window = new Stage();
        window.setTitle(title);
        window.setResizable(true);
        window.setHeight(height);
        window.setWidth(width);
        window.setX(x);
        window.setY(y);
        window.getIcons().add(new Image("assets/info.png"));

        linkMap = new HashMap<>();

        manager = new R34ImagePreviewManager("Preview", 200, 200, window.getX() + window.getWidth() + 10, window.getY());

        list = new ListView<>();

        layout = new AnchorPane(list);

        scene = new Scene(layout, width, height);

        list.setPrefSize(scene.getWidth(), scene.getHeight());
        list.setEditable(false);
        this.doFileLogging = true;
        this.doLogTrim = false;

        AnchorPane.setLeftAnchor(list, 0.0);
        AnchorPane.setTopAnchor(list, 0.0);

        scene.widthProperty().addListener(e -> list.setPrefWidth(scene.getWidth()));
        scene.heightProperty().addListener(e -> list.setPrefHeight(scene.getHeight()));

        try {
            logFileWriter = new BufferedWriter(new FileWriter(log));
            logRefFile = log;
        } catch (IOException e) {
            e.printStackTrace();
        }

        list.setOnMouseClicked(e ->{
            int index = list.getSelectionModel().getSelectedIndex();
            if(index >= 0 && linkMap.containsKey(index) && doPreview){
                try {
                    manager.setImage(new Image(new FileInputStream(linkMap.get(index))));
                    if(manager.getVisibilityState()) {
                        manager.hide();
                        manager.display();
                    }else{
                        manager.display();
                    }
                }catch (IOException e1){
                    list.getItems().add(e1.getMessage() == null ? e1.toString() : e1.getMessage());
                    e1.printStackTrace();
                    logEvent("Error generating preview!");
                }
            }
        });

        list.setOnKeyReleased(e ->{
            int index = list.getSelectionModel().getSelectedIndex();
            if(e.getCode() == KeyCode.ENTER && index >= 0
                    && linkMap.containsKey(index)){
                try {
                    if(linkMap.get(index).exists()) {
                        Desktop.getDesktop().open(linkMap.get(index));
                    }
                } catch (IOException e1) {
                    list.getItems().add(e1.getMessage() == null ? e1.toString() : e1.getMessage());
                    e1.printStackTrace();
                    logEvent("Error opening linked file!");
                }
            }
        });

        window.setScene(scene);
    }

    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    public void display()
    {
        if(!window.isShowing()) {
            window.show();
            list.scrollTo(list.getItems().size() - 1);
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

    /**
     * Logs an event to the current log view and the local log file, and tags it with a timestamp.
     * Trims the live log view if set to do so.
     * @param event the text to log to the live view and file
     */
    public void logEvent(String event)
    {
        if(doLogTrim && list != null && list.getItems().size() > 100) {
            list.getItems().remove(0);
        }

        long tmp = System.currentTimeMillis();
        String log = "T+" + (tmp - lastLogTime) + ": " + event;
        list.getItems().add(log);
        //list.scrollTo(list.getItems().size() - 1);

        if(logFileWriter != null && doFileLogging) {
            try {
                logFileWriter.write(log);
                logFileWriter.newLine();
                logFileWriter.flush();
            } catch (IOException e) {
                list.getItems().add(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }

        lastLogTime = tmp;
    }

    /**
     * Logs an Exception report to the current log view and the local log file, and tags it with a timestamp.
     * @param e the Exception to log. Will log the Exception's message if present, if not, will log the toString() transliteration of the object
     */
    public void logEvent(Exception e){
        logEvent(e.getMessage() == null ? e.toString() : e.getMessage());
    }

    /**
     * Logs an event to the current log view and the local log file, and tags it with a timestamp.
     * Also links a file URI to the log entry. When the log entry is double-clicked, the file linked to the entry
     * will be executed by the system shell.
     * @param event the text to log to the live view and file
     * @param link the file URI to link to the event entry
     */
    public void logEventWithLink(String event, File link) {
        logEvent(event);
        linkMap.put(list.getItems().size() - 1, link);
    }

    /**
     * Updates the manager's logging settings.
     * @param doLogTrim whether to trim the active log to 100 entries
     * @param doFileLogging whether to log entries to a file or just to the live view
     */
    public void updateLogOptions(boolean doLogTrim, boolean doFileLogging, boolean doPreview, File logFile)
    {
        this.doLogTrim = doLogTrim;
        this.doFileLogging = doFileLogging;
        this.doPreview = doPreview;

        if(logRefFile != logFile){
            logRefFile = logFile;
            logEvent("Changing log location to " + logFile.getAbsolutePath() + "...");
            try {
                logFileWriter.flush();
                logFileWriter.close();
                logFileWriter = new BufferedWriter(new FileWriter(logFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes the log manager's file writer.
     * @throws IOException if the writer cannot be closed properly
     */
    public void closeLog() throws IOException
    {
        logFileWriter.flush();
        logFileWriter.close();
    }

    /**
     * Clears the displayed list of log entries.
     */
    public void resetLogEntries()
    {
        list.getItems().clear();
    }

    /**
     * Scrolls to the bottom of the log list.
     */
    public void showBottomOfLog()
    {
        list.scrollTo(list.getItems().size() - 1);
    }
}
