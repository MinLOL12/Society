package io.github.minlol12.society.core.system;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.Blueprint;
import io.github.minlol12.society.core.build.Blueprints;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;

/**
 * What turns a number into a town. Each day this decides what the
 * settlement most needs built, stakes out a plot for it, spends real
 * timber and stone, and lets the builders work it course by course until
 * it stands.
 *
 * <p>Housing is no longer an abstract counter: a settlement's beds are the
 * beds inside the houses it has actually finished. If the builders never
 * get the wood, the town never grows - and you can walk over and see why.</p>
 */
public final class ConstructionSystem {

    /** Sites a settlement will work on at once. */
    private static final int MAX_ACTIVE_SITES = 3;
    /** How far out from the centre plots may be staked. */
    private static final int MAX_PLOT_RADIUS = 72;

    private ConstructionSystem() { }

    public static void tick(SocietyEngine engine, Settlement s) {
        if (!engine.cfg().buildStructures) return;
        List<Citizen> people = engine.liveCitizensOf(s);
        if (people.isEmpty()) return;

        decayRuins(engine, s);
        buildToday(engine, s, people);
        maybeStartNewSite(engine, s, people);
    }

    // =====================================================================
    // Working the sites
    // =====================================================================

    private static void buildToday(SocietyEngine engine, Settlement s, List<Citizen> people) {
        List<Building> sites = activeSites(s);
        if (sites.isEmpty()) return;

        List<Citizen> builders = new ArrayList<Citizen>();
        for (Citizen c : people) {
            if (c.profession() == SimProfession.BUILDER && c.isAdult(engine.day())) {
                builders.add(c);
            }
        }
        // Even with no dedicated builders, a settlement raises what it can:
        // neighbours lend a hand, just far more slowly.
        double labour = 0.0;
        for (Citizen builder : builders) {
            double skill = 0.6 + builder.skillLevel(Skill.BUILDING) / 100.0;
            labour += 1.0 * skill * s.tech().buildModifier();
        }
        if (builders.isEmpty()) {
            labour = Math.min(2.0, people.size() * 0.06) * s.tech().buildModifier();
        }
        if (labour <= 0.0) return;

        // Split the day's labour across the active sites, oldest first.
        double perSite = labour / sites.size();
        int index = 0;
        for (Building site : sites) {
            Citizen worker = builders.isEmpty() ? null : builders.get(index % builders.size());
            index++;
            if (worker != null) {
                site.setWorkerId(worker.id());
            }
            boolean finished = site.addWork(site.totalWork() / 10.0, engine.day());
            if (worker != null) {
                worker.gainXp(Skill.BUILDING, 0.9
                        * worker.personality().archetype().aptitude(Skill.BUILDING));
            }
            if (finished) {
                onBuildingFinished(engine, s, site, worker);
            }
        }
    }

    private static void onBuildingFinished(SocietyEngine engine, Settlement s,
                                           Building building, Citizen builder) {
        StructureType type = building.type();
        s.addMorale(type.beds() > 0 ? 1.5 : 1.0);
        if (builder != null) {
            builder.addReputation(2);
            builder.addMemory(engine.day(), "finished the " + type.display().toLowerCase()
                    + " in " + s.name());
        }
        s.culture().addFact("raised a " + type.display().toLowerCase() + " on Day " + engine.day());
        engine.record(EventType.CONSTRUCTION, s,
                s.name() + " has finished building a " + type.display().toLowerCase() + ".");
        engine.markDirty();
    }

    /** Fire and war leave ruins; without repair they slowly fall apart. */
    private static void decayRuins(SocietyEngine engine, Settlement s) {
        for (Building b : new ArrayList<Building>(s.buildings())) {
            if (!b.isRuined()) continue;
            b.damage(0.4);
            if (b.progress() <= 0.0 && engine.random().nextDouble() < 0.2) {
                s.buildings().remove(b);
                engine.markDirty();
            }
        }
    }

    // =====================================================================
    // Deciding what to build next
    // =====================================================================

    private static void maybeStartNewSite(SocietyEngine engine, Settlement s, List<Citizen> people) {
        if (activeSites(s).size() >= MAX_ACTIVE_SITES) return;
        if (s.buildings().size() >= maxBuildings(s)) return;

        // Take the most-wanted thing the town can actually pay for today.
        // A settlement short of stone still raises timber cottages rather
        // than standing idle waiting for a quarry.
        List<StructureType> ranked = rankStructures(engine, s, people.size());
        if (ranked.isEmpty()) return;

        StructureType next = null;
        for (StructureType candidate : ranked) {
            if (canAfford(s, candidate)) {
                next = candidate;
                break;
            }
        }
        if (next == null) {
            // Nothing affordable: remember the dearest want so the
            // settlement page can explain what the town is waiting for.
            s.setBlockedBuild(ranked.get(0));
            return;
        }
        s.setBlockedBuild(null);

        int[] plot = findPlot(engine, s, next);
        if (plot == null) return;

        payFor(s, next);
        Blueprint blueprint = Blueprints.of(next);
        double work = 10.0;
        Building building = new Building(UUID.randomUUID().toString(), next,
                plot[0], plot[1], plot[2], plot[3], work, engine.day());
        s.buildings().add(building);
        engine.record(EventType.CONSTRUCTION_START, s,
                s.name() + " breaks ground on a new " + next.display().toLowerCase() + ".");
        engine.markDirty();
    }

