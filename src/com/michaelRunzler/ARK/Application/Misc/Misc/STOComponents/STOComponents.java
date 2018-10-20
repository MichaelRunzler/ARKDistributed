package Misc.STOComponents;

import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.ARKJsonParser.ARKJsonObject;
import core.CoreUtil.ARKJsonParser.ARKJsonParser;
import core.CoreUtil.JFXUtil;
import core.UI.ARKManagerBase;
import core.UI.InterfaceDialogs.ARKInterfaceAlert;
import core.UI.InterfaceDialogs.ARKInterfaceDialog;
import core.UI.InterfaceDialogs.ARKInterfaceDialogYN;
import core.UI.ModeLocal.ModeLocal;
import core.UI.ModeLocal.ModeSwitchController;
import core.system.ARKAppCompat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class STOComponents extends Application
{
    @Override
    public void start(Stage primaryStage) {
        STOComponentUI main = new STOComponentUI("ARK Industries R&D Profit Calculator", (int)(450 * JFXUtil.SCALE), (int)(500 * JFXUtil.SCALE), -1, -1);
        main.display();
    }
}

@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
class STOComponentUI extends ARKManagerBase
{
    private static final int MODE_CALCULATION_VIEW = 0;
    private static final int MODE_COMPONENT_VIEW = 1;
    private static final int MODE_MATERIAL_VIEW = 2;
    private static final int MODE_OPTIONS_VIEW = 3;
    private static final File cache = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\STOComponents", "items.stx");
    private static final File schemaCache = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\STOComponents", "schemas.stc");

    private static final String MATERIAL_COST_BASE = "Raw materials: ";
    private static final String COMPONENT_COST_BASE = "Components: ";
    private static final String PROFIT_MARGIN_BASE = "Maximum Profit: ";

    private TabPane selector;

    @ModeLocal(MODE_MATERIAL_VIEW)
    private ListView<ItemEntry> materialIndex;

    @ModeLocal(MODE_COMPONENT_VIEW)
    private ListView<ItemEntry> componentIndex;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private HBox activeContainer;
    private ListView<ItemEntry> activeComponents;
    private ListView<ItemEntry> activeMaterials;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private HBox componentContainer;
    private ChoiceBox<Item> components;
    private TextField quantity;
    private Button add;
    private Label componentLabel;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private HBox filterContainer;
    private ChoiceBox<Item.Specialization> specFilter;
    private ChoiceBox<Item.Rarity> rarityFilter;
    private Button remove;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private HBox actionBarContainer;
    private Button calculate;
    private Button reset;
    private Button save;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private TextField resultExchangePrice;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private HBox resultContainer;
    private Label resultRawCost;
    private Label resultComponentCost;

    @ModeLocal(MODE_CALCULATION_VIEW)
    private HBox resultContainer2;
    private Label maximumProfitMargin;
    private Label totalDilQuantity;
    private ImageView separator1;
    private ImageView separator2;

    @ModeLocal(MODE_OPTIONS_VIEW)
    private ListView<ItemSchema> schemaList;

    @ModeLocal(MODE_OPTIONS_VIEW)
    private HBox schemaControlContainer;
    private Label schemaLabel;
    private Button loadSchema;
    private Button saveSchema;
    private Button deleteSchema;

    @ModeLocal(MODE_OPTIONS_VIEW)
    private VBox registryControlContainer;
    private Button exportRegistry;
    private Button importRegistry;
    private Button resetRegistry;
    private Button openCache;

    private Map<Item, ObservableValue<String>> itemValues;

    private ModeSwitchController modeController;
    private ItemRegistry registry;

    STOComponentUI(String title, int width, int height, double x, double y)
    {
        super(title, width, height, x, y);

        super.setIcon(new Image("Misc/STOComponents/assets/gui/icon/energy_credit.png"));

        window.setMinHeight(height);
        window.setMinWidth(width);

        if(x < 0) window.setX((Screen.getPrimary().getBounds().getWidth() / 2) - (width / 2));
        if(y < 0) window.setY((Screen.getPrimary().getBounds().getHeight() / 2) - (height / 2));

        itemValues = new HashMap<>();

        registry = new ItemRegistry();
        if(!cache.exists() || !registry.loadRegistryFromFile(cache))
            registry.loadDefaultItemSet(ItemRegistry.Category.ALL);

        window.setOnCloseRequest(e -> onExit());

        Tab calculation = new Tab("Calculate");
        Tab componentView = new Tab("Components");
        Tab materialView = new Tab("Materials");
        Tab optionsView = new Tab("Settings");

        calculation.setGraphic(JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/icon/energy_credit.png", (int)(15 * JFXUtil.SCALE)));
        componentView.setGraphic(JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + "components/very_rare/isolinear_circuitry.png", (int)(15 * JFXUtil.SCALE)));
        materialView.setGraphic(JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + "materials/rare/tetrazine.png", (int)(15 * JFXUtil.SCALE)));
        optionsView.setGraphic(JFXUtil.generateGraphicFromResource("core/assets/options.png", (int)(15 * JFXUtil.SCALE)));

