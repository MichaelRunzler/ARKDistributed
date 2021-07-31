package RMan.Core.Types;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Contains a list of {@link Card Cards} along with a name. Used with the {@link RMan.Core.ListGenerator} class.
 */
public class List extends NamedObject implements Serializable
{
    public ArrayList<Card> cards;

    /**
     * Default constructor.
     */
    public List()
    {
        super();
        this.name = "Unnamed List";
        this.cards = new ArrayList<>();
    }

    /**
     * Full constructor.
     * @param name The name that this list should have.
     * @param cards The list of cards to store in this List.
     */
    public List(String name, ArrayList<Card> cards)
    {
        this(cards);
        this.name = name;
    }

    /**
     * Partial constructor. Defaults the name to whatever is set by {@link #List()}.
     * @param cards The list of cards to store in this List.
     */
    public List(ArrayList<Card> cards)
    {
        this();
        this.cards = cards;
    }
}
