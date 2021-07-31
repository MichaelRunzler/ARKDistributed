package RMan.Core.Managers;

import RMan.Core.Types.Card;
import RMan.Core.Types.NamedObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A subclass of the {@link CollectionManager} that specializes in {@link NamedObject Named Objects}.
 * Includes the ability to search the index by name.
 */
public class NamedObjManager<E extends NamedObject> extends CollectionManager<E>
{
    /**
     * @see CollectionManager#CollectionManager()
     */
    public NamedObjManager() {
        super();
    }

    /**
     * Gets a displayable array of the names of all indexed objects. This is useful for presenting the indexed objects
     * in a UI or CLI, where only the string representations of the objects matter. Objects in the index may be found
     * by running their names through {@link #searchByName(String, boolean)} with {@code fragment} set to {@code false}.
     * If a less computationally-expensive method of back-referencing objects is needed, such as with UIs that frequently
     * access and modify indexed objects, use {@link #getLinkableList()} instead.
     * The list of names returned by this method is not updated as the index is updated.
     * @return A list of all names of currently indexed objects.
     */
    public String[] getDisplayableList()
    {
        String[] results = new String[index.size()];

        for(int i = 0; i < index.size(); i++)
            results[i] = index.get(i).name;

        return results;
    }

    /**
     * Gets a name-to-index map of all indexed objects. This is useful for presenting the indexed objects in a UI or CLI
     * where there is a frequent need to refer back to the indexed objects themselves from their names. If the objects
     * themselves are not needed (only their names), use {@link #getDisplayableList()} instead.
     * The map that is returned by this method is not updated as the index is updated.
     * @return A map pairing object names with their indices in the index.
     */
    public Map<String, Integer> getLinkableList()
    {
        HashMap<String, Integer> results = new HashMap<>();
        for(int i = 0; i < index.size(); i++)
            results.put(index.get(i).name, i);

        return results;
    }

    /**
     * Searches the index and returns all objects whose names match the search.
     * @param search The search query.
     * @param fragment If this is set to {@code false}, an exact search is performed. If {@code true}, objects whose names
     *                 contain the query (but don't exactly match it) will also be included.
     * @return The list of {@link NamedObject objects} that matched the query, or an empty {@link ArrayList} if none were found.
     */
    public ArrayList<E> searchByName(String search, boolean fragment)
    {
        ArrayList<E> found = new ArrayList<>();

        // Default to an empty result set if the index is empty
        if(index.size() == 0) return found;
        // Default to the entire index for an empty search
        if(search == null || search.length() == 0) return index;

        // Iterate through the index, checking the name of each Card against the search
        for(E c : index) {
            if(fragment) {
                if(c.name.contains(search)) found.add(c); // Substring matching for fragment=true
            } else {
                if(c.name.equals(search)) found.add(c); // Whole-string matching for fragment=false
            }
        }

        return found;
    }
}
