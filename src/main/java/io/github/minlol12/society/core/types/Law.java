package io.github.minlol12.society.core.types;

/**
 * Standing laws of a settlement, derived from its dominant cultural values
 * and its form of government. Laws nudge the simulation in small, visible
 * ways and change when society changes.
 */
public enum Law {

    GRANARY_TITHE("Grain Tithe", "every household owes labour to the granary",
            "food production is higher, but the people chafe"),
    OPEN_MARKET("Open Market", "strangers may trade freely in the square",
            "caravans pay better prices here"),
    MILITIA_EDICT("Militia Edict", "every able adult drills at dawn",
            "the settlement fields stronger guards"),
    OPEN_ARCHIVES("Open Archives", "no knowledge may be kept secret",
            "scholars work faster"),
    HEARTH_CHARTER("Hearth Charter", "no one sleeps hungry while the granary holds",
            "famines hurt less and spirits recover quickly"),
    VIGIL_CHARTER("Vigil Charter", "roofs of tile and buckets by every door",
            "fires and disasters cause less damage");

    private final String display;
    private final String summary;
    private final String effect;

    Law(String display, String summary, String effect) {
        this.display = display;
        this.summary = summary;
        this.effect = effect;
    }

    public String display() { return display; }

    public String summary() { return summary; }

    public String effect() { return effect; }
}
