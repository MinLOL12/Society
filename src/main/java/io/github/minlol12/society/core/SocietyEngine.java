package io.github.minlol12.society.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import io.github.minlol12.society.core.data.ChronicleEntry;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Culture;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Government;
import io.github.minlol12.society.core.data.Household;
import io.github.minlol12.society.core.data.Personality;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.data.TradeRoute;
import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.system.ConstructionSystem;
import io.github.minlol12.society.core.system.DiplomacySystem;
import io.github.minlol12.society.core.system.EconomySystem;
import io.github.minlol12.society.core.system.EventSystem;
import io.github.minlol12.society.core.system.LifecycleSystem;
import io.github.minlol12.society.core.system.PoliticsSystem;
import io.github.minlol12.society.core.system.SettlementLocator;
import io.github.minlol12.society.core.types.Archetype;
import io.github.minlol12.society.core.types.CultureOrigin;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.GovernmentType;
import io.github.minlol12.society.core.types.Season;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;
import io.github.minlol12.society.core.types.TechNode;
import io.github.minlol12.society.core.util.NameGen;

/**
 * The facade of the whole simulation. Owns the ledger (citizens, families,
 * settlements, relations, routes, chronicles), advances it one day at a
 * time, and is the only door through which the Minecraft adapter speaks.
 *
 * <p>The engine and everything under {@code core} is deliberately free of
 * any Minecraft class so it can be compiled and exercised headlessly.</p>
 */
public final class SocietyEngine {

    private final EngineConfig config;
    private final Random random;

    private int currentDay;
    private Season currentSeason = Season.SPRING;

    private final Map<String, Settlement> settlements = new LinkedHashMap<String, Settlement>();
    private final Map<String, Citizen> citizens = new LinkedHashMap<String, Citizen>();
    private final Map<String, Household> households = new LinkedHashMap<String, Household>();
    private final List<DiplomaticRelation> relations = new ArrayList<DiplomaticRelation>();
    private final List<TradeRoute> routes = new ArrayList<TradeRoute>();
    private final List<ChronicleEntry> worldChronicle = new ArrayList<ChronicleEntry>();
    /** entity uuid string -> citizen id. */
    private final Map<String, String> entityToCitizen = new HashMap<String, String>();

    // --- Transient daily bookkeeping (not persisted) ------------------------
    private final Set<String> touchedToday = new HashSet<String>();
    private final Map<String, Integer> abandonedDays = new HashMap<String, Integer>();
    private final Map<String, Integer> announceBudget = new HashMap<String, Integer>();
    private final Map<String, Integer> lastPlagueDays = new HashMap<String, Integer>();
    private final Map<String, Integer> lastDepositionDays = new HashMap<String, Integer>();
    private final Map<String, Integer> lastFamineDays = new HashMap<String, Integer>();
    private final List<Announcement> pendingAnnouncements = new ArrayList<Announcement>();
    private final List<SpawnRequest> pendingSpawns = new ArrayList<SpawnRequest>();
    private boolean dirty;

    public SocietyEngine(long seed, EngineConfig config) {
        this.random = new Random(seed ^ 0x50C13C13L);
        this.config = config == null ? new EngineConfig() : config;
        this.currentDay = 0;
    }

    // =====================================================================
    // Basic accessors
    // =====================================================================

    public EngineConfig cfg() { return config; }

    public Random random() { return random; }

    public int day() { return currentDay; }

    public Season season() { return currentSeason; }

    public Map<String, Settlement> settlements() { return settlements; }

    public Map<String, Citizen> citizens() { return citizens; }

    public Map<String, Household> households() { return households; }

    public List<DiplomaticRelation> relations() { return relations; }

    public List<TradeRoute> routes() { return routes; }

    public List<ChronicleEntry> worldChronicle() { return worldChronicle; }

    public Set<String> touchedToday() { return touchedToday; }

    public Map<String, Integer> lastPlagueDays() { return lastPlagueDays; }

    public Map<String, Integer> lastDepositionDays() { return lastDepositionDays; }

    public Map<String, Integer> lastFamineDays() { return lastFamineDays; }

    public int settlementsAlive() {
        int count = 0;
        for (Settlement s : settlements.values()) {
            if (!s.isDestroyed()) count++;
        }
        return count;
    }

    public Citizen citizenForEntity(String entityUuid) {
        String id = entityToCitizen.get(entityUuid);
        if (id == null) return null;
        Citizen c = citizens.get(id);
        return c != null && c.isAlive() ? c : null;
    }

    public Citizen leaderOf(Settlement s) {
        if (s.government() == null) return null;
        return citizens.get(s.government().leaderId());
    }

