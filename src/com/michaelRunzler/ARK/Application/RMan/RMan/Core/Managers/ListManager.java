package RMan.Core.Managers;

import RMan.Core.Types.List;

/**
 * A subclass of {@link NamedObjManager} that specializes in {@link RMan.Core.Types.List Lists}.
 * This is used by the List Manager interface to keep track of Lists.
 */
public class ListManager extends NamedObjManager<List>
{
    /**
     * @see NamedObjManager#NamedObjManager()
     */
    public ListManager(){
        super();
    }

    // This class is mostly a convenience stub subclass of NamedObjManager, but if specific functionality is needed for
    // Lists, this is where it goes.
}
