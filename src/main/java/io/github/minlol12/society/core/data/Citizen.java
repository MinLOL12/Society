package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;

/**
 * One individual villager. May correspond to a living Minecraft entity
 * (a "manifested" citizen, bound by entity UUID) or exist only in the
 * ledger ("unmanifested"), in which case the simulation still feeds,
 * employs, weds and buries them until a player comes close enough that the
 * world materialises them as a real villager.
 */
public final class Citizen {

    private final String id;
    private String entityUuid = "";
    private String firstName;
    private String familyName;
    private String motherId = "";
    private String fatherId = "";
    private String spouseId = "";
    private String householdId = "";
    private String homeSettlementId = "";
    private int birthDay;
    private int deathDay = -1;
    private String deathCause = "";
    private boolean alive = true;
    private boolean manifested;
    private Personality personality = new Personality();
    private final EnumMap<Skill, Integer> skillLevel = new EnumMap<Skill, Integer>(Skill.class);
    private final EnumMap<Skill, Double> skillXp = new EnumMap<Skill, Double>(Skill.class);
    private SimProfession profession = SimProfession.NONE;
    private SimProfession preferredProfession = SimProfession.NONE;
    private final List<String> childrenIds = new ArrayList<String>();
    private double personalWealth;
    private int reputation = 50;
    private int lastChildBornDay = -1000;
    private int lastSeenDay = -1;
    private final List<String> memories = new ArrayList<String>();

    public Citizen(String id, String firstName, String familyName, int birthDay) {
        this.id = id;
        this.firstName = firstName;
        this.familyName = familyName;
        this.birthDay = birthDay;
    }

    public String id() { return id; }

    /** Entity UUID string of the bound villager, or empty when unmanifested. */
    public String entityUuid() { return entityUuid; }

    public void bindEntity(String entityUuid) {
        this.entityUuid = entityUuid == null ? "" : entityUuid;
        this.manifested = !this.entityUuid.isEmpty();
    }

    public void unbindEntity() {
        this.entityUuid = "";
        this.manifested = false;
    }

    public boolean isManifested() { return manifested; }

    public String firstName() { return firstName; }

    public void setFirstName(String name) { this.firstName = name; }

    public String familyName() { return familyName; }

    public void setFamilyName(String name) { this.familyName = name; }

    public String fullName() { return firstName + " " + familyName; }

    public String motherId() { return motherId; }

    public void setMotherId(String id) { this.motherId = id == null ? "" : id; }

    public String fatherId() { return fatherId; }

    public void setFatherId(String id) { this.fatherId = id == null ? "" : id; }

    public String spouseId() { return spouseId; }

    public void setSpouseId(String id) { this.spouseId = id == null ? "" : id; }

    public boolean isMarried() { return !spouseId.isEmpty(); }

    public String householdId() { return householdId; }

    public void setHouseholdId(String id) { this.householdId = id == null ? "" : id; }

    public String homeSettlementId() { return homeSettlementId; }

    public void setHomeSettlementId(String id) { this.homeSettlementId = id == null ? "" : id; }

    public int birthDay() { return birthDay; }

    /** Narrative age: one Minecraft day equals one year of a villager's life. */
    public int ageYears(int currentDay) {
        return Math.max(0, currentDay - birthDay);
    }

    public boolean isAdult(int currentDay) {
        return ageYears(currentDay) >= 16;
    }

    public boolean isElder(int currentDay) {
        return ageYears(currentDay) >= 60;
    }

    public int deathDay() { return deathDay; }

    public String deathCause() { return deathCause; }

    public boolean isAlive() { return alive; }

    public void die(int day, String cause) {
        this.alive = false;
        this.deathDay = day;
        this.deathCause = cause == null ? "died" : cause;
        this.manifested = false;
        this.entityUuid = "";
    }

    public Personality personality() { return personality; }

    public void setPersonality(Personality p) { this.personality = p; }

    public int skillLevel(Skill skill) {
        Integer v = skillLevel.get(skill);
        return v == null ? 0 : v.intValue();
    }

    /** Total accumulated experience is what raises a level. */
    public double skillXpTotal(Skill skill) {
        Double v = skillXp.get(skill);
        return v == null ? 0.0 : v.doubleValue();
    }

    /** Cost of each successive skill level; masters grow slowly. */
    private static double xpForNext(int level) {
        return 40.0 + level * 22.0;
    }

    /**
     * Adds experience to a skill, raising its level when thresholds are
     * crossed. Returns the new level milestone (25/50/75/90) when one was
     * just reached, else -1.
     */
    public int gainXp(Skill skill, double amount) {
        if (amount <= 0.0) return -1;
        double xp = skillXpTotal(skill) + amount;
        int level = skillLevel(skill);
        int oldLevel = level;
        while (level < 100 && xp >= xpForNext(level)) {
            xp -= xpForNext(level);
            level++;
        }
        if (level >= 100) xp = 0.0;
        skillXp.put(skill, Double.valueOf(xp));
        skillLevel.put(skill, Integer.valueOf(level));
        int[] milestones = {90, 75, 50, 25};
        for (int m : milestones) {
            if (oldLevel < m && level >= m) return m;
        }
        return -1;
    }

    public Skill bestSkill() {
        Skill best = null;
        int bestLevel = -1;
        for (Skill s : Skill.values()) {
            int level = skillLevel(s);
            if (level > bestLevel) {
                bestLevel = level;
                best = s;
            }
        }
        return best;
    }