        selector = new TabPane(calculation, componentView, materialView, optionsView);

        activeContainer = new HBox();
        componentContainer = new HBox();
        actionBarContainer = new HBox();
        filterContainer = new HBox();
        resultContainer = new HBox();
        resultContainer2 = new HBox();
        schemaControlContainer = new HBox();
        registryControlContainer = new VBox();

        materialIndex = new ListView<>();
        componentIndex = new ListView<>();
        activeComponents = new ListView<>();
        activeMaterials = new ListView<>();
        schemaList = new ListView<>();
        components = new ChoiceBox<>();
        rarityFilter = new ChoiceBox<>();
        specFilter = new ChoiceBox<>();

        calculate = new Button("Calculate...");
        reset = new Button("Clear Components");
        save = new Button("Save Values");
        add = new Button("Add");
        remove = new Button("Remove Selected Component");
        resetRegistry = new Button("Reset Registries");
        importRegistry = new Button("Import Registry");
        exportRegistry = new Button("Export Registry");
        openCache = new Button("Open Storage Folder");
        loadSchema = new Button("Load");
        saveSchema = new Button("Save");
        deleteSchema = new Button("Delete");

        quantity = new TextField("1");
        resultRawCost = new Label(MATERIAL_COST_BASE + "N/A");
        resultComponentCost = new Label(COMPONENT_COST_BASE + "N/A");
        maximumProfitMargin = new Label(PROFIT_MARGIN_BASE + "N/A");
        totalDilQuantity = new Label(": 0");
        schemaLabel = new Label("Schema Controls:");
        resultExchangePrice = new TextField("0");

        resultRawCost.setGraphic(JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/icon/energy_credit.png", (int)(15 * JFXUtil.SCALE)));
        resultComponentCost.setGraphic(JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/icon/energy_credit.png", (int)(15 * JFXUtil.SCALE)));
        maximumProfitMargin.setGraphic(JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/icon/energy_credit.png", (int)(15 * JFXUtil.SCALE)));
        totalDilQuantity.setGraphic(JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + registry.getByName(ItemRegistry.Category.ALL, "Refined Dilithium").iconRelPath, (int)(15 * JFXUtil.SCALE)));

        separator1 = JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/decorator/ic_line_taper_horiz_256x4.png", 4, JFXUtil.DEFAULT_SPACING * 4);
        separator2 = JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/decorator/ic_line_taper_horiz_256x4.png", 4, JFXUtil.DEFAULT_SPACING);

        resultExchangePrice.setPromptText("Result exchange price...");
        JFXUtil.limitTextFieldToNumerical(resultExchangePrice, 12);

        componentLabel = new Label("Available: ");

        JFXUtil.limitTextFieldToNumerical(quantity, 3);

        try {
            modeController = new ModeSwitchController(this);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(4041);
        }

        selector.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(modeController.getCurrentMode() != newValue.intValue()) modeController.switchMode(newValue.intValue());
        });

        selector.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        selector.setSide(Side.TOP);

        final StringConverter<Item> scI = new StringConverter<Item>() {
            @Override
            public String toString(Item object) {
                return object.name;
            }

            @Override
            public Item fromString(String string) {
                return registry.getByName(ItemRegistry.Category.ALL, string);
            }
        };
        final StringConverter<Item.Rarity> scR = new StringConverter<Item.Rarity>() {
            @Override
            public String toString(Item.Rarity object) {
                String s = object.name();

                return (s.charAt(0) + s.substring(1, s.length()).toLowerCase()).replace('_', ' ');
            }

            @Override
            public Item.Rarity fromString(String string) {
                return Item.Rarity.valueOf(string.replace(' ', '_').toUpperCase());
            }
        };
        final StringConverter<Item.Specialization> scS = new StringConverter<Item.Specialization>() {
            @Override
            public String toString(Item.Specialization object) {
                String s = object.name();

                return (s.charAt(0) + s.substring(1, s.length()).toLowerCase()).replace('_', ' ');
            }

            @Override
            public Item.Specialization fromString(String string) {
                return Item.Specialization.valueOf(string.replace(' ', '_').toUpperCase());
            }
        };

        components.setConverter(scI);
        rarityFilter.setConverter(scR);
        specFilter.setConverter(scS);

