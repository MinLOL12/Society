package io.github.minlol12.society.core.types;

/**
 * The broad "shape" of a personality, derived from trait values. Archetypes
 * colour a citizen's natural aptitudes more than their job: two farmers can
 * be a WORKER and an EXPLORER, and will climb the farming skill at different
 * speeds and make very different life choices.
 */
public enum Archetype {

    WORKER("Worker"),
    ARTISAN("Artisan"),
    MERCHANT("Merchant"),
    EXPLORER("Explorer"),
    WARRIOR("Warrior"),
    SAGE("Sage"),
    LEADER("Leader"),
    CARETAKER("Caretaker");

    private final String display;

    Archetype(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    /** Natural learning speed multiplier for a skill. */
    public double aptitude(Skill skill) {
        switch (this) {
            case WORKER:
                return (skill == Skill.FARMING || skill == Skill.WOODCUTTING || skill == Skill.MINING) ? 1.3 : 0.95;
            case ARTISAN:
                return (skill == Skill.CRAFTING || skill == Skill.BUILDING) ? 1.35 : 0.95;
            case MERCHANT:
                return skill == Skill.TRADING ? 1.5 : skill == Skill.STEWARDSHIP ? 1.1 : 0.9;
            case EXPLORER:
                return (skill == Skill.WOODCUTTING || skill == Skill.MINING) ? 1.15
                        : skill == Skill.SCHOLARSHIP ? 1.1 : 0.95;
            case WARRIOR:
                return skill == Skill.COMBAT ? 1.6 : 0.85;
            case SAGE:
                return skill == Skill.SCHOLARSHIP ? 1.4 : skill == Skill.HEALING ? 1.2 : 0.9;
            case LEADER:
                return skill == Skill.STEWARDSHIP ? 1.45 : skill == Skill.TRADING ? 1.1 : 0.9;
            case CARETAKER:
                return skill == Skill.HEALING ? 1.4 : skill == Skill.FARMING ? 1.15 : 0.95;
            default:
                return 1.0;
        }
    }
}
