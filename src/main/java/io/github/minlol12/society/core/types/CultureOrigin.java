package io.github.minlol12.society.core.types;

/**
 * Where a culture took root. The dominant environment at a settlement's
 * founding decides its origin; everything else (habits, buildings, dress,
 * values, festivals) grows out of that seed and the settlement's later
 * history.
 */
public enum CultureOrigin {

    PLAINS("Plains", "Fieldfolk", Trait.INDUSTRY,
            "timber-and-daub cottages", "wheat-gold sashes", "Harvest Home"),
    FOREST("Forest", "Woodfolk", Trait.CAUTION,
            "mossy log lodges", "green-dyed hoods", "Feast of First Green"),
    MOUNTAIN("Mountain", "Stonefolk", Trait.WISDOM,
            "granite halls", "grey wool cloaks", "Echo Night"),
    COASTAL("Coastal", "Saltfolk", Trait.CURIOSITY,
            "weathered boardwalks", "sea-blue coats", "the Tide-Turning"),
    DESERT("Desert", "Sandfolk", Trait.AMBITION,
            "sandstone walls", "crimson veils", "the Rite of the Long Sun"),
    SNOWY("Snow", "Frostfolk", Trait.GENEROSITY,
            "log longhouses", "fur-trimmed parkas", "Emberfast"),
    JUNGLE("Jungle", "Leaffolk", Trait.SOCIABILITY,
            "stilted palm huts", "orchid wreaths", "the Canopy Dance"),
    SWAMP("Swamp", "Mirefolk", Trait.AGGRESSION,
            "peat-roofed shacks", "bog-iron amulets", "the Night of Fires");

    private final String display;
    private final String folkName;
    private final Trait virtue;
    private final String buildingStyle;
    private final String dressStyle;
    private final String festivalName;

    CultureOrigin(String display, String folkName, Trait virtue,
                  String buildingStyle, String dressStyle, String festivalName) {
        this.display = display;
        this.folkName = folkName;
        this.virtue = virtue;
        this.buildingStyle = buildingStyle;
        this.dressStyle = dressStyle;
        this.festivalName = festivalName;
    }

    public String display() { return display; }

    public String folkName() { return folkName; }

    /** The virtue this culture admires; children raised here lean toward it. */
    public Trait virtue() { return virtue; }

    public String buildingStyle() { return buildingStyle; }

    public String dressStyle() { return dressStyle; }

    public String festivalName() { return festivalName; }

    /** How the land favours each trade's yield here. */
    public double productionModifier(Good good) {
        switch (this) {
            case PLAINS: return good == Good.FOOD ? 1.15 : 1.0;
            case FOREST: return good == Good.WOOD ? 1.25 : 1.0;
            case MOUNTAIN:
                switch (good) {
                    case STONE: return 1.2;
                    case IRON: return 1.2;
                    case FOOD: return 0.9;
                    default: return 1.0;
                }
            case COASTAL: return good == Good.FOOD ? 1.1 : 1.0;
            case DESERT:
                switch (good) {
                    case FOOD: return 0.8;
                    case GEMS: return 1.35;
                    case LUXURY: return 1.25;
                    default: return 1.0;
                }
            case SNOWY:
                switch (good) {
                    case CLOTH: return 1.35;
                    case FOOD: return 0.85;
                    default: return 1.0;
                }
            case JUNGLE:
                switch (good) {
                    case MEDICINE: return 1.4;
                    case WOOD: return 1.1;
                    default: return 1.0;
                }
            case SWAMP:
                switch (good) {
                    case MEDICINE: return 1.2;
                    case FOOD: return 0.95;
                    default: return 1.0;
                }
            default: return 1.0;
        }
    }

    /** Coastal and river cultures keep shrewder trading houses. */
    public double tradeProfitModifier() {
        return this == COASTAL ? 1.15 : this == DESERT ? 1.1 : 1.0;
    }
}
