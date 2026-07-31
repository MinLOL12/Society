package io.github.minlol12.society.core.types;

/** Tradeable goods of the settlement economy. */
public enum Good {

    FOOD("Food", 0.5),
    WOOD("Wood", 1.0),
    STONE("Stone", 1.0),
    IRON("Iron", 4.0),
    GEMS("Gems", 12.0),
    TOOLS("Tools", 6.0),
    CLOTH("Cloth", 3.0),
    MEDICINE("Medicine", 8.0),
    LUXURY("Luxury Goods", 15.0),
    BOWS("Bows", 7.0),
    WEAPONS("Weapons", 10.0),
    SHIELDS("Shields", 8.0),
    POTIONS("Potions", 12.0),
    ENCHANTED("Enchanted Items", 25.0);

    private final String display;
    private final double baseValue;

    Good(String display, double baseValue) {
        this.display = display;
        this.baseValue = baseValue;
    }

    public String display() {
        return display;
    }

    /** Baseline value in emeralds, before local supply and demand. */
    public double baseValue() {
        return baseValue;
    }
}
