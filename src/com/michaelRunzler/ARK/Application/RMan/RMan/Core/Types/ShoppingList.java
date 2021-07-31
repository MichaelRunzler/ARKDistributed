package RMan.Core.Types;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a shopping list comprised of the components required to make one or more Cards.
 * Each entry is represented as a {@link ShoppingListEntry} and can be "checked off" individually.
 */
public class ShoppingList extends NamedObject implements Serializable
{
    public ArrayList<ShoppingListEntry> entries;

    /**
     * Default constructor.
     */
    public ShoppingList() {
        this.entries = new ArrayList<>();
    }

    /**
     * Standard constructor. Populates the list with entries from the provided {@link List}.
     * @param list The list to populate the Shopping List from.
     */
    public ShoppingList(List list)
    {
        this();
        if(list == null) throw new NullPointerException();

        this.loadList(list);
    }

    /**
     * Internal utility method to populate the internal Shopping List Entry array from the given List object.
     */
    private void loadList(List list)
    {
        // todo finish: pull list data and file by name (HM?)
    }
}
