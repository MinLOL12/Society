package io.github.minlol12.society.core.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.Law;
import io.github.minlol12.society.core.types.Season;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;
import io.github.minlol12.society.core.types.TechNode;

/**
 * The beating heart of each settlement: everyone produces, everyone eats,
 * builders raise roofs, treasuries collect what caravans leave behind.
 * Also recomputes the settlement's <em>needs</em> daily - the profile from
 * which professions emerge, so a hungry village raises farmers and a
 * crowded one raises builders.
 */
public final class EconomySystem {

    private EconomySystem() { }

    /** Hard cap on real villager entities standing around one settlement. */
    public static final int ENTITY_CAP = 40;

    /** A settlement never keeps more than this many guards on the payroll. */
    private static final int GUARD_HARD_CAP = 60;
    /** Guards a town starts with before population is counted toward the watch. */
    private static final int GUARD_BASE = 12;
    /** One extra guard is warranted per this many citizens, landing the
     *  typical watch around twenty-five and never above the hard cap. */
    private static final int GUARD_PER_HEAD = 7;

    public static void tick(SocietyEngine engine, Settlement s, Season season) {
        Random random = engine.random();
        int day = engine.day();
        List<Citizen> people = engine.liveCitizensOf(s);
        int population = people.size();
        s.setCachedPopulation(population);
        if (population == 0) {
            return;
        }

        countProfessions(s, people);
        double security = computeSecurity(engine, s, people);
        s.setCachedSecurity(security);

        // --- Production -----------------------------------------------------
        boolean toolsAvailable = s.stock(Good.TOOLS) > population * 0.2;
        for (Citizen citizen : people) {
            produce(engine, s, citizen, population, season, toolsAvailable);
        }

        // --- Consumption ----------------------------------------------------
        double foodEaten = population * 0.85;
        s.addStock(Good.FOOD, -foodEaten);
        s.addStock(Good.TOOLS, -population * 0.02);
        s.addStock(Good.CLOTH, -population * 0.01);
        if (season == Season.WINTER
                && (s.culture().origin().productionModifier(Good.CLOTH) > 1.0
                        || s.culture().origin().productionModifier(Good.STONE) > 1.0)) {
            s.addStock(Good.WOOD, -population * 0.04); // heating through winter
        }
        if (s.stock(Good.LUXURY) > population * 0.3) {
            s.addStock(Good.LUXURY, -population * 0.05);
            s.addMorale(0.4);
        }
        if (s.stock(Good.MEDICINE) > population * 0.2) {
            s.addStock(Good.MEDICINE, -population * 0.01);
        }

        // --- Hunger ----------------------------------------------------------
        handleHunger(engine, s, people);

        // --- Morale ----------------------------------------------------------
        tickMorale(engine, s, population, security);

        // --- Growth: births, travellers, departures -------------------------
        tickGrowth(engine, s, people);

        // --- Development: tiers rise and fall -------------------------------
        evaluateTier(engine, s, population);

        // --- Storage audit: surplus is sold off or spoils -------------------
        auditStock(engine, s);

        // --- Needs: what does the settlement want to become? ----------------
        computeNeeds(engine, s, population, security);
        runAssignment(engine, s, people, random);

        // --- Manifest world presence ----------------------------------------
        engine.requestManifestations(s, people);
    }

    // =====================================================================
    // Production
    // =====================================================================

