package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.List;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.GovernmentType;
import io.github.minlol12.society.core.types.Law;

/** Who rules a settlement today, under which laws, and for how long. */
public final class Government {

    private GovernmentType type;
    private String leaderId;
    private final List<String> councilIds = new ArrayList<String>();
    private final List<Law> laws = new ArrayList<Law>();
    private int establishedDay;
    private int leaderSinceDay;

    public Government(GovernmentType type, String leaderId, int day) {
        this.type = type;
        this.leaderId = leaderId == null ? "" : leaderId;
        this.establishedDay = day;
        this.leaderSinceDay = day;
    }

    public GovernmentType type() { return type; }

    public void setType(GovernmentType type, int day) {
        this.type = type;
        this.establishedDay = day;
    }

    public String leaderId() { return leaderId; }

    public void setLeader(String leaderId, int day) {
        this.leaderId = leaderId == null ? "" : leaderId;
        this.leaderSinceDay = day;
    }

    public List<String> councilIds() { return councilIds; }

    public List<Law> laws() { return laws; }

    public boolean hasLaw(Law law) {
        return laws.contains(law);
    }

    public int establishedDay() { return establishedDay; }

    public int leaderSinceDay() { return leaderSinceDay; }

    public Compound save() {
        List<String> council = new ArrayList<String>(councilIds);
        List<String> lawNames = new ArrayList<String>();
        for (Law l : laws) lawNames.add(l.name());
        return new Compound()
                .put("type", type.name())
                .put("leader", leaderId)
                .putStringList("council", council)
                .putStringList("laws", lawNames)
                .put("established", establishedDay)
                .put("leaderSince", leaderSinceDay);
    }

    public static Government load(Compound c) {
        GovernmentType type;
        try {
            type = GovernmentType.valueOf(c.getString("type", "ELDER_COUNCIL"));
        } catch (IllegalArgumentException e) {
            type = GovernmentType.ELDER_COUNCIL;
        }
        Government g = new Government(type, c.getString("leader", ""), c.getInt("established", 0));
        g.leaderSinceDay = c.getInt("leaderSince", 0);
        for (String id : c.getStringList("council")) g.councilIds.add(id);
        for (String name : c.getStringList("laws")) {
            try {
                g.laws.add(Law.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // skip unknown laws
            }
        }
        return g;
    }
}
