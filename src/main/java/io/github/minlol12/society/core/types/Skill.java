package io.github.minlol12.society.core.types;

/** Practical skills, raised by doing. Levels run 0-100. */
public enum Skill {

    FARMING("Farming"),
    WOODCUTTING("Woodcutting"),
    MINING("Mining"),
    BUILDING("Building"),
    CRAFTING("Crafting"),
    TRADING("Trading"),
    SCHOLARSHIP("Scholarship"),
    HEALING("Healing"),
    COMBAT("Combat"),
    STEWARDSHIP("Stewardship");

    private final String display;

    Skill(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
