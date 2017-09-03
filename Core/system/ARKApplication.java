package system;

import com.michaelRunzler.ARK.core.UI.ARKInterfaceDialogYN;
import com.sun.istack.internal.NotNull;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

/**
 * Superclass for all ARK application platform programs. Allows subclasses to be
 * started and managed from the ARK launcher.
 */
public class ARKApplication
{
    /**
     * Required no-arg constructor for this application.
     */
    public ARKApplication(){}

    /**
     * Launches the application's core threads and initializes basic UI elements.
     * @throws Exception if the program throws an exception. Duh.
     */
    public void launch() throws Exception
    {
    }

    /**
     * Initializes all UI elements and sets preliminary properties for objects.
     */
    private void sysPreInit()
    {
    }

    /**
     * Displays the UI and manages user input logic for all main UI elements.
     */
    private void sysInit()
    {
    }

    /**
     * Exits the program after asking for user confirmation.
     */
    private void exitSystem()
    {
        if(new ARKInterfaceDialogYN("Query", "Are you sure you want to exit?", "Yes", "No", 125, 125).display()){
            System.exit(0);
        }
    }

    /**
     * Sets the alignment of a JavaFX Node in the layout pane. Utility method to simplify code.
     * Set an offset number to -1 to leave that axis at default (the equivalent of not calling the
     * method on that axis for this node).
     * @param n the node to set positioning for
     * @param left the left offset
     * @param right the right offset
     * @param top the top offset
     * @param bottom you get the idea, right?
     */
    public void setNodeAlignment(@NotNull Node n, int left, int right, int top, int bottom)
    {
        if(left >= 0)
            AnchorPane.setLeftAnchor(n, (double)left);
        if(right >= 0)
            AnchorPane.setRightAnchor(n, (double)right);
        if(top >= 0)
            AnchorPane.setTopAnchor(n, (double)top);
        if(bottom >= 0)
            AnchorPane.setBottomAnchor(n, (double)bottom);
    }
}
