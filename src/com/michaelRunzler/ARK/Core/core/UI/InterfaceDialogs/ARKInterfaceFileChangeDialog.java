package core.UI.InterfaceDialogs;

import core.CoreUtil.JFXUtil;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * Provides a simple file-select/viewing interface, with a 'current file' display, a 'change' button, and a 'reset' button
 * (optional), along with a brief provided description and prompt.
 */
public class ARKInterfaceFileChangeDialog
{
    /**
     * Represents a mode of file or directory selection, usually implemented via a {@link FileChooser} or {@link DirectoryChooser}.
     */
    public enum ChoiceMode{
        DIR_SELECT, FILE_SELECT, FILE_SAVE
    }

    private Stage window;
    private Scene scene;
    private AnchorPane layout;

    private File cachedFile;
    private File defaultState;
    private ChoiceMode mode;
    private boolean automaticSizingEnabled;

    private FileChooser fChooser;
    private DirectoryChooser dChooser;

    private VBox container;

    private Button close;
    private Button change;
    private Button reset;
    private TextField currentFile;
    private Label description;

    public ARKInterfaceFileChangeDialog(String title, String desc, int width, int height) {
        this(title, width, height, null, desc, null, ChoiceMode.FILE_SELECT);
        automaticSizingEnabled = false;
    }

    public ARKInterfaceFileChangeDialog(String title, String desc) {
        this(title, desc, -1, -1);
        automaticSizingEnabled = true;
    }

    public ARKInterfaceFileChangeDialog(String title, int width, int height, File defaultState, String desc, File homeDir, ChoiceMode mode)
    {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setResizable(false);
        window.getIcons().addAll(new Image("core/assets/info.png"));

        cachedFile = null;
        this.defaultState = defaultState;
        this.mode = mode;
        fChooser = new FileChooser();
        dChooser = new DirectoryChooser();

        close = new Button("Save & Close");

        String changeText = null;
        switch (mode){
            case FILE_SAVE:
                changeText = "Change Save Location";
                break;
            case FILE_SELECT:
                changeText = "Change File";
                break;
            case DIR_SELECT:
                changeText = "Change Directory";
                break;
        }

        change = new Button(changeText);
        fChooser.setTitle(changeText);
        dChooser.setTitle(changeText);
        fChooser.setInitialDirectory(homeDir);
        dChooser.setInitialDirectory(homeDir);
        reset = new Button("Reset to Default");
        currentFile = new TextField();
        description = new Label(desc);

        currentFile.setEditable(false);
        currentFile.setPromptText("No file/directory selected!");

        description.setWrapText(true);

        container = new VBox();
        layout = new AnchorPane();
        layout.setPadding((new Insets(15, 15, 15, 15)));

        container.setAlignment(Pos.CENTER);
        container.setSpacing(5 * JFXUtil.SCALE);
        container.getChildren().addAll(description, currentFile, change, reset, close);

        layout.getChildren().add(container);

        JFXUtil.setElementPositionInGrid(layout, container, 0, 0, 0, -1);

        if(width <= 0 || height <= 0) {
            // set to temporary oversized values util we can determine the necessary size of the Vbox and size the window to that
            width = (int)(200 * JFXUtil.SCALE);
            height = (int)(200 * JFXUtil.SCALE);
        }

        description.setMaxWidth(width - layout.getPadding().getLeft() - layout.getPadding().getRight());

        scene = new Scene(layout, width, height);

        window.setScene(scene);
        automaticSizingEnabled = false;
    }

    /**
     * Gets the last result of this dialog. This can also be thought of as the 'stored' value of this dialog.
     * If the user cancelled the last operation, or this dialog has not been shown yet, the result of this method will
     * be {@code null}. Otherwise, it will be the {@link File} that was previously displayed in the current-file field.
     * @return the {@link File} that was chosen by the user
     */
    public File getResult() {
        return cachedFile;
    }

    /**
     * Sets what this dialog should store as its 'default' value. If this is non-{@code null}, a button will be
     * shown that allows the user to reset the stored value to the value that was set as default.
     * @param defaultValue the value to store as this dialog's default value. If this is {@code null}, the default-value
     *                     button will be hidden.
     */
    public void setDefaultValue(File defaultValue) {
        this.defaultState = defaultValue;
    }

    /**
     * Sets what this dialog should display in its current-file field. If this value is {@code null}, the displayed
     * field will be empty until the user chooses a file. This value will be overwritten by whatever file the user chooses,
     * if any. Setting this value will overwrite whatever stored file the user chose last time, if there is any such result stored.
     * @param value the value to be displayed in the current-file field
     */
    public void setValue(File value){
        this.cachedFile = value;
    }

    /**
     * Sets the initial directory that is displayed by the file chooser window.
     * Setting this to {@code null} will change the initial directory to the default directory chosen by the {@link DirectoryChooser} class.
     * @param initialDir the initial displayed directory
     */
    public void setInitialDirectory(File initialDir)
    {
        fChooser.setInitialDirectory(initialDir);
        dChooser.setInitialDirectory(initialDir);
    }

    /**
     * Change the selection mode of this dialog. This will change which mode the file chooser itself uses, as well as
     * the title of the choice window and the text on the change-file button. These text changes will override any manually-set
     * values, so make sure you call this method <i>before</i> setting any manual values.
     * @param mode the {@link ChoiceMode} to use for file/directory selection
     */
    public void setChoiceMode(ChoiceMode mode)
    {
        this.mode = mode;

        String changeText = null;
        switch (mode){
            case FILE_SAVE:
                changeText = "Change Save Location";
                break;
            case FILE_SELECT:
                changeText = "Change File";
                break;
            case DIR_SELECT:
                changeText = "Change Directory";
                break;
        }

        change.setText(changeText);
        fChooser.setTitle(changeText);
        dChooser.setTitle(changeText);
    }

    /**
     * Sets the title of the window that is displayed when the user opts to change the current file or directory.
     * @param title the chooser window title
     */
    public void setChooserWindowTitle(String title){
        fChooser.setTitle(title);
        dChooser.setTitle(title);
    }

    /**
     * Sets the text displayed on the change-file button.
     * @param text the text on the change-file button
     */
    public void setChangeButtonText(String text){
        change.setText(text);
    }

    public File display()
    {
        if(defaultState == null) reset.setVisible(false);
        currentFile.setText(cachedFile == null ? "" : cachedFile.getAbsolutePath());

        close.setOnAction(e -> window.close());

        change.setOnAction(e -> {
            File f;

            switch (mode){
                case DIR_SELECT:
                    f = dChooser.showDialog(window);
                    break;
                case FILE_SELECT:
                    f = fChooser.showOpenDialog(window);
                    break;
                case FILE_SAVE:
                    f = fChooser.showSaveDialog(window);
                    break;
                default:
                    f = null;
            }

            if(f != null){
                currentFile.setText(f.getAbsolutePath());
                cachedFile = f;
            }
        });

        reset.setOnAction(e ->{
            if(defaultState != null){
                cachedFile = new File(defaultState.getAbsolutePath());
                currentFile.setText(defaultState.getAbsolutePath());
            }
        });

        container.widthProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() > 0) window.setWidth(container.getWidth() + layout.getPadding().getRight() * 4);
        });
        container.heightProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() > 0) window.setHeight(container.getHeight() + layout.getPadding().getTop() * 4);
        });

        window.showAndWait();

        return cachedFile;
    }
}
