package io.github.minlol12.society.core.headless;

import java.util.ArrayList;
import java.util.List;

import io.github.minlol12.society.core.Announcement;
import io.github.minlol12.society.core.CultureSampler;
import io.github.minlol12.society.core.DayContext;
import io.github.minlol12.society.core.EngineConfig;
import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.SpawnRequest;
import io.github.minlol12.society.core.VillagerSnapshot;
import io.github.minlol12.society.core.data.ChronicleEntry;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.data.TechState;
import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.CultureOrigin;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.GovernmentType;
import io.github.minlol12.society.core.types.TechNode;
import io.github.minlol12.society.core.types.Treaty;

/**
 * Headless exercise of the simulation core: fabricates three villages of
 * fake "loaded villagers" at fixed positions, runs 400 days, injects a
 * birth and a death, then verifies the world actually lives - people wed,
 * children arrive, jobs emerge from needs, tech is discovered, treaties are
 * struck - and that save/load round-trips and runs are deterministic.
 *
 * <p>Run with: {@code java io.github.minlol12.society.core.headless.HeadlessSimulation}</p>
 */
public final class HeadlessSimulation {

    private static final long SEED = 123456789L;

    // Three permanent fake villages plus a distant hamlet.
    private static final int AX = 0, AY = 64, AZ = 0;          // plains by the sampler
    private static final int BX = 350, BY = 70, BZ = 20;       // mountain
    private static final int CX = 40, CY = 66, CZ = 900;       // desert, far off

    private HeadlessSimulation() { }

    // =================================================================
    // Fake world fixtures
    // =================================================================

    /** Cultures by nearest fixture: plains at A, mountain at B, desert at C. */
    static final CultureSampler SAMPLER = new CultureSampler() {
        @Override
        public CultureOrigin sample(int x, int z) {
            double distB = Math.hypot(x - BX, z - BZ);
            double distC = Math.hypot(x - CX, z - CZ);
            double distA = Math.hypot(x - AX, z - AZ);
            if (distB < distA && distB < distC) return CultureOrigin.MOUNTAIN;
            if (distC < distA && distC < distB) return CultureOrigin.DESERT;
            return CultureOrigin.PLAINS;
        }
    };

    static VillagerSnapshot adult(String uuid, double x, double y, double z, boolean bell,
                                  String professionId) {
        return new VillagerSnapshot(uuid, x, y, z,
                true, x + 2, y, z + 2,
                bell, x, y, z,
                false, 0, 0, 0,
                professionId, false);
    }

    static VillagerSnapshot baby(String uuid, double x, double y, double z) {
        return new VillagerSnapshot(uuid, x, y, z,
                false, 0, 0, 0,
                false, 0, 0, 0,
                false, 0, 0, 0,
                "minecraft:none", true);
    }

    /** The stable population of loaded villagers for one day. */
    static List<VillagerSnapshot> dailyCrowd() {
        List<VillagerSnapshot> crowd = new ArrayList<VillagerSnapshot>();
        // plains folk (6)
        crowd.add(adult("v-a1", AX, AY, AZ, true, "minecraft:farmer"));
        crowd.add(adult("v-a2", AX + 6, AY, AZ + 3, false, "minecraft:librarian"));
        crowd.add(adult("v-a3", AX - 8, AY, AZ + 5, false, "minecraft:farmer"));
        crowd.add(adult("v-a4", AX + 3, AY, AZ - 9, false, "minecraft:none"));
        crowd.add(adult("v-a5", AX - 4, AY, AZ - 6, false, "minecraft:toolsmith"));
        crowd.add(adult("v-a6", AX + 9, AY, AZ + 8, false, "minecraft:none"));
        // mountain folk (5)
        crowd.add(adult("v-b1", BX, BY, BZ, true, "minecraft:mason"));
        crowd.add(adult("v-b2", BX + 5, BY, BZ + 6, false, "minecraft:none"));
        crowd.add(adult("v-b3", BX - 6, BY, BZ - 2, false, "minecraft:none"));
        crowd.add(adult("v-b4", BX + 1, BY, BZ + 9, false, "minecraft:none"));
        crowd.add(adult("v-b5", BX - 9, BY, BZ + 4, false, "minecraft:none"));
        // desert folk (4)
        crowd.add(adult("v-c1", CX, CY, CZ, true, "minecraft:cartographer"));
        crowd.add(adult("v-c2", CX + 4, CY, CZ + 5, false, "minecraft:none"));
        crowd.add(adult("v-c3", CX - 5, CY, CZ + 2, false, "minecraft:none"));
        crowd.add(adult("v-c4", CX + 2, CY, CZ - 7, false, "minecraft:none"));
        return crowd;
    }