    private static void produce(SocietyEngine engine, Settlement s, Citizen citizen,
                                int population, Season season, boolean toolsAvailable) {
        SimProfession profession = citizen.profession();
        if (!citizen.isAdult(engine.day())) return;

        Skill primary = profession.primarySkill();
        int level = primary == null ? 0 : citizen.skillLevel(primary);
        double skillFactor = 0.6 + level / 100.0;
        double toolFactor = toolsAvailable ? 1.0 : 0.7;

        switch (profession) {
            case NONE:
            case FARMER:
            case LUMBERJACK:
            case MINER:
            case CRAFTER:
            case HEALER:
                for (Good good : Good.values()) {
                    double base = profession.dailyOutput(good);
                    if (base <= 0.0) continue;
                    double out = base * skillFactor * toolFactor
                            * s.culture().productionModifier(good)
                            * s.tech().goodModifier(good)
                            * s.buildingProductionModifier(good);
                    if (good == Good.FOOD) {
                        out *= engine.season().farmModifier();
                        if (s.government().hasLaw(Law.GRANARY_TITHE)) out *= 1.1;
                    }
                    // Crafters need raw material; without it they make cloth only.
                    if (profession == SimProfession.CRAFTER && good == Good.TOOLS) {
                        if (s.stock(Good.IRON) >= 0.4) {
                            s.addStock(Good.IRON, -0.4);
                            s.addStock(Good.WOOD, -Math.min(s.stock(Good.WOOD), 0.2));
                        } else {
                            continue;
                        }
                    }
                    s.addStock(good, out);
                }
                grantWorkXp(engine, citizen, primary, 0.9);
                break;
            case BUILDER:
                // Builders no longer conjure abstract housing: they work
                // real sites in ConstructionSystem, which grants their xp
                // and spends the settlement's timber and stone.
                break;
            case TRADER: {
                double surplusFactor = s.stock(Good.FOOD) > s.desiredStock(Good.FOOD)
                        || s.stock(Good.TOOLS) > s.desiredStock(Good.TOOLS) ? 1.2 : 0.6;
                double routesBonus = 1.0 + engine.routesInvolving(s.id()) * 0.15;
                double lawBonus = s.government().hasLaw(Law.OPEN_MARKET) ? 1.2 : 1.0;
                double income = (0.6 + population * 0.03) * skillFactor * surplusFactor
                        * routesBonus * lawBonus * s.tech().coinageModifier()
                        * s.buildingTradeModifier()
                        * s.culture().origin().tradeProfitModifier();
                s.addTreasury(income);
                grantWorkXp(engine, citizen, primary, 0.9);
                break;
            }
            case SCHOLAR: {
                TechNode focus = chooseResearchFocus(engine, s);
                if (focus != null) {
                    double lawBonus = s.government().hasLaw(Law.OPEN_ARCHIVES) ? 1.15 : 1.0;
                    double points = 1.25 * skillFactor * s.tech().researchModifier() * lawBonus
                            * s.buildingResearchModifier();
                    if (s.tech().addResearch(focus, points)) {
                        engine.onDiscovery(s, citizen, focus);
                    }
                }
                grantWorkXp(engine, citizen, primary, 1.0);
                break;
            }
            case GUARD:
                grantWorkXp(engine, citizen, primary, 0.8);
                break;
            case STEWARD: {
                double tax = 0.025 * population * s.tech().coinageModifier();
                s.addTreasury(tax);
                grantWorkXp(engine, citizen, primary, 0.8);
                break;
            }
            default:
                break;
        }
        citizen.addWealth(0.4); // everyone earns a little for their work
    }

    private static void grantWorkXp(SocietyEngine engine, Citizen citizen, Skill skill, double base) {
        if (skill == null) return;
        double aptitude = citizen.personality().archetype().aptitude(skill);
        int milestone = citizen.gainXp(skill, base * aptitude);
        if (milestone >= 0) {
            citizen.addReputation(2);
            citizen.addMemory(engine.day(), "reached " + milestone + " in " + skill.display());
            if (milestone >= 75) {
                engine.record(EventType.SKILL_MASTERY, null,
                        citizen.fullName() + " has mastered " + skill.display() + ".");
            }
        }
    }

