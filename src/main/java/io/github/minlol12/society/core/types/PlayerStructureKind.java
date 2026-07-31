package io.github.minlol12.society.core.types;

/**
 * What a player-built structure counts as. A structure labelled GOVERNMENT
 * becomes a government building: the player who claims it may be crowned
 * there, and it appears on the settlement page. CUSTOM is for anything the
 * players decide is something else entirely.
 */
public enum PlayerStructureKind {

    GOVERNMENT("Government Building",
            "The seat of rule. Sovereigns are crowned here."),
    HOUSING("Housing",
            "A home for the settlement's people."),
    FOOD("Food Building",
            "Feeds the settlement - farms, kitchens, storehouses."),
    INDUSTRY("Industry",
            "Workshops, forges and mills."),
    TRADE("Trade Building",
            "Markets, stalls and warehouses."),
    KNOWLEDGE("Knowledge Building",
            "Libraries, schools and shrines."),
    DEFENCE("Defence",
            "Walls, towers and barracks."),
    CUSTOM("Custom Structure",
            "Something only you know what it is for.");

    private final String display;
    private final String description;

    PlayerStructureKind(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public String display() {
        return display;
    }

    public String description() {
        return description;
    }

    public boolean isGovernment() {
        return this == GOVERNMENT;
    }

    public static PlayerStructureKind byName(String name) {
        if (name == null) return CUSTOM;
        for (PlayerStructureKind k : values()) {
            if (k.name().equalsIgnoreCase(name) || k.display.equalsIgnoreCase(name)) {
                return k;
            }
        }
        return CUSTOM;
    }
}
