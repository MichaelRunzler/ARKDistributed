package RMan.Core.Managers;

import RMan.Core.Types.Measurement;

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

    // This class is mostly a convenience stub subclass of NamedObjManager, but if specific functionality is needed for
    // Measurements, this is where it goes.
}
