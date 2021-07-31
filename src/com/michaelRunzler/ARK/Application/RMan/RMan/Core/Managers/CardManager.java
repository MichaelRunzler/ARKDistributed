package RMan.Core.Managers;

import RMan.Core.Types.Card;
import RMan.Core.Types.Category;

import java.util.ArrayList;

/**
 * A subclass of the {@link NamedObjManager} that specializes in {@link Card Cards}.
 * Includes the ability to search the index by description as well as name.
 * A master instance of this class is used by the main UI to keep track of all stored Cards.
 */
public class CardManager extends NamedObjManager<Card>
{
    /**
     * @see NamedObjManager#NamedObjManager()
     */
    public CardManager() {
        super();
    }

    /**
     * Searches the index and returns all cards whose descriptions contain the search query. This search uses substring
     * matching only; exclusion of partial matches is not possible.
     * @param search The search query.
     * @return The list of {@link Card Cards} that matched the query, or an empty {@link ArrayList} if none were found.
     */
    public ArrayList<Card> searchByDescription(String search)
    {
        ArrayList<Card> found = new ArrayList<>();

        // Default to an empty result set if the index is empty
        if(index.size() == 0) return found;
        // Default to the entire index for an empty search
        if(search == null || search.length() == 0) return index;

        // Iterate through the index, checking the description of each Card against the search
        for(Card c : index)
            if(c.description.contains(search)) found.add(c);

        return found;
    }

    /**
     * Searches the index and returns all cards whose prepared servings are within the given bounds. Both bounds are
     * inclusive, so setting them equal to one another will return all cards whose servings exactly match the given
     * bounds. Setting bounds which have no possible results will return an empty list. Setting a lower bound less than
     * zero will result in all values below the upper bound being included.
     * @param lowerBound The minimum number of servings that a recipe can make to be included in the search (inclusive).
     * @param upperBound The maximum number of servings that a recipe can make to be included in the search (inclusive).
     * @return The list of {@link Card Cards} that matched the query, or an empty {@link ArrayList} if none were found.
     */
    public ArrayList<Card> searchByServings(int lowerBound, int upperBound)
    {
        ArrayList<Card> found = new ArrayList<>();

        // Default to an empty result set if the index is empty or the bounds include no possible values
        if(index.size() == 0 || upperBound < lowerBound) return found;

        // Iterate through the index, checking that the servings given by that Card are between the bounds
        for(Card c : index)
            if(c.makesNum >= lowerBound && c.makesNum <= upperBound) found.add(c);

        return found;
    }

    /**
     * Searches the index and returns all cards whose prep times are within the given bounds. Both bounds are
     * inclusive, so setting them equal to one another will return all cards whose prep times exactly match the given
     * bounds. Setting bounds which have no possible results will return an empty list. Setting a lower bound less than
     *      * zero will result in all values below the upper bound being included.
     * @param lowerBound The minimum time that a recipe can take to make to be included in the search (inclusive).
     * @param upperBound The maximum time that a recipe can take to make to be included in the search (inclusive).
     * @return The list of {@link Card Cards} that matched the query, or an empty {@link ArrayList} if none were found.
     */
    public ArrayList<Card> searchByPrepTime(int lowerBound, int upperBound)
    {
        ArrayList<Card> found = new ArrayList<>();

        // Default to an empty result set if the index is empty or the bounds include no possible values
        if(index.size() == 0 || upperBound < lowerBound) return found;

        // Iterate through the index, checking that the prep time of that Card is between the bounds
        for(Card c : index)
            if(c.prepTime >= lowerBound && c.prepTime <= upperBound) found.add(c);

        return found;
    }

    public ArrayList<Card> searchByCategory(Category category)
    {
        ArrayList<Card> found = new ArrayList<>();

        // Default to an empty result set if the index is empty or the category is invalid
        if(index.size() == 0 || category == null) return found;

        // Iterate through the index, checking the category of each Card
        for(Card c : index)
            if(c.category.equals(category)) found.add(c);

        return found;
    }
}
