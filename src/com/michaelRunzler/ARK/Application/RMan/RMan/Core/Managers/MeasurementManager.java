package RMan.Core.Managers;

import RMan.Core.Types.Measurement;

import java.util.ArrayList;

/**
 * A subclass of {@link NamedObjManager} that specializes in {@link Measurement Measurements}.
 * This is used by the config backend to keep track of system- and user-added measurement units.
 */
public class MeasurementManager extends NamedObjManager<Measurement>
{
    /**
     * @see NamedObjManager#NamedObjManager()
     */
    public MeasurementManager(){
        super();
    }

    /**
     * Searches the index and returns all measurements whose shortened names match the search.
     * @param search The search query.
     * @param fragment If this is set to {@code false}, an exact search is performed. If {@code true}, objects whose short names
     *                 contain the query (but don't exactly match it) will also be included.
     * @return The list of {@link Measurement Measurements} that matched the query, or an empty {@link ArrayList} if none were found.
     */
    public ArrayList<Measurement> searchByShortName(String search, boolean fragment)
    {
        ArrayList<Measurement> found = new ArrayList<>();

        // Default to an empty result set if the index is empty
        if(index.size() == 0) return found;
        // Default to the entire index for an empty search
        if(search == null || search.length() == 0) return index;

        // Iterate through the index, checking the name of each Card against the search
        for(Measurement c : index) {
            if(fragment) {
                if(c.shortName.contains(search)) found.add(c); // Substring matching for fragment=true
            } else {
                if(c.shortName.equals(search)) found.add(c); // Whole-string matching for fragment=false
            }
        }

        return found;
    }
}
