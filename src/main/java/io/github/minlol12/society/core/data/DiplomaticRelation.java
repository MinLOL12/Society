package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.Treaty;

/**
 * The remembered relationship between two settlements: a slow-moving score,
 * the current treaty, and running totals that feed back into the score.
 */
public final class DiplomaticRelation {

    private final String aId;
    private final String bId;
    private double score;
    private Treaty treaty;
    private int contactDay;
    private int warStartDay = -1;
    private int warScore;
    private int daysAtWar;
    private double totalTradeValue;

    public DiplomaticRelation(String aId, String bId, int contactDay) {
        this.aId = aId;
        this.bId = bId;
        this.contactDay = contactDay;
        this.score = 0;
        this.treaty = Treaty.NONE;
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

    public double score() { return score; }

    public void addScore(double delta) {
        score = Math.max(-100.0, Math.min(100.0, score + delta));
    }

    public void setScore(double value) {
        score = Math.max(-100.0, Math.min(100.0, value));
    }

    public Treaty treaty() { return treaty; }

    public void setTreaty(Treaty treaty) { this.treaty = treaty; }

    public int contactDay() { return contactDay; }

    public int warStartDay() { return warStartDay; }

    public void beginWar(int day) {
        treaty = Treaty.WAR;
        warStartDay = day;
        warScore = 0;
        daysAtWar = 0;
    }

    public int warScore() { return warScore; }

    public void addWarScore(int delta) { warScore += delta; }

    public int daysAtWar() { return daysAtWar; }

    public void noteWarDay() { daysAtWar++; }

    public void endWarAsTruce() {
        treaty = Treaty.TRUCE;
        warStartDay = -1;
        warScore = 0;
        daysAtWar = 0;
    }

    public double totalTradeValue() { return totalTradeValue; }

    public void addTrade(double value) { totalTradeValue += value; }

    public Compound save() {
        return new Compound()
                .put("a", aId)
                .put("b", bId)
                .put("score", score)
                .put("treaty", treaty.name())
                .put("contactDay", contactDay)
                .put("warStart", warStartDay)
                .put("warScore", warScore)
                .put("daysAtWar", daysAtWar)
                .put("trade", totalTradeValue);
    }

    public static DiplomaticRelation load(Compound c) {
        DiplomaticRelation r = new DiplomaticRelation(c.getString("a", ""), c.getString("b", ""), c.getInt("contactDay", 0));
        r.score = c.getDouble("score", 0.0);
        try {
            r.treaty = Treaty.valueOf(c.getString("treaty", "NONE"));
        } catch (IllegalArgumentException e) {
            r.treaty = Treaty.NONE;
        }
        r.warStartDay = c.getInt("warStart", -1);
        r.warScore = c.getInt("warScore", 0);
        r.daysAtWar = c.getInt("daysAtWar", 0);
        r.totalTradeValue = c.getDouble("trade", 0.0);
        return r;
    }
}
