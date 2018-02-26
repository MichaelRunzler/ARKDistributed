package X34.UI.JFX.Managers;

import core.UI.ARKManagerBase;

import java.io.File;

public class X34UIFileManager extends ARKManagerBase
{
    public static final String TITLE = "File Management";
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 400;

    //todo finish
    public X34UIFileManager(double x, double y)
    {
        super(TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);
    }

    public void setParentDirectory(File parent)
    {

    }

    public void computeAvailableFiles()
    {

    }
}
