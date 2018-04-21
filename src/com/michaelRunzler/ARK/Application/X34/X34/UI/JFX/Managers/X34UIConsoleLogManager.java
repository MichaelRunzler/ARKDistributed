package X34.UI.JFX.Managers;

import core.CoreUtil.AUNIL.Callback;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.LogVerbosityLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.ARKManagerBase;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.awt.*;
import java.io.IOException;

/**
 * Displays and manages a copy of the system logger output, viewable in a separate window.
 */
public class X34UIConsoleLogManager extends ARKManagerBase
{
    public static final String DEFAULT_TITLE = "System Log Output";
    public static final int DEFAULT_WIDTH = (int)(100 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(200 * JFXUtil.SCALE);

    private Button showLogFolder;
    private Label verbosityLabel;
    private ListView<String> logView;
    private ChoiceBox<LogVerbosityLevel> verbositySetting;

    private XLoggerInterpreter log;

    private int cacheLimit;
    private int bufferSize;

    public X34UIConsoleLogManager(int width, int height, double x, double y)
    {
        super(DEFAULT_TITLE, (int)(width * JFXUtil.SCALE), (int)(height * JFXUtil.SCALE), x, y);

        cacheLimit = 100;
        bufferSize = 50;

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        log = new XLoggerInterpreter();

        showLogFolder = new Button("Open Log Folder");
        verbosityLabel = new Label("Verbosity: ");
        logView = new ListView<>();
        verbositySetting = new ChoiceBox<>();

        verbositySetting.converterProperty().setValue(new StringConverter<LogVerbosityLevel>() {
            @Override
            public String toString(LogVerbosityLevel object) {
                return object.name();
            }

            @Override
            public LogVerbosityLevel fromString(String string) {
                return LogVerbosityLevel.valueOf(string);
            }
        });

        verbositySetting.getItems().addAll(LogVerbosityLevel.values());
        verbositySetting.getSelectionModel().select(log.getCurrentVerbosity());

        layout.getChildren().addAll(showLogFolder, verbosityLabel, verbositySetting, logView);

        verbositySetting.setOnAction(e -> log.changeLoggerVerbosity(verbositySetting.getValue()));

        showLogFolder.setOnAction(e ->{
            try {
                Desktop.getDesktop().browse(log.getLogDirectory().toURI());
            } catch (IOException e1) {
                log.logEvent("Error 04042: Unable to open log directory.");
                log.logEvent(e1);
            }
        });

        log.addStreamCallback(new Callback(true) {
            @Override
            public void call(String data, String IID, LogEventLevel level, String compiled) {
                Platform.runLater(() -> {
                    if(Callback.checkVerbosityLevel(log.getCurrentVerbosity(), level)) logView.getItems().add(compiled);

                    // Trim the list if necessary
                    trimLog();
                    logView.scrollTo(logView.getItems().size() - 1);
                });
            }
        });

        setNodeTooltips();
    }

    /**
     * Clears the current event log completely, leaving limiting and buffer settings intact.
     */
    public void clearLog(){
        logView.getItems().clear();
    }

    /**
     * Initiates a manual trim of the log cache using the current buffer and limiting settings set via {@link #setCacheSize(int, int)}.
     * It's not usually necessary to call this externally, but it's here if it is needed for some reason.
     */
    public void trimLog(){
        if(logView.getItems().size() >= (cacheLimit + bufferSize))
            logView.getItems().remove(0, logView.getItems().size() - cacheLimit);
    }

    /**
     * Sets how large the cache of log entries can get before the log is automatically trimmed, and how many entries it
     * should be trimmed to when the trim operation is triggered. Larger buffer sizes will require less processing power,
     * since the trim operation is not triggered as frequently, while smaller buffer sizes will require less memory,
     * since there are less total messages in the log buffer. Default values are 100 for size limit, 50 for buffer size.
     * @param limit the number of entries the log should be trimmed to. Limit settings of {@code 0} or less will result
     *              in the log being cleared completely when the combined limit and buffer size is exceeded.
     * @param buffer the number of entries by which the log cache can exceed the limit before a trim operation is triggered.
     *               For example, if a limit of 100 and buffer size of 50 are set, the log will be allowed to grow to 149
     *               entries, and will be trimmed back down to 100 entries when the 150<sup>th</sup> entry is added.
     *               Buffer sizes of {@code 0} or less will result in the log being kept at the exact size specified
     *               by the set limit amount.
     */
    public void setCacheSize(int limit, int buffer)
    {
        cacheLimit = limit >= 0 ? limit : 0;
        bufferSize = buffer >= 0 ? buffer : 0;
    }

    private void setNodeTooltips()
    {
        logView.setTooltip(new Tooltip("The current system log output, filtered by the\nverbosity setting set below."));
        verbositySetting.setTooltip(new Tooltip("The log verbosity level.\nChanging this will affect what types of events\nshow up in the log view."));
        showLogFolder.setTooltip(new Tooltip("Open the currently set system log folder,\nwhere you can view and copy logged data."));
    }

    private void repositionElements()
    {
        JFXUtil.setElementPositionInGrid(layout, logView, 0, 0, 0, 2);
        JFXUtil.setElementPositionInGrid(layout, showLogFolder, 0, 0, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, verbositySetting, -1, 0, -1, -1);
        JFXUtil.setElementPositionInGrid(layout, verbosityLabel, 0, -1, -1, 1);
        Platform.runLater(() -> JFXUtil.bindAlignmentToNode(layout, verbosityLabel, verbositySetting, 5, Orientation.HORIZONTAL, JFXUtil.Alignment.CENTERED));
    }

    @Override
    public void display(){
        repositionElements();
        super.display();
    }

}
