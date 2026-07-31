package io.github.minlol12.society.core.types;

/** Personality traits. Every citizen holds a 0-100 value for each trait. */
public enum Trait {

    INDUSTRY("Industriousness", "how hard they work"),
    SOCIABILITY("Sociability", "how well they get along with others"),
    AMBITION("Ambition", "how strongly they seek wealth and status"),
    CURIOSITY("Curiosity", "how drawn they are to new ideas"),
    AGGRESSION("Aggressiveness", "how quickly they turn to force"),
    CAUTION("Caution", "how carefully they avoid risk"),
    GENEROSITY("Generosity", "how freely they share with others"),
    WISDOM("Wisdom", "how much weight their judgment carries");

    private final String display;
    private final String description;

    Trait(String display, String description) {
        this.display = display;
        this.description = description;
    }

    public String display() {
        return display;
    }

    public String description() {
        return description;
    }
}
