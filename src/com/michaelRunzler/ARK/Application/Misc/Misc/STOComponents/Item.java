package Misc.STOComponents;

import java.util.HashMap;
import java.util.Map;

public class Item
{
    public static final String ICON_BASE_PATH = "Misc/STOComponents/assets/gui/icon/items/";

    public enum Rarity{
        COMMON, UNCOMMON, RARE, VERY_RARE, ULTRA_RARE, EPIC, UNIQUE, OTHER, ALL
    }

    public enum Specialization{
        BEAMS, CANNONS, ENGINEERING, GROUND, KITS, PROJECTILES, SCIENCE, SHIELDS, TRAINING, OTHER, ALL
    }

    public String name;
    public String iconRelPath;
    public int value;
    public Rarity rarity;
    public Specialization[] schools;
    private Map<String, Integer> components;

    Item(String name, String iconRelPath, Rarity rarity){
        this(name, iconRelPath, rarity, 0);
    }

    Item(String name, String iconRelPath, Rarity rarity, int initialValue){
        this.name = name;
        this.iconRelPath = iconRelPath;
        this.value = initialValue;
        this.rarity = rarity;
        this.components = new HashMap<>();
        this.schools = new Specialization[0];
    }

    public Item setComponents(String[] components, int[] quantities) {
        this.components.clear();
        return addComponents(components, quantities);
    }

    public Item addComponents(String[] components, int[] quantities){
        for(int i = 0; i < components.length; i++)
            this.components.put(components[i], quantities[i]);

        return this;
    }

    public Item setSchools(Specialization... specs){
        this.schools = specs;
        return this;
    }

    public Map<String, Integer> getComponentList() {
        return components;
    }
}