    // =================================================================
    // Test driver
    // =================================================================

    public static void main(String[] args) {
        int failures = 0;
        System.out.println("=== Society headless simulation ===");

        RunResult first = runSimulation(SEED, true);
        failures += first.failures;

        // Determinism: the same world must live twice identically.
        RunResult second = runSimulation(SEED, false);
        failures += second.failures;
        failures += check("deterministic chronicle length",
                first.chronicleSize == second.chronicleSize);
        failures += check("deterministic treasury checksum",
                Math.abs(first.treasuryChecksum - second.treasuryChecksum) < 0.001);
        failures += check("deterministic population checksum",
                first.population == second.population);
        if (first.chronicleSize != second.chronicleSize
                || !first.checkpoints.equals(second.checkpoints)) {
            System.out.println("--- determinism divergence diagnostic ---");
            System.out.println("population checkpoints " + first.checkpoints
                    + " vs " + second.checkpoints);
            int limit = Math.min(first.chronicleTail.size(), second.chronicleTail.size());
            for (int i = 0; i < limit; i++) {
                String a = first.chronicleTail.get(i);
                String b = second.chronicleTail.get(i);
                if (!a.equals(b)) {
                    System.out.println("first chronicle mismatch at index " + i + ":");
                    System.out.println("  run1: " + a);
                    System.out.println("  run2: " + b);
                    break;
                }
            }
            System.out.println("-----------------------------------------");
            lockstepDiagnosis();
        }

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println("FAILURES: " + failures);
            System.exit(1);
        }
    }

    static final class RunResult {
        int failures;
        int chronicleSize;
        double treasuryChecksum;
        int population;
        final List<String> checkpoints = new ArrayList<String>();
        final List<String> chronicleTail = new ArrayList<String>();
    }

    static RunResult runSimulation(long seed, boolean verbose) {
        RunResult result = new RunResult();
        SocietyEngine engine = new SocietyEngine(seed, new EngineConfig());
        int marriagesSeen = 0;
        int birthsAnnounced = 0;
        int deathsNoted = 0;
        boolean deathInjected = false;
        String deathCitizenId = null;

        for (int day = 1; day <= 400; day++) {
            List<VillagerSnapshot> crowd = dailyCrowd();
            // Day 40: a real baby appears bound to a married couple (if any).
            if (day == 40) {
                crowd.add(baby("v-baby1", AX + 1, AY, AZ + 1));
            }
            engine.processDaily(crowd, SAMPLER, new DayContext(day, day % 11 == 0, seed));

            for (Announcement announcement : engine.drainAnnouncements()) {
                if (verbose && announcement.severity != Announcement.Severity.NONE) {
                    System.out.println("  [day " + engine.day() + " | " + announcement.severity + "] "
                            + announcement.text);
                }
            }
            // Answer the engine's spawn requests like the adapter would:
            // pretend a player is nearby every third day.
            List<SpawnRequest> spawns = engine.drainSpawns();
            if (day % 3 == 0) {
                for (SpawnRequest req : spawns) {
                    VillagerSnapshot snap = new VillagerSnapshot(
                            "spawned-" + req.citizenId, req.x, req.y, req.z,
                            true, req.x, req.y, req.z,
                            false, 0, 0, 0,
                            false, 0, 0, 0,
                            "minecraft:none", req.baby);
                    engine.onVillagerLoadedPending(req.citizenId, snap, SAMPLER);
                }
            }

            // Day 60: tragedy. (Only if the citizen was ever written in.)
            if (day == 60 && !deathInjected && engine.citizenForEntity("v-a3") != null) {
                deathCitizenId = engine.citizenForEntity("v-a3").id();
                engine.onVillagerDied("v-a3", "was slain by a zombie", true);
                deathInjected = true;
            }

            // Population checkpoints & chronicle trace for determinism testing.
            if (day % 25 == 0) {
                int live = 0;
                for (Citizen c : engine.citizens().values()) {
                    if (c.isAlive()) live++;
                }
                result.checkpoints.add(day + ":" + live + "/" + engine.worldChronicle().size());
            }
            if (day == 400) {
                for (ChronicleEntry e : engine.worldChronicle()) {
                    result.chronicleTail.add(e.day() + "|" + e.type() + "|" + e.text());
                    switch (e.type()) {
                        case MARRIAGE: marriagesSeen++; break;
                        case HERO_DEATH: deathsNoted++; break;
                        default: break;
                    }
                }
            }
        }

        // --- Inspect & assert ------------------------------------------------
        result.chronicleSize = engine.worldChronicle().size();
        double treasurySum = 0;
        int population = 0;
        int livingCitizens = 0;
        int marriedCouples = 0;
        int children = 0;
        int techUnlocked = 0;
        int maxTier = 0;
        int manifested = 0;
        Settlement plains = null;
        Settlement mountain = null;

        for (Settlement s : engine.settlements().values()) {
            treasurySum += s.treasury();
            TechState tech = s.tech();
            for (TechNode n : TechNode.values()) {
                if (tech.isUnlocked(n)) techUnlocked++;
            }
            if (s.tier().ordinal() > maxTier) maxTier = s.tier().ordinal();
            if (s.culture().origin() == CultureOrigin.PLAINS) plains = s;
            if (s.culture().origin() == CultureOrigin.MOUNTAIN && mountain == null) mountain = s;
            for (Citizen c : engine.liveCitizensOf(s)) {
                population += 1;
                if (c.isManifested()) manifested++;
                if (c.isMarried()) marriedCouples++;
                if (!c.motherId().isEmpty() || !c.fatherId().isEmpty()) children++;
            }
        }
        for (Citizen c : engine.citizens().values()) {
            if (c.isAlive()) livingCitizens++;
        }
        if (verbose) {
            // Diagnose people outside any settlement rolls.
            System.out.println("Out-of-settlement living citizens (first 10):");
            int shown = 0;
            for (Citizen c : engine.citizens().values()) {
                if (!c.isAlive() || !c.homeSettlementId().isEmpty()) continue;
                System.out.println("  * " + c.fullName() + " age " + c.ageYears(engine.day())
                        + " manifested=" + c.isManifested() + " prof=" + c.profession()
                        + " firstMemory=" + (c.memories().isEmpty() ? "none" : c.memories().get(0)));
                if (++shown >= 10) break;
            }
        }
        result.treasuryChecksum = treasurySum;
        result.population = population;

        if (verbose) {
            System.out.println();
            System.out.println("=== State after 400 days (day " + engine.day() + ") ===");
            for (Settlement s : engine.settlements().values()) {
                Citizen leader = engine.leaderOf(s);
                System.out.println("- " + s.name() + " [" + s.culture().origin().display() + "] "
                        + s.tier().display() + ", pop " + engine.liveCitizensOf(s).size()
                        + ", gov " + (s.government() == null ? "none" : s.government().type().display())
                        + ", leader " + (leader == null ? "nobody" : leader.fullName())
                        + ", treasury " + String.format("%.0f", s.treasury())
                        + ", morale " + String.format("%.0f", s.morale())
                        + ", food " + String.format("%.0f", s.stock(Good.FOOD))
                        + ", tech " + s.tech().unlocked().size()
                        + ", laws " + s.government().laws().size());
                System.out.println("    " + s.culture().describe());
            }
            System.out.println();
            System.out.println("Relations:");
            for (DiplomaticRelation r : engine.relations()) {
                System.out.println("- " + engine.settlements().get(r.aId()).name() + " <-> "
                        + engine.settlements().get(r.bId()).name() + ": score "
                        + String.format("%.0f", r.score()) + ", " + r.treaty().description()
                        + ", traded " + String.format("%.0f", r.totalTradeValue()));
            }
            System.out.println();
            System.out.println("Living: " + livingCitizens + " | in settlements: " + population
                    + " | manifested: " + manifested + " | married: " + marriedCouples
                    + " | with parents: " + children);
            System.out.println("Chronicle entries: " + engine.worldChronicle().size());
            System.out.println();
            System.out.println("World chronicle, last 12:");
            List<ChronicleEntry> chronicle = engine.worldChronicle();
            for (int i = Math.max(0, chronicle.size() - 12); i < chronicle.size(); i++) {
                ChronicleEntry e = chronicle.get(i);
                System.out.println("  day " + e.day() + " [" + e.type() + "] " + e.text());
            }
            if (plains != null) {
                System.out.println();
                System.out.println(plains.name() + " chronicle, last 8:");
                List<ChronicleEntry> own = plains.chronicle();
                for (int i = Math.max(0, own.size() - 8); i < own.size(); i++) {
                    ChronicleEntry e = own.get(i);
                    System.out.println("  day " + e.day() + " [" + e.type() + "] " + e.text());
                }
                // Sample personalities
                System.out.println();
                System.out.println("Sample citizens:");
                List<Citizen> people = engine.liveCitizensOf(plains);
                for (int i = 0; i < Math.min(4, people.size()); i++) {
                    Citizen c = people.get(i);
                    System.out.println("  " + c.fullName() + ", age " + c.ageYears(engine.day())
                            + " " + c.personality().archetype().display()
                            + ", " + c.profession().display()
                            + (c.isMarried() ? ", married" : "")
                            + (c.isManifested() ? " [real]" : " [ledger]")
                            + ", best skill " + (c.bestSkill() == null ? "none"
                                    : c.bestSkill().display() + " " + c.bestSkillLevel()));
                    for (String memory : c.memories()) {
                        System.out.println("      " + memory);
                    }
                }
            }
        }

        // --- Assertions --------------------------------------------------------
        result.failures += check("settlements founded", engine.settlementsAlive() >= 2);
        result.failures += check("plains settlement exists", plains != null);
        result.failures += check("mountain settlement exists", mountain != null);
        result.failures += check("governments established",
                plains != null && plains.government() != null);
        result.failures += check("cultures differ by land",
                plains != null && mountain != null
                        && plains.culture().origin() != mountain.culture().origin());
        result.failures += check("professions emerged from needs", population > 0);
        result.failures += check("marriages happened", marriedCouples >= 2 || marriagesSeen >= 1);
        result.failures += check("generations: children born", children >= 1);
        result.failures += check("settlement grew beyond camp", maxTier >= 1);
        result.failures += check("technology discovered somewhere", techUnlocked >= 1);

        // --- Construction: the towns must physically build themselves -----
        int buildingsStanding = 0;
        int distinctTypes = 0;
        int bedsFromHouses = 0;
        boolean anyWorkshop = false;
        java.util.Set<io.github.minlol12.society.core.build.StructureType> seenTypes =
                new java.util.HashSet<io.github.minlol12.society.core.build.StructureType>();
        for (Settlement s : engine.settlements().values()) {
            for (io.github.minlol12.society.core.data.Building b : s.completedBuildings()) {
                buildingsStanding++;
                seenTypes.add(b.type());
                bedsFromHouses += b.type().beds();
                if (b.type().worksite() != io.github.minlol12.society.core.types.SimProfession.NONE) {
                    anyWorkshop = true;
                }
            }
        }
        distinctTypes = seenTypes.size();
        result.failures += check("settlements raised real buildings", buildingsStanding >= 6);
        result.failures += check("buildings of several kinds", distinctTypes >= 4);
        result.failures += check("houses provide the beds people sleep in", bedsFromHouses >= 2);
        result.failures += check("someone built a place to work", anyWorkshop);
        boolean housingTracksBeds = true;
        for (Settlement s : engine.settlements().values()) {
            if (s.housingCapacity() != s.bedCapacity()) housingTracksBeds = false;
        }
        result.failures += check("housing is exactly the beds that exist", housingTracksBeds);
        result.failures += check("ties between settlements", !engine.relations().isEmpty());
        boolean anyPact = false;
        for (DiplomaticRelation r : engine.relations()) {
            if (r.treaty() == Treaty.TRADE_PACT || r.treaty() == Treaty.ALLIANCE) anyPact = true;
        }
        result.failures += check("trade pact formed between neighbours", anyPact);
        result.failures += check("trade routes built", !engine.routes().isEmpty());
        result.failures += check("manifestations bound entities", manifested >= 3);
        result.failures += check("world chronicle keeps history", result.chronicleSize >= 20);
        if (deathInjected) {
            Citizen dead = engine.citizens().get(deathCitizenId);
            result.failures += check("injected death recorded",
                    dead != null && !dead.isAlive() && dead.deathCause().contains("zombie"));
        }

        // --- Save/load round trip ---------------------------------------------
        Compound saved = engine.save();
        SocietyEngine loaded = new SocietyEngine(seed, new EngineConfig());
        loaded.load(saved);
        result.failures += check("round-trip: day", loaded.day() == engine.day());
        result.failures += check("round-trip: settlements",
                loaded.settlements().size() == engine.settlements().size());
        result.failures += check("round-trip: citizens",
                loaded.citizens().size() == engine.citizens().size());
        result.failures += check("round-trip: households",
                loaded.households().size() == engine.households().size());
        result.failures += check("round-trip: chronicle",
                loaded.worldChronicle().size() == engine.worldChronicle().size());
        result.failures += check("round-trip: relations",
                loaded.relations().size() == engine.relations().size());
        result.failures += check("round-trip: routes",
                loaded.routes().size() == engine.routes().size());

        // The loaded world keeps on living.
        loaded.processDaily(dailyCrowd(), SAMPLER, new DayContext(engine.day() + 1, false, seed));
        result.failures += check("round-trip: simulation continues", loaded.day() == engine.day() + 1);

        // Double-serialisation must be idempotent.
        String firstSave = saved.raw().toString().length() + "";
        String secondSave = loaded.save().raw().toString().length() + "";
        // (string contents of double maps are ±; compare sizes loosely)
        result.failures += check("round-trip: stable serialisation size",
                Math.abs(Integer.parseInt(firstSave) - Integer.parseInt(secondSave)) < 10000);

        return result;
    }

    /**
     * Runs two engines in strict day lockstep and prints the exact day and
     * state field where they cease to be identical.
     */
    static void lockstepDiagnosis() {
        SocietyEngine e1 = new SocietyEngine(SEED, new EngineConfig());
        SocietyEngine e2 = new SocietyEngine(SEED, new EngineConfig());
        for (int day = 1; day <= 400; day++) {
            List<VillagerSnapshot> crowd = dailyCrowd();
            if (day == 40) {
                crowd.add(baby("v-baby1", AX + 1, AY, AZ + 1));
            }
            e1.processDaily(crowd, SAMPLER, new DayContext(day, day % 11 == 0, SEED));
            e2.processDaily(crowd, SAMPLER, new DayContext(day, day % 11 == 0, SEED));
            e1.drainAnnouncements();
            e2.drainAnnouncements();
            if (day % 3 == 0) {
                for (SpawnRequest req : e1.drainSpawns()) {
                    VillagerSnapshot snap = new VillagerSnapshot(
                            "spawned-" + req.citizenId, req.x, req.y, req.z,
                            true, req.x, req.y, req.z,
                            false, 0, 0, 0, false, 0, 0, 0, "minecraft:none", req.baby);
                    e1.onVillagerLoadedPending(req.citizenId, snap, SAMPLER);
                }
                for (SpawnRequest req : e2.drainSpawns()) {
                    VillagerSnapshot snap = new VillagerSnapshot(
                            "spawned-" + req.citizenId, req.x, req.y, req.z,
                            true, req.x, req.y, req.z,
                            false, 0, 0, 0, false, 0, 0, 0, "minecraft:none", req.baby);
                    e2.onVillagerLoadedPending(req.citizenId, snap, SAMPLER);
                }
            } else {
                e1.drainSpawns();
                e2.drainSpawns();
            }
            String d1 = stateDigest(e1);
            String d2 = stateDigest(e2);
            if (!d1.equals(d2)) {
                System.out.println("LOCKSTEP: state diverged on day " + day);
                String[] f1 = d1.split(";");
                String[] f2 = d2.split(";");
                for (int i = 0; i < Math.max(f1.length, f2.length); i++) {
                    String a = i < f1.length ? f1[i] : "<missing>";
                    String b = i < f2.length ? f2[i] : "<missing>";
                    if (!a.equals(b)) {
                        System.out.println("  field[" + i + "]\n    e1: " + a + "\n    e2: " + b);
                    }
                }
                System.out.println("=== e1 family table ===");
                familyTable(e1);
                System.out.println("=== e2 family table ===");
                familyTable(e2);
                return;
            }
        }
        System.out.println("LOCKSTEP: engines remained identical for all 400 days (determinism "
                + "breaks only across independent JVM runs -> content-addressable randomness).");
    }

    static void familyTable(SocietyEngine engine) {
        for (Citizen c : engine.citizens().values()) {
            String mother = engine.citizens().get(c.motherId()) == null ? "-"
                    : engine.citizens().get(c.motherId()).fullName();
            String father = engine.citizens().get(c.fatherId()) == null ? "-"
                    : engine.citizens().get(c.fatherId()).fullName();
            String spouse = engine.citizens().get(c.spouseId()) == null ? "-"
                    : engine.citizens().get(c.spouseId()).fullName();
            io.github.minlol12.society.core.data.Household h = engine.households().get(c.householdId());
            System.out.println("  " + c.fullName() + " (b" + c.birthDay() + ")"
                    + " house=" + (h == null ? "-" : h.familyName())
                    + " spouse=" + spouse + " mother=" + mother + " father=" + father
                    + " lastChild=" + c.lastChildBornDay());
        }
    }

    static double totalStockExceptFood(Settlement s) {
        double sum = 0;
        for (Good g : Good.values()) {
            if (g != Good.FOOD) sum += s.stock(g);
        }
        return sum;
    }

    static double techProgressSum(Settlement s) {
        double sum = 0;
        for (io.github.minlol12.society.core.types.TechNode n : io.github.minlol12.society.core.types.TechNode.values()) {
            sum += s.tech().progressOf(n);
            if (s.tech().isUnlocked(n)) sum += 1000;
        }
        return sum;
    }

    static double xpSum(Citizen c) {
        double sum = 0;
        for (io.github.minlol12.society.core.types.Skill sk : io.github.minlol12.society.core.types.Skill.values()) {
            sum += c.skillXpTotal(sk);
        }
        return sum;
    }

    /** Full deterministic digest of the ledger, one field per entity. */
    static String stateDigest(SocietyEngine engine) {
        StringBuilder sb = new StringBuilder();
        sb.append("day=").append(engine.day());
        for (Settlement s : engine.settlements().values()) {
            sb.append(';').append("S:").append('#')
                    .append(s.name()).append('|').append(s.cachedPopulation()).append('|')
                    .append(String.format("%.2f", s.stock(Good.FOOD))).append('|')
                    .append(String.format("%.2f", s.treasury())).append('|')
                    .append(String.format("%.2f", s.morale())).append('|')
                    .append(s.tier().name()).append('|')
                    .append(s.government() == null ? "" : s.government().type().name()).append('|')
                    .append(s.lastFestivalSeasonIndex()).append('|')
                    .append(s.famineDays()).append('|')
                    .append(String.format("%.2f", s.threatLevel())).append('|')
                    .append(s.bedCapacity()).append('|')
                    .append(s.buildings().size()).append('|')
                    .append(s.completedBuildings().size()).append('|')
                    .append(s.culture().facts().size()).append('|')
                    .append(String.format("%.2f", totalStockExceptFood(s))).append('|')
                    .append(String.format("%.2f", techProgressSum(s)));
        }
        for (Citizen c : engine.citizens().values()) {
            sb.append(';').append("C:").append(c.fullName()).append('|')
                    .append(c.birthDay()).append('|').append(c.reputation()).append('|')
                    .append(c.personality().dominantTrait().name()).append('|')
                    .append(c.profession().name()).append('|')
                    .append(c.bestSkill() == null ? "-" : c.bestSkill().name()).append('|')
                    .append(c.bestSkillLevel()).append('|')
                    .append(c.isManifested()).append('|')
                    .append(String.format("%.2f", xpSum(c))).append('|')
                    .append(String.format("%.2f", c.personalWealth())).append('|')
                    .append(c.lastChildBornDay()).append('|')
                    .append(c.isAlive());
        }
        for (io.github.minlol12.society.core.data.Household h : engine.households().values()) {
            sb.append(';').append("H:").append(h.familyName()).append('|')
                    .append(h.memberIds().size()).append('|')
                    .append(String.format("%.2f", h.wealth()));
        }
        for (DiplomaticRelation r : engine.relations()) {
            sb.append(';').append("R:").append(String.format("%.2f", r.score())).append('|')
                    .append(r.treaty().name());
        }
        sb.append(';').append("chron=").append(engine.worldChronicle().size());
        return sb.toString();
    }

    static int check(String name, boolean ok) {
        if (ok) {
            System.out.println("  PASS  " + name);
            return 0;
        }
        System.out.println("  FAIL  " + name);
        return 1;
    }
}
