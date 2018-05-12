package Misc.STOComponents;

import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.ARKJsonParser.ARKJsonElement;
import core.CoreUtil.ARKJsonParser.ARKJsonObject;
import core.CoreUtil.ARKJsonParser.ARKJsonParser;
import core.CoreUtil.JFXUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ItemRegistry
{
    public enum Category{
        COMPONENT, MATERIAL, ALL
    }

    private static final String ITEM_NAME_KEY = "item-name";
    private static final String ITEM_CATEGORY_KEY = "item-category";
    private static final String ITEM_SPEC_KEY = "item-schools";
    private static final String ITEM_RARITY_KEY = "item-rarity";
    private static final String ITEM_RARITY_NUM_KEY = "item-rarity-numeric";
    private static final String ITEM_VALUE_KEY = "item-value-EC";
    private static final String ITEM_ICON_KEY = "item-icon";
    private static final String ITEM_COMPONENT_KEY = "item-components";

    private ArrayList<Item> componentRegistry;
    private ArrayList<Item> materialRegistry;

    public ItemRegistry(){
        componentRegistry = new ArrayList<>();
        materialRegistry = new ArrayList<>();
    }

    public void addItem(Item item, Category category){
        switch (category){
            case MATERIAL:
                materialRegistry.add(item);
                break;
            case COMPONENT:
                componentRegistry.add(item);
                break;
            case ALL:
                addItem(item, Category.MATERIAL);
                addItem(item, Category.COMPONENT);
                break;
        }
    }

    public void removeItem(Item item, Category category)
    {
        switch (category){
            case MATERIAL:
                materialRegistry.remove(item);
                break;
            case COMPONENT:
                componentRegistry.remove(item);
                break;
            case ALL:
                removeItem(item, Category.MATERIAL);
                removeItem(item, Category.COMPONENT);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean loadRegistryFromFile(File source)
    {
        try {
            ARKJsonObject obj = ARKJsonParser.loadFromFile(source);

            ARKJsonElement materials = obj.getArrayByName("materials");
            ARKJsonElement components = obj.getArrayByName("components");

            for(ARKJsonElement item : materials.getSubElements()) materialRegistry.add(decompileItemJSON(item));

            for(ARKJsonElement item : components.getSubElements()) componentRegistry.add(decompileItemJSON(item));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Item decompileItemJSON(ARKJsonElement entry)
    {
        String name = entry.getSubElementByName(ITEM_NAME_KEY).getDeQuotedValue();
        Item.Rarity rarity = Item.Rarity.valueOf(entry.getSubElementByName(ITEM_RARITY_KEY).getDeQuotedValue());
        String iconPath = entry.getSubElementByName(ITEM_ICON_KEY).getDeQuotedValue();
        ARKJsonElement componentEntry = entry.getSubElementByName(ITEM_COMPONENT_KEY);
        int value;

        try {
            value = Integer.parseInt(entry.getSubElementByName(ITEM_VALUE_KEY).getDeQuotedValue());
        }catch (NumberFormatException e){
            value = 0;
        }

        ArrayList<String> componentNames = new ArrayList<>();
        ArrayList<Integer> componentCounts = new ArrayList<>();

        for(ARKJsonElement e : componentEntry.getSubElements()){
            componentNames.add(e.getName());
            try{
                componentCounts.add(Integer.parseInt(e.getDeQuotedValue()));
            }catch (NumberFormatException e1){
                componentNames.remove(componentNames.size() - 1);
            }
        }
        
        ArrayList<Item.Specialization> specs = new ArrayList<>();
        for(ARKJsonElement e : entry.getSubElementByName(ITEM_SPEC_KEY).getSubElements())
            specs.add(Item.Specialization.valueOf(ARKJsonParser.deQuoteJSONValue(e.getName())));

        int[] counts = new int[componentCounts.size()];
        for(int i = 0; i < componentCounts.size(); i++) counts[i] = componentCounts.get(i);

        return new Item(name, iconPath, rarity, value).setComponents(componentNames.toArray(new String[0]), counts).setSchools(specs.toArray(new Item.Specialization[0]));
    }

    public void saveRegistry(File dest) throws IOException
    {
        if(dest.exists() && !dest.delete()) throw new IOException("File already exists; cannot delete!");
        dest.createNewFile();

        BufferedWriter br = new BufferedWriter(new FileWriter(dest));

        ARKJsonObject obj = new ARKJsonObject("{\r\n}");
        obj.parse();

        ARKJsonElement materials = new ARKJsonElement("materials", true, null);
        ARKJsonElement components = new ARKJsonElement("components", true, null);

        for(Item i : materialRegistry) materials.addSubElement(assembleItemJSON(i, Category.MATERIAL));

        for(Item i : componentRegistry) components.addSubElement(assembleItemJSON(i, Category.COMPONENT));

        obj.getArrayMap().add(materials);
        obj.getArrayMap().add(components);

        br.write(obj.getJSONText());
        br.flush();
        br.close();
    }

    private ARKJsonElement assembleItemJSON(Item i, Category category)
    {
        ARKJsonElement componentList = new ARKJsonElement(ITEM_COMPONENT_KEY, false, null);
        ARKJsonElement specList = new ARKJsonElement(ITEM_SPEC_KEY, false, null);

        for(String c : i.getComponentList().keySet())
            componentList.addSubElement(new ARKJsonElement(c, false, "" + i.getComponentList().get(c)));
        
        for(Item.Specialization spec : i.schools)
            specList.addSubElement(new ARKJsonElement(spec.name(), false, ""));

        return new ARKJsonElement(null, false, null,
                new ARKJsonElement(ITEM_NAME_KEY, false, i.name),
                new ARKJsonElement(ITEM_CATEGORY_KEY, false, category.name()),
                new ARKJsonElement(ITEM_RARITY_KEY, false, i.rarity.name()),
                new ARKJsonElement(ITEM_RARITY_NUM_KEY, false, "" + i.rarity.ordinal()),
                new ARKJsonElement(ITEM_VALUE_KEY, false, "" + i.value),
                new ARKJsonElement(ITEM_ICON_KEY, false, "" + i.iconRelPath),
                specList,
                componentList);
    }

    public void updateItemValue(Item item, Category category)
    {
        Item ref = this.getByName(category, item.name);
        if(ref != null) ref.value = item.value;
    }

    public Item getByName(Category category, String name)
    {
        switch (category){
            case MATERIAL:
                for(Item i : materialRegistry) if (i.name.equals(name)) return i;
                return null;
            case COMPONENT:
                for(Item i : componentRegistry) if (i.name.equals(name)) return i;
                return null;
            case ALL:
                Item i = getByName(Category.MATERIAL, name);
                if(i == null) i = getByName(Category.COMPONENT, name);
                return i;
            default: return null;
        }
    }

    public ArrayList<Item> filterByRarity(ArrayList<Item> items, Item.Rarity rarity)
    {
        if(rarity == Item.Rarity.ALL) //noinspection unchecked
            return (ArrayList<Item>)items.clone();

        ArrayList<Item> results = new ArrayList<>();

        for(Item i : items) {
            if(i.rarity == rarity) results.add(i);
        }

        return results;
    }

    public ArrayList<Item> getByRarity(Category category, Item.Rarity rarity)
    {
        switch (category)
        {
            case COMPONENT:
                return filterByRarity(this.componentRegistry, rarity);
            case MATERIAL:
                return filterByRarity(this.materialRegistry, rarity);
            case ALL:
                ArrayList<Item> results = filterByRarity(this.materialRegistry, rarity);
                results.addAll(filterByRarity(this.componentRegistry, rarity));
                return results;
            default:
                return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Item> getByCategory(Category category)
    {
        switch (category){
            case MATERIAL:
                return (ArrayList<Item>)materialRegistry.clone();
            case COMPONENT:
                return (ArrayList<Item>)componentRegistry.clone();
            case ALL:
                ArrayList<Item> temp = (ArrayList<Item>)materialRegistry.clone();
                temp.addAll((ArrayList<Item>)componentRegistry.clone());
                return temp;
            default: return new ArrayList<>();
        }
    }

    public ArrayList<Item> filterBySpecialization(ArrayList<Item> items, Item.Specialization spec)
    {
        if(spec == Item.Specialization.ALL) //noinspection unchecked
            return (ArrayList<Item>)items.clone();

        ArrayList<Item> results = new ArrayList<>();

        for(Item i : items)
            if (ARKArrayUtil.contains(i.schools, spec) >= 0) results.add(i);

        return results;
    }

    public ArrayList<Item> getBySpecialization(Item.Specialization spec) {
        return this.filterBySpecialization(this.componentRegistry, spec);
    }

    public void loadDefaultItemSet(Category category)
    {
        componentRegistry.clear();
        materialRegistry.clear();

        loadItemSetInternal(category);
    }

    private void loadItemSetInternal(Category category)
    {
        switch (category){
            case MATERIAL:
                this.addItem(new Item("Duranium Ore", "materials/common/duranium.png", Item.Rarity.COMMON), Category.MATERIAL);
                this.addItem(new Item("Hydrazine Gas", "materials/common/hydrazine.png", Item.Rarity.COMMON), Category.MATERIAL);
                this.addItem(new Item("Magnesite", "materials/common/magnesite.png", Item.Rarity.COMMON), Category.MATERIAL);
                this.addItem(new Item("Trionium Gas", "materials/common/trionium.png", Item.Rarity.COMMON), Category.MATERIAL);

                this.addItem(new Item("Hexafluorine Gas", "materials/uncommon/hexafluorine.png", Item.Rarity.UNCOMMON), Category.MATERIAL);
                this.addItem(new Item("Thoron Particle", "materials/uncommon/thoron.png", Item.Rarity.UNCOMMON), Category.MATERIAL);
                this.addItem(new Item("Tritanium Ore", "materials/uncommon/tritanium.png", Item.Rarity.UNCOMMON), Category.MATERIAL);
                this.addItem(new Item("Verteron Particle", "materials/uncommon/verteron.png", Item.Rarity.UNCOMMON), Category.MATERIAL);

                this.addItem(new Item("Beta-Tachyon Particle", "materials/rare/beta-tachyon.png", Item.Rarity.RARE), Category.MATERIAL);
                this.addItem(new Item("Rubidium", "materials/rare/rubidium.png", Item.Rarity.RARE), Category.MATERIAL);
                this.addItem(new Item("Tetrazine Gas", "materials/rare/tetrazine.png", Item.Rarity.RARE), Category.MATERIAL);
                this.addItem(new Item("Z-Particle", "materials/rare/z_particle.png", Item.Rarity.RARE), Category.MATERIAL);

                this.addItem(new Item("Argonite Gas", "materials/very_rare/argonite.png", Item.Rarity.VERY_RARE), Category.MATERIAL);
                this.addItem(new Item("Craylon Gas", "materials/very_rare/craylon.png", Item.Rarity.VERY_RARE), Category.MATERIAL);
                this.addItem(new Item("Dentarium", "materials/very_rare/dentarium.png", Item.Rarity.VERY_RARE), Category.MATERIAL);
                this.addItem(new Item("Plekton", "materials/very_rare/plekton.png", Item.Rarity.VERY_RARE), Category.MATERIAL);
                this.addItem(new Item("Radiogenic Particle", "materials/very_rare/radiogenic.png", Item.Rarity.VERY_RARE), Category.MATERIAL);
                this.addItem(new Item("Trellium-K", "materials/very_rare/trellium-k.png", Item.Rarity.VERY_RARE), Category.MATERIAL);

                this.addItem(new Item("Salvaged Technology", "materials/ultra_rare/salvaged_technology.png", Item.Rarity.ULTRA_RARE), Category.MATERIAL);
                this.addItem(new Item("Refined Dilithium", "materials/ultra_rare/dilithium.png", Item.Rarity.ULTRA_RARE), Category.MATERIAL);
                break;
            case COMPONENT:
                this.addItem(new Item("Barrel Synchronizer", "components/common/barrel_synchronizer.png", Item.Rarity.COMMON)
                        .setComponents(new String[]{"Magnesite", "Duranium Ore"}, new int[]{3, 2}).setSchools(Item.Specialization.CANNONS), Category.COMPONENT);

                this.addItem(new Item("Focusing Lens", "components/common/focusing_lens.png", Item.Rarity.COMMON)
                        .setComponents(new String[]{"Trionium Gas", "Hydrazine Gas"}, new int[]{3, 2})
                        .setSchools(Item.Specialization.CANNONS, Item.Specialization.BEAMS, Item.Specialization.GROUND, Item.Specialization.SHIELDS), Category.COMPONENT);

                this.addItem(new Item("Lab Equipment", "components/common/lab_equipment.png", Item.Rarity.COMMON)
                        .setComponents(new String[]{"Trionium Gas", "Duranium Ore"}, new int[]{3, 2}).setSchools(Item.Specialization.ENGINEERING, Item.Specialization.SCIENCE), Category.COMPONENT);

                this.addItem(new Item("Particle Field Generator", "components/common/particle_generator.png", Item.Rarity.COMMON)
                        .setComponents(new String[]{"Duranium Ore", "Hydrazine Gas"}, new int[]{3, 2}).setSchools(Item.Specialization.KITS, Item.Specialization.SCIENCE, Item.Specialization.SHIELDS), Category.COMPONENT);

                this.addItem(new Item("Industrial Replicator Supplies", "components/common/replicator_supplies.png", Item.Rarity.COMMON)
                        .setComponents(new String[]{"Magnesite", "Hydrazine Gas"}, new int[]{3, 2}).setSchools(Item.Specialization.ENGINEERING, Item.Specialization.GROUND, Item.Specialization.PROJECTILES), Category.COMPONENT);

                this.addItem(new Item("Targeting Interface", "components/common/targeting_interface.png", Item.Rarity.COMMON)
                        .setComponents(new String[]{"Magnesite", "Hydrazine Gas"}, new int[]{3, 2}).setSchools(Item.Specialization.BEAMS, Item.Specialization.KITS, Item.Specialization.PROJECTILES), Category.COMPONENT);


                this.addItem(new Item("Coolant Injector", "components/uncommon/coolant_injector.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Tritanium Ore", "Verteron Particle"}, new int[]{2, 2}).setSchools(Item.Specialization.ENGINEERING, Item.Specialization.GROUND), Category.COMPONENT);

                this.addItem(new Item("Electromagnetic Coupling", "components/uncommon/em_coupling.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Tritanium Ore", "Verteron Particle"}, new int[]{2, 2}).setSchools(Item.Specialization.CANNONS, Item.Specialization.PROJECTILES), Category.COMPONENT);

                this.addItem(new Item("Emitter Module", "components/uncommon/emitter_module.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Tritanium Ore", "Verteron Particle"}, new int[]{2, 2}).setSchools(Item.Specialization.BEAMS, Item.Specialization.KITS), Category.COMPONENT);

                this.addItem(new Item("EPS Conduit", "components/uncommon/eps_conduit.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Hexafluorine Gas", "Thoron Particle", "Tritanium Ore"}, new int[]{2, 2, 3}).setSchools(Item.Specialization.BEAMS, Item.Specialization.SHIELDS), Category.COMPONENT);

                this.addItem(new Item("EPS Regulator", "components/uncommon/eps_regulator.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Hexafluorine Gas", "Thoron Particle"}, new int[]{3, 4}).setSchools(Item.Specialization.CANNONS, Item.Specialization.SCIENCE), Category.COMPONENT);

                this.addItem(new Item("I.F.F. Beacon", "components/uncommon/iff_beacon.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Hexafluorine Gas", "Verteron Particle", "Thoron Particle"}, new int[]{2, 2, 3}).setSchools(Item.Specialization.KITS, Item.Specialization.PROJECTILES), Category.COMPONENT);

                this.addItem(new Item("Plasma Capacitor", "components/uncommon/plasma_capacitor.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Hexafluorine Gas", "Thoron Particle"}, new int[]{4, 3}).setSchools(Item.Specialization.ENGINEERING, Item.Specialization.GROUND), Category.COMPONENT);

                this.addItem(new Item("Subprocessor Unit", "components/uncommon/subprocessor_unit.png", Item.Rarity.UNCOMMON)
                        .setComponents(new String[]{"Verteron Particle", "Tritanium Ore"}, new int[]{2, 2}).setSchools(Item.Specialization.SCIENCE, Item.Specialization.SHIELDS), Category.COMPONENT);


                this.addItem(new Item("Ejection System", "components/rare/ejection_system.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Rubidium", "Beta-Tachyon Particle"}, new int[]{2, 3}).setSchools(Item.Specialization.ENGINEERING, Item.Specialization.PROJECTILES), Category.COMPONENT);

                this.addItem(new Item("Firing Sequencer", "components/rare/firing_sequencer.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Rubidium", "Z-Particle"}, new int[]{1, 2}).setSchools(Item.Specialization.BEAMS, Item.Specialization.CANNONS), Category.COMPONENT);

                this.addItem(new Item("Handheld Calibration Control", "components/rare/handheld_calibrator.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Z-Particle", "Beta-Tachyon Particle"}, new int[]{3, 2}).setSchools(Item.Specialization.GROUND, Item.Specialization.KITS), Category.COMPONENT);

                this.addItem(new Item("Micro Power Cell", "components/rare/micro_power_cell.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Tetrazine Gas", "Z-Particle"}, new int[]{2, 1}).setSchools(Item.Specialization.GROUND, Item.Specialization.KITS), Category.COMPONENT);

                this.addItem(new Item("Plasma Compressor", "components/rare/plasma_compressor.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Z-Particle", "Beta-Tachyon Particle"}, new int[]{3, 2}).setSchools(Item.Specialization.CANNONS, Item.Specialization.SHIELDS), Category.COMPONENT);

                this.addItem(new Item("Power Surge Regulator", "components/rare/power_surge_regulator.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Beta-Tachyon Particle", "Rubidium"}, new int[]{2, 3}).setSchools(Item.Specialization.BEAMS), Category.COMPONENT);

                this.addItem(new Item("Pressurization Chamber", "components/rare/pressurization_chamber.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Tetrazine Gas", "Z-Particle"}, new int[]{2, 1}).setSchools(Item.Specialization.PROJECTILES, Item.Specialization.SCIENCE), Category.COMPONENT);

                this.addItem(new Item("Quantum Field Focus", "components/rare/quantum_focus.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Tetrazine Gas", "Rubidium"}, new int[]{2, 3}).setSchools(Item.Specialization.SCIENCE), Category.COMPONENT);

                this.addItem(new Item("Rerouting Lattice", "components/rare/rerouting_lattice.png", Item.Rarity.RARE)
                        .setComponents(new String[]{"Tetrazine Gas", "Z-Particle"}, new int[]{2, 1}).setSchools(Item.Specialization.ENGINEERING, Item.Specialization.SHIELDS), Category.COMPONENT);



                this.addItem(new Item("Particle Alignment Matrix", "components/very_rare/alignment_matrix.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Craylon Gas", "Trellium-K", "Refined Dilithium"}, new int[]{1, 1, 4000}).setSchools(Item.Specialization.GROUND, Item.Specialization.KITS), Category.COMPONENT);

                this.addItem(new Item("Emitter Array", "components/very_rare/emitter_array.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Craylon Gas", "Refined Dilithium"}, new int[]{1, 500}).setSchools(Item.Specialization.KITS, Item.Specialization.SCIENCE, Item.Specialization.SHIELDS), Category.COMPONENT);

                this.addItem(new Item("Intermix Chamber", "components/very_rare/intermix_chamber.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Radiogenic Particle", "Refined Dilithium"}, new int[]{1, 500}).setSchools(Item.Specialization.PROJECTILES, Item.Specialization.ENGINEERING), Category.COMPONENT);

                this.addItem(new Item("Isolinear Chip", "components/very_rare/isolinear_chip.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Argonite Gas", "Plekton Particle", "Refined Dilithium"}, new int[]{1, 1, 4000}).setSchools(Item.Specialization.BEAMS, Item.Specialization.CANNONS), Category.COMPONENT);

                this.addItem(new Item("Isolinear Circuitry", "components/very_rare/isolinear_circuitry.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Dentarium", "Refined Dilithium"}, new int[]{1, 500}).setSchools(Item.Specialization.BEAMS, Item.Specialization.CANNONS, Item.Specialization.GROUND), Category.COMPONENT);

                this.addItem(new Item("Signal Enhancer", "components/very_rare/signal_enhancer.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Plekton Particle", "Argonite Gas", "Refined Dilithium"}, new int[]{1, 1, 4000}).setSchools(Item.Specialization.SCIENCE, Item.Specialization.SHIELDS), Category.COMPONENT);

                this.addItem(new Item("Warp Field Regulator", "components/very_rare/warp_regulator.png", Item.Rarity.VERY_RARE)
                        .setComponents(new String[]{"Radiogenic Particle", "Refined Dilithium"}, new int[]{1, 500}).setSchools(Item.Specialization.PROJECTILES, Item.Specialization.ENGINEERING), Category.COMPONENT);

                break;
            case ALL:
                loadItemSetInternal(Category.MATERIAL);
                loadItemSetInternal(Category.COMPONENT);
        }
    }
}