    /** Research follows need, then culture, then curiosity. */
    static TechNode chooseResearchFocus(SocietyEngine engine, Settlement s) {
        List<TechNode> available = s.tech().available();
        if (available.isEmpty()) return null;

        List<TechNode> preference = new ArrayList<TechNode>();
        if (s.famineDays() > 0 || s.stock(Good.FOOD) < s.desiredStock(Good.FOOD) * 0.5) {
            preference.add(TechNode.AGRICULTURE);
            preference.add(TechNode.CROP_ROTATION);
        }
        if (s.housingCapacity() < s.cachedPopulation() + 2) {
            preference.add(TechNode.CARPENTRY);
            preference.add(TechNode.STONEMASONRY);
        }
        if (s.stock(Good.TOOLS) < s.desiredStock(Good.TOOLS) * 0.4) {
            preference.add(TechNode.MINING);
            preference.add(TechNode.SMELTING);
            preference.add(TechNode.METALWORKING);
        }
        if (engine.isAtWar(s.id())) {
            preference.add(TechNode.MILITARY_DRILL);
        }
        switch (s.culture().origin()) {
            case COASTAL: preference.add(TechNode.NAVIGATION); break;
            case DESERT: preference.add(TechNode.COINAGE); break;
            case MOUNTAIN: preference.add(TechNode.MINING); preference.add(TechNode.SMELTING); break;
            case SNOWY: preference.add(TechNode.MEDICINE); break;
            case JUNGLE: preference.add(TechNode.MEDICINE); break;
            case FOREST: preference.add(TechNode.CARPENTRY); break;
            default: break;
        }
        if (!s.tech().cityAllowed()) {
            preference.add(TechNode.ARCHITECTURE);
            preference.add(TechNode.STONEMASONRY);
        }
        preference.add(TechNode.WRITING);

        for (TechNode want : preference) {
            if (available.contains(want)) return want;
        }
        // Cheapest remaining tech keeps research moving.
        TechNode cheapest = null;
        for (TechNode node : available) {
            if (cheapest == null || node.cost() < cheapest.cost()) cheapest = node;
        }
        return cheapest;
    }

    // =====================================================================
    // Hunger and morale
    // =====================================================================

    private static void handleHunger(SocietyEngine engine, Settlement s, List<Citizen> people) {
        if (s.stock(Good.FOOD) > 0.0) {
            if (s.famineDays() > 0) {
                s.culture().noteFamineSurvived();
                s.culture().addFact("survived the famine of Day " + (engine.day() - s.famineDays()));
            }
            s.setFamineDays(0);
            return;
        }
        s.setFamineDays(s.famineDays() + 1);
        if (s.famineDays() == 1) {
            Integer lastFamine = engine.lastFamineDays().get(s.id());
            if (lastFamine == null || engine.day() - lastFamine.intValue() > 15) {
                engine.lastFamineDays().put(s.id(), Integer.valueOf(engine.day()));
                engine.record(EventType.FAMINE, s,
                        "Famine grips " + s.name() + " - the granaries are empty.");
            }
        }
        boolean hearth = s.government().hasLaw(Law.HEARTH_CHARTER);
        if (s.famineDays() <= 3) {
            s.addMorale(hearth ? -1.2 : -2.5);
        }
        if (engine.cfg().famineCasualties && s.famineDays() > 3) {
            double deathMod = s.tech().deathModifier() * s.buildingHealthModifier();
            Random random = engine.random();
            for (Citizen citizen : new ArrayList<Citizen>(people)) {
                if (citizen.isManifested() || !citizen.isAlive()) continue;
                double chance = citizen.isElder(engine.day()) ? 0.08 : 0.05;
                if (random.nextDouble() < chance * deathMod) {
                    engine.handleCitizenDeath(citizen, "starved in the famine of Day " + engine.day(), false);
                }
            }
        }
    }

    private static void tickMorale(SocietyEngine engine, Settlement s, int population, double security) {
        double baseline = 55.0;
        if (s.government().hasLaw(Law.HEARTH_CHARTER)) baseline += 6;
        if (s.government().hasLaw(Law.GRANARY_TITHE)) baseline -= 5;
        if (s.stock(Good.FOOD) > population * 3.0) baseline += 5;
        if (s.housingCapacity() >= population) baseline += 3;
        // Fountains, gardens, inns and shrines make a place worth living in.
        baseline += Math.min(12.0, s.buildingMorale() * 10.0);
        baseline -= Math.min(10.0, s.threatLevel() * 1.5);
        baseline -= Math.min(12.0, security < 1.5 ? 4.0 : 0.0);

        double step = baseline > s.morale() ? 1.0 : -1.5;
        s.setMorale(s.morale() + step);
        s.addThreat(-0.1);
    }

