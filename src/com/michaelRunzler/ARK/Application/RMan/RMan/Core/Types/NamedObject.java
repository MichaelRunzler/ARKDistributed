package RMan.Core.Types;

import java.io.Serializable;

/**
 * Represents a class of objects which have searchable names.
 */
public abstract class NamedObject implements Serializable
{
    public String name;

    @Override
    public String toString() {
        return this.name;
    }
}
