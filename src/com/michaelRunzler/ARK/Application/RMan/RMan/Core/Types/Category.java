package RMan.Core.Types;

import java.io.Serializable;

/**
 * Represents a category with which {@link Card cards} may be tagged. For example, a card might be tagged with "Baking" or "Snacks".
 * User-added categories may be modified or deleted; system categories are fixed and cannot be changed.
 */
public class Category extends NamedObject implements Serializable
{
    public String name;
    public boolean userAdded;

    // The default category object. Eliminates the need to repeatedly call the default constructor.
    public static Category defaultCategory = new Category();

    /**
     * Default constructor. Creates a default category with the userAdded field set to {@code false}.
     */
    public Category()
    {
        this.name = "Uncategorized";
        this.userAdded = false;
    }

    /**
     * Full constructor. Initializes all fields from the provided arguments.
     * @param name The name of the category, e.g "Lunch" or "Easy Meals".
     * @param userAdded Whether the user added this category or not. This is used to prevent users from deleting
     *                  system-added categories.
     */
    public Category(String name, boolean userAdded)
    {
        this.name = name;
        this.userAdded = userAdded;
    }
}