    // =====================================================================
    // Growth
    // =====================================================================

    private static void tickGrowth(SocietyEngine engine, Settlement s, List<Citizen> people) {
        Random random = engine.random();
        int day = engine.day();
        int population = people.size();
        boolean foodSecure = s.stock(Good.FOOD) > population * 2.0;
        boolean roomToGrow = s.housingCapacity() > population;
        boolean withinCaps = population < engine.cfg().maxCitizensPerSettlement;
        boolean content = s.morale() > 55.0;

        if (foodSecure && roomToGrow && withinCaps && content) {
            // --- Births: children take their parents' names and aptitudes. ---
            List<Citizen> adults = new ArrayList<Citizen>();
            for (Citizen c : people) {
                if (c.isAdult(day) && c.isMarried()) adults.add(c);
            }
            int manifestBudget = 1;
            for (Citizen c : adults) {
                Citizen spouse = engine.citizens().get(c.spouseId());
                if (spouse == null || !spouse.isAlive()) continue;
                if (!spouse.homeSettlementId().equals(s.id())) continue;
                if (isSecondaryPartner(c, spouse)) continue; // process a couple once
                if (day - c.lastChildBornDay() < 2 || day - spouse.lastChildBornDay() < 2) continue;
                // Higher birth rate to support larger cities (was 0.16)
                double birthChance = 0.32 + Math.min(0.15, population / 80.0);
                if (random.nextDouble() >= birthChance) continue;
                Citizen child = engine.birthChild(c, spouse, s);
                if (child != null && manifestBudget > 0 && engine.canManifest(s)) {
                    engine.queueSpawn(s, child, true);
                    manifestBudget--;
                }
            }
            // --- Travellers join thriving places. ---
            if (random.nextDouble() < 0.18) {
                Citizen traveler = engine.createAdultCitizen(s.culture().origin(),
                        16 + random.nextInt(30), s);
                engine.record(EventType.TRAVELER, s,
                        "A traveller named " + traveler.fullName() + " has settled in " + s.name() + ".");
                traveler.addMemory(day, "arrived in " + s.name() + " with empty pockets");
                if (engine.canManifest(s)) {
                    engine.queueSpawn(s, traveler, false);
                }
            }
        }

        // --- Emigration: the unhappy quietly leave. ---
        if (engine.cfg().enableMigration && (s.morale() < 35.0 || s.famineDays() > 0)
                && population > 2 && random.nextDouble() < 0.3) {
            Citizen leaver = pickEmigrant(engine, s, people);
            if (leaver != null) {
                Settlement target = engine.bestMigrationTarget(s, leaver);
                if (target != null) {
                    leaver.addMemory(day, "left " + s.name() + " for " + target.name());
                    String oldName = s.name();
                    engine.transferCitizen(leaver, target);
                    leaver.addMemory(day, "arrived in " + target.name() + " from " + oldName);
                    engine.record(EventType.MIGRATION, target,
                            leaver.fullName() + " arrived from " + oldName + " seeking a better life.");
                }
            }
        }
    }

    /**
     * Decides which partner of a married couple "represents" the couple for
     * the daily birth roll, using only stable content (never UUIDs, which
     * differ between sessions and would let roll assignments shuffle).
     * The elder partner goes first; exact same-age ties fall to names.
     */
    private static boolean isSecondaryPartner(Citizen a, Citizen b) {
        if (a.birthDay() != b.birthDay()) {
            return a.birthDay() > b.birthDay();
        }
        int cmp = a.fullName().compareTo(b.fullName());
        if (cmp != 0) {
            return cmp > 0;
        }
        return a.id().compareTo(b.id()) > 0; // unreachable in practice
    }