    /** Living citizens of a settlement, in join order. */
    public List<Citizen> liveCitizensOf(Settlement s) {
        List<Citizen> out = new ArrayList<Citizen>();
        for (String id : s.citizenIds()) {
            Citizen c = citizens.get(id);
            if (c != null && c.isAlive()) out.add(c);
        }
        return out;
    }

    public List<Citizen> liveCitizensOf(String settlementId) {
        Settlement s = settlements.get(settlementId);
        return s == null ? new ArrayList<Citizen>() : liveCitizensOf(s);
    }

    public Settlement findSettlementNear(int x, int z, double maxDistance) {
        Settlement best = null;
        double bestDist = maxDistance;
        for (Settlement s : settlements.values()) {
            if (s.isDestroyed()) continue;
            double d = s.distanceTo(x, z);
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    public Settlement findSettlementByName(String query) {
        if (query == null) return null;
        String q = query.trim();
        for (Settlement s : settlements.values()) {
            if (s.name().equalsIgnoreCase(q)) return s;
        }
        for (Settlement s : settlements.values()) {
            if (s.name().toLowerCase().startsWith(q.toLowerCase())) return s;
        }
        return null;
    }

    public DiplomaticRelation findRelation(String aId, String bId) {
        for (DiplomaticRelation r : relations) {
            if ((r.aId().equals(aId) && r.bId().equals(bId))
                    || (r.aId().equals(bId) && r.bId().equals(aId))) {
                return r;
            }
        }
        return null;
    }

    public TradeRoute findRoute(String aId, String bId) {
        for (TradeRoute r : routes) {
            if ((r.aId().equals(aId) && r.bId().equals(bId))
                    || (r.aId().equals(bId) && r.bId().equals(aId))) {
                return r;
            }
        }
        return null;
    }

    public int routesInvolving(String settlementId) {
        int count = 0;
        for (TradeRoute r : routes) {
            if (r.involves(settlementId)) count++;
        }
        return count;
    }

    public List<DiplomaticRelation> relationsInvolving(String settlementId) {
        List<DiplomaticRelation> out = new ArrayList<DiplomaticRelation>();
        for (DiplomaticRelation r : relations) {
            if (r.involves(settlementId)) out.add(r);
        }
        return out;
    }

    public boolean isAtWar(String settlementId) {
        for (DiplomaticRelation r : relations) {
            if (r.treaty().atWar() && r.involves(settlementId)) return true;
        }
        return false;
    }

    /**
     * Forces two settlements into open war, no matter the distance or their
     * history. Used by a player wielding the War Baton: the first village
     * marked is pitted against the second. Creates the diplomatic relation if
     * the two have never met, then opens hostilities. Returns false when the
     * pair is invalid or already at war.
     */
    public boolean declareWar(Settlement a, Settlement b) {
        if (a == null || b == null || a.isDestroyed() || b.isDestroyed()
                || a.id().equals(b.id())) {
            return false;
        }
        DiplomaticRelation relation = findRelation(a.id(), b.id());
        if (relation == null) {
            relation = new DiplomaticRelation(a.id(), b.id(), currentDay);
            seedContactScore(relation);
            relations.add(relation);
        }
        if (relation.treaty().atWar()) {
            return false;
        }
        relation.setScore(-60.0);
        relation.beginWar(currentDay);
        a.addThreat(4.0);
        b.addThreat(4.0);
        a.culture().noteWar();
        b.culture().noteWar();
        recordBilateral(EventType.WAR_START, a, b,
                "War! At a player's decree, " + a.name() + " and " + b.name()
                        + " have taken up arms against one another.");
        markDirty();
        return true;
    }

    /** Initial cautious score for a relation a player's decree conjured up. */
    private void seedContactScore(DiplomaticRelation relation) {
        Settlement a = settlements.get(relation.aId());
        Settlement b = settlements.get(relation.bId());
        double initial = (a != null && b != null
                && a.culture().origin() == b.culture().origin()) ? 4.0 : 0.0;
        relation.setScore(initial);
    }

    // =====================================================================
    // World observation: villager snapshots
    // =====================================================================

    /**
     * Called the moment a villager entity loads into the world: binds an
     * existing citizen to it, or writes a new person into the ledger.
     */
    public Citizen onVillagerLoaded(VillagerSnapshot snapshot, CultureSampler sampler) {
        Citizen existing = citizenForEntity(snapshot.entityUuid);
        if (existing != null) {
            existing.noteSeen(currentDay);
            return existing;
        }
        return citizenForSnapshot(snapshot, sampler);
    }

    /**
     * Called when an entity the engine itself asked for appears in the
     * world (manifestation): binds the already-existing citizen instead of
     * writing a copy. Returns {@code null} when the pending citizen died or
     * was bound since the request; the world should discard that entity.
     */
    public Citizen onVillagerLoadedPending(String citizenId, VillagerSnapshot snapshot, CultureSampler sampler) {
        Citizen c = citizens.get(citizenId);
        if (c != null && c.isAlive() && c.entityUuid().isEmpty()) {
            bind(c, snapshot.entityUuid);
            c.noteSeen(currentDay);
            return c;
        }
        // The ledger page was turned while the body was in transit: no
        // phantom citizen may rise from it.
        return null;
    }

    /** Create-or-bind used by the locator and by direct entity loads. */
    public Citizen citizenForSnapshot(VillagerSnapshot snapshot, CultureSampler sampler) {
        Citizen existing = citizenForEntity(snapshot.entityUuid);
        if (existing != null) return existing;

        CultureOrigin origin = sampler.sample((int) snapshot.x, (int) snapshot.z);
        Citizen citizen;
        if (snapshot.baby) {
            citizen = createFoundling(snapshot, sampler, origin);
        } else {
            int age = 14 + random.nextInt(30);
            citizen = createAdultCitizen(origin, age, null);
        }
        SimProfession mapped = SimProfession.fromVanillaId(snapshot.professionId);
        if (mapped != SimProfession.NONE) {
            citizen.setProfession(mapped);
            if (citizen.preferredProfession() == SimProfession.NONE) {
                citizen.setPreferredProfession(mapped);
            }
        }
        bind(citizen, snapshot.entityUuid);
        citizen.noteSeen(currentDay);
        return citizen;
    }

    /** A baby villager appears: graft it onto a married couple nearby if any. */
    private Citizen createFoundling(VillagerSnapshot snapshot, CultureSampler sampler, CultureOrigin origin) {
        Settlement near = findSettlementNear((int) snapshot.x, (int) snapshot.z, 150);
        if (near != null) {
            List<Citizen> people = liveCitizensOf(near);
            for (Citizen a : people) {
                if (!a.isMarried() || !a.isAdult(currentDay)) continue;
                Citizen b = citizens.get(a.spouseId());
                if (b == null || !b.isAlive()) continue;
                if (currentDay - a.lastChildBornDay() < 2 || currentDay - b.lastChildBornDay() < 2) continue;
                return birthChild(a, b, near);
            }
        }
        // Otherwise: a foundling with a nameless past.
        String firstName = NameGen.firstName(origin, random);
        String familyName = NameGen.familyName(origin, random);
        Citizen child = new Citizen(nextCitizenId(), firstName, familyName, currentDay);
        child.setPersonality(Personality.random(random, origin.virtue()));
        child.setPreferredProfession(pickPreferredProfession(child.personality(), null));
        Household household = createHousehold(familyName, child);
        registerCitizen(child, household, near);
        child.addMemory(currentDay, near == null
                ? "was found alone in the wilds"
                : "was taken in at " + near.name() + " as a foundling");
        return child;
    }

    public void onVillagerDied(String entityUuid, String cause, boolean violent) {
        Citizen citizen = citizenForEntity(entityUuid);
        if (citizen != null) {
            handleCitizenDeath(citizen, cause, violent);
        } else {
            entityToCitizen.remove(entityUuid);
        }
    }

    public void onVillagerUnloaded(String entityUuid) {
        // Entities unload on every chunk unload; the ledger keeps the bond
        // and the ghost-sweep repairs only true disappearances.
    }

    private void bind(Citizen citizen, String entityUuid) {
        citizen.bindEntity(entityUuid);
        entityToCitizen.put(entityUuid, citizen.id());
    }

    // =====================================================================
    // Lifecycle entry points used by systems
    // =====================================================================

    // Martha Stewart would call this delegation "a good thing".
    private String nextCitizenId() {
        return UUID.randomUUID().toString();
    }

    /** A little credit for villagers working actual workstations. */
    public void noteWorkstationJob(Settlement settlement, Citizen citizen) {
        Skill primary = citizen.profession().primarySkill();
        if (primary != null) {
            citizen.gainXp(primary, 0.15);
        }
    }

    /** Logs a fresh adult with personality, household, name, starting skill. */
    public Citizen createAdultCitizen(CultureOrigin origin, int ageYears, Settlement settlement) {
        String firstName = NameGen.firstName(origin, random);
        String familyName = NameGen.familyName(origin, random);
        Citizen citizen = new Citizen(nextCitizenId(), firstName, familyName,
                currentDay - Math.max(0, ageYears));
        citizen.setPersonality(Personality.random(random, origin.virtue()));
        Household household = createHousehold(familyName, citizen);
        SimProfession preferred = pickPreferredProfession(citizen.personality(), household);
        citizen.setPreferredProfession(preferred);
        Skill primary = preferred.primarySkill();
        if (primary != null) {
            int start = Math.min(30, (int) (4 + ageYears * 0.5 + random.nextInt(6)));
            if (start > 0) {
                citizen.gainXp(primary, xpToReach(start));
            }
        }
        registerCitizen(citizen, household, settlement);
        return citizen;
    }

    private static double xpToReach(int level) {
        // Mirrors Citizen.xpForNext so starting levels look earned.
        double total = 0;
        for (int i = 0; i < level; i++) {
            total += 40.0 + i * 22.0;
        }
        return total;
    }

    private SimProfession pickPreferredProfession(Personality personality, Household household) {
        if (household != null && household.hereditaryProfession() != SimProfession.NONE) {
            return household.hereditaryProfession();
        }
        Archetype archetype = personality.archetype();
        SimProfession best = SimProfession.FARMER;
        double bestScore = -1;
        for (SimProfession p : SimProfession.values()) {
            if (p == SimProfession.NONE) continue;
            Skill primary = p.primarySkill();
            double score = (primary == null ? 1.0 : archetype.aptitude(primary))
                    * (0.5 + random.nextDouble());
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    /** A child born to two citizens of the same settlement. */
    public Citizen birthChild(Citizen mother, Citizen father, Settlement settlement) {
        if (liveCitizensOf(settlement).size() >= config.maxCitizensPerSettlement) {
            return null;
        }
        CultureOrigin origin = settlement.culture().origin();
        Household household = households.get(mother.householdId());
        String familyName = household == null ? father.familyName() : household.familyName();
        Citizen child = new Citizen(nextCitizenId(),
                NameGen.firstName(origin, random), familyName, currentDay);
        child.setPersonality(Personality.inherit(
                mother.personality(), father.personality(), random, origin.virtue()));
        child.setMotherId(mother.id());
        child.setFatherId(father.id());
        if (household != null) {
            household.addMember(child.id());
            child.setHouseholdId(household.id());
            SimProfession inherited = household.hereditaryProfession();
            child.setPreferredProfession(inherited != SimProfession.NONE
                    ? inherited : pickPreferredProfession(child.personality(), household));
        } else {
            Household created = createHousehold(familyName, child);
            child.setPreferredProfession(pickPreferredProfession(child.personality(), created));
        }
        registerCitizen(child, households.get(child.householdId()), settlement);
        mother.childrenIds().add(child.id());
        father.childrenIds().add(child.id());
        mother.noteChildBorn(currentDay);
        father.noteChildBorn(currentDay);
        mother.addMemory(currentDay, "welcomed " + child.firstName() + " into the world");
        father.addMemory(currentDay, "welcomed " + child.firstName() + " into the world");
        child.addMemory(currentDay, "was born in " + settlement.name());
        if (random.nextDouble() < 0.15) {
            announce(Announcement.Severity.LOCAL, settlement,
                    "A child, " + child.fullName() + ", is born to "
                            + mother.firstName() + " and " + father.firstName()
                            + " of " + settlement.name() + ".");
        }
        return child;
    }

    /** Creates and registers a household containing its founder. */
    public Household createHousehold(String familyName, Citizen founder) {
        Household household = new Household(UUID.randomUUID().toString(), familyName);
        households.put(household.id(), household);
        if (founder != null) {
            household.addMember(founder.id());
            founder.setHouseholdId(household.id());
            founder.setFamilyName(familyName);
        }
        return household;
    }

    private void registerCitizen(Citizen citizen, Household household, Settlement settlement) {
        citizens.put(citizen.id(), citizen);
        if (settlement != null && !settlement.isDestroyed()) {
            touchCitizenSettlement(citizen, settlement);
        }
    }

    /** Moves a citizen into a settlement's rolls (no history side-effects). */
    private void touchCitizenSettlement(Citizen citizen, Settlement settlement) {
        String old = citizen.homeSettlementId();
        if (old.equals(settlement.id())) return;
        Settlement oldSettlement = settlements.get(old);
        if (oldSettlement != null) {
            oldSettlement.citizenIds().remove(citizen.id());
        }
        citizen.setHomeSettlementId(settlement.id());
        if (!settlement.citizenIds().contains(citizen.id())) {
            settlement.citizenIds().add(citizen.id());
        }
    }

    /** Transfers a citizen between settlements, remembering the move. */
    public void transferCitizen(Citizen citizen, Settlement to) {
        String oldId = citizen.homeSettlementId();
        if (oldId.equals(to.id())) return;
        Settlement old = settlements.get(oldId);
        if (old != null) {
            citizen.addMemory(currentDay, "left " + old.name() + " for " + to.name());
            record(EventType.MIGRATION, old,
                    citizen.fullName() + " has left for " + to.name() + ".");
        }
        touchCitizenSettlement(citizen, to);
        markDirty();
    }

    /** The user has led villagers together; start a settlement around them. */
    public Settlement foundSettlement(List<Citizen> foundingMembers, int x, int y, int z, CultureSampler sampler) {
        CultureOrigin origin = sampler.sample(x, z);
        List<String> taken = new ArrayList<String>();
        for (Settlement s : settlements.values()) taken.add(s.name());
        String name = NameGen.uniqueSettlementName(origin, random, taken);
        Culture culture = new Culture(origin);
        Settlement settlement = new Settlement(UUID.randomUUID().toString(),
                name, currentDay, culture, x, y, z);

        Citizen founder = null;
        for (Citizen c : foundingMembers) {
            if (founder == null || c.reputation() > founder.reputation()
                    || c.birthDay() < founder.birthDay()) {
                founder = c;
            }
        }
        if (founder != null) {
            settlement.setFounderId(founder.id());
            settlement.setGovernment(new Government(GovernmentType.ELDER_COUNCIL, founder.id(), currentDay));
            culture.addFact("founded on Day " + currentDay + " by " + founder.fullName());
        }
        // Seed with whatever the founders carried in their pockets.
        int mouths = Math.max(1, foundingMembers.size());
        settlement.setStock(Good.FOOD, 6.0 * mouths);
        settlement.setStock(Good.WOOD, 2.0 * mouths);
        settlement.setStock(Good.TOOLS, 0.5 * mouths);
        settlement.addTreasury(10.0 + mouths * 2.0);
        settlements.put(settlement.id(), settlement);

        for (Citizen c : new ArrayList<Citizen>(foundingMembers)) {
            touchCitizenSettlement(c, settlement);
            c.addMemory(currentDay, "raised the first fires of " + name);
            c.addReputation(5);
        }

        record(EventType.FOUNDING, settlement,
                "The " + origin.folkName().toLowerCase() + " of " + name
                        + " raise their first fires; a " + settlement.tier().display().toLowerCase()
                        + " is born among the " + origin.buildingStyle() + " to come.");
        if (settlementsAlive() == 1) {
            announce(Announcement.Severity.GLOBAL, settlement,
                    "Somewhere, civilization begins: " + name + " has been founded.");
        }
        markDirty();
        return settlement;
    }

    // =====================================================================
    // Deaths
    // =====================================================================

    /**
     * Every death, whatever its cause, passes through here: bonds break,
     * property moves down the family line, and the famous earn obituaries.
     */
    public void handleCitizenDeath(Citizen citizen, String cause, boolean violent) {
        if (citizen == null || !citizen.isAlive()) return;
        int day = currentDay;
        String entityUuid = citizen.entityUuid();
        Settlement s = settlements.get(citizen.homeSettlementId());

        citizen.die(day, cause);
        if (!entityUuid.isEmpty()) {
            entityToCitizen.remove(entityUuid);
        }
        if (s != null) {
            s.citizenIds().remove(citizen.id());
        }

        // Spouse and children grieve.
        Citizen spouse = citizens.get(citizen.spouseId());
        if (spouse != null && spouse.isAlive()) {
            spouse.setSpouseId("");
            spouse.addMemory(day, "lost " + citizen.fullName() + ", who " + cause);
        }
        for (String childId : citizen.childrenIds()) {
            Citizen child = citizens.get(childId);
            if (child != null && child.isAlive()) {
                child.addMemory(day,
                        (child.fatherId().equals(citizen.id()) ? "father" : "mother")
                                + " " + citizen.fullName() + " " + cause);
            }
        }

        // Property descends: half to the children grown, the rest to the house.
        double wealth = citizen.personalWealth();
        List<Citizen> heirs = new ArrayList<Citizen>();
        for (String childId : citizen.childrenIds()) {
            Citizen child = citizens.get(childId);
            if (child != null && child.isAlive() && child.isAdult(day)) heirs.add(child);
        }
        double householdShare = wealth;
        if (!heirs.isEmpty()) {
            double perHeir = wealth * 0.5 / heirs.size();
            householdShare = wealth * 0.5;
            for (Citizen heir : heirs) {
                heir.addWealth(perHeir);
                heir.addMemory(day, "inherited " + String.format("%.1f", perHeir)
                        + " from " + citizen.fullName());
            }
        }

        Household household = households.get(citizen.householdId());
        if (household != null) {
            household.removeMember(citizen.id());
            household.noteProfession(citizen.profession());
            household.addWealth(householdShare);
            boolean anyLiving = false;
            for (String memberId : household.memberIds()) {
                Citizen member = citizens.get(memberId);
                if (member != null && member.isAlive()) {
                    anyLiving = true;
                    break;
                }
            }
            if (!anyLiving) {
                if (s != null) {
                    s.addTreasury(household.wealth());
                }
                households.remove(household.id());
            }
        } else if (wealth > 0 && s != null) {
            s.addTreasury(wealth);
        }

        // History: the famous get obituaries, everyone else is mourned in
        // family memories only.
        if (s != null && citizen.isFamous()) {
            String note = citizen.fullName() + ", " + describeStanding(citizen, s)
                    + ", " + cause + " at the age of " + citizen.ageYears(day) + ".";
            record(EventType.HERO_DEATH, s, note);
        }
        if (violent && s != null) {
            s.addThreat(1.0);
        }
        markDirty();
    }

    private String describeStanding(Citizen citizen, Settlement s) {
        Government government = s.government();
        if (government != null && citizen.id().equals(government.leaderId())) {
            return government.type().leaderTitle() + " of " + s.name();
        }
        Skill best = citizen.bestSkill();
        if (best != null && citizen.bestSkillLevel() >= 75) {
            return "master of " + best.display() + " in " + s.name();
        }
        Archetype archetype = citizen.personality().archetype();
        return archetype.display().toLowerCase() + " of " + s.name();
    }

    // =====================================================================
    // Discoveries, culture
    // =====================================================================

    /** A technology was just completed. */
    public void onDiscovery(Settlement s, Citizen scholar, TechNode node) {
        int day = currentDay;
        s.culture().noteDiscovery();
        s.culture().addFact("discovered " + node.display() + " on Day " + day);
        String text = "The scholars of " + s.name()
                + (scholar == null ? "" : ", led by " + scholar.fullName() + ",")
                + " have discovered " + node.display() + " - " + node.effect() + ".";
        record(EventType.DISCOVERY, s, text);
        if (scholar != null) {
            scholar.addReputation(8);
            scholar.addMemory(day, "helped unravel " + node.display());
        }
    }

    // =====================================================================
    // Manifestation (ledger -> flesh)
    // =====================================================================

    public boolean canManifest(Settlement s) {
        if (!config.enableManifestSpawns) return false;
        List<Citizen> people = liveCitizensOf(s);
        int manifested = 0;
        for (Citizen c : people) {
            if (c.isManifested()) manifested++;
        }
        return manifested < Math.min(people.size(), EconomySystem.ENTITY_CAP);
    }

    public void queueSpawn(Settlement s, Citizen citizen, boolean baby) {
        if (!config.enableManifestSpawns) return;
        double jitterX = (random.nextDouble() - 0.5) * 8;
        double jitterZ = (random.nextDouble() - 0.5) * 8;
        pendingSpawns.add(new SpawnRequest(s.id(), citizen.id(),
                s.centerX() + jitterX, s.centerY(), s.centerZ() + jitterZ, baby));
    }

    /** Each day, a couple of ledger-only souls put on bodies if players are near. */
    public void requestManifestations(Settlement s, List<Citizen> people) {
        if (!config.enableManifestSpawns) return;
        int manifested = 0;
        int population = people.size();
        for (Citizen c : people) {
            if (c.isManifested()) manifested++;
        }
        int cap = Math.min(population, EconomySystem.ENTITY_CAP);
        int budget = 2;
        for (Citizen c : people) {
            if (budget <= 0 || manifested >= cap) break;
            if (c.isManifested() || !c.isAlive()) continue;
            queueSpawn(s, c, c.ageYears(currentDay) < 16);
            manifested++;
            budget--;
        }
    }

    /** Picks where an unhappy emigrant of {@code from} would actually go. */
    public Settlement bestMigrationTarget(Settlement from, Citizen leaver) {
        Settlement best = null;
        double bestScore = 5.0;
        for (DiplomaticRelation relation : relationsInvolving(from.id())) {
            if (relation.treaty().atWar()) continue;
            String otherId = relation.other(from.id());
            Settlement other = otherId == null ? null : settlements.get(otherId);
            if (other == null || other.isDestroyed()) continue;
            if (other.stock(Good.FOOD) < other.cachedPopulation() * 2.0) continue;
            if (other.cachedPopulation() >= config.maxCitizensPerSettlement) continue;
            double score = relation.score() + other.morale() / 10.0;
            if (score > bestScore) {
                bestScore = score;
                best = other;
            }
        }
        return best;
    }

    // =====================================================================
    // Announcing & chronicles
    // =====================================================================

    /**
     * Records an event: local events go into the settlement chronicle,
     * every non-private event also enters the world chronicle, and notable
     * ones are announced to nearby players.
     */
    public void record(EventType type, Settlement settlement, String text) {
        ChronicleEntry entry = new ChronicleEntry(currentDay, type,
                settlement == null ? "" : settlement.id(), text);
        if (type.level() != EventType.Level.MEMORY) {
            worldChronicle.add(entry);
            while (worldChronicle.size() > 1500) {
                worldChronicle.remove(0);
            }
        }
        if (settlement != null && type.level() != EventType.Level.MEMORY) {
            settlement.addChronicle(entry);
        }
        if (type.level() == EventType.Level.GLOBAL) {
            announce(Announcement.Severity.GLOBAL, settlement, text);
        } else if (type.level() == EventType.Level.LOCAL && settlement != null) {
            announceBudgeted(settlement, text);
        }
        markDirty();
    }

    /** One event that belongs equally to two settlements and to the world. */
    public void recordBilateral(EventType type, Settlement a, Settlement b, String text) {
        ChronicleEntry entry = new ChronicleEntry(currentDay, type, a.id(), text);
        a.addChronicle(entry);
        b.addChronicle(entry);
        worldChronicle.add(entry);
        while (worldChronicle.size() > 1500) {
            worldChronicle.remove(0);
        }
        if (type.level() == EventType.Level.GLOBAL) {
            announce(Announcement.Severity.GLOBAL, a, text);
        } else {
            // One shared announcement for both sides; players between two
            // settlements shouldn't hear everything twice.
            announceBudgeted(a, text);
        }
        markDirty();
    }

    private void announceBudgeted(Settlement s, String text) {
        if (!config.announcements) return;
        Integer used = announceBudget.get(s.id());
        int spent = used == null ? 0 : used.intValue();
        if (spent >= config.dailyAnnouncementBudget) return;
        announceBudget.put(s.id(), Integer.valueOf(spent + 1));
        announce(Announcement.Severity.LOCAL, s, text);
    }

    public void announce(Announcement.Severity severity, Settlement settlement, String text) {
        if (!config.announcements && severity != Announcement.Severity.NONE) return;
        double x = settlement == null ? 0 : settlement.centerX();
        double z = settlement == null ? 0 : settlement.centerZ();
        pendingAnnouncements.add(new Announcement(severity, x, z, text,
                settlement == null ? "" : settlement.name()));
    }

    public List<Announcement> drainAnnouncements() {
        List<Announcement> out = new ArrayList<Announcement>(pendingAnnouncements);
        pendingAnnouncements.clear();
        return out;
    }

    public List<SpawnRequest> drainSpawns() {
        List<SpawnRequest> out = new ArrayList<SpawnRequest>(pendingSpawns);
        pendingSpawns.clear();
        return out;
    }

    public boolean isDirty() { return dirty; }

    public void markDirty() { dirty = true; }

    public void clearDirty() { dirty = false; }

    // =====================================================================
    // Daily tick
    // =====================================================================

    /**
     * Aligns the ledger with whatever villagers are loaded right now,
     * advances day-by-day to the world's own day, then lets every system
     * run. Called once per server tick by the adapter; the day loop guards
     * against long sleeps.
     */
    public void processDaily(List<VillagerSnapshot> snapshots, CultureSampler sampler, DayContext context) {
        if (currentDay == 0 && worldChronicle.isEmpty() && context.worldDay > 0) {
            currentDay = context.worldDay; // fresh ledger on an older world
        }
        SettlementLocator.processDaily(this, snapshots, sampler);

        int catchUp = 0;
        while (currentDay < context.worldDay && catchUp < 5) {
            tickOneDay(context);
            catchUp++;
        }
        if (currentDay < context.worldDay) {
            currentDay = context.worldDay; // graciously skip very long sleeps
        }
    }

    private void tickOneDay(DayContext context) {
        currentDay++;
        currentSeason = Season.ofDay(currentDay, config.seasonLengthDays);
        announceBudget.clear();

        int seasonIndex = currentDay / config.seasonLengthDays;

        // 1. Politics first: laws and leaders colour everything below.
        for (Settlement s : new ArrayList<Settlement>(settlements.values())) {
            if (s.isDestroyed()) continue;
            PoliticsSystem.tick(this, s);
        }

        // 2. Diplomacy across the whole map.
        DiplomacySystem.tick(this);

        // 3. Household and hearth systems per settlement.
        for (Settlement s : new ArrayList<Settlement>(settlements.values())) {
            if (s.isDestroyed()) continue;
            EconomySystem.tick(this, s, currentSeason);
            // Roofs before children: the town raises what it needs, and
            // its real beds decide whether it can grow at all.
            ConstructionSystem.tick(this, s);
            LifecycleSystem.tick(this, s);
            EventSystem.tick(this, s, context.raining, seasonIndex);
        }

        // 4. Abandonment and cleanup.
        for (Settlement s : new ArrayList<Settlement>(settlements.values())) {
            if (s.isDestroyed()) continue;
            if (liveCitizensOf(s).isEmpty()) {
                Integer days = abandonedDays.get(s.id());
                int left = days == null ? 1 : days.intValue() + 1;
                abandonedDays.put(s.id(), Integer.valueOf(left));
                if (left > 5) {
                    s.setDestroyed(true);
                    record(EventType.TIER_DOWN, s,
                            s.name() + " stands silent - its last hearth has gone cold.");
                }
            } else {
                abandonedDays.remove(s.id());
            }
        }
        pruneDeadBonds();
    }

    /** Households and entity bindings for citizens that no longer exist. */
    private void pruneDeadBonds() {
        for (String entityUuid : new ArrayList<String>(entityToCitizen.keySet())) {
            String id = entityToCitizen.get(entityUuid);
            Citizen c = citizens.get(id);
            if (c == null || !c.isAlive() || !entityUuid.equals(c.entityUuid())) {
                entityToCitizen.remove(entityUuid);
            }
        }
        // Dead citizens keep their ledger pages (genealogy!), but trim very
        // old non-famous dead to keep saves light.
        int dead = 0;
        for (Citizen c : citizens.values()) {
            if (!c.isAlive()) dead++;
        }
        if (dead > 600) {
            int toRemove = dead - 600;
            for (Citizen c : new ArrayList<Citizen>(citizens.values())) {
                if (toRemove <= 0) break;
                if (!c.isAlive() && !c.isFamous()
                        && currentDay - c.deathDay() > 200) {
                    citizens.remove(c.id());
                    toRemove--;
                }
            }
        }
    }

    // =====================================================================
    // Persistence
    // =====================================================================

    public Compound save() {
        Compound root = new Compound()
                .put("day", currentDay);
        List<Compound> settlementList = new ArrayList<Compound>();
        for (Settlement s : settlements.values()) settlementList.add(s.save());
        root.putCompoundList("settlements", settlementList);
        List<Compound> citizenList = new ArrayList<Compound>();
        for (Citizen c : citizens.values()) citizenList.add(c.save());
        root.putCompoundList("citizens", citizenList);
        List<Compound> householdList = new ArrayList<Compound>();
        for (Household h : households.values()) householdList.add(h.save());
        root.putCompoundList("households", householdList);
        List<Compound> relationList = new ArrayList<Compound>();
        for (DiplomaticRelation r : relations) relationList.add(r.save());
        root.putCompoundList("relations", relationList);
        List<Compound> routeList = new ArrayList<Compound>();
        for (TradeRoute r : routes) routeList.add(r.save());
        root.putCompoundList("routes", routeList);
        List<Compound> chronicleList = new ArrayList<Compound>();
        for (ChronicleEntry e : worldChronicle) chronicleList.add(e.save());
        root.putCompoundList("chronicle", chronicleList);
        Compound entityMap = new Compound();
        for (Map.Entry<String, String> e : entityToCitizen.entrySet()) {
            entityMap.put(e.getKey(), e.getValue());
        }
        root.put("entityMap", entityMap);
        return root;
    }

    public void load(Compound root) {
        currentDay = root.getInt("day", 0);
        settlements.clear();
        citizens.clear();
        households.clear();
        relations.clear();
        routes.clear();
        worldChronicle.clear();
        entityToCitizen.clear();
        for (Compound c : root.getCompoundList("settlements")) {
            Settlement s = Settlement.load(c);
            if (!s.id().isEmpty()) settlements.put(s.id(), s);
        }
        for (Compound c : root.getCompoundList("citizens")) {
            Citizen citizen = Citizen.load(c);
            if (!citizen.id().isEmpty()) citizens.put(citizen.id(), citizen);
        }
        for (Compound c : root.getCompoundList("households")) {
            Household h = Household.load(c);
            if (!h.id().isEmpty()) households.put(h.id(), h);
        }
        for (Compound c : root.getCompoundList("relations")) {
            DiplomaticRelation r = DiplomaticRelation.load(c);
            relations.add(r);
        }
        for (Compound c : root.getCompoundList("routes")) {
            routes.add(TradeRoute.load(c));
        }
        for (Compound c : root.getCompoundList("chronicle")) {
            worldChronicle.add(ChronicleEntry.load(c));
        }
        Compound entityMap = root.getCompound("entityMap");
        for (Map.Entry<String, Object> e : entityMap.raw().entrySet()) {
            if (e.getValue() instanceof String) {
                entityToCitizen.put(e.getKey(), (String) e.getValue());
            }
        }
        // Referential integrity: prune dangling links one way or the other.
        for (Settlement s : settlements.values()) {
            List<String> keep = new ArrayList<String>();
            for (String id : s.citizenIds()) {
                Citizen c = citizens.get(id);
                if (c != null && c.isAlive() && s.id().equals(c.homeSettlementId())) {
                    keep.add(id);
                }
            }
            s.citizenIds().clear();
            s.citizenIds().addAll(keep);
        }
        relations.removeIf(r -> {
            Settlement a = settlements.get(r.aId());
            Settlement b = settlements.get(r.bId());
            return a == null || b == null || a.isDestroyed() || b.isDestroyed();
        });
        routes.removeIf(r -> {
            Settlement a = settlements.get(r.aId());
            Settlement b = settlements.get(r.bId());
            return a == null || b == null || a.isDestroyed() || b.isDestroyed();
        });
        pruneDeadBonds();
    }
}
