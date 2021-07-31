package RMan.Core.Types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        this.loadList(list);
    }

    /**
     * Internal utility method to populate the internal Shopping List Entry array from the given List object.
     */
    private void loadList(List list)
    {
        this.name = list.name;

        Map<String, ArrayList<Component>> componentMapping = new HashMap<>();

        // Iterate through the list of cards, grabbing the components from each and storing them in the map
        for(Card c : list.cards)
        {
            for(Component cm : c.components)
            {
                // Initialize the map entry if this is the first component by that name; otherwise, add it to the
                // existing entry.
                if(!componentMapping.containsKey(cm.name)) componentMapping.put(cm.name, new ArrayList<>());
                componentMapping.get(cm.name).add(cm);
            }
        }

        // Translate the map entries into ShoppingListEntry objects using the builder-constructor
        for(String k : componentMapping.keySet())
           this.entries.add(new ShoppingListEntry(componentMapping.get(k).toArray(new Component[0])));
    }
}
