package X34.UI.JFX.Managers;

import core.CoreUtil.JFXUtil;
import core.UI.ARKManagerBase;

/**
 * Displays and manages a copy of the system logger output, viewable in a separate window.
 */
public class X34UIConsoleLogManager extends ARKManagerBase
{
    public static final String DEFAULT_TITLE = "System Log Output";

    public X34UIConsoleLogManager(int width, int height, double x, double y)
    {
        super(DEFAULT_TITLE, (int)(width * JFXUtil.SCALE), (int)(height * JFXUtil.SCALE), x, y);

        //todo finish
    }
}