    private static Citizen pickEmigrant(SocietyEngine engine, Settlement s, List<Citizen> people) {
        String leaderId = s.government().leaderId();
        for (Citizen c : people) {
            if (c.isManifested()) continue; // real entities live by Minecraft rules
            if (c.isMarried() || !c.isAdult(engine.day())) continue;
            if (c.id().equals(leaderId)) continue;
            return c;
        }
        return null;
    }

    // =====================================================================
    // Development
    // =====================================================================

    private static void evaluateTier(SocietyEngine engine, Settlement s, int population) {
        SettlementTier current = s.tier();
        SettlementTier target = SettlementTier.forPopulation(population);
        if (target == SettlementTier.CITY && !s.tech().cityAllowed()) {
            target = SettlementTier.TOWN;
        }
        if (target.ordinal() > current.ordinal()) {
            if (s.stock(Good.FOOD) >= population * 2.0 && s.housingCapacity() >= population) {
                s.setTier(target);
                s.addMorale(8);
                s.culture().addFact("grew from a " + current.display().toLowerCase()
                        + " into a " + target.display().toLowerCase());
                engine.record(EventType.TIER_UP, s,
                        s.name() + " has grown into a " + target.display().toLowerCase() + ".");
            }
        } else if (target.ordinal() < current.ordinal()) {
            SettlementTier previous = current.previous();
            if (population <= Math.max(1, previous.minPopulation() - 2)) {
                s.setTier(target);
                s.addMorale(-6);
                engine.record(EventType.TIER_DOWN, s,
                        s.name() + " has dwindled to a " + target.display().toLowerCase() + ".");
            }
        }
    }

    private static void auditStock(SocietyEngine engine, Settlement s) {
        for (Good good : Good.values()) {
            double cap = s.storageCap(good);
            double stock = s.stock(good);
            if (stock <= cap) continue;
            double excess = stock - cap;
            double sold = excess * 0.25;
            double spoiled = excess * 0.25;
            s.addStock(good, -(sold + spoiled));
            s.addTreasury(sold * s.priceOf(good) * 0.4);
        }
    }

    // =====================================================================
    // Needs and profession evolution
    // =====================================================================

    private static void countProfessions(Settlement s, List<Citizen> people) {
        for (SimProfession p : SimProfession.values()) {
            s.setProfessionCount(p, 0);
        }
        for (Citizen c : people) {
            SimProfession p = c.profession();
            s.setProfessionCount(p, s.professionCount(p) + 1);
        }
    }

    private static double computeSecurity(SocietyEngine engine, Settlement s, List<Citizen> people) {
        double security = 0.5 + people.size() * 0.04;
        for (Citizen c : people) {
            if (c.profession() == SimProfession.GUARD) {
                int level = c.skillLevel(Skill.COMBAT);
                security += (2.0 + level / 25.0) * s.tech().guardModifier();
            }
        }
        if (s.government().hasLaw(Law.MILITIA_EDICT)) {
            security += people.size() * 0.1;
        }
        security += s.buildingDefence();
        return security;
    }

    private static double deficit(Settlement s, Good good) {
        double desired = s.desiredStock(good);
        return Math.max(0.0, Math.min(1.0, 1.0 - s.stock(good) / desired));
    }

