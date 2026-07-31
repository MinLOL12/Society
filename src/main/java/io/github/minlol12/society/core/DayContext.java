package io.github.minlol12.society.core;

/** What the world looked like on the morning a simulation day starts. */
public final class DayContext {

    public final int worldDay;
    public final boolean raining;
    public final long worldSeed;

    public DayContext(int worldDay, boolean raining, long worldSeed) {
        this.worldDay = worldDay;
        this.raining = raining;
        this.worldSeed = worldSeed;
    }
}
