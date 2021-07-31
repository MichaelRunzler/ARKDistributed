package RMan.Core.Types;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

/**
 * Represents a "recipe card", including a list of ingredients (as {@link Component Components}), preparation time,
 * creation date/time, instructions, a short description, and other metadata.
 */
public class Card extends NamedObject implements Serializable
{
    public String description;
    public String instructions;
    public double prepTime;
    public String creator;
    public Date creationDate;
    public int makesNum;
    public ArrayList<Component> components;
    public Category category;

    /**
     * Default constructor. Creates a default recipe card.
     */
    public Card()
    {
        this.name = "Unnamed Card";
        this.description = "No Description";
        this.instructions = "No Instructions";
        this.prepTime = 0.0d;
        this.creator = "Name";
        this.creationDate = Date.from(Instant.now());
        this.makesNum = 0;
        this.components = new ArrayList<>();
        this.category = Category.defaultCategory;
    }

    /**
     * Full constructor. Initializes all fields from the provided arguments.
     * @param name The name of the card, e.g "Spaghetti Carbonara" or "Cinnamon Swirl Bread".
     * @param description A (typically) short description of the recipe on the Card. This will show up in search results
     *                    for those unfamiliar with the recipe on the Card.
     * @param instructions The step-by-step instructions for preparing the dish. This can be in whatever format the user
     *                     wants, and mimics the "instructions" area on a physical recipe index card.
     * @param prepTime The approximate amount of time required to prepare the recipe on this Card, in minutes.
     * @param creator Who (1) input this Card into the system, or (2) wrote the original recipe, whichever the user prefers.
     * @param creationDate The date on which this Card was created, as a {@link Date} object.
     * @param makesNum How many resultant servings this dish makes.
     * @param components The list of {@link Component Components} that this Card requires.
     * @param category The {@link Category} that this recipe should be classified under.
     */
    public Card(String name, String description, String instructions, double prepTime, String creator,
                Date creationDate, int makesNum, ArrayList<Component> components, Category category)
    {
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.prepTime = prepTime;
        this.creator = creator;
        this.creationDate = creationDate;
        this.makesNum = makesNum;
        this.components = components;
        this.category = category;
    }

    /**
     * Reduced constructor. Only takes essential information and sets all other fields to their defaults.
     * @param name The name of the card, e.g "Spaghetti Carbonara" or "Cinnamon Swirl Bread".
     * @param description A (typically) short description of the recipe on the Card. This will show up in search results
     *                    for those unfamiliar with the recipe on the Card.
     * @param instructions The step-by-step instructions for preparing the dish. This can be in whatever format the user
     *                     wants, and mimics the "instructions" area on a physical recipe index card.
     * @param creator Who (1) input this Card into the system, or (2) wrote the original recipe, whichever the user prefers.
     */
    public Card(String name, String description, String instructions, String creator)
    {
        this();
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.creator = creator;
    }

    /**
     * Minimal constructor. Creates an empty Card with only a title, creation date, and creator name set.
     * ALl other fields are defaulted.
     * @param name The name of the card, e.g "Spaghetti Carbonara" or "Cinnamon Swirl Bread".
     * @param creator Who (1) input this Card into the system, or (2) wrote the original recipe, whichever the user prefers.
     */
    public Card(String name, String creator)
    {
        this();
        this.name = name;
        this.creator = creator;
    }

    /**
     * Empty constructor. Only initializes the creator name and creation date fields, initializes all others to blank
     * (NOT default!). The only other field that is defaulted is the category, which is set to Uncategorized.
     * @param creator Who (1) input this Card into the system, or (2) wrote the original recipe, whichever the user prefers.
     */
    public Card(String creator)
    {
        this.name = "";
        this.description = "";
        this.instructions = "";
        this.prepTime = 0.0d;
        this.creator = creator;
        this.creationDate = Date.from(Instant.now());
        this.makesNum = 0;
        this.components = new ArrayList<>();
        this.category = Category.defaultCategory;
    }

    public String fullToString()
    {
        return String.format("\"%s\"\n" +
                "Author: %s\n" +
                "Created on: %s\n" +
                "Category: %s\n" +
                "Preparation Time: %.2f min.\n" +
                "Makes %d serving(s)\n" +
                "%s\n\n" +
                "Preparation Instructions:\n" +
                "%s",
                this.name, this.creator, new SimpleDateFormat("yyyy-MM-dd hh:mm aa").format(this.creationDate),
                this.category.name, this.prepTime, this.makesNum, this.description, this.instructions);
    }
}
