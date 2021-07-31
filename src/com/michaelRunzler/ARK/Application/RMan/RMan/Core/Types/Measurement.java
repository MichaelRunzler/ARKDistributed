package RMan.Core.Types;

import java.io.Serializable;

/**
 * Represents a measurement type along with its shortened name; for example, "Tablespoon (tbsp)" or "Kilogram (kg)".
 * User-added measurement types may be modified or deleted; system measurements are fixed and cannot be changed.
 */
public class Measurement extends NamedObject implements Serializable
{
    public String shortName;
    public String plural;
    public boolean allowFractional;
    public boolean userAdded;

    // The default category object. Eliminates the need to repeatedly call the default constructor.
    public static Measurement defaultMeasurement = new Measurement();

    /**
     * Default constructor. Creates a default measurement with the {@link #allowFractional} and {@link #userAdded} fields set to {@code false}.
     */
    public Measurement()
    {
        this.name = "Unnamed Measurement";
        this.shortName = "none";
        this.plural = "s";
        this.allowFractional = false;
        this.userAdded = false;
    }

    /**
     * Full constructor. Initializes all fields from the provided arguments.
     * @param name The singular name of the measurement, e.g "Teaspoon" or "Pound".
     * @param shortName The shortened name of the measurement, e.g "tsp" or "lbs". Pick a shortened name that ignores
     *                  plurality if possible; otherwise, use the plural version.
     * @param plural The plural suffix that should be appended to the long-form name of the measurement.
     *               For example, "Pound" should have a plurality suffix of "s", resulting in "Pounds".
     *               "Pinch" should have a suffix of "es", for "Pinches".
     * @param allowFractional Some measurements can't be fractionated - for example, you can't have "1/4 pinch" of salt,
     *                        since a pinch isn't a precise measurement. Those measurements should have this flag set to
     *                        {@code false}.
     * @param userAdded Whether the user added this measurement type or not. This is used to prevent users from deleting
     *                  system-added measurements.
     */
    public Measurement(String name, String shortName, String plural, boolean allowFractional, boolean userAdded)
    {
        this.name = name;
        this.shortName = shortName;
        this.plural = plural;
        this.allowFractional = allowFractional;
        this.userAdded = userAdded;
    }
}
