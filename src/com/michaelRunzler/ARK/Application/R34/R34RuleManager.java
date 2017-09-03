import com.sun.istack.internal.NotNull;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import system.ARKManagerInterface;

import java.io.File;

public class R34RuleManager implements ARKManagerInterface
{
    private Stage window;
    private Scene scene;
    private AnchorPane layout;

    private Button close;
    private Button cancel;
    private ChoiceBox<String> repo;
    private TextField tag;
    private TextField name;
    private Button setDest;
    private CheckBox pushChanges;

    private Rule editing;
    private File dest;
    private DirectoryChooser destCh;

    public R34RuleManager(String title, double x, double y)
    {
        window = new Stage();
        window.setTitle(title);
        window.setResizable(false);
        window.setHeight(300);
        window.setWidth(220);
        window.setX(x);
        window.setY(y);
        window.getIcons().add(new Image("assets/options.png"));
        window.initModality(Modality.APPLICATION_MODAL);

        window.setOnCloseRequest(e -> hide());

        close = new Button("Save & Close");
        cancel = new Button("Cancel");
        setDest = new Button("Download Directory...");
        repo = new ChoiceBox<>();
        tag = new TextField();
        name = new TextField();
        pushChanges = new CheckBox("Push changes");

        destCh = new DirectoryChooser();
        destCh.setInitialDirectory(new File(System.getProperty("user.home")));
        destCh.setTitle("Choose Download Directory");

        layout = new AnchorPane(close, cancel, repo, tag, name, pushChanges, setDest);
        layout.setPadding(new Insets(15, 15, 15, 15));

        repo.getItems().addAll("Both", "Paheal", "R34");
        tag.setPromptText("Tag...");
        name.setPromptText("Name...");

        scene = new Scene(layout, 200, 200);

        close.setOnAction(e ->{
            if(editing != null){
                editing.name = name.getText();
                editing.tag = tag.getText();
                editing.repo = repo.getSelectionModel().getSelectedIndex();
                editing.push = pushChanges.isSelected();
                editing.dest = dest;
            }else{
                editing = new Rule(name.getText(), repo.getSelectionModel().getSelectedIndex(), tag.getText(), pushChanges.isSelected(), dest);
            }
            hide();
        });

        cancel.setOnAction(e ->{
            repo.getSelectionModel().select(0);
            tag.setText("");
            name.setText("");
            pushChanges.setSelected(true);
            editing = null;
            dest = new File(System.getProperty("user.home"));
            hide();
        });

        setDest.setOnAction(e ->{
            if(dest != null && dest.exists()){
                destCh.setInitialDirectory(dest);
            }else{
                destCh.setInitialDirectory(new File(System.getProperty("user.home")));
            }
            File f = destCh.showDialog(window);
            dest = f == null ? dest : (f);
        });

        setNodeAlignment(close, 0, -1, -1, 0);
        setNodeAlignment(cancel, -1, 0, -1, 0);
        setNodeAlignment(repo, 0, -1, 0, -1);
        setNodeAlignment(name, 0, -1, 40, -1);
        setNodeAlignment(tag, 0, -1, 80, -1);
        setNodeAlignment(pushChanges, 0, -1, 120, -1);
        setNodeAlignment(setDest, 0, -1, 160, -1);

        window.setScene(scene);
    }

    /**
     * Displays this Manager's interface if it is not already being displayed.
     */
    public void display()
    {
        if(!window.isShowing()) {
            window.showAndWait();
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
     * Prepares the UI to edit a new rule.
     */
    public void showNewRuleUI()
    {
        editing = null;
        repo.getSelectionModel().select(0);
        tag.setText("");
        name.setText("");
        dest = new File(System.getProperty("user.home"));
        pushChanges.setSelected(true);
        display();
    }

    /**
     * Gets the currently processed rule. Clears the edit buffer when called.
     * @return the Rule object that has been processed by the editor
     */
    public Rule getCurrentRule()
    {
        Rule tmp = null;
        if(editing != null)
            tmp = new Rule(editing.name, editing.repo, editing.tag, editing.push, editing.dest);

        editing = null;
        return tmp;
    }

    /**
     * Sets the fields in the editor to be equal to an existing Rule object.
     * @param rule the Rule object to edit
     */
    public void showEditRuleUI(Rule rule)
    {
        if(rule != null){
            repo.getSelectionModel().select(rule.repo);
            tag.setText(rule.tag);
            name.setText(rule.name);
            pushChanges.setSelected(rule.push);
            dest = rule.dest;
            editing = rule;
            display();
        }
    }
}
