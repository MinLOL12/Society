package io.github.minlol12.society.core.types;

/**
 * Technologies a settlement can discover through scholarship. Discovery
 * order is not fixed: a hungry town studies agriculture, a rich one studies
 * coinage. Parents must be discovered first.
 */
public enum TechNode {

    AGRICULTURE("Agriculture", 150, null,
            "fields yield 15% more food"),
    CARPENTRY("Carpentry", 170, null,
            "builders raise housing 30% faster"),
    MINING("Mining", 160, null,
            "mines yield 20% more iron and stone"),
    STONEMASONRY("Stonemasonry", 200, null,
            "sturdier buildings; stone yield up 20%"),
    WRITING("Writing", 220, null,
            "scholars research 25% faster; history is kept in detail"),
    MEDICINE("Medicine", 280, null,
            "fewer deaths from age, plague and famine"),
    CROP_ROTATION("Crop Rotation", 340, "AGRICULTURE",
            "fields yield a further 15% more food"),
    SMELTING("Smelting", 260, "MINING",
            "iron yield up 30%"),
    NAVIGATION("Navigation", 300, null,
            "caravans carry 10 more value per day"),
    COINAGE("Coinage", 320, null,
            "trade profits up 25%"),
    MILITARY_DRILL("Military Drill", 260, null,
            "guards fight 30% better"),
    METALWORKING("Metalworking", 340, "SMELTING",
            "tool production up 40%"),
    ARCHITECTURE("Architecture", 420, "STONEMASONRY",
            "the settlement may grow into a city");

    private final String display;
    private final int cost;
    private final String parentName;
    private final String effect;

    TechNode(String display, int cost, String parentName, String effect) {
        this.display = display;
        this.cost = cost;
        this.parentName = parentName;
        this.effect = effect;
    }

    public String display() { return display; }

    public int cost() { return cost; }

    public TechNode parent() {
        if (parentName == null) return null;
        return TechNode.valueOf(parentName);
    }

    public String effect() { return effect; }

    /** What this technology multiplies the production of, once unlocked. */
    public double goodModifier(Good good) {
        switch (this) {
            case AGRICULTURE:
            case CROP_ROTATION: return good == Good.FOOD ? 1.15 : 1.0;
            case MINING: return (good == Good.IRON || good == Good.STONE) ? 1.2 : 1.0;
            case STONEMASONRY: return good == Good.STONE ? 1.2 : 1.0;
            case SMELTING: return good == Good.IRON ? 1.3 : 1.0;
            case METALWORKING: return good == Good.TOOLS ? 1.4 : 1.0;
            default: return 1.0;
        }
    }
}
