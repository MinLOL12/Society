package io.github.minlol12.society.core.types;

/**
 * Roles a <em>player</em> can take on, layered on top of the villager
 * professions. A role is a public identity ("I am the blacksmith of this
 * town") that also nudges the ledger: when an online player keeps a role
 * and belongs to a settlement, that settlement quietly benefits from the
 * player's craft. The highest roles - KING and QUEEN - must be claimed at
 * a government building, and their holder rules the settlement in the
 * ledger's eyes.
 */
public enum PlayerRole {

    NONE("None", "No role. A wanderer without a trade.", SimProfession.NONE),
    WORKER("Worker", "A pair of strong hands. Helps with everything.", SimProfession.BUILDER),
    FARMER("Farmer", "Tends the fields; the settlement eats better.", SimProfession.FARMER),
    BLACKSMITH("Blacksmith", "Works the forge; tools and weapons flow.", SimProfession.CRAFTER),
    MINER("Miner", "Descends into the dark; stone and iron arrive.", SimProfession.MINER),
    BUILDER("Builder", "Raises walls; the settlement builds faster.", SimProfession.BUILDER),
    CRAFTER("Crafter", "Makes cloth, bows and shields for the town.", SimProfession.CRAFTER),
    TRADER("Trader", "Haggles on the road; the treasury grows.", SimProfession.TRADER),
    SCHOLAR("Scholar", "Pores over scrolls; research moves faster.", SimProfession.SCHOLAR),
    HEALER("Healer", "Brews cures; medicine and potions appear.", SimProfession.HEALER),
    GUARD("Guard", "Stands the watch; the town feels safer.", SimProfession.GUARD),
    STEWARD("Steward", "Counts the grain; taxes flow to the treasury.", SimProfession.STEWARD),
    KING("King", "Sovereign of a settlement, crowned at its government building.",
            SimProfession.STEWARD),
    QUEEN("Queen", "Sovereign of a settlement, crowned at its government building.",
            SimProfession.STEWARD);

    private final String display;
    private final String description;
    private final SimProfession profession;

    PlayerRole(String display, String description, SimProfession profession) {
        this.display = display;
        this.description = description;
        this.profession = profession;
    }

    public String display() {
        return display;
    }

    public String description() {
        return description;
    }

    /** The villager profession whose craft this role most resembles. */
    public SimProfession profession() {
        return profession;
    }

    /** Only sovereigns may be crowned at a government building. */
    public boolean isSovereign() {
        return this == KING || this == QUEEN;
    }

    public static PlayerRole byName(String name) {
        if (name == null) return NONE;
        for (PlayerRole r : values()) {
            if (r.name().equalsIgnoreCase(name) || r.display.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return NONE;
    }
}
