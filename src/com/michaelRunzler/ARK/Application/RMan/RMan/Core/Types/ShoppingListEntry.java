package RMan.Core.Types;

import java.io.Serializable;

/**
 * Represents an entry on a {@link ShoppingList}, including a name, a quantity (composited from one or more
 * {@link Component Components}), and a checkbox state.
 */
public class ShoppingListEntry extends NamedObject implements Serializable
{
    public String quantity;
    public boolean isChecked;

    /**
     * Default constructor.
     */
    public ShoppingListEntry()
    {
        this.name = "Unnamed Item";
        this.isChecked = false;
    }

    /**
     * Full constructor. Sets variables literally from their arguments.
     * @param name The name of the list (what Component name it's representing).
     * @param quantity The amount of this item that's required. This is typically composited from one or more Components.
     * @param isChecked If this item is "checked-off" or not.
     */
    public ShoppingListEntry(String name, String quantity, boolean isChecked)
    {
        this.name = name;
        this.quantity = quantity;
        this.isChecked = isChecked;
    }

    /**
     * Reduced constructor. Same as {@link #ShoppingListEntry(String, String, boolean)}, but sets {@link #isChecked}
     * to its default value.
     * @param name The name of the list (what Component name it's representing).
     * @param quantity The amount of this item that's required. This is typically composited from one or more Components.
     */
    public ShoppingListEntry(String name, String quantity) {
        this(name, quantity, false);
    }

    /**
     * Builder-constructor. Derives name and quantity from provided components.
     * @param isChecked If this item is "checked-off" or not.
     * @param parents One or more {@link Component Components} representing the items to be included on the list.
     *                Providing an empty or null list will result in a {@link NullPointerException} being thrown.
     *                Providing a list containing one or more Components with differing names will result in a
     *                {@link IllegalArgumentException} being thrown.
     */
    public ShoppingListEntry(boolean isChecked, Component... parents)
    {
        if(parents == null || parents.length == 0) throw new NullPointerException();

        this.isChecked = isChecked;
        this.name = parents[0].name;
        StringBuilder sb = new StringBuilder();

        // Construct composite quantity from parent(s); check to ensure that all parent Components have the same name
        for(int i = 0; i < parents.length; i++)
        {
            Component c = parents[i];
            if (!c.name.equals(this.name))
                throw new IllegalArgumentException("One or more Components have differing names.");

            sb.append(c.quantity);
            if(i < parents.length - 1) sb.append(" + ");
        }

        this.quantity = sb.toString();
    }

    /**
     * Reduced builder-constructor. Same as {@link #ShoppingListEntry(boolean, Component...)}, but sets {@link #isChecked}
     * to its default value.
     * @param parents One or more {@link Component Components} representing the items to be included on the list.
     *                Providing an empty or null list will result in a {@link NullPointerException} being thrown.
     *                Providing a list containing one or more Components with differing names will result in a
     *                {@link IllegalArgumentException} being thrown.
     */
    public ShoppingListEntry(Component... parents) {
        this(false, parents);
    }
}
