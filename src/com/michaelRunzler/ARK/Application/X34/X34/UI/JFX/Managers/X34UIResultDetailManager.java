package X34.UI.JFX.Managers;

import X34.Core.X34Image;
import X34.Processors.X34ProcessorRegistry;
import X34.UI.JFX.Util.RetrievalResultCache;
import com.sun.istack.internal.Nullable;
import core.CoreUtil.JFXUtil;
import core.UI.ARKInterfaceDialogYN;
import core.UI.ARKManagerBase;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Manages result detail and editor functions for result caches from the {@link X34.UI.JFX.X34UI} class.
 */
public class X34UIResultDetailManager extends ARKManagerBase
{
    public static final String DEFAULT_TITLE = "Result Details";
    public static final int DEFAULT_WIDTH = (int)(150 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(300 * JFXUtil.SCALE);

    private ListView<X34Image> entries;
    private VBox buttonContainer;
    private Button close;
    private Button removeResult;
    private Button undo;
    private Button open;

    private Label resultDetail;

    private boolean hasWindowChangeListener;
    private boolean isModified;
    private RetrievalResultCache cached;

    public X34UIResultDetailManager(double x, double y)
    {
        super(DEFAULT_TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        hasWindowChangeListener = false;
        isModified = false;
        cached = null;

        entries = new ListView<>();
        buttonContainer = new VBox();
        close = new Button("Save & Close");
        removeResult = new Button("Remove");
        undo = new Button("Revert Changes");
        open = new Button("Open");

        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(JFXUtil.DEFAULT_SPACING / 4);
        buttonContainer.getChildren().addAll(close, removeResult, undo, open);

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

        layout.getChildren().addAll(buttonContainer, entries);

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
            return new RetrievalResultCache(resultDetail.getText(), temp, cached.sourceRule);
        }else return cached;
    }

    private void setTooltips()
    {
        entries.setTooltip(new Tooltip("The list of images contained in this result cache"));
        close.setTooltip(new Tooltip("Save any changes and close this window"));
        removeResult.setTooltip(new Tooltip("Remove the selected image(s) from the list"));
        open.setTooltip(new Tooltip("Open the selected image(s) in your default web browser.\n This does NOT use Incognito mode (or its equivalent), beware!"));
        undo.setTooltip(new Tooltip("Undo any changes made to this result cache"));
    }

    private void repositionElements()
    {
        if(!hasWindowChangeListener) {
            layout.widthProperty().addListener(e -> repositionOnReize());
            layout.heightProperty().addListener(e -> repositionOnReize());
            hasWindowChangeListener = true;
        }

        JFXUtil.setElementPositionInGrid(layout, entries, 0, -1, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, buttonContainer, -1, -1, -1, 0);

        Platform.runLater(this::repositionOnReize);
    }

    private void repositionOnReize()
    {
        JFXUtil.setElementPositionCentered(layout, buttonContainer, true, false);

        entries.setPrefWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight());
        removeResult.setPrefWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight() - (5 * JFXUtil.SCALE));
        open.setPrefWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight() - (5 * JFXUtil.SCALE));
        close.setPrefWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight() - (5 * JFXUtil.SCALE));
        undo.setPrefWidth(layout.getWidth() - layout.getPadding().getLeft() - layout.getPadding().getRight() - (5 * JFXUtil.SCALE));
        entries.setPrefHeight(layout.getHeight() - layout.getPadding().getBottom() - layout.getPadding().getTop() - (JFXUtil.DEFAULT_SPACING * 4));
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