    static void computeNeeds(SocietyEngine engine, Settlement s, int population, double security) {
        double foodSecurity = s.stock(Good.FOOD) / Math.max(1.0, s.desiredStock(Good.FOOD) * 0.5);
        s.setCachedFoodBalance((s.stock(Good.FOOD) - population) - foodSecurity);

        setDemand(s, SimProfession.FARMER,
                clamp(1.8 - foodSecurity * 1.5, 0, 2) * 1.0);

        // Construction is the loudest customer for timber and stone: a town
        // that cannot start the building it wants needs the trade that
        // supplies it, not another guard.
        StructureType wanted = s.blockedBuild();
        double woodPull = deficit(s, Good.WOOD) * 1.3;
        double stonePull = Math.max(deficit(s, Good.IRON), deficit(s, Good.STONE)) * 1.2;
        if (wanted != null) {
            if (s.stock(Good.WOOD) < wanted.cost(Good.WOOD)) woodPull = Math.max(woodPull, 1.8);
            if (s.stock(Good.STONE) < wanted.cost(Good.STONE)
                    || s.stock(Good.IRON) < wanted.cost(Good.IRON)) {
                stonePull = Math.max(stonePull, 1.7);
            }
        }
        // Even with nothing blocked, a growing settlement keeps a reserve.
        if (!s.sitesUnderConstruction().isEmpty() || s.bedCapacity() < population + 2) {
            woodPull = Math.max(woodPull, 1.1);
            stonePull = Math.max(stonePull, 0.8);
        }
        setDemand(s, SimProfession.LUMBERJACK, woodPull);
        setDemand(s, SimProfession.MINER, stonePull);

        double housingShort = (population + 2) - s.housingCapacity();
        setDemand(s, SimProfession.BUILDER,
                housingShort > 0 || !s.sitesUnderConstruction().isEmpty() ? 1.7 : 0.15);
        setDemand(s, SimProfession.CRAFTER,
                clamp(deficit(s, Good.TOOLS) * 1.4 + deficit(s, Good.CLOTH) * 0.7, 0, 2));
        double tradePull = s.treasury() < population * 8 ? 1.0 : 0.3;
        tradePull += engine.routesInvolving(s.id()) * 0.1;
        setDemand(s, SimProfession.TRADER,
                Math.min(1.6, tradePull));
        setDemand(s, SimProfession.SCHOLAR,
                0.35 + s.tier().ordinal() * 0.2);
        setDemand(s, SimProfession.HEALER,
                population >= 10 ? 0.9 : 0.3);
        setDemand(s, SimProfession.GUARD,
                s.threatLevel() > 0.5 || engine.isAtWar(s.id()) || security < 2.0 ? 1.9 : 0.45);
        setDemand(s, SimProfession.STEWARD,
                s.tier().ordinal() >= SettlementTier.VILLAGE.ordinal() ? 0.6 : 0.1);
    }

