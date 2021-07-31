package RMan.Core.Types;

import java.io.Serializable;

/**
 * Represents a component (ingredient, usually) used in a {@link Card}. Each instance of a {@link Component} represents
 * an actual ingredient used in a Card, so it includes a quantity or measurement.
 */
public class Component extends NamedObject implements Serializable
{
    public String quantity; // Yes, this is a string - users like to input fractional measurements like 3/4, not decimals
                            // like 0.75. They also like to use size modifiers for some god-damn reason.
    public Measurement quantityMeasure;

    /**
     * Default constructor. Creates a default component.
     */
    public Component()
    {
        this.name = "Unnamed Component";
        this.quantity = "0";
        this.quantityMeasure = Measurement.defaultMeasurement;
    }

    /**
     * Full constructor. Initializes all fields from the provided arguments.
     * @param name The name of the component, e.g "Tomato" or "All-Purpose Flour".
     * @param quantity The amount of this component that's needed for its recipe card. This is usually numeric, like "1"
     *                 or "2", but it can be fractional, like "3/4" or "1/3" (used with precise measurements like cups
     *                 or ounces), or descriptive, like "Small" (used with imprecise measurements like pinches or dashes).
     * @param quantityMeasure The {@link Measurement} that describes the quantity.
     */
    public Component(String name, String quantity, Measurement quantityMeasure)
    {
        this.name = name;
        this.quantity = quantity;
        this.quantityMeasure = quantityMeasure;
    }
}
