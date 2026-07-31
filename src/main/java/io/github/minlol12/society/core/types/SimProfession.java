package io.github.minlol12.society.core.types;

/**
 * Simulated professions. These are <em>not</em> assigned: they emerge from
 * settlement needs, personal aptitude and opportunity (see the economy
 * system). Real villager workstation professions are mapped onto these as
 * well so the ledger and the world agree.
 */
public enum SimProfession {

    FARMER("Farmer", Skill.FARMING),
    LUMBERJACK("Lumberjack", Skill.WOODCUTTING),
    MINER("Miner", Skill.MINING),
    BUILDER("Builder", Skill.BUILDING),
    CRAFTER("Crafter", Skill.CRAFTING),
    TRADER("Trader", Skill.TRADING),
    SCHOLAR("Scholar", Skill.SCHOLARSHIP),
    HEALER("Healer", Skill.HEALING),
    GUARD("Guard", Skill.COMBAT),
    STEWARD("Steward", Skill.STEWARDSHIP),
    NONE("Drifter", null);

    private final String display;
    private final Skill primarySkill;

    SimProfession(String display, Skill primarySkill) {
        this.display = display;
        this.primarySkill = primarySkill;
    }

    public String display() {
        return display;
    }

    public Skill primarySkill() {
        return primarySkill;
    }

    /** Daily baseline production of goods for this profession, before modifiers. */
    public double dailyOutput(Good good) {
        switch (this) {
            case FARMER: return good == Good.FOOD ? 4.5 : 0.0;
            case LUMBERJACK: return good == Good.WOOD ? 2.8 : 0.0;
            case MINER:
                switch (good) {
                    case STONE: return 2.2;
                    case IRON: return 0.75;
                    case GEMS: return 0.12;
                    default: return 0.0;
                }
            case CRAFTER:
                switch (good) {
                    case TOOLS: return 0.55;
                    case CLOTH: return 0.7;
                    default: return 0.0;
                }
            case HEALER: return good == Good.MEDICINE ? 0.3 : 0.0;
            // Drifters pick berries and set snares; nobody quite starves alone.
            case NONE: return good == Good.FOOD ? 0.2 : 0.0;
            default: return 0.0;
        }
    }

    /** Maps a vanilla villager profession registry id (e.g. "minecraft:farmer") to a sim profession. */
    public static SimProfession fromVanillaId(String id) {
        if (id == null) return NONE;
        if (id.endsWith(":farmer") || id.endsWith(":fisherman") || id.endsWith(":shepherd")
                || id.endsWith(":fletcher")) return FARMER;
        if (id.endsWith(":armorer") || id.endsWith(":toolsmith") || id.endsWith(":weaponsmith")) return CRAFTER;
        if (id.endsWith(":butcher") || id.endsWith(":chef") || id.endsWith(":cook")) return FARMER;
        if (id.endsWith(":cartographer")) return TRADER;
        if (id.endsWith(":cleric")) return HEALER;
        if (id.endsWith(":leatherworker") || id.endsWith(":mason")) return CRAFTER;
        if (id.endsWith(":librarian")) return SCHOLAR;
        if (id.endsWith(":nitwit") || id.endsWith(":none")) return NONE;
        return NONE;
    }
}
