package R34;

import com.sun.istack.internal.NotNull;
import core.CoreUtil.RetrievalTools;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import core.system.ARKManagerInterface;

import java.io.IOException;

/**
 * Gathers and shows connection diagnostic information.
 */
public class R34ConnectionTestManager implements ARKManagerInterface
{
    private Stage window;
    private Scene scene;
    private AnchorPane layout;
    private Button close;
    private Button prev;
    private Button next;
    private Label resultG;
    private Label resultR;
    private Label resultP;

    private int page;

    /**
     * Creates a new Manager object of this type.
     * @param title the title of the window
     */
    public R34ConnectionTestManager(String title)
    {
        page = 1;

        window = new Stage();
        window.setTitle(title);
        window.setResizable(false);
        window.setHeight(400);
        window.setWidth(400);
        window.getIcons().add(new Image("core/assets/info.png"));

        close = new Button("Close");
        prev = new Button("Previous Page");
        next = new Button("Next Page");

        resultG = new Label("");

        resultR = new Label("");

        resultP = new Label("");

        layout = new AnchorPane(close, prev, next, resultG, resultR, resultP);
        layout.setPadding(new Insets(15, 15, 15, 15));
        scene = new Scene(layout, 400, 400);

        setNodeAlignment(close, ((int)(window.getWidth() / 2) - 40), -1, -1, 0);
        setNodeAlignment(prev, 0, -1, -1, 0);
        setNodeAlignment(next, -1, 0, -1, 0);
        setNodeAlignment(resultG, 0, -1, 0, -1);
        setNodeAlignment(resultR, 0, -1, 0, -1);
        setNodeAlignment(resultP, 0, -1, 0, -1);

        close.setOnAction(e -> window.close());

        next.setOnAction(e ->
        {
            page ++;

            if(page == 2){
                resultP.setVisible(false);
                resultG.setVisible(false);
                resultR.setVisible(true);
                prev.setVisible(true);
                next.setVisible(true);
            }else if(page == 3){
                resultP.setVisible(true);
                resultR.setVisible(false);
                resultG.setVisible(false);
                prev.setVisible(true);
                next.setVisible(false);
            }
        });

        prev.setOnAction(e ->
        {
            page --;

            if(page == 1){
                resultP.setVisible(false);
                resultG.setVisible(true);
                resultR.setVisible(false);
                prev.setVisible(false);
                next.setVisible(true);
            }else if(page == 2){
                resultP.setVisible(false);
                resultG.setVisible(false);
                resultR.setVisible(true);
                prev.setVisible(true);
                next.setVisible(true);
            }
        });

        window.setScene(scene);
    }

    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    public void display()
    {
        if(!window.isShowing()){
            window.show();
        }
    }

    /**
     * Hides this window if it is not already hidden.
     */
    public void hide()
    {
        if(window.isShowing()){
            window.hide();
        }
    }

    /**
     * Returns this Manager's visibility state.
     * @return true if this window is being displayed, false if otherwise
     */
    public boolean getVisibilityState()
    {
        return window.isShowing();
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

    /**
     * Tests the connection to Google's DNS servers, R34.Rule 34, and Paheal.
     */
    public void testConnection()
    {
        page = 1;

        prev.setVisible(false);
        next.setVisible(true);
        resultG.setVisible(true);
        resultR.setVisible(false);
        resultP.setVisible(false);

        boolean DNS = false;
        int timeG = -1;
        try {
            timeG = RetrievalTools.pingTestURL("8.8.8.8");
            DNS = timeG >= 0;
        } catch (IOException e) {
            e.printStackTrace();
        }

        String messageG = "Your Internet connection appears to be " + (DNS ? "functioning correctly.\n" : "down. \nCheck it before trying again!\n");

        if(timeG > 50 && DNS){
            messageG += "DNS is responding slowly. \nYour connection may be slow, or the servers might \nbe under heavy load. Retrieval might be slow.";
        }else if(timeG <= 15 && DNS){
            messageG += "DNS is responding quickly. \nRetrieval should go quickly.";
        }else if(timeG > 15 && timeG <= 50 && DNS){
            messageG += "DNS is responding normally. \nRetrieval should proceed at standard speeds.";
        }else{
            messageG += ".";
        }

        resultG.setText("General Connection Test Results:\n" +
                        "\n" +
                        "Can connect to DNS: " + DNS + "\n" +
                        "DNS ping time: " + timeG + "\n" +
                        "\n" +
                        "Summary:\n" +
                        "\n" +
                        messageG);

        boolean R34 = false;
        int timeR = -1;
        try{
            timeR = RetrievalTools.pingTestURL("rule34.xxx");
            R34 = timeR >= 0;
        } catch (IOException e){
            e.printStackTrace();
        }

        String messageR = "Your connection to Rule 34 appears to be " + (R34 ? "online.\n" : "down.\n");

        if(timeR > 250 && R34){
            messageR = messageR + "Servers are responding slowly. \nYour connection may be slow, or the servers might \nbe under heavy load. Retrieval might be slow.";
        }else if(timeR <= 100 && R34){
            messageR = messageR + "Servers are responding quickly. \nRetrieval should go quickly.";
        }else if(timeR > 100 && timeR <= 250 && R34){
            messageR = messageR + "Servers are responding normally. \nRetrieval should proceed at standard speeds.";
        }

        resultR.setText("Rule 34 Connection Test Results:\n" +
                        "\n" +
                        "Can connect to server: " + R34 + "\n" +
                        "Ping time: " + timeR + "\n" +
                        "\n" +
                        "Summary:\n" +
                        messageR);
        
        boolean R34PP = false;
        int timePP = -1;
        boolean R34PD = false;
        int timePD = -1;
        try{
            timePP = RetrievalTools.pingTestURL("rule34.paheal.net");
            timePD = RetrievalTools.pingTestURL("rule34-data-008.paheal.net");
            R34PP = timePP >= 0;
            R34PD = timePD >= 0;
        } catch (IOException e){
            e.printStackTrace();
        }
        
        String messagePP = "Pageservers appear to be " + (R34PP ? "up" : "down");
        String messagePD = "Data (image) servers appear to be " + (R34PD ? "up" : "down");

        if(timePP > 250 && R34PP){
            messagePP += " and are responding slowly. \nYour connection may be slow, or the servers might \nbe under heavy load. Retrieval might be slow.";
        }else if(timePP <= 100 && R34PP){
            messagePP += " and are responding quickly. \nRetrieval should go quickly.";
        }else if(timePP > 100 && timePP <= 250 && R34PP){
            messagePP += " and are responding normally. \nRetrieval should proceed at standard speeds.";
        }else{
            messagePP += ".";
        }

        if(timePD > 250 && R34PD){
            messagePD += " and are responding slowly. \nYour connection may be slow, or the servers might \nbe under heavy load. Downloads might be slow.";
        }else if(timePD <= 100 && R34PD){
            messagePD += " and are responding quickly. \nDownloads should go quickly.";
        }else if(timePD > 100 && timePD <= 250 && R34PD){
            messagePD += " and are responding normally. \nDownloads should proceed at standard speeds.";
        }else{
            messagePD += ".";
        }
        
        resultP.setText("Rule 34 Paheal Connection Test Results:\n" +
                        "\n" +
                        "Can connect to pageserver: " + R34PP + "\n" +
                        "Can connect to data server: " + R34PD + "\n" +
                        "Pageserver ping time: " + timePP + "\n" +
                        "Data server ping time: " + timePD + "\n" +
                        "\n" +
                        "Summary:\n" +
                        "\n" +
                        messagePP + "\n" +
                        messagePD);
    }
}
