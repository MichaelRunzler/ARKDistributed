package core.UI;

import com.sun.istack.internal.NotNull;
import core.CoreUtil.JFXUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * A subvariant of the {@link CheckBoxListCell} that is, by default, editable via a built-in {@link javafx.scene.control.TextField}.
 * @param <T> the type of elements contained within this cell's parent {@link javafx.scene.control.ListView}
 */
public class CheckBoxEditableListCell<T> extends CheckBoxListCell<T>
{
    private ChangeListener<? extends String> numericListener;

    protected TextField editField;
    protected Node superclassGraphicStorage;
    protected String superclassStringStorage;

    private KeyCodeCombination editKey;
    private KeyCodeCombination cancelKey;

    private long lastClickReleased;
    private boolean isLastClickPrimary;

    public static KeyCodeCombination DEFAULT_EDIT_KEYBIND = new KeyCodeCombination(KeyCode.ENTER);
    public static KeyCodeCombination DEFAULT_CANCEL_KEYBIND = new KeyCodeCombination(KeyCode.ESCAPE);

    /**
     * Creates a default CheckBoxEditableListCell.
     */
    public CheckBoxEditableListCell() {
        this(null);
    }

    /**
     * Creates a default CheckBoxEditableListCell.
     *
     * @param getSelectedProperty A {@link Callback} that will return an
     *      {@code ObservableValue<Boolean>} given an item from the ListView.
     */
    public CheckBoxEditableListCell(final Callback<T, ObservableValue<Boolean>> getSelectedProperty)
    {
        // cannot immediately sub-delegate, since we don't have access to the required utility class to get the default converter
        super(getSelectedProperty);
        setupInitialValues();
    }

    /**
     * Creates a CheckBoxEditableListCell with a custom string converter.
     *
     * @param getSelectedProperty A {@link Callback} that will return an
     *      {@code ObservableValue<Boolean>} given an item from the ListView.
     * @param converter A StringConverter that, given an object of type T, will
     *      return a String that can be used to represent the object visually.
     */
    public CheckBoxEditableListCell(final Callback<T, ObservableValue<Boolean>> getSelectedProperty, final StringConverter<T> converter)
    {
        super(getSelectedProperty, converter);
        setupInitialValues();
    }

    private void setupInitialValues()
    {
        this.numericListener = null;
        editField = null;
        superclassGraphicStorage = null;
        superclassStringStorage = null;
        setEditKeyBinding(DEFAULT_EDIT_KEYBIND);
        setCancelKeyBinding(DEFAULT_CANCEL_KEYBIND);
        lastClickReleased = -1;
        isLastClickPrimary = false;

        // This is used to tell the release listener if the last click was primary or not, since it cannot tell by itself
        super.setOnMousePressed(event -> isLastClickPrimary = event.isPrimaryButtonDown());

        // Set up mouse double-click editing trigger support
        super.setOnMouseReleased(event -> {
            if(!isLastClickPrimary) return;

            // If the last click counter has not been set, or it has been more than 1 second since the last click event,
            // reset the click timer and return
            if(lastClickReleased < 0 || System.currentTimeMillis() - lastClickReleased > 500){
                lastClickReleased = System.currentTimeMillis();
            // Otherwise, if it has been less than 1 second since the last click release, count it as a double click and start editing
            }else if (!super.isEditing()) startEdit();
        });

        super.setOnKeyReleased(event -> {
            if(editKey.match(event) && super.isEditing()) commitEdit(super.getConverter().fromString(editField.getText()));
            else if(cancelKey.match(event) && super.isEditing()) cancelEdit();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateItem(T item, boolean empty)
    {
        super.updateItem(item, empty);

        if(!empty && editField == null) editField = new TextField();
        if(editField != null && !editField.maxWidthProperty().isBound()) editField.maxWidthProperty().bind(super.widthProperty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startEdit()
    {
        if(isEmpty()) return;
        super.startEdit();

        editField.setText(super.getText());
        superclassGraphicStorage = super.getGraphic();
        superclassStringStorage = super.getText();
        super.setText(null);
        super.setGraphic(editField);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit(T newValue)
    {
        if(superclassGraphicStorage != null) super.setGraphic(superclassGraphicStorage);

        super.commitEdit(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEdit()
    {
        if(superclassGraphicStorage != null) super.setGraphic(superclassGraphicStorage);
        if(superclassStringStorage != null) super.setText(superclassStringStorage);

        super.cancelEdit();
    }

    /**
     * Sets which combination of keys trigger the editing mode for this cell.
     * By default, this is set to 'Enter'.
     * @param key a {@link KeyCodeCombination} representing the desired keybinding
     */
    public void setEditKeyBinding(@NotNull KeyCodeCombination key) {
       this.editKey = key;
    }

    /**
     * Sets which combination of keys cancel an ongoing edit if there is one.
     * By default, this is set to 'Escape'.
     * @param key a {@link KeyCodeCombination} representing the desired keybinding
     */
    public void setCancelKeyBinding(@NotNull KeyCodeCombination key) {
       this.cancelKey = key;
    }

    /**
     * Gets the key combination currently assigned to this object's editing function.
     * @return the {@link KeyCodeCombination} representing the current editing shortcut
     */
    public KeyCodeCombination getEditKey() {
        return editKey;
    }

    /**
     * Gets the key combination currently assigned to this object's edit-cancel function.
     * @return the {@link KeyCodeCombination} representing the current edit cancel shortcut
     */
    public KeyCodeCombination getCancelKey() {
        return cancelKey;
    }

    /**
     * Limits or delimits this cell's editing field. Limited editing fields will only accept numbers, minus signs, and
     * decimal points, and will enforce a maximum character limit specified with the limit parameter. Any input not
     * conforming to these parameters will be blocked from entering the field.
     * Input to the field from any non-user source (i.e direct method calls, edit name generation via internal logic, etc.)
     * will also be subjected to these parameters.
     * If the field's content somehow includes an illegal character, the only allowed actions will be (1) clearing the field,
     * or (2) deleting one or more characters from the field's contents.
     * @param numeric if this is {@code true}, the field will be limited to numeric entry only (including minus signs
     *                and decimal points). If {@code false}, the field will be delimited if it is currently limited,
     *                and no action will be taken if it is not limited.
     * @param limit the maximum number of characters that can be present in the field at any given time, including
     *                       punctuation and signing, if this field is set to be numeric-only
     */
    public void setEditFieldNumeric(boolean numeric, int limit)
    {
        if(numeric){
            this.numericListener = JFXUtil.limitTextFieldToNumerical(this.editField, limit);
            return;
        }

        if(this.numericListener != null)
        {
            //noinspection unchecked
            this.editField.textProperty().removeListener((ChangeListener<String>)numericListener);
            this.numericListener = null;
        }
    }
}
