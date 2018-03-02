package X34.UI.JFX.Managers;

import X34.Core.IO.X34Config;
import X34.Core.IO.X34ConfigDelegator;
import X34.Core.X34Rule;
import X34.Processors.X34ProcessorRegistry;
import X34.Processors.X34RetrievalProcessor;
import X34.UI.JFX.Util.CheckBoxEditableListCell;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.AUNIL.LogEventLevel;
import core.CoreUtil.AUNIL.XLoggerInterpreter;
import core.CoreUtil.JFXUtil;
import core.UI.ARKInterfaceAlert;
import core.UI.ARKInterfaceDialog;
import core.UI.ARKInterfaceDialogYN;
import core.UI.ARKManagerBase;
import core.system.ARKAppCompat;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class X34UIRuleManager extends ARKManagerBase
{
    //
    // CONSTANTS
    //

    enum State{
        IDLE, INVALID, BUILDING, FINALIZED
    }

    public static final String TITLE = "Rule/Schema Management";
    public static final int DEFAULT_WIDTH = (int)(400 * JFXUtil.SCALE);
    public static final int DEFAULT_HEIGHT = (int)(400 * JFXUtil.SCALE);
    private static final String PROCESSOR_CONFIG_KEY = "external_processors";
    private static final File PROCESSOR_STORAGE_DIR = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\X34\\Processors");

    //
    // JFX NODES
    //

    private Button close;
    private Button discard;
    private Button addRule;
    private Button removeRule;
    private Button ruleUp;
    private Button ruleDown;
    private Button addProcessor;
    private Button removeProcessor;

    private ImageView arrow;
    private ImageView divider1;
    private ImageView divider2;
    private ImageView divider3;

    private Label warning;
    private Label info;

    private ListView<X34Rule> ruleList;
    private ListView<X34RetrievalProcessor> processors;

    //
    // INSTANCE VARIABLES
    //

    private ArrayList<X34Rule> fallback;
    private HashMap<X34RetrievalProcessor, SimpleBooleanProperty> selected;
    private HashMap<X34Rule, SimpleBooleanProperty> enabled;
    private HashMap<X34RetrievalProcessor[], File> externalProcessors;

    private XLoggerInterpreter log;
    private X34Config config;

    private boolean modified;
    private boolean errored;
    private State ruleState;
    private State enableState;
    private Timer UITimer;

    public X34UIRuleManager(double x, double y)
    {
        super(TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, x, y);

        //
        // BASE OBJECT INIT
        //

        ruleState = State.IDLE;
        enableState = State.IDLE;
        log = new XLoggerInterpreter();
        config = X34ConfigDelegator.getMainInstance();
        UITimer = new Timer();

        fallback = null;
        modified = false;
        errored = false;

        window.initModality(Modality.APPLICATION_MODAL);

        window.setMinWidth(DEFAULT_WIDTH);
        window.setMinHeight(DEFAULT_HEIGHT);

        externalProcessors = config.getSettingOrDefault(PROCESSOR_CONFIG_KEY, new HashMap<>());

        //
        // NODE INIT
        //

        close = new Button("Save & Close");
        discard = new Button("Undo Changes");
        addRule = new Button("Add");
        removeRule = new Button("Delete");
        ruleUp = new Button("Up");
        ruleDown = new Button("Down");
        addProcessor = new Button("Import Processor(s)...");
        removeProcessor = new Button("Remove Processor");
        arrow = JFXUtil.generateGraphicFromResource("X34/assets/GUI/icon/ic_arrow_right_256px.png", 25);
        divider1 = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_vert_256x8.png", 8, 128, true, true));
        divider2 = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_vert_256x8.png", 8, 128, true, true));
        divider3 = new ImageView(new Image("X34/assets/GUI/decorator/ic_line_rounded_horiz_256x8.png", 128, 8, true, true));

        warning = new Label("");
        info = new Label("");
        warning.setWrapText(true);
        info.setWrapText(true);

        warning.setGraphic(JFXUtil.generateGraphicFromResource("core/assets/warning.png", 15));
        info.setGraphic(JFXUtil.generateGraphicFromResource("core/assets/info.png", 15));

        processors = new ListView<>();
        ruleList = new ListView<>();

        processors.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        ruleList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        setElementTooltips();

        selected = new HashMap<>();
        enabled = new HashMap<>();

        // Load the proper string interpreter and cell type for the processor list
        final StringConverter<X34RetrievalProcessor> scp = new StringConverter<X34RetrievalProcessor>() {
            @Override
            public String toString(X34RetrievalProcessor object) {
                return object.getInformalName();
            }

            @Override
            public X34RetrievalProcessor fromString(String string) {
                return X34ProcessorRegistry.getProcessorForID(string);
            }
        };
        processors.setCellFactory((ListView<X34RetrievalProcessor> param) -> {
            CheckBoxListCell<X34RetrievalProcessor> cell = new CheckBoxListCell<>(param1 -> {
                // If the new cell is valid, create a new property with the correct settings and set it to the proper position in the list.
                if(processors.getItems().contains(param1)){
                    SimpleBooleanProperty bp = new SimpleBooleanProperty();
                    bp.addListener(e1 -> onIndexModified());

                    selected.put(param1, bp);
                    return bp;
                }else return null;
            });

            cell.setConverter(scp);

            return cell;
        });

        ruleList.setEditable(true);

        // Load the proper string interpreter and cell type for the rule list
        final StringConverter<X34Rule> scr = new StringConverter<X34Rule>() {
            @Override
            public String toString(X34Rule object) {
                return object.query;
            }

            @Override
            public X34Rule fromString(String string) {
                X34Rule foundObject = null;

                if(ruleList.getItems() != null)
                for(X34Rule r : ruleList.getItems())
                    if (r.query.equals(string)) foundObject = r;

                if(foundObject != null){
                    return new X34Rule(string, foundObject.getMetaData(), foundObject.getProcessorList());
                }else{
                    try {
                        return new X34Rule(string, null, X34ProcessorRegistry.getAvailableProcessorIDs()[0]);
                    } catch (ClassNotFoundException | IOException e) {
                        return null;
                    }
                }
            }
        };
        ruleList.setCellFactory((ListView<X34Rule> param) ->{
            CheckBoxEditableListCell<X34Rule> cell = new CheckBoxEditableListCell<>(param1 -> {
                // If the new cell is valid, create a new property with the correct settings and set it to the proper position in the list.
                if(ruleList.getItems().contains(param1)){
                    SimpleBooleanProperty bp = new SimpleBooleanProperty();

                    // Check to see what the initial state of the selection box should be
                    if(param1 != null) bp.setValue(param1.getMetaData() == null || !param1.getMetaData().containsKey("disabled"));
                    bp.addListener(e1 -> onRuleModified());

                    enabled.put(param1, bp);
                    return bp;
                }else return null;
            });

            cell.setConverter(scr);

            return cell;
        });

        // Add modification listener for the internal rule list.
        ruleList.getItems().addListener((ListChangeListener<X34Rule>) c ->
        {
            // Modification logic check for rule list: check if the fallback and live rule lists are the same size and null state.
            // If they are, check element properties to see if they match. If so, the list has not changed.
            if(fallback == null && ruleList.getItems() == null) modified = false;
            else if(fallback != null && ruleList.getItems() == null || fallback == null && ruleList.getItems() != null) modified = true;
            else if(fallback.size() != ruleList.getItems().size()) modified = true;
            else if(fallback.size() == 0 && ruleList.getItems().size() == 0) modified = false;
            else{
                for(int i = 0; i < fallback.size(); i++)
                {
                    if(fallback.get(i) != ruleList.getItems().get(i)){
                        modified = true;
                        break;
                    }else modified = false;
                }
            }
        });

        // Add selection update listener for the rule list, link it to the processor list
        ruleList.getSelectionModel().selectedIndexProperty().addListener(e -> onIndexChange());

        layout.getChildren().addAll(close, discard, ruleList, processors, addRule, removeRule, ruleUp, ruleDown, info, warning, addProcessor, removeProcessor);

        // Load processors from registry. If that fails, the dialog is essentially useless, so hide it.
        try {
            if(externalProcessors.keySet().size() > 0) loadExternalProcessors();
            processors.getItems().addAll(X34ProcessorRegistry.getAvailableProcessorObjects());
        } catch (ClassNotFoundException | IOException e) {
            log.logEvent(LogEventLevel.ERROR,"Unable to load processor list.");
            log.logEvent(e);
            new ARKInterfaceAlert("Error", "Unable to load processor list! Please restart the program and try again.").display();
            errored = true;
            hide();
        }

        //
        // NODE ACTIONS
        //

        close.setOnAction(e -> hide());

        discard.setOnAction(e -> {
            if(!modified){
                displayMessage("No changes to discard!", false, 2500);
            }else if(new ARKInterfaceDialogYN("Query", "Are you sure you want to discard any changes to the rule list?", "Yes", "No").display()) {
                ruleList.getItems().clear();
                // Shallow clone is OK here, since the setWorkingList method runs a deep clone anyway. We have to do a shallow
                // clone instead of a direct pass to avoid ConcurrentModificationExceptions during the deep clone.
                setWorkingList((ArrayList<X34Rule>)fallback.clone());
                displayMessage("Changes discarded.", false, 2500);
            }
        });

        addRule.setOnAction(e ->{
            String name = new ARKInterfaceDialog("Query", "Please enter the search string for the new rule:", "Confirm", "Cancel", "Tag...").display();
            if(name == null || name.isEmpty()) return;

            // Add new rule with first processor preselected
            ruleList.getItems().add(new X34Rule(name, null, processors.getItems().get(0).getID()));
            ruleList.getSelectionModel().select(ruleList.getItems().size() - 1);
            displayMessage("Rule added.", false, 1500);
        });

        removeRule.setOnAction(e ->{
            int index = ruleList.getSelectionModel().getSelectedIndex();
            if(index < 0 || index > ruleList.getItems().size() - 1) return;

            if(new ARKInterfaceDialogYN("Warning", "Deleting this rule (" + ruleList.getItems().get(index).query
                    + ") cannot be undone! Proceed?", "Proceed", "Cancel").display()){
                ruleList.getItems().remove(index);
                displayMessage("Rule removed!", false, 1500);
            }
        });

        ruleUp.setOnAction(e ->{
            int index = ruleList.getSelectionModel().getSelectedIndex();
            if(index == 0){
                displayMessage("Rule is already at the top of the list!", false, 2000);
                return;
            }else if(index < 0) return;

            X34Rule r = ruleList.getItems().remove(index);
            ruleList.getItems().add(index - 1, r);
            ruleList.getSelectionModel().select(index - 1);
        });

        ruleDown.setOnAction(e ->{
            int index = ruleList.getSelectionModel().getSelectedIndex();
            if(index < 0) return;
            else if(index >= ruleList.getItems().size() - 1){
                displayMessage("Rule is already at the bottom of the list!", false, 2000);
                return;
            }

            X34Rule r = ruleList.getItems().remove(index);
            ruleList.getItems().add(index + 1, r);
            ruleList.getSelectionModel().select(index + 1);
        });

        addProcessor.setOnAction(e ->
        {
            // Get the source file from the user.
            FileChooser fc = new FileChooser();
            fc.setTitle("Import Processor Plugins...");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java JAR File", "*.jar"));

            File f = fc.showOpenDialog(window);
            if(f == null || !f.exists()) return;

            displayMessage("Importing processor(s), please wait...", false, 5000);

            // Cache a snapshot of the current processor list for use in ID conflict checking later on.
            X34RetrievalProcessor[] avTemp;
            try {
                avTemp = X34ProcessorRegistry.getAvailableProcessorObjects();
            } catch (ClassNotFoundException | IOException e2) {
                avTemp = new X34RetrievalProcessor[0];
            }

            final X34RetrievalProcessor[] available = avTemp;

            // Delegate import to a multi-thread task instead of the main thread to avoid UI lockup
            Task<X34RetrievalProcessor[]> importProcessors = new Task<X34RetrievalProcessor[]>() {
                @Override
                protected X34RetrievalProcessor[] call() {
                    return X34ProcessorRegistry.getProcessorObjectsFromExternalJar(f);
                }
            };

            importProcessors.setOnSucceeded(e1 ->
            {
                X34RetrievalProcessor[] result = importProcessors.getValue();

                if(result == null || result.length < 1){
                    displayMessage("No valid processor plugin files were found at the specified location.", false, 4000L);
                    return;
                }

                displayMessage("Processor import successful. " + result.length + " processor(s) imported.", false, 4000L);

                // Check if the IDs of any processors in the new-processor list match any existing ones.
                int removed = 0;
                for(X34RetrievalProcessor xpa : available)
                {
                    int removedIndex = -1;
                    for(X34RetrievalProcessor xp : result)
                    {
                        // If the ID matches, it means that this processor cannot coexist with the previous one.
                        // Remove it from the new-processor list and the processor registry.
                        if(xpa.getID().equals(xp.getID())){
                            removedIndex = ARKArrayUtil.contains(result, xp);
                            // Use a manual index calculation instead of a class search. This is because a class search
                            // would end up removing the first instance of the target processor, not the duplicate instance.
                            X34ProcessorRegistry.removeProcessorFromList(available.length + removedIndex);
                            removed ++;
                            break;
                        }
                    }

                    // This must be done outside of the inner loop to avoid the possibility of a ConcurrentModificationException.
                    if(removedIndex > -1) result = ARKArrayUtil.remove(result, removedIndex);
                }

                // Let the user know if we got rid of any conflicting processors.
                if(removed > 0) {
                    // Use a Task instead of a Timer, since apparently JFX doesn't like Timers running inside of each other...
                    final int removedF = removed;
                    Task<Void> timer = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            Thread.sleep(2000);
                            return null;
                        }
                    };

                    timer.setOnSucceeded(e2 -> displayMessage(removedF + " processor import(s) cancelled due to ID conflicts with existing processor(s).", true, 2500));

                    Thread timerThread = new Thread(timer);
                    timerThread.setDaemon(true);
                    timerThread.start();
                }

                // If there are no more processors left (if they all conflicted), return without doing anything else.
                if(result.length == 0) return;

                // Cyclically rename the destination file until we get an available one.
                File dest = new File(PROCESSOR_STORAGE_DIR, f.getName());
                int itr = 0;
                while (dest.exists()){
                    dest = new File(PROCESSOR_STORAGE_DIR, f.getName().substring(0, f.getName().lastIndexOf('.') - 1)
                            + itr + f.getName().substring(f.getName().lastIndexOf('.'), f.getName().length()));
                }

                try {
                    if(!PROCESSOR_STORAGE_DIR.exists() && !PROCESSOR_STORAGE_DIR.mkdirs()) throw new IOException("Unable to create storage directory path");
                    Files.copy(Paths.get(f.toURI()), Paths.get(dest.toURI()), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e2) {
                    log.logEvent(e2);
                    displayMessage("Unable to cache imported processor(s)! You may have to re-import.", true, 4000);
                }

                // Store the results in the external processor registry.
                externalProcessors.put(result, f);
                config.storeSetting(PROCESSOR_CONFIG_KEY, externalProcessors);

                // Add the new processors to the display list
                processors.getItems().addAll(result);
            });

            importProcessors.setOnFailed(e1 -> displayMessage("Processor import failed. Please try again later.", true, 4000));

            Thread importThread = new Thread(importProcessors);
            importThread.setDaemon(true);
            importThread.start();
        });

        removeProcessor.setOnAction(e ->{
            int index = processors.getSelectionModel().getSelectedIndex();
            if(index < 0 || index > processors.getItems().size()) return;

            // Make sure that the external processor list contains the processor in question. If it doesn't, warn the user
            // and return, since you can't remove base internal processor listings.
            boolean found = false;
            X34RetrievalProcessor[] foundKey = null;
            for(X34RetrievalProcessor[] xps : externalProcessors.keySet()) {
                if(ARKArrayUtil.contains(xps, processors.getItems().get(index)) > -1){
                    found = true;
                    foundKey = xps;
                    break;
                }
            }

            if(!found){
                displayMessage("Processor is part of the default set, cannot be removed!", true, 4000);
                return;
            }

            // Request confirmation from the user
            if(!new ARKInterfaceDialogYN("Warning", "Removing an externally-linked processor cannot be undone! Are you sure you want to proceed?", "Yes", "No").display()) return;

            // Cache the linked value from the map before deletion, in case we need to put the value back in after removal is done
            File tempF = externalProcessors.get(foundKey);

            // Remove the specified processor from the array
            X34RetrievalProcessor[] result = ARKArrayUtil.remove(foundKey, ARKArrayUtil.contains(foundKey, processors.getItems().get(index)));
            externalProcessors.remove(foundKey);

            // If the array still has more processors in it, add it back to the map.
            if(result.length > 0) externalProcessors.put(result, tempF);
            // Otherwise, the file is no longer needed, since all of its processors have been removed. Delete it from the cache.
            // If the deletion fails, don't worry about it, it will be ignored anyway, since it's not in the list anymore.
            else tempF.delete();

            // Forcibly disable the removed processor on all rules in the list to avoid errors down the line.
            // This is done by cycling through the available indices in the list, and unchecking the correct processor on each
            // one - the automatic listener routines do the rest, acting as though the user unchecked them.
            int prevIndex = ruleList.getSelectionModel().getSelectedIndex();

            for(int i = 0; i < ruleList.getItems().size(); i++) {
                ruleList.getSelectionModel().select(i);
                selected.get(processors.getItems().get(index)).setValue(false);
            }

            // If the user had a rule selected before the force-change, select it again as if nothing happened.
            if(prevIndex >= 0 && prevIndex < ruleList.getItems().size()) ruleList.getSelectionModel().select(prevIndex);

            // Finally, remove the displayed value from the processor list and delete it from the availability index.
            X34RetrievalProcessor xp = processors.getItems().remove(index);
            X34ProcessorRegistry.removeProcessorFromList(xp.getClass());
        });
    }

    public void setWorkingList(ArrayList<X34Rule> rules)
    {
        enableState = State.INVALID;
        ruleList.getItems().clear();
        enabled.clear();
        if(rules != null){
            enableState = State.BUILDING;
            ruleList.getItems().addAll(rules);

            this.fallback = new ArrayList<>();

            // Deep-clone the array of rules to its fallback copy. This must be done to ensure that metadata changes do not
            // carry over to the fallback array if they occur.
            for (X34Rule x : rules) {
                X34Rule newRule = new X34Rule(x.query + "", new HashMap<>(), x.getProcessorList().clone());
                if(x.getMetaData() == null) newRule.setMetaData(null);
                else{
                    for(String s : x.getMetaData().keySet()){
                        newRule.getMetaData().put(s + "", x.getMetaData().get(s) + "");
                    }
                }
                fallback.add(newRule);
            }
        }else this.fallback = null;
        modified = false;
        enableState = State.FINALIZED;
    }

    public ArrayList<X34Rule> getCurrentRuleList() {
        return new ArrayList<>(ruleList.getItems());
    }

    private void onIndexChange()
    {
        int index = ruleList.getSelectionModel().getSelectedIndex();
        ruleState = State.INVALID;

        if(index < 0 || index > ruleList.getItems().size()){
            for(SimpleBooleanProperty o : selected.values()) {
                o.set(false);
            }
            processors.setDisable(true);
            return;
        }else{
            processors.setDisable(false);
        }

        ruleState = State.BUILDING;

        // Update display of the selected index
        X34Rule r = ruleList.getItems().get(index);

        // Clear the selection list
        for(SimpleBooleanProperty o : selected.values()) {
            o.set(false);
        }

        // Display the new rule's processor selection options
        if(r != null){
            for(String s : r.getProcessorList()){
                X34RetrievalProcessor xp = X34ProcessorRegistry.getProcessorForID(s);
                if(xp != null) selected.get(xp).set(true);
            }
        }

        ruleState = State.FINALIZED;
    }

    private void onIndexModified()
    {
        if(ruleState != State.FINALIZED) return;

        int ruleIndex = ruleList.getSelectionModel().getSelectedIndex();

        // Lock out further state changes to avoid cyclic calls during modification
        ruleState = State.INVALID;

        // Ensure that at least one processor is selected before processing changes
        boolean isSelected = false;
        for(SimpleBooleanProperty b : selected.values()) {
            if(b.getValue()){
                isSelected = true;
                break;
            }
        }

        if(!isSelected)
        {
            // If there is no processor selected, get the previously-selected processor for the rule.
            displayMessage("At least one processor must be selected!", true, 4000);

            X34Rule r = ruleList.getItems().get(ruleIndex);
            X34RetrievalProcessor rp = X34ProcessorRegistry.getProcessorForID(r.getProcessorList()[0]);
            if(processors.getItems().contains(rp)){
                // Revert the selection state of the previously-selected processor and return, since the rule will not have
                // changed state effectively, and force a refresh of the list, since it will not refresh automatically
                //fixme not updating checkbox when reset
                selected.get(rp).setValue(true);
                processors.refresh();
                ruleState = State.FINALIZED;
                return;
            }else{
                // Default to selecting the first index instead, and go ahead and update to make sure that the change propagates
                selected.get(processors.getItems().get(0)).setValue(true);
                processors.refresh();
            }
        }

        X34Rule r = ruleList.getItems().get(ruleIndex);
        if(r != null) {
            // Get the selection state of each of the processor IDs, store it, and use it to construct a new version
            // of the rule with the updated list.
            ArrayList<String> IDs = new ArrayList<>();
            for(X34RetrievalProcessor xp : processors.getItems()) {
                if(selected.get(xp).getValue()) IDs.add(xp.getID());
            }

            ruleList.getItems().set(ruleIndex, new X34Rule(r.query, r.getSchemas()[0].metadata, IDs.toArray(new String[IDs.size()])));
            ruleList.getSelectionModel().select(ruleIndex);
        }

        ruleState = State.FINALIZED;
    }

    private void onRuleModified()
    {
        if(enableState != State.FINALIZED) return;

        // Lock out any further listeners from firing, since that would result in an infinite loop.
        enableState = State.BUILDING;

        for(int i = 0; i < ruleList.getItems().size(); i++)
        {
            X34Rule r = ruleList.getItems().get(i);

            if (r != null) {
                if (r.getMetaData() == null) r.setMetaData(new HashMap<>());

                if (!enabled.get(r).getValue()) r.getMetaData().put("disabled", null);
                else if (r.getMetaData().containsKey("disabled")) r.getMetaData().remove("disabled");

                // Manually set the modification flag. Since there has been no change to the list of rules, only its contents,
                // the change listener will not fire by itself.
                modified = true;
            }
        }

        enableState = State.FINALIZED;
    }

    private void displayMessage(String text, boolean isWarning, long duration)
    {
        if(duration <= 0) return;

        warning.setVisible(false);
        info.setVisible(false);

        // Maintain a direct reference to the appropriate Label node
        Label target;
        if(isWarning) target = warning;
        else target = info;

        UITimer.cancel();
        UITimer = new Timer();

        target.setText(text);

        FadeTransition in = new FadeTransition();
        in.setFromValue(0.0);
        in.setToValue(1.0);
        in.setDuration(new Duration(250));
        in.setNode(target);
        in.play();
        target.setVisible(true);

        UITimer.schedule(new TimerTask() {
            @Override
            public void run() {
                FadeTransition out = new FadeTransition();
                out.setFromValue(1.0);
                out.setToValue(0.0);
                out.setDuration(new Duration(250));
                out.setNode(target);
                out.play();
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
                target.setVisible(false);
            }
        }, duration);
    }

    private void loadExternalProcessors() throws ClassNotFoundException, IOException
    {
        ArrayList<X34RetrievalProcessor[]> invalid = new ArrayList<>();

        // Check each entry in the external JAR registry for validity, including duplication, nonexistence, and user removal.
        for(X34RetrievalProcessor[] xps : externalProcessors.keySet())
        {
            File f = externalProcessors.get(xps);
            // Make sure the referenced file still exists
            if(!f.exists()){
                invalid.add(xps);
                continue;
            }

            // Cache a snapshot of the current processor list for use in ID conflict checking later on.
            X34RetrievalProcessor[] available = X34ProcessorRegistry.getAvailableProcessorObjects();
            X34RetrievalProcessor[] result = X34ProcessorRegistry.getProcessorObjectsFromExternalJar(f);

            if(result == null || result.length == 0) continue;

            // Check if the IDs of any processors in the new-processor list match any existing ones.
            for(X34RetrievalProcessor xpa : available)
            {
                int removedIndex = -1;
                for(X34RetrievalProcessor xp : result)
                {
                    // If the ID matches, it means that this processor cannot coexist with the previous one.
                    // Remove it from the new-processor list and the processor registry.
                    if(xpa.getID().equals(xp.getID())){
                        removedIndex = ARKArrayUtil.contains(result, xp);
                        // Use a manual index calculation instead of a class search. This is because a class search
                        // would end up removing the first instance of the target processor, not the duplicate instance.
                        X34ProcessorRegistry.removeProcessorFromList(available.length + removedIndex);
                        break;
                    }
                }

                // This must be done outside of the inner loop to avoid the possibility of a ConcurrentModificationException.
                if(removedIndex > -1) result = ARKArrayUtil.remove(result, removedIndex);
            }

            if(result.length == 0) continue;

            // Remove any processors that were added that are not in the external processor registry. Such processors have
            // probably been removed by the user, and as such, should not be loaded.
            int removed = 0;
            for(X34RetrievalProcessor xp : result) {
                if(ARKArrayUtil.contains(xps, xp) == -1){
                    X34ProcessorRegistry.removeProcessorFromList(xp.getClass());
                    removed ++;
                }
            }

            // If we removed all of the remaining entries, mark this external JAR entry as invalid
            if(removed == result.length) invalid.add(xps);
        }

        // Remove any invalid processor listings from the registry.
        if(invalid.size() > 0)
            for(X34RetrievalProcessor[] xps : invalid)
                externalProcessors.remove(xps);
    }

    private void repositionElements()
    {
        processors.setDisable(true);
        info.setVisible(false);
        warning.setVisible(false);

        // Link list widths and heights to the window size
        layout.widthProperty().addListener(e ->{
            JFXUtil.setElementPositionCentered(layout, close, true, false);
            JFXUtil.setElementPositionCentered(layout, discard, true, false);
            warning.setMaxWidth(layout.getWidth() - layout.getPadding().getLeft());
            info.setMaxWidth(layout.getWidth() - layout.getPadding().getLeft());

            repositionOnResize();
        });

        layout.heightProperty().addListener(e -> repositionOnResize());

        JFXUtil.setElementPositionInGrid(layout, close, -1, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, discard, -1, -1, -1, 1);

        JFXUtil.setElementPositionInGrid(layout, ruleList, 0, -1, 1, -1);
        JFXUtil.setElementPositionInGrid(layout, processors, -1, 0, 1, -1);

        JFXUtil.setElementPositionInGrid(layout, warning, 0, -1, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, info, 0, -1, 0, -1);

        ruleList.setPrefWidth(layout.getWidth() / 3);
        ruleList.setPrefHeight(layout.getHeight() - (JFXUtil.DEFAULT_SPACING * 4));
        processors.setPrefWidth(layout.getWidth() / 3);
        processors.setPrefHeight(layout.getHeight() - (JFXUtil.DEFAULT_SPACING * 4));

        ruleUp.prefWidthProperty().bind(ruleDown.widthProperty());
        addRule.prefWidthProperty().bind(removeRule.widthProperty());
    }

    private void repositionOnResize()
    {
        ruleList.setPrefWidth(layout.getWidth() / 3);
        processors.setPrefWidth(layout.getWidth() / 3);
        ruleList.setPrefHeight(layout.getHeight() - (JFXUtil.DEFAULT_SPACING * 4));
        processors.setPrefHeight(layout.getHeight() - (JFXUtil.DEFAULT_SPACING * 4));

        Platform.runLater(() ->{
            JFXUtil.alignToNode(layout, ruleList, ruleUp, 10, Orientation.VERTICAL, JFXUtil.Alignment.NEGATIVE);
            JFXUtil.alignToNode(layout, ruleList, ruleDown, JFXUtil.DEFAULT_SPACING + 10, Orientation.VERTICAL, JFXUtil.Alignment.NEGATIVE);

            JFXUtil.alignToNode(layout, ruleList, removeRule, JFXUtil.DEFAULT_SPACING + 10, Orientation.VERTICAL, JFXUtil.Alignment.POSITIVE);
            JFXUtil.alignToNode(layout, ruleList, addRule, 10, Orientation.VERTICAL, JFXUtil.Alignment.POSITIVE);

            JFXUtil.alignToNode(layout, processors, addProcessor, 10, Orientation.VERTICAL, JFXUtil.Alignment.CENTERED);
            JFXUtil.alignToNode(layout, processors, removeProcessor, JFXUtil.DEFAULT_SPACING + 10, Orientation.VERTICAL, JFXUtil.Alignment.CENTERED);

            JFXUtil.centerToNode(layout, ruleList, getBoundsFromRegion(ruleList), arrow, getBoundsFromImageView(arrow), Orientation.HORIZONTAL);

            JFXUtil.setElementPositionCentered(layout, arrow, getBoundsFromImageView(arrow), true, false);

            layout.layout();

            JFXUtil.setElementPositionCentered(layout, divider1, getBoundsFromImageView(divider1), true, false);
            AnchorPane.setTopAnchor(divider1, ruleList.getLayoutY() - layout.getPadding().getTop());
            divider1.setFitHeight(arrow.getLayoutY() - (5 * JFXUtil.SCALE) - ruleList.getLayoutY());

            JFXUtil.alignToNode(layout, arrow, getBoundsFromImageView(arrow), divider2, getBoundsFromImageView(divider2), 10, Orientation.VERTICAL, JFXUtil.Alignment.CENTERED);
            divider2.setFitHeight(discard.getLayoutY() - (5 * JFXUtil.SCALE) - arrow.getLayoutY() - getBoundsFromImageView(arrow).getHeight() - layout.getPadding().getTop());

            JFXUtil.alignToNode(layout, ruleList, getBoundsFromRegion(ruleList), divider3, getBoundsFromImageView(divider3), 5, Orientation.HORIZONTAL, JFXUtil.Alignment.NEGATIVE);
            divider3.setFitWidth(processors.getLayoutX() - divider3.getLayoutX() - (5 * JFXUtil.SCALE));

            warning.setPrefWidth(layout.getWidth() - layout.getPadding().getRight() - layout.getPadding().getLeft());
            info.setPrefWidth(layout.getWidth() - layout.getPadding().getRight() - layout.getPadding().getRight());
        });
    }

    private void setElementTooltips()
    {
        close.setTooltip(new Tooltip("Save any changes and return to the main UI."));
        discard.setTooltip(new Tooltip("Revert any unsaved changes back to their previous state."));
        addRule.setTooltip(new Tooltip("Add a new rule to the list."));
        removeRule.setTooltip(new Tooltip("Remove the currently selected rule from the list."));
        ruleUp.setTooltip(new Tooltip("Move the selected rule up in the list by one."));
        ruleDown.setTooltip(new Tooltip("Move the selected rule down in the list by one."));
        addProcessor.setTooltip(new Tooltip("Load a processor plugin JAR file from an external source."));
        removeProcessor.setTooltip(new Tooltip("Permanently remove an externally loaded processor from the registry."));
        ruleList.setTooltip(new Tooltip("The list of retrieval rules. The ordering in this list\ndictates the order in which rules will be run.\nSee the Help menu for more information."));
        processors.setTooltip(new Tooltip("The list of processor types.\nEach processor can retrieve from a specific site.\nSee the Help menu for more information"));
    }

    private Rectangle2D getBoundsFromRegion(Region r)
    {
        return new Rectangle2D(r.getLayoutX(), r.getLayoutY(), JFXUtil.getNormalizedWidth(r),
                JFXUtil.getNormalizedHeight(r));
    }

    private Rectangle2D getBoundsFromImageView(ImageView v)
    {
        return new Rectangle2D(v.getX(), v.getY(), v.boundsInParentProperty().getValue().getWidth(),
                v.boundsInParentProperty().getValue().getHeight());
    }

    @Override
    public void display()
    {
        if(!window.isShowing()){
            if(errored) return;
            window.show();
            repositionElements();
            window.hide();
            if(ruleList.getItems() == null || ruleList.getItems().size() == 0) displayMessage("The rule list is currently empty! Add some using the controls below.", false, 5000);
            else displayMessage("This is the notification area. System messages and other notifications will appear here.", false, 5000);
            window.showAndWait();
        }
    }

    @Override
    public void hide()
    {
        if(getVisibilityState()){
            modified = false;
            // Force-trip the selection listener to make sure that all rule entries are up-to-date.
            ruleList.getSelectionModel().select(ruleList.getSelectionModel().getSelectedIndex() == 0 ? 1 : 0);
            window.hide();
        }
    }
}