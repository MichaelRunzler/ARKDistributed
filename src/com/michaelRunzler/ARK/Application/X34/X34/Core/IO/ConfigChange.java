package X34.Core.IO;

public abstract class ConfigChange<T>
{
    public abstract T originalValue();

    public abstract T newValue();

    public abstract boolean wasRemoved();
}