    /**
     * Rough cap so a city doesn't cover the entire overworld. It follows
     * <em>population</em>, not tier: tier needs beds, beds need houses, and
     * capping houses by tier would lock a crowded camp out of ever growing
     * into the village it already is.
     */
    private static int maxBuildings(Settlement s) {
        int population = Math.max(1, s.cachedPopulation());
        int byTier;
        switch (s.tier()) {
            case CAMP: byTier = 12; break;
            case HAMLET: byTier = 24; break;
            case VILLAGE: byTier = 44; break;
            case TOWN: byTier = 70; break;
            default: byTier = 110; break;
        }
        // Always leave room for roughly two plots per head, so a settlement
        // can always build its way up to the next rung of the ladder.
        return Math.max(byTier, population * 2 + 6);
    }

    /**
     * The planner. Scores every structure the settlement could build by how
     * badly it is wanted right now and returns them best-first, so the
     * caller can fall back to the next-best thing it can afford.
     */
    public static List<StructureType> rankStructures(SocietyEngine engine, Settlement s, int population) {
        SettlementTier tier = s.tier();
        Map<StructureType, Integer> have = countByType(s);
        int workers = Math.max(1, population - population / 3);

        final Map<StructureType, Double> scores = new EnumMap<StructureType, Double>(StructureType.class);
        for (StructureType type : StructureType.values()) {
            if (tier.ordinal() < type.minTier().ordinal()) continue;
            if (type.requiredTech() != null && !s.tech().isUnlocked(type.requiredTech())) continue;

            int existing = have.containsKey(type) ? have.get(type).intValue() : 0;
            // Sites already staked out count toward the target, so the town
            // doesn't start six cottages at once.
            for (Building b : s.buildings()) {
                if (b.type() == type && !b.isComplete() && !b.isRuined()) existing++;
            }
            int wanted = type.desiredCount(population, tier, workers);
            if (type.unique()) wanted = Math.min(1, wanted);
            if (existing >= wanted) continue;

            double score = priority(engine, s, type, population, existing, wanted);
            if (score > 0.35) scores.put(type, Double.valueOf(score));
        }

        List<StructureType> ranked = new ArrayList<StructureType>(scores.keySet());
        // Sort by score, then by name so identical scores never depend on
        // hash order - the simulation has to stay deterministic.
        ranked.sort((a, b) -> {
            int cmp = Double.compare(scores.get(b).doubleValue(), scores.get(a).doubleValue());
            return cmp != 0 ? cmp : a.name().compareTo(b.name());
        });
        return ranked;
    }

