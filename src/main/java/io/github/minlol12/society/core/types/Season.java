package io.github.minlol12.society.core.types;

/** Seasons of the world; each lasts a configurable number of in-game days. */
public enum Season {

    SPRING("Spring", 1.05),
    SUMMER("Summer", 1.2),
    AUTUMN("Autumn", 1.0),
    WINTER("Winter", 0.6);

    private final String display;
    private final double farmModifier;

    Season(String display, double farmModifier) {
        this.display = display;
        this.farmModifier = farmModifier;
    }

    public String display() { return display; }

    /** How well fields produce this season. */
    public double farmModifier() { return farmModifier; }

    public Season next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Season ofDay(long worldDay, int seasonLengthDays) {
        long index = (worldDay / seasonLengthDays) % values().length;
        return values()[(int) index];
    }
}
