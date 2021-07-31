package RMan.Core;


import RMan.Core.Managers.CardManager;
import RMan.Core.Types.Card;
import RMan.Core.Types.Category;
import RMan.Core.Types.List;

import java.util.ArrayList;
import java.util.Random;

/**
 * Generates and edits lists of {@link Card Cards}. Can export those lists as {@link RMan.Core.Types.List Lists}.
 */
public class ListGenerator
{
    private final Random rng;
    public ArrayList<Card> cardSet;
    public ArrayList<Card> list;
    public Category[] cats;
    public int len;

    /**
     * Default constructor. The generator is initially in a state where it cannot generate lists without setting the
     * {@link #len} variable and calling {@link #updateCardSet(CardManager, Category...)} first.
     */
    public ListGenerator()
    {
        rng = new Random();
        cardSet = new ArrayList<>();
        list = new ArrayList<>();
        cats = new Category[0];
        len = 0;
    }

    /**
     * Standard constructor. Generates a list of cards with the specified length and categories from the given {@link CardManager}
     * using {@link #generateList(int)}.
     * @param manager The {@link CardManager} from which the list of {@link Card Cards} should be pulled.
     * @param length The number of {@link Card Cards} that should be included in the list.
     * @param categories One or more {@link Category} objects. If this is left empty, all categories will be included.
     */
    public ListGenerator(CardManager manager, int length, Category... categories)
    {
        this();
        if(length < 0) throw new IllegalArgumentException("Invalid length argument provided.");

        // Update the card set
        this.updateCardSet(manager, categories);

        // Generate list
        this.len = length;
        if(!this.generateList(length)) throw new IllegalArgumentException("Error while generating initial list: not enough cards in the index!");
    }

    /**
     * Generates a new list of cards from the internal card set. If the default constructor was used, {@link #updateCardSet(CardManager, Category...)}
     * must be called before generating any lists.
     * @param length The number of {@link Card Cards} that should be included in the list.
     * @return {@code true} if the generation operation succeeded, {@code false} if otherwise. If the operation fails,
     * the current list of cards will be left unaffected.
     */
    public boolean generateList(int length)
    {
        // Holds changes until the operation succeeds to avoid invalidating the existing list
        ArrayList<Card> cache = new ArrayList<>();
        // Cache the card set too
        ArrayList<Card> cardSetCache = new ArrayList<>(this.cardSet);

        // Bail out if the card set has fewer cards than the requested length
        if(cardSetCache.size() < length) return false;

        // Generate the list of cards from the cached collection
        for(int i = 0; i < length; i++)
        {
            // If the card cache is empty, we ran out of possible additions somehow - abort.
            if(cardSetCache.size() == 0) return false;

            int idx = rng.nextInt(cardSetCache.size());
            Card tmp = cardSetCache.get(idx);
            // Make sure this card isn't already in the list; if it is, remove it from the pool and try again
            if(cache.contains(tmp)) {
               cardSetCache.remove(idx);
               i--;
            } else
                cache.add(tmp);
        }

        // Push the cache to the live list
        this.list = cache;

        return true;
    }

    /**
     * Generates a new list of cards from the internal card set. Uses the currently set length.
     * @return {@code true} if the generation operation succeeded, {@code false} if otherwise. If the operation fails,
     * the current list of cards will be left unaffected.
     */
    public boolean generateList(){
        return generateList(this.len);
    }

    /**
     * Generates a new Card to replace the given element index from the current card list.
     * @param index The index of the element to regenerate.
     * @return {@code true} if the generation operation succeeded, {@code false} if otherwise. If the operation fails,
     * the current list of cards will be left unaffected.
     */
    public boolean regenerateElement(int index)
    {
        // Check index bounds
        if(index >= list.size() || index < 0) throw new IllegalArgumentException("Index out of range.");

        // Cache the card set as in generateList()
        ArrayList<Card> cardSetCache = new ArrayList<>(this.cardSet);

        boolean success = false;
        // Try to generate a new card for this index
        while(!success)
        {
            // If the card cache is empty, we ran out of possible replacements somehow - abort.
            if(cardSetCache.size() == 0) return false;

            // Try getting a card from the set. If it's already in the list (including the card that we're trying to
            // replace), remove it from the set and try again.
            int idx = rng.nextInt(cardSetCache.size());
            Card tmp = cardSetCache.get(idx);
            if(list.contains(tmp)) cardSetCache.remove(idx);
            else{
                list.set(index, tmp);
                success = true;
            }
        }

        return true;
    }

    /**
     * Refresh the internal card set from the provided {@link CardManager}. If you wish to leave the current category
     * set untouched, pass {@link #cats} as the second argument.
     * @param manager The {@link CardManager} from which the list of {@link Card Cards} should be pulled.
     * @param categories One or more {@link Category} objects. If this is left empty, all categories will be included.
     */
    public void updateCardSet(CardManager manager, Category... categories)
    {
        if(manager == null) throw new NullPointerException();

        // Update category list
        if(categories == null) this.cats = new Category[0];
        else this.cats = categories;

        // Grab the list of cards that match the category settings from the Card Manager...
        if(categories != null && categories.length > 0)
            for(Category c : categories)
                cardSet.addAll(manager.searchByCategory(c));
        else // ...or, if no categories were given, get ALL the cards from the manager instead.
            cardSet.addAll(manager.index);
    }

    /**
     * Exports the currently held list as a {@link List} object for use elsewhere.
     * @param name A name for the list. If {@code null} is passed, no name will be set (the name will default to
     *             whatever is specified in {@link List#List()}).
     * @return The current card list as a {@link List}.
     */
    public List exportList(String name) {
        return name == null ? new List(this.list) : new List(name, this.list);
    }

    /**
     * Imports the given list into the generator for editing. This replaces all currently held list data, although it
     * does not affect the set categories. The list length will be set to the length of the provided list.
     * @param list The list to import.
     */
    public void importList(List list)
    {
        if(list == null) throw new NullPointerException();

        this.list = list.cards;
        this.len = list.cards.size();
    }
}
