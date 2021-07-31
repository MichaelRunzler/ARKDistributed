package RMan.Core.Managers;

import RMan.Core.Types.ShoppingList;

/**
 * A subclass of {@link NamedObjManager} that specializes in {@link ShoppingList Shopping Lists}.
 * This is used by the List Manager interface to keep track of Shopping Lists.
 */
public class ShoppingListManager extends NamedObjManager<ShoppingList>
{
    /**
     * @see NamedObjManager#NamedObjManager()
     */
    public ShoppingListManager(){
        super();
    }

    // This class is mostly a convenience stub subclass of NamedObjManager, but if specific functionality is needed for
    // Shopping Lists, this is where it goes.
}
