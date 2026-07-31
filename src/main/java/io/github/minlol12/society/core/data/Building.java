package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.io.Compound;

/**
 * A real building on the ground: a plot the settlement has claimed, the
 * blueprint being raised there, and how far along it is. Buildings start
 * as a staked-out site and finish as standing architecture; the world
 * places blocks as {@link #progress()} climbs, so players watch walls go
 * up course by course.
 */
public final class Building {

    private final String id;
    private final StructureType type;
    private final int x;
    private final int y;
    private final int z;
    /** 0 = north (entrance faces -Z), 1 = east, 2 = south, 3 = west. */
    private final int rotation;

    private double progress;
    private final double totalWork;
    private int startedDay;
    private int completedDay = -1;
    /** How many blueprint cells the world has already placed. */
    private int placedCells;
    private boolean ruined;
    private String workerId = "";

    public Building(String id, StructureType type, int x, int y, int z, int rotation,
                    double totalWork, int startedDay) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = ((rotation % 4) + 4) % 4;
        this.totalWork = Math.max(1.0, totalWork);
        this.startedDay = startedDay;
    }

    public String id() { return id; }

    public StructureType type() { return type; }

    public int x() { return x; }

    public int y() { return y; }

    public int z() { return z; }

    public int rotation() { return rotation; }

    /** Work done so far, in builder-days. */
    public double progress() { return progress; }

    public double totalWork() { return totalWork; }

    /** 0.0 to 1.0 - how much of the building actually stands. */
    public double fraction() {
        return Math.max(0.0, Math.min(1.0, progress / totalWork));
    }

    public boolean isComplete() { return completedDay >= 0; }

    public int startedDay() { return startedDay; }

    public int completedDay() { return completedDay; }

    public int placedCells() { return placedCells; }

    public void setPlacedCells(int cells) { this.placedCells = Math.max(0, cells); }

    public boolean isRuined() { return ruined; }

    public void setRuined(boolean ruined) { this.ruined = ruined; }

    /** The citizen currently working this site, for flavour and reporting. */
    public String workerId() { return workerId; }

    public void setWorkerId(String id) { this.workerId = id == null ? "" : id; }

    /**
     * Adds a day's labour. Returns true exactly once - on the day the
     * building is finished.
     */
    public boolean addWork(double amount, int day) {
        if (isComplete() || amount <= 0.0) return false;
        progress += amount;
        if (progress >= totalWork) {
            progress = totalWork;
            completedDay = day;
            return true;
        }
        return false;
    }

    /** Knocks a finished building back into a damaged, unfinished state. */
    public void damage(double amount) {
        if (amount <= 0.0) return;
        progress = Math.max(0.0, progress - amount);
        if (progress < totalWork) {
            completedDay = -1;
        }
    }

    public double distanceTo(int px, int pz) {
        double dx = x - px;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Half-extent of the plot, used for spacing new sites apart. */
    public int radius() {
        return type.footprint() / 2 + 1;
    }

    public Compound save() {
        return new Compound()
                .put("id", id)
                .put("type", type.name())
                .put("x", x)
                .put("y", y)
                .put("z", z)
                .put("rot", rotation)
                .put("progress", progress)
                .put("total", totalWork)
                .put("started", startedDay)
                .put("completed", completedDay)
                .put("placed", placedCells)
                .put("ruined", ruined)
                .put("worker", workerId);
    }

    /** Returns null when the saved type no longer exists in this version. */
    public static Building load(Compound c) {
        StructureType type;
        try {
            type = StructureType.valueOf(c.getString("type", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
        Building b = new Building(
                c.getString("id", ""),
                type,
                c.getInt("x", 0),
                c.getInt("y", 64),
                c.getInt("z", 0),
                c.getInt("rot", 0),
                c.getDouble("total", 10.0),
                c.getInt("started", 0));
        b.progress = c.getDouble("progress", 0.0);
        b.completedDay = c.getInt("completed", -1);
        b.placedCells = c.getInt("placed", 0);
        b.ruined = c.getBool("ruined", false);
        b.workerId = c.getString("worker", "");
        return b;
    }
}
