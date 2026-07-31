package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.SimProfession;

/**
 * A family under one roof and one name. Households pool wealth, remember
 * what their line has always done ("the Oakwinters have long been
 * lumberjacks"), pass a fraction of their skill to their children, and
 * hand their property down when elders die.
 */
public final class Household {

    private final String id;
    private String familyName;
    private final List<String> memberIds = new ArrayList<String>();
    private double wealth;
    /** Historical profession tally across generations, driving traditions. */
    private final Map<String, Integer> professionHeritage = new HashMap<String, Integer>();

    public Household(String id, String familyName) {
        this.id = id;
        this.familyName = familyName;
    }

    public String id() { return id; }

    public String familyName() { return familyName; }

    public void setFamilyName(String name) { this.familyName = name; }

    public List<String> memberIds() { return memberIds; }

    public void addMember(String citizenId) {
        if (!memberIds.contains(citizenId)) memberIds.add(citizenId);
    }

    public void removeMember(String citizenId) {
        memberIds.remove(citizenId);
    }

    public double wealth() { return wealth; }

    public void addWealth(double delta) { this.wealth = Math.max(0.0, wealth + delta); }

    public void noteProfession(SimProfession profession) {
        String key = profession.name();
        Integer count = professionHeritage.get(key);
        professionHeritage.put(key, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
    }

    /** A family tradition, if any single calling spans three or more members. */
    public String tradition() {
        String bestKey = null;
        int best = 0;
        for (Map.Entry<String, Integer> e : professionHeritage.entrySet()) {
            if (e.getValue().intValue() > best) {
                best = e.getValue().intValue();
                bestKey = e.getKey();
            }
        }
        if (bestKey == null || best < 3) return "";
        SimProfession p;
        try {
            p = SimProfession.valueOf(bestKey);
        } catch (IllegalArgumentException e) {
            return "";
        }
        if (p == SimProfession.NONE) return "";
        return "The " + familyName + "s have long been " + Citizen.pluralProfession(p) + ".";
    }

    /** The profession this family's children are gently steered toward. */
    public SimProfession hereditaryProfession() {
        String bestKey = null;
        int best = 0;
        for (Map.Entry<String, Integer> e : professionHeritage.entrySet()) {
            if (e.getValue().intValue() > best) {
                best = e.getValue().intValue();
                bestKey = e.getKey();
            }
        }
        if (bestKey == null || best < 2) return SimProfession.NONE;
        try {
            return SimProfession.valueOf(bestKey);
        } catch (IllegalArgumentException e) {
            return SimProfession.NONE;
        }
    }

    public Compound save() {
        Compound c = new Compound()
                .put("id", id)
                .put("name", familyName)
                .put("wealth", wealth)
                .putStringList("members", memberIds);
        Compound heritage = new Compound();
        for (Map.Entry<String, Integer> e : professionHeritage.entrySet()) {
            heritage.put(e.getKey(), e.getValue().intValue());
        }
        c.put("heritage", heritage);
        return c;
    }

    public static Household load(Compound c) {
        Household h = new Household(c.getString("id", ""), c.getString("name", "Wanderer"));
        h.wealth = c.getDouble("wealth", 0.0);
        for (String id : c.getStringList("members")) h.memberIds.add(id);
        Compound heritage = c.getCompound("heritage");
        for (Map.Entry<String, Object> e : heritage.raw().entrySet()) {
            if (e.getValue() instanceof Integer) {
                h.professionHeritage.put(e.getKey(), (Integer) e.getValue());
            }
        }
        return h;
    }
}
