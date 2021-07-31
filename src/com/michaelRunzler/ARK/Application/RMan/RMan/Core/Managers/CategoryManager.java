package RMan.Core.Managers;

import RMan.Core.Types.Measurement;

/**
 * A subclass of {@link NamedObjManager} that specializes in {@link RMan.Core.Types.Category Categories}.
 * This is used by the config backend to keep track of categories.
 */
public class CategoryManager extends NamedObjManager<Measurement>
{
    /**
     * @see NamedObjManager#NamedObjManager()
     */
    public CategoryManager(){
        super();
    }

    // This class is mostly a convenience stub subclass of NamedObjManager, but if specific functionality is needed for
    // Categories, this is where it goes.
}
