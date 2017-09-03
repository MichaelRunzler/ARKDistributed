package com.michaelRunzler.ARK.core.system;

/**
 * Provides a common interface for all ARK Manager classes.
 */
public interface ARKManagerInterface
{
    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    void display();

    /**
     * Hides this window if it is not already hidden.
     */
    void hide();

    /**
     * Returns this Manager's visibility state.
     * @return true if this window is being displayed, false if otherwise
     */
    boolean getVisibilityState();
}