    private static void setDemand(Settlement s, SimProfession p, double demand) {
        s.setProfessionDemand(p, Math.max(0.0, Math.min(2.0, demand)));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Caps per profession, so a village doesn't become all guards. Guards are
     * deliberately scarce: a town keeps a small standing watch (about twenty
     * to twenty-five for an ordinary village) and never raises more than a
     * hard sixty, no matter how large it grows - the rest farm, build and
     * trade.
     */
    private static int professionCap(SimProfession p, int population) {
        switch (p) {
            case FARMER: return Math.max(2, (int) Math.ceil(population * 0.5));
            case TRADER: return Math.max(1, population / 5);
            case SCHOLAR: return Math.max(1, 1 + population / 6);
            case HEALER: return Math.max(1, population / 7);
            case GUARD: return Math.max(1, Math.min(GUARD_HARD_CAP, GUARD_BASE + population / GUARD_PER_HEAD));
            case STEWARD: return 1;
            default: return Math.max(1, population / 2);
        }
    }

    /**
     * Unemployed adults pick the work the settlement needs most, weighted by
     * their own aptitude and calling. This is where professions
     * "evolve": never assigned, only ever argued over by need and talent.
     */
    private static void runAssignment(SocietyEngine engine, Settlement s, List<Citizen> people, Random random) {
        int day = engine.day();
        int population = people.size();
        List<Citizen> jobless = new ArrayList<Citizen>();
        for (Citizen c : people) {
            if (!c.isAdult(day)) continue;
            if (c.ageYears(day) >= 65) continue; // elders have earned their rest
            if (c.profession() == SimProfession.NONE) {
                jobless.add(c);
            } else {
                maybeRetrain(engine, s, c, population, random);
                maybeRetire(engine, s, c, random);
            }
            if (jobless.size() >= 8) break;
        }
        int assigned = 0;
        for (Citizen c : jobless) {
            if (assigned >= 3) break;
            SimProfession best = null;
            double bestScore = 0.0;
            for (SimProfession p : SimProfession.values()) {
                if (p == SimProfession.NONE) continue;
                double demand = s.professionDemand(p);
                if (demand < 0.35) continue;
                if (s.professionCount(p) >= professionCap(p, population)) continue;
                double score = demand * 2.2;
                // A trade nobody in town practises is worth more than one
                // that already has hands; this is how small settlements
                // cover every craft they need instead of doubling up.
                if (s.professionCount(p) == 0 && demand >= 0.9) score += 0.8;
                Skill primary = p.primarySkill();
                if (primary != null) {
                    score += c.skillLevel(primary) / 25.0 * 0.5;
                    score += c.personality().archetype().aptitude(primary) * 0.5;
                }
                if (c.preferredProfession() == p) score += 0.6;
                score += s.culture().productionModifier(professionGood(p)) > 1.0 ? 0.2 : 0.0;
                score += random.nextDouble() * 0.3;
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
            if (best != null && bestScore >= 1.0) {
                c.setProfession(best);
                c.addMemory(day, "took up work as a " + best.display().toLowerCase());
                s.setProfessionCount(best, s.professionCount(best) + 1);
                assigned++;
            }
        }
    }

    private static Good professionGood(SimProfession p) {
        switch (p) {
            case FARMER: return Good.FOOD;
            case LUMBERJACK: return Good.WOOD;
            case MINER: return Good.IRON;
            case CRAFTER: return Good.TOOLS;
            case HEALER: return Good.MEDICINE;
            default: return Good.FOOD;
        }
    }

    private static void maybeRetrain(SocietyEngine engine, Settlement s, Citizen c, int population, Random random) {
        SimProfession current = c.profession();
        if (current == SimProfession.NONE) return;
        // In a food crisis, everyone who can pick up a hoe, picks up a hoe.
        boolean foodCrisis = s.professionDemand(SimProfession.FARMER) > 1.4;
        // A town whose building work has stalled for want of timber will
        // put spare hands into the woods and the quarry instead.
        boolean materialCrisis = s.professionDemand(SimProfession.LUMBERJACK) > 1.4
                || s.professionDemand(SimProfession.MINER) > 1.4;
        double currentDemand = s.professionDemand(current);
        if (!foodCrisis && !materialCrisis && currentDemand > 0.15) return;
        if (!foodCrisis && !materialCrisis
                && s.professionDemand(SimProfession.BUILDER) < 1.2
                && s.professionDemand(SimProfession.FARMER) < 1.2) return;
        // Never strip the last farmer to chase timber.
        if (materialCrisis && !foodCrisis && current == SimProfession.FARMER
                && s.professionCount(SimProfession.FARMER) <= Math.max(1, population / 5)) {
            return;
        }
        if (random.nextDouble() >= 0.08) return;
        // Retrain into the sharpest need that fits. Need is weighed against
        // how many already do that work, so a trade nobody practises wins
        // over one that merely wants a third pair of hands - which is how a
        // village with no miner and no stone finally sends someone digging.
        SimProfession best = null;
        double bestScore = 0.5;
        for (SimProfession p : new SimProfession[]{SimProfession.FARMER, SimProfession.LUMBERJACK,
                SimProfession.MINER, SimProfession.BUILDER}) {
            if (p == current) continue;
            double demand = s.professionDemand(p);
            if (demand <= 1.0) continue;
            int count = s.professionCount(p);
            if (count >= professionCap(p, population)) continue;
            double score = demand / (1.0 + count);
            if (score > bestScore) {
                best = p;
                bestScore = score;
            }
        }
        if (best != null) {
            c.setProfession(best);
            c.addMemory(engine.day(), "retrained as a " + best.display().toLowerCase()
                    + " - " + s.name() + " had greater need");
        }
    }

    private static void maybeRetire(SocietyEngine engine, Settlement s, Citizen c, Random random) {
        if (!c.isElder(engine.day())) return;
        if (c.ageYears(engine.day()) < 65) return;
        if (random.nextDouble() >= 0.1) return;
        c.setProfession(SimProfession.NONE);
        c.addMemory(engine.day(), "retired from work to a seat by the fire");
    }
}