        final Item defaultItem = new Item("No items available", Item.ICON_BASE_PATH + "shaders/invalid.png", Item.Rarity.COMMON, -1).setSchools(Item.Specialization.ALL);
        specFilter.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            components.getItems().clear();
            components.getItems().addAll(registry.filterByRarity(registry.getBySpecialization(specFilter.getItems().get(newValue.intValue())), rarityFilter.getValue() == null ? Item.Rarity.ALL : rarityFilter.getValue()));
            if(components.getItems().size() == 0){
                components.getItems().add(defaultItem);
            }
            components.getSelectionModel().select(0);
        });
        rarityFilter.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            components.getItems().clear();
            components.getItems().addAll(registry.filterByRarity(registry.getBySpecialization(specFilter.getValue()), rarityFilter.getItems().get(newValue.intValue())));
            if(components.getItems().size() == 0){
                components.getItems().add(defaultItem);
            }
            components.getSelectionModel().select(0);
        });

        materialIndex.setEditable(true);
        componentIndex.setEditable(true);

        Callback<ListView<ItemEntry>, ListCell<ItemEntry>> factoryI = param -> {
            IconifiedEditableListCell<ItemEntry> cell = new IconifiedEditableListCell<>(param1 -> {
                if(param1 == null) return null;
                try {
                    return JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + param1.item.iconRelPath, (int)(20 * JFXUtil.SCALE));
                } catch (IllegalArgumentException e) {
                    return JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + "shaders/invalid.png", (int)(20 * JFXUtil.SCALE));
                }
            }, param2 -> {
                if(itemValues.containsKey(param2.item)) return itemValues.get(param2.item);
                else{
                    ObservableValue<String> value = new SimpleStringProperty("" + param2.item.value);
                    itemValues.put(param2.item, value);
                    value.addListener((observable, oldValue, newValue) -> {
                        int newItemValue = param2.quantity;
                        try{
                            newItemValue = Integer.parseInt(newValue);
                        }catch (NumberFormatException e){
                            try{
                                newItemValue = (int)Double.parseDouble(newValue);
                            }catch (NumberFormatException ignored){}
                        }

                        param2.item.value = newItemValue;
                        param2.quantity = newItemValue;
                        registry.updateItemValue(param2.item, ItemRegistry.Category.ALL);
                    });
                    return value;
                }
            });
            cell.setEditFieldNumeric(true, 12);

            return cell;
        };

        materialIndex.setCellFactory(factoryI);
        componentIndex.setCellFactory(factoryI);

        Callback<ListView<ItemEntry>, ListCell<ItemEntry>> factoryA = param -> new IconifiedListCell<>(param1 -> {
            if(param1 == null) return null;
            try{
                return JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + param1.item.iconRelPath, (int)(20 * JFXUtil.SCALE));
            } catch (IllegalArgumentException e) {
                return JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + "shaders/invalid.png", (int)(20 * JFXUtil.SCALE));
            }
        });
        activeMaterials.setCellFactory(factoryA);
        activeComponents.setCellFactory(factoryA);

        activeMaterials.setPlaceholder(new Label("No materials!"));
        activeComponents.setPlaceholder(new Label("No components selected!"));
        schemaList.setPlaceholder(new Label("No saved schemas!"));

        for(Item i : registry.getByCategory(ItemRegistry.Category.MATERIAL))
            materialIndex.getItems().add(new ItemEntry(i, i.value));

        for(Item i : registry.getByCategory(ItemRegistry.Category.COMPONENT))
            componentIndex.getItems().add(new ItemEntry(i, i.value));

        specFilter.getItems().addAll(Item.Specialization.values());
        rarityFilter.getItems().addAll(Item.Rarity.values());

        specFilter.getSelectionModel().select(Item.Specialization.BEAMS);
        rarityFilter.getSelectionModel().select(Item.Rarity.ALL);

        ItemSchema[] schemas = ItemSchema.loadSchemaList(schemaCache, registry);
        schemaList.getItems().addAll(schemas == null ? new ItemSchema[0] : schemas);

        add.setOnAction(e ->{
            if(components.getValue() == defaultItem || quantity.getText().contains("-") || quantity.getText().equals("0")) return;

            int count = 1;
            try{
                count = (int)(Double.parseDouble(quantity.getText()));
            }catch (NumberFormatException ignored){}

            Item current = components.getValue();

            this.addComponent(current, count);
        });

        remove.setOnAction(e ->{
            int index = activeComponents.getSelectionModel().getSelectedIndex();
            if(index < 0 | index >= activeComponents.getItems().size()) return;

            ItemEntry target = activeComponents.getItems().get(index);
            this.removeComponent(target);
        });

        save.setOnAction(e -> {
            try {
                cache.getParentFile().mkdirs();
                registry.saveRegistry(cache);
                new ARKInterfaceAlert("Notice", "Registry saved successfully!").display();
            } catch (IOException e1) {
                e1.printStackTrace();
                new ARKInterfaceAlert("Warning", "Unable to save registry entries to disk!").display();
            }
        });

        reset.setOnAction(e -> resetFields());

        calculate.setOnAction(e ->{
            if(activeMaterials.getItems().size() == 0 || activeComponents.getItems().size() == 0) return;

            int totalMaterialValue = 0;
            for(ItemEntry e1 : activeMaterials.getItems()){
                if(e1.item.name.equals("Refined Dilithium")){
                    totalDilQuantity.setText(": " + e1.quantity);
                    continue;
                }

                totalMaterialValue += (e1.item.value * e1.quantity);
            }

            int totalComponentValue = 0;
            for(ItemEntry e1 : activeComponents.getItems()){
                totalComponentValue += (e1.item.value * e1.quantity);
            }

            resultRawCost.setText(MATERIAL_COST_BASE + totalMaterialValue + " EC");
            resultComponentCost.setText(COMPONENT_COST_BASE + totalComponentValue + " EC");

            int sellPrice;
            try {
                sellPrice = Integer.parseInt(resultExchangePrice.getText());
            } catch (NumberFormatException e1) {
                maximumProfitMargin.setText(PROFIT_MARGIN_BASE + "N/A");
                return;
            }

            maximumProfitMargin.setText(PROFIT_MARGIN_BASE + (totalMaterialValue > totalComponentValue ? sellPrice - totalComponentValue : sellPrice - totalMaterialValue)
                    + " from " + (totalMaterialValue > totalComponentValue ? "components" : "raw materials"));
        });

        saveSchema.setOnAction(e -> {
            if(activeComponents.getItems().size() == 0 || activeMaterials.getItems().size() == 0){
                new ARKInterfaceAlert("Notice", "No selected components! Select some and try to save again.").display();
                return;
            }

            String name;
            int index = schemaList.getSelectionModel().getSelectedIndex();
            if((index >= 0 && index < schemaList.getItems().size()) && new ARKInterfaceDialogYN("Warning", "Overwrite the schema in the current slot?", "Yes", "No").display())
                    name = schemaList.getItems().get(index).name;
            else{
                name = new ARKInterfaceDialog("Query", "Enter a name for the new schema:", "Confirm", "Cancel", "Name...").display();
                if(name == null || name.length() == 0) return;
                index = -1;
            }

            int value;
            try {
                value = Integer.parseInt(resultExchangePrice.getText());
            }catch (NumberFormatException e1){
                value = 0;
            }

            ItemSchema schema = new ItemSchema(name, value, activeComponents.getItems().toArray(new ItemEntry[0]));
            if(index >= 0 && index < schemaList.getItems().size())
                schemaList.getItems().set(index, schema);
            else
                schemaList.getItems().add(schema);
        });

        loadSchema.setOnAction(e ->{
            int index = schemaList.getSelectionModel().getSelectedIndex();
            if(index < 0 || index >= schemaList.getItems().size()) return;

            if(!new ARKInterfaceDialogYN("Warning", "Loading this schema will overwrite the current component choice list. Continue?", "Yes", "No").display())
                return;

            ItemSchema schema = schemaList.getItems().get(index);

            resetFields();
            for(ItemEntry i : schema.componentList){
                addComponent(i.item, i.quantity);
            }

            resultExchangePrice.setText("" + schema.value);

            new ARKInterfaceAlert("Notice", "Schema loaded.").display();
        });

        deleteSchema.setOnAction(e ->{
            int index = schemaList.getSelectionModel().getSelectedIndex();
            if(index < 0 || index >= schemaList.getItems().size()) return;

            if(new ARKInterfaceDialogYN("Query", "Really delete this schema?", "Yes", "No").display())
                schemaList.getItems().remove(index);
        });

        exportRegistry.setOnAction(e ->{
            FileChooser fch = new FileChooser();
            fch.setInitialDirectory(cache.getParentFile());
            fch.getExtensionFilters().add(new FileChooser.ExtensionFilter("ARK Item Registry", "*.stx"));
            fch.setTitle("Export Registry");

            File f = fch.showSaveDialog(window);
            if(f == null) return;

            if(f.exists() && !f.delete()){
                new ARKInterfaceAlert("Error", "Unable to delete existing file. Please try again.").display();
                return;
            }

            try {
                registry.saveRegistry(f);
                new ARKInterfaceAlert("Notice", "Successfully exported registry data!").display();
            } catch (IOException e1) {
                e1.printStackTrace();
                new ARKInterfaceAlert("Error", "Could not save registry. Try another destination directory.").display();
            }
        });

        importRegistry.setOnAction(e ->{
            FileChooser fch = new FileChooser();
            fch.setInitialDirectory(cache.getParentFile());
            fch.getExtensionFilters().add(new FileChooser.ExtensionFilter("ARK Item Registry", "*.stx"));
            fch.setTitle("Import Registry");

            File f = fch.showOpenDialog(window);
            if(f == null || !f.exists()) return;

            if(!new ARKInterfaceDialogYN("Warning", "Loading this registry will overwrite all existing registry entries! Continue?", "Continue", "Cancel").display())
                return;

            if(!registry.loadRegistryFromFile(f)){
                new ARKInterfaceAlert("Error", "Unable to load registry from file.").display();
            }else{
                new ARKInterfaceAlert("Notice", "Registry successfully loaded. Program will now close. Restart to apply changes.").display();
                onExit();
            }

        });

        resetRegistry.setOnAction(e ->{
            if(new ARKInterfaceDialogYN("WARNING", "   ! REGISTRY RESET !   \n\nWarning! This will reset all stored values and edits to the config file! Proceed?", "Reset", "Cancel").display()){
                registry.loadDefaultItemSet(ItemRegistry.Category.ALL);
                new ARKInterfaceAlert("Notice", "Item registry has been reset to defaults. Program will now close. Restart to apply changes.").display();
                onExit();
            }
        });

        openCache.setOnAction(e ->{
            try {
                Desktop.getDesktop().browse(cache.getParentFile().toURI());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        setupHBox(activeContainer, 5, true, activeMaterials, activeComponents);
        HBox.setHgrow(activeMaterials, Priority.ALWAYS);
        HBox.setHgrow(activeComponents, Priority.ALWAYS);

        setupHBox(componentContainer, 5, false, componentLabel, components, quantity, add);
        HBox.setHgrow(componentLabel, Priority.NEVER);
        HBox.setHgrow(components, Priority.ALWAYS);
        HBox.setHgrow(quantity, Priority.NEVER);
        HBox.setHgrow(add, Priority.SOMETIMES);

        setupHBox(actionBarContainer, 10, false, reset, calculate, save);

        setupHBox(filterContainer, 10, false, specFilter, rarityFilter, remove);
        HBox.setHgrow(specFilter, Priority.SOMETIMES);
        HBox.setHgrow(rarityFilter, Priority.SOMETIMES);
        HBox.setHgrow(remove, Priority.NEVER);

        setupHBox(resultContainer, 5, false, resultComponentCost, separator1, resultRawCost);
        setupHBox(resultContainer2, 5, false, maximumProfitMargin, separator2, totalDilQuantity);

        setupHBox(schemaControlContainer, 5, false, schemaLabel, saveSchema, loadSchema, deleteSchema);

        registryControlContainer.getChildren().addAll(openCache, importRegistry, exportRegistry, resetRegistry);
        registryControlContainer.setSpacing(10 * JFXUtil.SCALE);
        registryControlContainer.setFillWidth(true);
        registryControlContainer.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(selector, materialIndex, componentIndex, activeContainer, componentContainer, actionBarContainer,
                filterContainer, resultContainer, resultContainer2, resultExchangePrice, schemaControlContainer, schemaList, registryControlContainer);

        setTooltips();

        positionElements();

        modeController.switchMode(MODE_CALCULATION_VIEW);
    }

    private void setupHBox(HBox node, int spacing, boolean fillHeight, Node... children)
    {
        node.getChildren().addAll(children);
        node.setSpacing(spacing * JFXUtil.SCALE);
        node.setFillHeight(fillHeight);
        node.setAlignment(Pos.CENTER);
    }

    private void addComponent(Item current, int count)
    {
        ItemEntry target = null;
        for(ItemEntry e1 : activeComponents.getItems()){
            if (e1.item == current) {
                target = e1;
                break;
            }
        }

        if(target == null) activeComponents.getItems().add(new ItemEntry(current, count));
        else activeComponents.getItems().set(activeComponents.getItems().indexOf(target), new ItemEntry(current, count + target.quantity));

        Map<String, Integer> elements = current.getComponentList();

        for(String s : elements.keySet())
        {
            int q = elements.get(s);
            Item i = registry.getByName(ItemRegistry.Category.MATERIAL, s);
            if(i == null || q < 1) continue;

            ItemEntry found = null;
            for(ItemEntry e1 : activeMaterials.getItems())
                if (e1.item == i) {
                    found = e1;
                    break;
                }

            if(found == null) activeMaterials.getItems().add(new ItemEntry(i, q * count));
            else activeMaterials.getItems().set(activeMaterials.getItems().indexOf(found), new ItemEntry(i, found.quantity + q * count));
        }
    }

    private void removeComponent(ItemEntry target)
    {
        Map<String, Integer> elements = target.item.getComponentList();

        for(String s : elements.keySet())
        {
            int q = elements.get(s);
            Item i = registry.getByName(ItemRegistry.Category.MATERIAL, s);
            if(i == null || q < 1) continue;

            ItemEntry found = null;
            for(ItemEntry e1 : activeMaterials.getItems())
                if (e1.item == i) {
                    found = e1;
                    break;
                }

            if(found == null) return;
            else if((q * target.quantity) >= found.quantity) activeMaterials.getItems().remove(found);
            else activeMaterials.getItems().set(activeMaterials.getItems().indexOf(found), new ItemEntry(i, found.quantity - (q * target.quantity)));
        }

        activeComponents.getItems().remove(target);
    }

    private void resetFields()
    {
        activeComponents.getItems().clear();
        activeMaterials.getItems().clear();
        rarityFilter.getSelectionModel().select(Item.Rarity.ALL);
        specFilter.getSelectionModel().select(Item.Specialization.BEAMS);
        quantity.setText("1");
        resultExchangePrice.setText("0");
    }

    private void positionElements()
    {
        layout.widthProperty().addListener(e -> positionOnWResize());

        Platform.runLater(() ->{
            // Bind the menu bar's height to the layout padding, to ensure that no layout elements clash with the menu bar.
            layout.setPadding(new Insets(selector.getHeight() + layout.getPadding().getTop(),
                    layout.getPadding().getRight(), layout.getPadding().getBottom(), layout.getPadding().getLeft()));
            AnchorPane.setTopAnchor(selector, -1 * layout.getPadding().getTop());
        });

        JFXUtil.setElementPositionInGrid(layout, materialIndex, 0, 0, 0, 0);
        JFXUtil.setElementPositionInGrid(layout, componentIndex, 0, 0, 0, 0);

        JFXUtil.setElementPositionInGrid(layout, filterContainer, 0, 0, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, componentContainer, 0, -1, 1, -1);

        JFXUtil.setElementPositionInGrid(layout, activeContainer, 0, 0, 2, 3);
        JFXUtil.setElementPositionInGrid(layout, actionBarContainer, -1, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, resultContainer, 0, 0, -1, 1.5);
        JFXUtil.setElementPositionInGrid(layout, resultContainer2, 0, 0, -1, 1);
        JFXUtil.setElementPositionInGrid(layout, resultExchangePrice, -1, -1, -1, 2);

        JFXUtil.setElementPositionInGrid(layout, schemaList, 0, 2, 0, 2);

        JFXUtil.setElementPositionInGrid(layout, registryControlContainer, -1, 0, 0, -1);

        Platform.runLater(() -> {
            positionOnWResize();
            JFXUtil.bindAlignmentToNode(layout, schemaList, schemaControlContainer, 5, Orientation.VERTICAL, JFXUtil.Alignment.CENTERED);
        });
    }

    private void positionOnWResize()
    {
        selector.setPrefWidth(layout.getWidth());
        JFXUtil.setElementPositionCentered(layout, actionBarContainer, true, false);
        JFXUtil.setElementPositionCentered(layout, resultExchangePrice, true, false);
    }

    private void setTooltips()
    {
        selector.setTooltip(new Tooltip("Calculate: Calculate profitability margins from material and component data.\nComponents: Manage the prices of various components.\nMaterials: Manage the pricing of raw materials.\nSettings: View and change registry and schematic settings."));
        calculate.setTooltip(new Tooltip("Calculate profitability margins for the currently set parameters."));
        reset.setTooltip(new Tooltip("Clear any currently set components and materials,\nand reset all fields to default."));
        save.setTooltip(new Tooltip("Save the current pricing data for materials and components."));
        activeComponents.setTooltip(new Tooltip("The list of components that you have selected."));
        activeMaterials.setTooltip(new Tooltip("The raw materials required to build all of the currently selected components."));
        resultComponentCost.setTooltip(new Tooltip("The total cost of all selected components,\ncomputed from their set market prices."));
        resultRawCost.setTooltip(new Tooltip("The total cost of the materials required to build the selected components,\ncomputed from their set market prices."));
        resultExchangePrice.setTooltip(new Tooltip("The average market price of the item that you are\nbuilding with the selected component list."));
        components.setTooltip(new Tooltip("The list of available components, as filtered by the above options."));
        rarityFilter.setTooltip(new Tooltip("The rarity of components that you wish to choose from."));
        specFilter.setTooltip(new Tooltip("The R&D School that you wish to choose components from."));
        quantity.setTooltip(new Tooltip("The quantity of the chosen component that you wish to add to the list."));
        add.setTooltip(new Tooltip("Add the set quantity of the chosen component to the list."));
        remove.setTooltip(new Tooltip("Remove all of the selected component in the list from it."));
        totalDilQuantity.setTooltip(new Tooltip("The amount of Refined Dilithium required to build\nthe selected components. Equivalent Dilithium value is\nNOT included in the 'Material Cost' field."));
        maximumProfitMargin.setTooltip(new Tooltip("The difference in value between the cheapest\nmanufacturing option and the set market price\nof the finished item."));
        materialIndex.setTooltip(new Tooltip("The index of all available raw materials.\nDouble-click (or press Enter) on an item to edit its market value.\nWhen done, press Enter to save, or Escape to cancel."));
        componentIndex.setTooltip(new Tooltip("The index of all available components.\nDouble-click (or press Enter) on an item to edit its market value.\nWhen done, press Enter to save, or Escape to cancel."));
        resetRegistry.setTooltip(new Tooltip("Reset all items in the registry to their default values,\nand remove any edits to the config file. Be careful!"));
        importRegistry.setTooltip(new Tooltip("Load a registry from an exported file,\noverwriting the current registry in the process."));
        exportRegistry.setTooltip(new Tooltip("Export the current item values to a file for later use."));
        openCache.setTooltip(new Tooltip("Open the folder containing the live copy of\nthe current item registry and any saved schemas."));
        saveSchema.setTooltip(new Tooltip("Save any current component selections and other fields\nto disk, so that it may be loaded at a later time."));
        loadSchema.setTooltip(new Tooltip("Load the selected schema into memory,\noverwriting any selected components and field data."));
        deleteSchema.setTooltip(new Tooltip("Delete the currently selected schema from the list."));
        schemaList.setTooltip(new Tooltip("The list of item schematics (schemas for short).\nSchemas allow for quick loading of preset field values\nand component lists."));
    }

    private void onExit()
    {
        try {
            cache.getParentFile().mkdirs();
            schemaCache.getParentFile().mkdirs();
            registry.saveRegistry(cache);
            if(!ItemSchema.saveSchemaList(schemaCache, schemaList.getItems().toArray(new ItemSchema[0])))
                throw new IOException("Unable to save schematic data!");
        } catch (IOException e1) {
            e1.printStackTrace();
            if(!new ARKInterfaceDialogYN("Warning", "Could not save value data for item or schema registry! Close anyway?", "Yes", "No").display())
                return;
        }

        System.exit(0);
    }
}

@SuppressWarnings("WeakerAccess")
class IconifiedEditableListCell<T> extends IconifiedListCell<T>
{
    private boolean queuedNumericLimit;
    private boolean queuedNumericDelimit;
    private int queuedLimitQuantity;

    private ChangeListener<? extends String> numericListener;
    protected Callback<T, ObservableValue<String>> getValueCallback;
    protected ObservableValue<String> value;

    protected TextField editField;
    protected Node superclassGraphicStorage;
    protected String superclassStringStorage;
    protected StringConverter<T> converter;

    private KeyCodeCombination editKey;
    private KeyCodeCombination cancelKey;

    private long lastClickReleased;
    private boolean isLastClickPrimary;

    public static KeyCodeCombination DEFAULT_EDIT_KEYBIND = new KeyCodeCombination(KeyCode.ENTER);
    public static KeyCodeCombination DEFAULT_CANCEL_KEYBIND = new KeyCodeCombination(KeyCode.ESCAPE);

    public IconifiedEditableListCell(){
        this(null, null, null);
    }

    public IconifiedEditableListCell(Callback<T, ObservableValue<String>> getValue){
        this(null, null, getValue);
    }

    public IconifiedEditableListCell(StringConverter<T> converter, Callback<T, ObservableValue<String>> getValue){
        this(null, converter, getValue);
    }

    public IconifiedEditableListCell(Callback<T, Node> iconSource, Callback<T, ObservableValue<String>> getValue){
        this(iconSource, null, getValue);
    }

    public IconifiedEditableListCell(Callback<T, Node> iconSource, StringConverter<T> converter, Callback<T, ObservableValue<String>> getValue)
    {
        super(iconSource);

        queuedNumericDelimit = false;
        queuedNumericLimit = false;
        queuedLimitQuantity = -1;

        getValueCallback = getValue;
        this.numericListener = null;
        editField = null;
        superclassGraphicStorage = null;
        superclassStringStorage = null;
        this.editKey = DEFAULT_EDIT_KEYBIND;
        this.cancelKey = DEFAULT_CANCEL_KEYBIND;
        lastClickReleased = -1;
        isLastClickPrimary = false;
        this.converter = converter;

        // This is used to tell the release listener if the last click was primary or not, since it cannot tell by itself
        super.setOnMousePressed(event -> isLastClickPrimary = event.isPrimaryButtonDown());

        // Set up mouse double-click editing trigger support
        super.setOnMouseReleased(event -> {
            if(!isLastClickPrimary) return;

            // If the last click counter has not been set, or it has been more than 1 second since the last click event,
            // reset the click timer and return
            if(lastClickReleased < 0 || System.currentTimeMillis() - lastClickReleased > 500){
                lastClickReleased = System.currentTimeMillis();
                // Otherwise, if it has been less than 1 second since the last click release, count it as a double click and start editing
            }else if (!super.isEditing()) startEdit();
        });

        super.setOnKeyReleased(event -> {
            if(editKey.match(event) && super.isEditing()) commitEdit(this.getItem());
            else if(cancelKey.match(event) && super.isEditing()) cancelEdit();
        });
    }

    @Override
    public void updateItem(T item, boolean empty)
    {
        super.updateItem(item, empty);
        super.setText(empty ? null : converter == null ? item.toString() : converter.toString(item));

        if(!empty) this.value = getValueCallback.call(item);
        else this.value = null;

        if(!empty && editField == null) editField = new TextField();
        if(editField != null && !editField.maxWidthProperty().isBound()) editField.maxWidthProperty().bind(super.widthProperty());

        if(queuedNumericLimit) this.setEditFieldNumeric(true, queuedLimitQuantity);
        else if(queuedNumericDelimit) this.setEditFieldNumeric(false, -1);
    }

    @Override
    public void startEdit()
    {
        if(isEmpty()) return;
        super.startEdit();

        editField.setText(this.value.getValue());
        superclassGraphicStorage = super.getGraphic();
        superclassStringStorage = super.getText();
        super.setText(null);
        super.setGraphic(editField);
    }

    @Override
    public void commitEdit(T newValue)
    {
        if(superclassGraphicStorage != null) super.setGraphic(superclassGraphicStorage);

        ((StringProperty)this.value).setValue(editField.getText());

        super.commitEdit(newValue);
    }

    @Override
    public void cancelEdit()
    {
        if(superclassGraphicStorage != null) super.setGraphic(superclassGraphicStorage);
        if(superclassStringStorage != null) super.setText(superclassStringStorage);

        super.cancelEdit();
    }

    public void setConverter(StringConverter<T> converter){
        this.converter = converter;
    }

    public StringConverter<T> getConverter(){
        return converter;
    }

    public void setEditFieldNumeric(boolean numeric, int limit)
    {
        if(numeric){
            if(editField == null){
                this.queuedNumericLimit = true;
                this.queuedLimitQuantity = limit;
            }else this.numericListener = JFXUtil.limitTextFieldToNumerical(this.editField, limit);
            return;
        }

        if(this.numericListener != null)
        {
            if(editField == null) this.queuedNumericDelimit = true;
            else{
                //noinspection unchecked
                this.editField.textProperty().removeListener((ChangeListener<String>)numericListener);
                this.numericListener = null;
            }
        }
    }
}

@SuppressWarnings("WeakerAccess")
class IconifiedListCell<T> extends ListCell<T>
{
    protected Callback<T, Node> iconSourceCallback;

    public IconifiedListCell(Callback<T, Node> iconSource)
    {
        super();
        this.iconSourceCallback = iconSource;
    }

    @Override
    public void updateItem(T item, boolean empty)
    {
        super.updateItem(item, empty);

        if(empty){
            super.setGraphic(null);
            super.setText(null);
        }else{
            super.setGraphic(iconSourceCallback.call(item));
            super.setText(item.toString());
        }
    }
}

class ItemEntry
{
    Item item;
    int quantity;

    ItemEntry(Item item, int quantity)
    {
        this.item = item;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return this.item.name + ": " + quantity;
    }
}

@SuppressWarnings("WeakerAccess")
class ItemSchema
{
    public static final String KEY_SCHEMA_NAME = "schema-name";
    public static final String KEY_SCHEMA_VALUE = "schema-value";
    public static final String KEY_SCHEMA_COMPONENTS = "schema-components";
    public static final String KEY_SCHEMA_LIST = "schema-list";

    public String name;
    public int value;
    public ItemEntry[] componentList;

    public ItemSchema(String name, int value, ItemEntry... components) {
        this.name = name;
        this.value = value;
        this.componentList = components;
    }

    public static ARKJsonElement serialize(ItemSchema source)
    {
        ARKJsonElement result = new ARKJsonElement(null, false, null);
        result.addSubElement(new ARKJsonElement(KEY_SCHEMA_NAME, false, source.name));
        result.addSubElement(new ARKJsonElement(KEY_SCHEMA_VALUE, false, "" + source.value));

        ARKJsonElement components = new ARKJsonElement(KEY_SCHEMA_COMPONENTS, false, null);
        result.addSubElement(components);

        if(source.componentList == null) return result;

        for(ItemEntry i : source.componentList)
            components.addSubElement(new ARKJsonElement(i.item.name, false, "" + i.quantity));

        return result;
    }

    public static ItemSchema deserialize(ARKJsonElement source, ItemRegistry registry)
    {
        ArrayList<ItemEntry> components = new ArrayList<>();
        String name = source.getSubElementByName(KEY_SCHEMA_NAME).getDeQuotedValue();

        int value;
        try{
            value = Integer.parseInt(source.getSubElementByName(KEY_SCHEMA_VALUE).getDeQuotedValue());
        }catch (NumberFormatException e1){
            value = 0;
        }

        for(ARKJsonElement e : source.getSubElementByName(KEY_SCHEMA_COMPONENTS).getSubElements())
        {
            int quantity;
            try{
                quantity = Integer.parseInt(e.getDeQuotedValue());
            }catch (NumberFormatException e1){
                quantity = 1;
            }

            components.add(new ItemEntry(registry.getByName(ItemRegistry.Category.ALL, e.getName()), quantity));
        }

        return new ItemSchema(name, value, components.toArray(new ItemEntry[0]));
    }

    public static boolean saveSchemaList(File dest, ItemSchema... source)
    {
        try {
            if((!dest.exists() || dest.delete()) && !dest.createNewFile()) return false;
            BufferedWriter br = new BufferedWriter(new FileWriter(dest));

            ARKJsonObject obj = new ARKJsonObject("{\r\n}");
            obj.parse();
            ARKJsonElement main = new ARKJsonElement(KEY_SCHEMA_LIST, true, null);

            for(ItemSchema s : source){
                main.addSubElement(serialize(s));
            }

            obj.getArrayMap().add(main);
            br.write(obj.getJSONText());
            br.flush();
            br.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ItemSchema[] loadSchemaList(File source, ItemRegistry registry)
    {
        if(!source.exists()) return null;
        ARKJsonObject obj;

        try {
            obj = ARKJsonParser.loadFromFile(source);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ARKJsonElement main = obj.getArrayByName(KEY_SCHEMA_LIST);
        ArrayList<ItemSchema> dest = new ArrayList<>();

        for(ARKJsonElement e : main.getSubElements()) dest.add(deserialize(e, registry));
        return dest.toArray(new ItemSchema[0]);
    }

    @Override
    public String toString(){
        return this.name;
    }
}