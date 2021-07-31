package RMan.Core.Managers;

import RMan.Core.Types.Component;

/**
 * A subclass of {@link NamedObjManager} that specializes in {@link Component Components}.
 * Normally, Cards don't hold their Components in a manager, just a list. However, an instance of this class is created
 * every time a Card needs to be edited or created, since the creation/editing UI needs search functionality. We could
 * just use the NamedObjManager with a type arg, but this is cleaner.
 */
public class ComponentManager extends NamedObjManager<Component>
{
    /**
     * @see NamedObjManager#NamedObjManager()
     */
    public ComponentManager(){
        super();
    }

    // This class is mostly a convenience stub subclass of NamedObjManager, but if specific functionality is needed for
    // Components, this is where it goes.
}