    public int bestSkillLevel() {
        Skill best = bestSkill();
        return best == null ? 0 : skillLevel(best);
    }

    public SimProfession profession() { return profession; }

    public void setProfession(SimProfession profession) { this.profession = profession; }

    /** The job this citizen would choose for themselves, from aptitude. */
    public SimProfession preferredProfession() { return preferredProfession; }

    public void setPreferredProfession(SimProfession p) { this.preferredProfession = p; }

    public List<String> childrenIds() { return childrenIds; }

    public double personalWealth() { return personalWealth; }

    public void addWealth(double delta) { this.personalWealth = Math.max(0.0, personalWealth + delta); }

    public int reputation() { return reputation; }

    public void addReputation(int delta) {
        reputation = Math.max(0, Math.min(100, reputation + delta));
    }

    public int lastChildBornDay() { return lastChildBornDay; }

    public void noteChildBorn(int day) { this.lastChildBornDay = day; }

    /** Last simulation day the bound entity was seen loaded in the world. */
    public int lastSeenDay() { return lastSeenDay; }

    public void noteSeen(int day) { this.lastSeenDay = day; }

    public List<String> memories() { return memories; }

    /** Keeps the newest eight personal memories. */
    public void addMemory(int day, String text) {
        if (memories.size() >= 8) {
            memories.remove(0);
        }
        memories.add("Day " + day + " - " + text);
    }

    /** Fame decides whether a death makes world history. */
    public boolean isFamous() {
        return reputation >= 80 || bestSkillLevel() >= 75;
    }

    public Compound save() {
        Compound c = new Compound()
                .put("id", id)
                .put("entity", entityUuid)
                .put("first", firstName)
                .put("family", familyName)
                .put("mother", motherId)
                .put("father", fatherId)
                .put("spouse", spouseId)
                .put("household", householdId)
                .put("settlement", homeSettlementId)
                .put("birthDay", birthDay)
                .put("deathDay", deathDay)
                .put("deathCause", deathCause)
                .put("alive", alive)
                .put("manifested", manifested)
                .put("profession", profession.name())
                .put("preferred", preferredProfession.name())
                .put("wealth", personalWealth)
                .put("reputation", reputation)
                .put("lastChild", lastChildBornDay)
                .put("lastSeen", lastSeenDay)
                .put("personality", personality.save())
                .putStringList("children", childrenIds)
                .putStringList("memories", memories);
        Compound skills = new Compound();
        for (Skill s : Skill.values()) {
            int level = skillLevel(s);
            if (level > 0) skills.put("l_" + s.name(), level);
            double xp = skillXpTotal(s);
            if (xp > 0.0) skills.put("x_" + s.name(), xp);
        }
        c.put("skills", skills);
        return c;
    }

    public static Citizen load(Compound c) {
        Citizen citizen = new Citizen(
                c.getString("id", ""),
                c.getString("first", "Nameless"),
                c.getString("family", "Wanderer"),
                c.getInt("birthDay", 0));
        citizen.entityUuid = c.getString("entity", "");
        citizen.motherId = c.getString("mother", "");
        citizen.fatherId = c.getString("father", "");
        citizen.spouseId = c.getString("spouse", "");
        citizen.householdId = c.getString("household", "");
        citizen.homeSettlementId = c.getString("settlement", "");
        citizen.deathDay = c.getInt("deathDay", -1);
        citizen.deathCause = c.getString("deathCause", "");
        citizen.alive = c.getBool("alive", true);
        citizen.manifested = c.getBool("manifested", false) && !citizen.entityUuid.isEmpty() && citizen.alive;
        try {
            citizen.profession = SimProfession.valueOf(c.getString("profession", "NONE"));
        } catch (IllegalArgumentException ignored) { }
        try {
            citizen.preferredProfession = SimProfession.valueOf(c.getString("preferred", "NONE"));
        } catch (IllegalArgumentException ignored) { }
        citizen.personalWealth = c.getDouble("wealth", 0.0);
        citizen.reputation = c.getInt("reputation", 50);
        citizen.lastChildBornDay = c.getInt("lastChild", -1000);
        citizen.lastSeenDay = c.getInt("lastSeen", -1);
        citizen.personality = Personality.load(c.getCompound("personality"));
        for (String id : c.getStringList("children")) citizen.childrenIds.add(id);
        for (String m : c.getStringList("memories")) citizen.memories.add(m);
        Compound skills = c.getCompound("skills");
        for (Skill s : Skill.values()) {
            int level = skills.getInt("l_" + s.name(), 0);
            if (level > 0) citizen.skillLevel.put(s, Integer.valueOf(level));
            double xp = skills.getDouble("x_" + s.name(), 0.0);
            if (xp > 0.0) citizen.skillXp.put(s, Double.valueOf(xp));
        }
        return citizen;
    }

    public static String pluralProfession(SimProfession p) {
        switch (p) {
            case FARMER: return "farmers";
            case LUMBERJACK: return "lumberjacks";
            case MINER: return "miners";
            case BUILDER: return "builders";
            case CRAFTER: return "crafters";
            case TRADER: return "traders";
            case SCHOLAR: return "scholars";
            case HEALER: return "healers";
            case GUARD: return "guards";
            case STEWARD: return "stewards";
            default: return "drifters";
        }
    }
}
