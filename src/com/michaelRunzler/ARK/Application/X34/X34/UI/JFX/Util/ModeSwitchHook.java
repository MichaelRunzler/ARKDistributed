package X34.UI.JFX.Util;

/**
 * Used to represent actions that should be taken when an application mode-switch is in progress.
 */
public interface ModeSwitchHook {

    /**
     * Called when the application that declared it switches modes.
     * @param mode the mode that the application is switching to
     */
    void onModeSwitch(int mode);
}
