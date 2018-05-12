package Misc.STOComponents;

import core.CoreUtil.JFXUtil;
import core.UI.ARKManagerBase;
import core.UI.InterfaceDialogs.ARKInterfaceAlert;
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
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
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
    private static final File cache = new File(ARKAppCompat.getOSSpecificAppPersistRoot().getAbsolutePath() + "\\STOComponents", "items.stx");

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

    @ModeLocal(invert = true, value = MODE_CALCULATION_VIEW)
    private Button resetRegistry;

    private Map<Item, ObservableValue<String>> itemValues;

    private ModeSwitchController modeController;
    private ItemRegistry registry;

    STOComponentUI(String title, int width, int height, double x, double y)
    {
        super(title, width, height, x, y);

        super.setIcon(new Image("Misc/STOComponents/assets/gui/icon/energy_credit.png"));

        window.setMinHeight(height);
        window.setMinWidth(width);

        if(x < 0) window.setX((Screen.getPrimary().getBounds().getWidth() / 2) - width);
        if(y < 0) window.setY((Screen.getPrimary().getBounds().getHeight() / 2) - height);

        itemValues = new HashMap<>();

        registry = new ItemRegistry();
        if(!cache.exists() || !registry.loadRegistryFromFile(cache))
            registry.loadDefaultItemSet(ItemRegistry.Category.ALL);

        window.setOnCloseRequest(e -> onExit());

        Tab calculation = new Tab("Calculate");
        Tab componentView = new Tab("Components");
        Tab materialView = new Tab("Materials");

        calculation.setGraphic(JFXUtil.generateGraphicFromResource("Misc/STOComponents/assets/gui/icon/energy_credit.png", (int)(15 * JFXUtil.SCALE)));
        componentView.setGraphic(JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + "components/very_rare/isolinear_circuitry.png", (int)(15 * JFXUtil.SCALE)));
        materialView.setGraphic(JFXUtil.generateGraphicFromResource(Item.ICON_BASE_PATH + "materials/rare/tetrazine.png", (int)(15 * JFXUtil.SCALE)));

        selector = new TabPane(calculation, componentView, materialView);

        activeContainer = new HBox();
        componentContainer = new HBox();
        actionBarContainer = new HBox();
        filterContainer = new HBox();
        resultContainer = new HBox();
        resultContainer2 = new HBox();

        materialIndex = new ListView<>();
        componentIndex = new ListView<>();
        activeComponents = new ListView<>();
        activeMaterials = new ListView<>();
        components = new ChoiceBox<>();
        rarityFilter = new ChoiceBox<>();
        specFilter = new ChoiceBox<>();

        calculate = new Button("Calculate...");
        reset = new Button("Clear Components");
        save = new Button("Save Values");
        add = new Button("Add");
        remove = new Button("Remove Selected Component");
        resetRegistry = new Button("Reset Component Registries");

        quantity = new TextField("1");
        resultRawCost = new Label(MATERIAL_COST_BASE + "N/A");
        resultComponentCost = new Label(COMPONENT_COST_BASE + "N/A");
        maximumProfitMargin = new Label(PROFIT_MARGIN_BASE + "N/A");
        totalDilQuantity = new Label(": 0");
        resultExchangePrice = new TextField();

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

        for(Item i : registry.getByCategory(ItemRegistry.Category.MATERIAL))
            materialIndex.getItems().add(new ItemEntry(i, i.value));

        for(Item i : registry.getByCategory(ItemRegistry.Category.COMPONENT))
            componentIndex.getItems().add(new ItemEntry(i, i.value));

        specFilter.getItems().addAll(Item.Specialization.values());
        rarityFilter.getItems().addAll(Item.Rarity.values());

        specFilter.getSelectionModel().select(Item.Specialization.BEAMS);
        rarityFilter.getSelectionModel().select(Item.Rarity.ALL);

        add.setOnAction(e ->{
            if(components.getValue() == defaultItem || quantity.getText().contains("-") || quantity.getText().equals("0")) return;

            int count = 1;
            try{
                count = (int)(Double.parseDouble(quantity.getText()));
            }catch (NumberFormatException ignored){}

            Item current = components.getValue();

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
        });

        remove.setOnAction(e ->{
            int index = activeComponents.getSelectionModel().getSelectedIndex();
            if(index < 0 | index >= activeComponents.getItems().size()) return;

            ItemEntry target = activeComponents.getItems().get(index);
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

        reset.setOnAction(e ->{
            activeComponents.getItems().clear();
            activeMaterials.getItems().clear();
            rarityFilter.getSelectionModel().select(Item.Rarity.ALL);
            specFilter.getSelectionModel().select(Item.Specialization.BEAMS);
            quantity.setText("1");
        });

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

        resetRegistry.setOnAction(e ->{
            if(new ARKInterfaceDialogYN("WARNING", "   ! REGISTRY RESET !   \n\nWarning! This will reset all stored values and edits to the config file! Proceed?", "Reset", "Cancel").display()){
                registry.loadDefaultItemSet(ItemRegistry.Category.ALL);
                new ARKInterfaceAlert("Notice", "Item registry has been reset to defaults. Program will now restart.").display();
                onExit();
            }
        });

        activeContainer.getChildren().addAll(activeMaterials, activeComponents);
        activeContainer.setSpacing(5 * JFXUtil.SCALE);
        activeContainer.setAlignment(Pos.CENTER);
        activeContainer.setFillHeight(true);
        HBox.setHgrow(activeMaterials, Priority.ALWAYS);
        HBox.setHgrow(activeComponents, Priority.ALWAYS);

        componentContainer.getChildren().addAll(componentLabel, components, quantity, add);
        componentContainer.setSpacing(5 * JFXUtil.SCALE);
        componentContainer.setAlignment(Pos.CENTER);
        componentContainer.setFillHeight(false);
        HBox.setHgrow(componentLabel, Priority.NEVER);
        HBox.setHgrow(components, Priority.ALWAYS);
        HBox.setHgrow(quantity, Priority.NEVER);
        HBox.setHgrow(add, Priority.SOMETIMES);

        actionBarContainer.getChildren().addAll(reset, calculate, save);
        actionBarContainer.setSpacing(10 * JFXUtil.SCALE);
        actionBarContainer.setAlignment(Pos.CENTER);
        actionBarContainer.setFillHeight(false);
        HBox.setHgrow(calculate, Priority.NEVER);
        HBox.setHgrow(reset, Priority.NEVER);
        HBox.setHgrow(save, Priority.NEVER);

        filterContainer.getChildren().addAll(specFilter, rarityFilter, remove);
        filterContainer.setSpacing(10 * JFXUtil.SCALE);
        filterContainer.setAlignment(Pos.CENTER);
        filterContainer.setFillHeight(false);
        HBox.setHgrow(specFilter, Priority.SOMETIMES);
        HBox.setHgrow(rarityFilter, Priority.SOMETIMES);
        HBox.setHgrow(remove, Priority.NEVER);

        resultContainer.getChildren().addAll(resultComponentCost, separator1, resultRawCost);
        resultContainer.setSpacing(5 * JFXUtil.SCALE);
        resultContainer.setAlignment(Pos.CENTER);
        resultContainer.setFillHeight(false);

        resultContainer2.getChildren().addAll(maximumProfitMargin, separator2, totalDilQuantity);
        resultContainer2.setSpacing(5 * JFXUtil.SCALE);
        resultContainer2.setAlignment(Pos.CENTER);
        resultContainer2.setFillHeight(false);

        layout.getChildren().addAll(selector, materialIndex, componentIndex, activeContainer, componentContainer, actionBarContainer,
                filterContainer, resultContainer, resultContainer2, resultExchangePrice, resetRegistry);

        setTooltips();

        positionElements();

        modeController.switchMode(MODE_CALCULATION_VIEW);
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

        JFXUtil.setElementPositionInGrid(layout, materialIndex, 0, 0, 0, 1);
        JFXUtil.setElementPositionInGrid(layout, componentIndex, 0, 0, 0, 1);

        JFXUtil.setElementPositionInGrid(layout, filterContainer, 0, 0, 0, -1);
        JFXUtil.setElementPositionInGrid(layout, componentContainer, 0, -1, 1, -1);

        JFXUtil.setElementPositionInGrid(layout, activeContainer, 0, 0, 2, 3);
        JFXUtil.setElementPositionInGrid(layout, actionBarContainer, -1, -1, -1, 0);
        JFXUtil.setElementPositionInGrid(layout, resultContainer, 0, 0, -1, 1.5);
        JFXUtil.setElementPositionInGrid(layout, resultContainer2, 0, 0, -1, 1);
        JFXUtil.setElementPositionInGrid(layout, resultExchangePrice, -1, -1, -1, 2);

        JFXUtil.setElementPositionInGrid(layout, resetRegistry, -1, -1, -1, 0);

        Platform.runLater(this::positionOnWResize);
    }

    private void positionOnWResize()
    {
        selector.setPrefWidth(layout.getWidth());
        JFXUtil.setElementPositionCentered(layout, actionBarContainer, true, false);
        JFXUtil.setElementPositionCentered(layout, resultExchangePrice, true, false);
        JFXUtil.setElementPositionCentered(layout, resetRegistry, true ,false);
    }

    private void setTooltips()
    {
        selector.setTooltip(new Tooltip("Calculate: Calculate profitability margins from material and component data.\nComponents: Manage the prices of various components.\nMaterials: Manage the pricing of raw materials."));
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
    }

    private void onExit()
    {
        try {
            cache.getParentFile().mkdirs();
            registry.saveRegistry(cache);
        } catch (IOException e1) {
            e1.printStackTrace();
            if(!new ARKInterfaceDialogYN("Warning", "Could not save value data for item registry! Close anyway?", "Yes", "No").display())
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