package X34.UI.JFX.Managers;

import X34.Core.X34Image;
import X34.Core.X34Rule;
import X34.Processors.X34ProcessorRegistry;
import X34.UI.JFX.Util.RetrievalResultCache;
import com.sun.istack.internal.Nullable;
import core.CoreUtil.JFXUtil;
import core.UI.InterfaceDialogs.ARKInterfaceDialogYN;
import core.UI.ARKManagerBase;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Manages result detail and editor functions for result caches from the {@link X34.UI.JFX.X34UI} class.
 */
public class X34UIResultDetailManager extends ARKManagerBase
{
    public static final String DEFAULT_TITLE = "Result Details";
    public static final int DEFAULT_WIDTH = (int)(250 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(300 * JFXUtil.SCALE);

    private ListView<X34Image> entries;
    private VBox buttonContainer;
    private HBox buttonContainerInner1;
    private Button close;
    private Button removeResult;
    private Button undo;
    private Button open;

    private Label resultDetail;

    private boolean isModified;
    private RetrievalResultCache cached;

    public X34UIResultDetailManager(double x, double y)
    {
        super(DEFAULT_TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        isModified = false;
        cached = null;

        entries = new ListView<>();
        buttonContainer = new VBox();
        buttonContainerInner1 = new HBox();
        close = new Button("Save & Close");
        removeResult = new Button("Remove");
        undo = new Button("Revert Changes");
        open = new Button("Open");

        buttonContainerInner1.setAlignment(Pos.CENTER);
        buttonContainerInner1.setSpacing(JFXUtil.DEFAULT_SPACING / 4);
        buttonContainerInner1.getChildren().addAll(removeResult, open);
        HBox.setHgrow(open, Priority.ALWAYS);
        HBox.setHgrow(removeResult, Priority.ALWAYS);

        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(JFXUtil.DEFAULT_SPACING / 4);
        buttonContainer.getChildren().addAll(close, buttonContainerInner1, undo);
        buttonContainer.setFillWidth(true);

        resultDetail = new Label("");

        // Set up interpreter for result list
        entries.setCellFactory(param -> new ListCell<X34Image>(){
            @Override
            protected void updateItem(X34Image item, boolean empty){
                super.updateItem(item, empty);

                if(empty || item == null || item.source == null) setText(null);
                else setText(X34ProcessorRegistry.getProcessorForID(item.processorID).getFilenameFromURL(item.source));
            }
        });

        layout.getChildren().addAll(buttonContainer, resultDetail, entries);

        setTooltips();

        close.setOnAction(e -> this.hide());

        removeResult.setOnAction(e ->{
            ObservableList<Integer> indices = entries.getSelectionModel().getSelectedIndices();

            // Iterate through the list of indices, queuing them for removal. Queuing is necessary to avoid ConcurrentModificationExceptions
            // as well as incorrect index removal due to shifting.
            ArrayList<X34Image> removed = new ArrayList<>();
            for(int i : indices) if(i >= 0 && i < entries.getItems().size()) removed.add(entries.getItems().get(i));

            // Remove all marked items
            for(X34Image i : removed) entries.getItems().remove(i);
            isModified = true;
        });

        open.setOnAction(e ->{
            ObservableList<Integer> indices = entries.getSelectionModel().getSelectedIndices();

            for(int i : indices){
                if(i >= 0 && i < entries.getItems().size() && entries.getItems().get(i) != null){
                    try {
                        Desktop.getDesktop().browse(entries.getItems().get(i).source.toURI());
                    } catch (IOException | URISyntaxException ignored) {}
                }
            }
        });

        undo.setOnAction(e ->{
            if(new ARKInterfaceDialogYN("Warning", "This will discard all changes made to this result! Continue?", "Yes", "No").display()){
                setCurrentResult(cached);
            }
        });
    }

    /**
     * Performs a single-call 'edit' operation on the provided {@link RetrievalResultCache} object.
     * This is done by calling {@link #setCurrentResult(RetrievalResultCache)}, followed by {@link #display()} (which waits
     * for edit completion before returning), and finally returning the result of {@link #getCurrentResult()}.
     * If the provided object is {@code null}, the result of this method will always be {@code null}.
     * @param entry the {@link RetrievalResultCache} to edit using this UI
     * @return the result of editing the provided object, which will be a new {@link RetrievalResultCache} object if any changes
     * were made, or the same object that was originally given to this method if no changes were made
     */
    public @Nullable RetrievalResultCache editResultEntry(@Nullable RetrievalResultCache entry)
    {
        setCurrentResult(entry);
        display();
        return getCurrentResult();
    }

    /**
     * Sets the current {@link RetrievalResultCache} to display in this UI. If the provided object is {@code null}, no data
     * will be displayed.
     * @param entry the object to display
     */
    public void setCurrentResult(@Nullable RetrievalResultCache entry)
    {
        entries.getItems().clear();
        resultDetail.setText("");
        isModified = false;
        cached = null;

        if(entry == null) return;

        resultDetail.setText(entry.name);

        entries.getItems().addAll(entry.results);

        cached = entry;
    }

    /**
     * Gets the current {@link RetrievalResultCache} equivalent of the data being displayed in the editor UI. This may be
     * {@code null} if there was no object set or if the object set was {@code null}.
     * @return a {@link RetrievalResultCache} object representing the current data in the editor UI
     */
    public @Nullable RetrievalResultCache getCurrentResult()
    {
        if(isModified){
            ArrayList<X34Image> temp = new ArrayList<>(entries.getItems());
            return new RetrievalResultCache(resultDetail.getText().contains(":") ? resultDetail.getText().substring(resultDetail.getText().indexOf(':') + 2) : resultDetail.getText(), temp, cached.sourceRule);
        }else return cached;
    }

    private void setTooltips()
    {
        entries.setTooltip(new Tooltip("The list of files contained in this result cache"));
        close.setTooltip(new Tooltip("Save any changes and close this window"));
        removeResult.setTooltip(new Tooltip("Remove the selected file(s) from the list"));
        open.setTooltip(new Tooltip("Open the selected file(s) in your default web browser.\n This does NOT use Incognito mode (or its equivalent), beware!"));
        undo.setTooltip(new Tooltip("Undo any changes made to this result cache"));
    }

    private void repositionElements()
    {
        JFXUtil.setElementPositionInGrid(layout, resultDetail, 0, 0, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, entries, 0, 0, 1, 2.5);
        JFXUtil.setElementPositionInGrid(layout, buttonContainer, 0, 0, -1, 0);
    }

    @Override
    public void display()
    {
        repositionElements();

        if(!window.isShowing()){
            window.showAndWait();
        }
    }
}