    /** The single most wanted structure, or null when nothing is needed. */
    public static StructureType chooseNextStructure(SocietyEngine engine, Settlement s, int population) {
        List<StructureType> ranked = rankStructures(engine, s, population);
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    /**
     * How badly the settlement wants one more of this. Shelter and food
     * come first, then work, then defence when threatened, then the things
     * that make a place worth living in.
     */
    private static double priority(SocietyEngine engine, Settlement s, StructureType type,
                                   int population, int existing, int wanted) {
        double score = 0.5 + (wanted - existing) * 0.12;

        // A roof over every head is the first duty of a settlement.
        if (type.beds() > 0) {
            int beds = s.bedCapacity();
            double shortfall = population + 2 - beds;
            if (shortfall > 0) {
                score += 1.6 + Math.min(2.0, shortfall * 0.18);
            } else {
                score += 0.1;
            }
        }

        // Food buildings follow hunger.
        double foodRatio = s.stock(Good.FOOD) / Math.max(1.0, s.desiredStock(Good.FOOD));
        if (type.productionBonus(Good.FOOD) > 0) {
            score += foodRatio < 0.5 ? 1.5 : foodRatio < 1.0 ? 0.7 : 0.15;
        }
        if (s.famineDays() > 0 && type.productionBonus(Good.FOOD) > 0) score += 1.2;

        // Storage matters once the granary keeps overflowing.
        if (type.storageBonus() > 0 && s.stock(Good.FOOD) > s.desiredStock(Good.FOOD) * 0.8) {
            score += 0.8;
        }

        // Workshops follow the shortage they fix.
        for (Good good : Good.values()) {
            double bonus = type.productionBonus(good);
            if (bonus <= 0 || good == Good.FOOD) continue;
            double ratio = s.stock(good) / Math.max(1.0, s.desiredStock(good));
            if (ratio < 0.4) score += 0.9;
            else if (ratio < 0.9) score += 0.4;
        }

        // Defence follows fear.
        if (type.defenceBonus() > 0) {
            if (engine.isAtWar(s.id())) score += 1.6;
            else if (s.threatLevel() > 2.0) score += 0.9;
            else if (s.cachedSecurity() < 2.0) score += 0.5;
            else score -= 0.3;
        }

        // Research and trade follow ambition, once the basics stand.
        if (type.researchBonus() > 0 && s.professionCount(SimProfession.SCHOLAR) > 0) score += 0.6;
        if (type.tradeBonus() > 0 && engine.routesInvolving(s.id()) > 0) score += 0.5;
        if (type.healthBonus() > 0 && population >= 12) score += 0.5;

        // Comforts matter when spirits are low, but never before shelter.
        if (type.moraleBonus() > 0) {
            score += s.morale() < 45 ? 0.8 : 0.2;
        }

        // A settlement's heart comes early: a well and a bell make a village.
        if (type == StructureType.TOWN_WELL || type == StructureType.BELL_PLAZA) {
            score += 1.4;
        }
        if (type == StructureType.MILITARY_BASE) {
            score += (engine.isAtWar(s.id()) || s.threatLevel() > 1.5) ? 2.5 : 0.8;
        }
        if (type == StructureType.JAIL) {
            score += 0.6;
        }

        // Don't put up the fifth market stall before the first smithy.
        score -= existing * 0.25;
        return score;
    }

    private static Map<StructureType, Integer> countByType(Settlement s) {
        Map<StructureType, Integer> counts = new EnumMap<StructureType, Integer>(StructureType.class);
        for (Building b : s.buildings()) {
            if (b.isRuined()) continue;
            Integer v = counts.get(b.type());
            counts.put(b.type(), Integer.valueOf(v == null ? 1 : v.intValue() + 1));
        }
        return counts;
    }

    private static List<Building> activeSites(Settlement s) {
        List<Building> out = new ArrayList<Building>();
        for (Building b : s.buildings()) {
            if (!b.isComplete() && !b.isRuined()) out.add(b);
        }
        return out;
    }

    // =====================================================================
    // Materials
    // =====================================================================

    private static boolean canAfford(Settlement s, StructureType type) {
        return s.stock(Good.WOOD) >= type.cost(Good.WOOD)
                && s.stock(Good.STONE) >= type.cost(Good.STONE)
                && s.stock(Good.IRON) >= type.cost(Good.IRON);
    }

    private static void payFor(Settlement s, StructureType type) {
        s.addStock(Good.WOOD, -type.cost(Good.WOOD));
        s.addStock(Good.STONE, -type.cost(Good.STONE));
        s.addStock(Good.IRON, -type.cost(Good.IRON));
    }

    // =====================================================================
    // Where to put it
    // =====================================================================

    /**
     * Stakes out a plot: spirals out from the settlement centre looking for
     * open ground that doesn't overlap an existing building. Returns
     * {@code {x, y, z, rotation}} or null when the town is too crowded.
     *
     * <p>Only the ledger's own geometry is consulted here - the Minecraft
     * side gets the final say on terrain when it comes to place blocks.</p>
     */
    private static int[] findPlot(SocietyEngine engine, Settlement s, StructureType type) {
        Random random = engine.random();
        int need = type.footprint() / 2 + 2;
        int minRing = type.outskirts() ? 18 : 5;

        for (int attempt = 0; attempt < 64; attempt++) {
            double ring = minRing + (MAX_PLOT_RADIUS - minRing)
                    * Math.sqrt(random.nextDouble())
                    * (type.outskirts() ? 1.0 : 0.55);
            double angle = random.nextDouble() * Math.PI * 2;
            int x = s.centerX() + (int) Math.round(Math.cos(angle) * ring);
            int z = s.centerZ() + (int) Math.round(Math.sin(angle) * ring);

            if (overlaps(engine, s, x, z, need)) continue;

            // Face the building toward the settlement centre so doors open
            // onto the town rather than the wilderness.
            int rotation = facingToward(x, z, s.centerX(), s.centerZ());
            return new int[]{x, s.centerY(), z, rotation};
        }
        return null;
    }

    private static boolean overlaps(SocietyEngine engine, Settlement s, int x, int z, int need) {
        for (Settlement other : engine.settlements().values()) {
            for (Building b : other.buildings()) {
                double gap = b.distanceTo(x, z);
                if (gap < need + b.radius() + 4) return true;
            }
        }
        return false;
    }

    /** Rotation whose entrance (-Z when unrotated) points at the target. */
    private static int facingToward(int x, int z, int targetX, int targetZ) {
        int dx = targetX - x;
        int dz = targetZ - z;
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? 1 : 3; // east : west
        }
        return dz >= 0 ? 2 : 0; // south : north
    }
}
