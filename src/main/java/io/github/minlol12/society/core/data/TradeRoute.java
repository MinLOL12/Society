package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.io.Compound;

/** A caravan road between two settlements with an active trade pact. */
public final class TradeRoute {

    private final String aId;
    private final String bId;
    private final int establishedDay;
    private double volumeToday;
    private double totalVolume;

    public TradeRoute(String aId, String bId, int establishedDay) {
        this.aId = aId;
        this.bId = bId;
        this.establishedDay = establishedDay;
    }

    public String aId() { return aId; }

    public String bId() { return bId; }

    public boolean involves(String settlementId) {
        return aId.equals(settlementId) || bId.equals(settlementId);
    }

    public String other(String settlementId) {
        if (aId.equals(settlementId)) return bId;
        if (bId.equals(settlementId)) return aId;
        return null;
    }

    public int establishedDay() { return establishedDay; }

    /** Value of goods yet allowed to move today (reset every morning). */
    public double volumeToday() { return volumeToday; }

    public void resetVolume(double dailyCapacity) {
        volumeToday = dailyCapacity;
    }

    /** Moves goods worth {@code value}; returns the value actually moved. */
    public double move(double value) {
        double moved = Math.min(value, Math.max(0.0, volumeToday));
        volumeToday -= moved;
        totalVolume += moved;
        return moved;
    }

    public double totalVolume() { return totalVolume; }

    public Compound save() {
        return new Compound()
                .put("a", aId)
                .put("b", bId)
                .put("established", establishedDay)
                .put("total", totalVolume);
    }

    public static TradeRoute load(Compound c) {
        TradeRoute r = new TradeRoute(c.getString("a", ""), c.getString("b", ""), c.getInt("established", 0));
        r.totalVolume = c.getDouble("total", 0.0);
        return r;
    }
}
