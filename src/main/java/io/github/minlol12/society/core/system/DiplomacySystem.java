package io.github.minlol12.society.core.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.data.TradeRoute;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.GovernmentType;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Treaty;

/**
 * Between the settlements: first contacts, caravans, competition, words
 * that turn into treaties and treaties that turn into wars. Relations drift
 * daily toward what culture, geography and history imply, and memories are
 * long.
 */
public final class DiplomacySystem {

    /** Settlements notice each other within this range. */
    private static final int CONTACT_RANGE = 1600;
    /** Long-distance trade reaches this far; established merchant towns hear
     *  of one another across the map through far-ranging caravans. */
    private static final int ROUTE_MAX_RANGE = 4500;
    /** Beyond ordinary contact, trading towns may still find each other. */
    private static final int LONG_CONTACT_RANGE = 4500;

    private DiplomacySystem() { }

    public static void tick(SocietyEngine engine) {
        discoverNeighbours(engine);
        for (DiplomaticRelation relation : new ArrayList<DiplomaticRelation>(engine.relations())) {
            tickRelation(engine, relation);
        }
        shareKnowledgeAmongAllies(engine);
        tickRoutes(engine);
    }

    // =====================================================================
    // Discovery
    // =====================================================================

    private static void discoverNeighbours(SocietyEngine engine) {
        List<Settlement> alive = new ArrayList<Settlement>();
        for (Settlement s : engine.settlements().values()) {
            if (!s.isDestroyed()) alive.add(s);
        }
        int day = engine.day();
        for (int i = 0; i < alive.size(); i++) {
            for (int j = i + 1; j < alive.size(); j++) {
                Settlement a = alive.get(i);
                Settlement b = alive.get(j);
                double distance = a.distanceTo(b);
                if (distance > CONTACT_RANGE) continue;
                DiplomaticRelation relation = engine.findRelation(a.id(), b.id());
                if (relation == null) {
                    relation = new DiplomaticRelation(a.id(), b.id(), day);
                    double initial = a.culture().origin() == b.culture().origin() ? 8.0 : 0.0;
                    relation.setScore(initial);
                    engine.relations().add(relation);
                    engine.recordBilateral(EventType.FIRST_CONTACT, a, b,
                            "Scouts of " + a.name() + " and " + b.name()
                                    + " have made first contact across "
                                    + (int) distance + " blocks of wilderness.");
                }
            }
        }
        discoverDistantTradingPartners(engine, alive, day);
    }

    /**
     * Even far across the map, two established trading towns eventually hear
     * of one another - a marketplace or dock sends caravans down long roads,
     * and complementary economies find each other and start to trade at a
     * distance no scout would ever have walked.
     */
    private static void discoverDistantTradingPartners(SocietyEngine engine,
                                                       List<Settlement> alive, int day) {
        for (int i = 0; i < alive.size(); i++) {
            for (int j = i + 1; j < alive.size(); j++) {
                Settlement a = alive.get(i);
                Settlement b = alive.get(j);
                double distance = a.distanceTo(b);
                if (distance <= CONTACT_RANGE || distance > LONG_CONTACT_RANGE) continue;
                if (!isTradingTown(a) || !isTradingTown(b)) continue;
                if (engine.findRelation(a.id(), b.id()) != null) continue;
                // Most days the caravans are still on the road.
                if (engine.random().nextDouble() >= 0.25) continue;
                DiplomaticRelation relation = new DiplomaticRelation(a.id(), b.id(), day);
                relation.setScore(4.0);
                engine.relations().add(relation);
                engine.recordBilateral(EventType.FIRST_CONTACT, a, b,
                        "Far-ranging traders of " + a.name() + " and " + b.name()
                                + " have met across " + (int) distance
                                + " blocks of road and river, and speak of trade.");
            }
        }
    }

    /** A settlement big enough and built up enough to reach across the map. */
    private static boolean isTradingTown(Settlement s) {
        if (s.tier().ordinal() < SettlementTier.VILLAGE.ordinal()) return false;
        return s.has(StructureType.MARKETPLACE) || s.has(StructureType.TRADING_POST)
                || s.has(StructureType.DOCK) || s.has(StructureType.MARKET_STALL);
    }

    // =====================================================================
    // Relations
    // =====================================================================

    private static void tickRelation(SocietyEngine engine, DiplomaticRelation relation) {
        Settlement a = engine.settlements().get(relation.aId());
        Settlement b = engine.settlements().get(relation.bId());
        if (a == null || b == null || a.isDestroyed() || b.isDestroyed()) {
            return;
        }
        if (relation.treaty().atWar()) {
            tickWar(engine, relation, a, b);
            return;
        }

        int day = engine.day();
        double distance = a.distanceTo(b);

        // --- Baseline attractor -------------------------------------------
        // Civilisations default to a cautious friendliness; the land you
        // share, your values and - most of all - how well your economies
        // fit together decide the rest.
        double baseline = 4;
        if (a.culture().origin() == b.culture().origin()) baseline += 18;
        else if (a.culture().origin().virtue() == b.culture().origin().virtue()) baseline += 8;
        baseline += complementaryEconomies(a, b) * 14;
        baseline += Math.min(6.0, (a.tier().ordinal() + b.tier().ordinal()) * 2.0);
        if (a.distanceTo(b) < 150
                && PoliticsSystem.averageTrait(engine, a, io.github.minlol12.society.core.types.Trait.AGGRESSION) > 55
                && PoliticsSystem.averageTrait(engine, b, io.github.minlol12.society.core.types.Trait.AGGRESSION) > 55) {
            baseline -= 12; // proud neighbours bristle
        }
        String exportA = dominantExport(a);
        String exportB = dominantExport(b);
        if (exportA != null && exportA.equals(exportB)
                && a.stock(Good.FOOD) < a.desiredStock(Good.FOOD) * 0.5
                && b.stock(Good.FOOD) < b.desiredStock(Good.FOOD) * 0.5) {
            baseline -= 10; // competing for the same dinner table
        }
        if (a.government().type() == GovernmentType.MERCHANT_LEAGUE
                || b.government().type() == GovernmentType.MERCHANT_LEAGUE) {
            baseline += 10;
        }
        baseline += Math.min(25.0, relation.totalTradeValue() / 150.0);

        // --- Drift toward it -------------------------------------------------
        double step = baseline > relation.score() ? 0.6 : -0.6;
        double next = relation.score() + step;
        if ((step > 0 && next > baseline) || (step < 0 && next < baseline)) next = baseline;
        relation.setScore(next + (engine.random().nextDouble() - 0.5) * 1.6);

        // --- Treaty machinery -------------------------------------------------
        switch (relation.treaty()) {
            case NONE:
                if (relation.score() >= 20.0) {
                    relation.setTreaty(Treaty.TRADE_PACT);
                    engine.recordBilateral(EventType.TRADE_PACT, a, b,
                            a.name() + " and " + b.name() + " have struck a trade pact.");
                    if (distance <= ROUTE_MAX_RANGE && engine.findRoute(a.id(), b.id()) == null) {
                        engine.routes().add(new TradeRoute(a.id(), b.id(), day));
                        engine.recordBilateral(EventType.TRADE_ROUTE, a, b,
                                "A caravan road now runs between " + a.name() + " and " + b.name() + ".");
                    }
                } else if (relation.score() <= -55.0
                        && a.cachedPopulation() >= 8 && b.cachedPopulation() >= 8
                        && a.tier().ordinal() >= 1 && b.tier().ordinal() >= 1) {
                    declareWar(engine, relation, a, b);
                }
                break;
            case TRADE_PACT:
            case ALLIANCE:
                if (relation.score() < -5.0) {
                    relation.setTreaty(Treaty.NONE);
                    engine.recordBilateral(EventType.TRADE_PACT, a, b,
                            "The " + (relation.treaty() == Treaty.ALLIANCE ? "alliance" : "trade pact")
                                    + " between " + a.name() + " and " + b.name() + " has collapsed.");
                } else if (relation.treaty() == Treaty.TRADE_PACT && relation.score() >= 65.0) {
                    relation.setTreaty(Treaty.ALLIANCE);
                    engine.recordBilateral(EventType.ALLIANCE, a, b,
                            a.name() + " and " + b.name() + " have sworn an alliance.");
                } else if (relation.score() <= -55.0
                        && a.cachedPopulation() >= 8 && b.cachedPopulation() >= 8) {
                    declareWar(engine, relation, a, b);
                }
                break;
            case TRUCE:
                if (relation.score() < -15.0) {
                    relation.addScore(1.2);
                }
                if (engine.random().nextDouble() < 0.08 && relation.score() > -40.0) {
                    relation.setTreaty(Treaty.NONE);
                }
                break;
            default:
                break;
        }
    }

    private static void declareWar(SocietyEngine engine, DiplomaticRelation relation, Settlement a, Settlement b) {
        relation.beginWar(engine.day());
        a.addThreat(4.0);
        b.addThreat(4.0);
        a.culture().noteWar();
        b.culture().noteWar();
        engine.recordBilateral(EventType.WAR_START, a, b,
                "War! " + a.name() + " and " + b.name()
                        + " have taken up arms against one another.");
    }

    // =====================================================================
    // War
    // =====================================================================

    private static void tickWar(SocietyEngine engine, DiplomaticRelation relation, Settlement a, Settlement b) {
        Random random = engine.random();
        relation.noteWarDay();

        int powerA = warPower(engine, a);
        int powerB = warPower(engine, b);
        // A war must always move somewhere: evenly matched towns trade blows
        // rather than staring at each other across an empty field.
        int sign;
        if (powerA == powerB) {
            sign = random.nextBoolean() ? 1 : -1;
        } else {
            sign = Integer.compare(powerA, powerB);
        }
        int sway = sign * Math.max(1, Math.min(2, Math.abs(powerA - powerB) / 3 + 1));
        relation.addWarScore(sway);

        if (random.nextDouble() < 0.5) {
            Settlement loser = powerA < powerB ? a : b;
            Settlement winner = loser == a ? b : a;
            // The loser is plundered; guards bleed; history remembers.
            double foodLoot = loser.stock(Good.FOOD) * 0.08;
            double toolLoot = loser.stock(Good.TOOLS) * 0.08;
            double coinLoot = loser.treasury() * 0.04;
            loser.addStock(Good.FOOD, -foodLoot);
            loser.addStock(Good.TOOLS, -toolLoot);
            loser.addTreasury(-coinLoot);
            winner.addStock(Good.FOOD, foodLoot);
            winner.addStock(Good.TOOLS, toolLoot);
            winner.addTreasury(coinLoot);
            loser.addMorale(-2.0);
            loser.addThreat(2.0);
            if (engine.cfg().warCasualties) {
                int casualties = random.nextInt(3);
                for (int i = 0; i < casualties; i++) {
                    Citizen victim = pickUnmanifestedAdult(engine, loser);
                    if (victim != null) {
                        engine.handleCitizenDeath(victim,
                                "fell defending " + loser.name() + " against " + winner.name(), true);
                    }
                }
            }
            if (random.nextDouble() < 0.6) {
                engine.recordBilateral(EventType.BATTLE, winner, loser,
                        "Battle near " + loser.name() + " (" + loser.centerX()
                                + ", " + loser.centerZ() + "): the warriors of "
                                + winner.name() + " prevailed.");
            }
        }

        if (Math.abs(relation.warScore()) >= 10 || relation.daysAtWar() > 25) {
            endWar(engine, relation, a, b);
        }
    }

    private static int warPower(SocietyEngine engine, Settlement s) {
        double guards = s.professionCount(SimProfession.GUARD) * s.tech().guardModifier()
                + s.buildingDefence() * 0.5;
        return (int) (guards * 3 + s.cachedPopulation() * 0.12 + engine.random().nextInt(4));
    }

    private static Citizen pickUnmanifestedAdult(SocietyEngine engine, Settlement s) {
        List<Citizen> people = engine.liveCitizensOf(s);
        List<Citizen> options = new ArrayList<Citizen>();
        for (Citizen c : people) {
            if (!c.isManifested() && c.isAdult(engine.day())) options.add(c);
        }
        if (options.isEmpty()) return null;
        return options.get(engine.random().nextInt(options.size()));
    }

    private static void endWar(SocietyEngine engine, DiplomaticRelation relation, Settlement a, Settlement b) {
        int score = relation.warScore();
        Settlement victor = score > 2 ? a : score < -2 ? b : null;
        Settlement defeated = victor == a ? b : victor == b ? a : null;
        int day = engine.day();
        if (victor != null) {
            double tribute = Math.min(defeated.treasury() * 0.15, 250.0);
            defeated.addTreasury(-tribute);
            victor.addTreasury(tribute);
            victor.culture().addFact("defeated " + defeated.name() + " in the war of Day " + day);
            defeated.culture().addFact("was defeated by " + victor.name() + " in the war of Day " + day);
            engine.recordBilateral(EventType.WAR_END, victor, defeated,
                    victor.name() + " has triumphed over " + defeated.name()
                            + "; tribute changes hands and the fires go out.");
        } else {
            engine.recordBilateral(EventType.WAR_END, a, b,
                    "The war between " + a.name() + " and " + b.name()
                            + " sputters out in exhaustion. Neither side remembers what it was for.");
        }
        relation.endWarAsTruce();
        relation.setScore(-25.0);
    }

    // =====================================================================
    // Alliances and knowledge exchange
    // =====================================================================

    private static void shareKnowledgeAmongAllies(SocietyEngine engine) {
        for (DiplomaticRelation relation : engine.relations()) {
            if (relation.treaty() != Treaty.ALLIANCE) continue;
            Settlement a = engine.settlements().get(relation.aId());
            Settlement b = engine.settlements().get(relation.bId());
            if (a == null || b == null || a.isDestroyed() || b.isDestroyed()) continue;
            // The less advanced partner learns by watching allied scholars.
            dripKnowledge(engine, a, b);
            dripKnowledge(engine, b, a);
        }
    }

    private static void dripKnowledge(SocietyEngine engine, Settlement from, Settlement to) {
        for (io.github.minlol12.society.core.types.TechNode node : from.tech().unlocked()) {
            if (to.tech().isUnlocked(node)) continue;
            io.github.minlol12.society.core.types.TechNode parent = node.parent();
            if (parent != null && !to.tech().isUnlocked(parent)) continue;
            to.tech().addResearch(node, 8.0);
            return; // one idea a day is all a caravan can carry
        }
    }

    // =====================================================================
    // Trade routes
    // =====================================================================

    private static void tickRoutes(SocietyEngine engine) {
        for (TradeRoute route : new ArrayList<TradeRoute>(engine.routes())) {
            Settlement a = engine.settlements().get(route.aId());
            Settlement b = engine.settlements().get(route.bId());
            DiplomaticRelation relation = engine.findRelation(route.aId(), route.bId());
            boolean pactOk = relation != null
                    && (relation.treaty() == Treaty.TRADE_PACT || relation.treaty() == Treaty.ALLIANCE);
            if (a == null || b == null || a.isDestroyed() || b.isDestroyed()
                    || !pactOk || a.distanceTo(b) > ROUTE_MAX_RANGE) {
                engine.routes().remove(route);
                if (a != null && b != null) {
                    engine.recordBilateral(EventType.TRADE_ROUTE, a, b,
                            "The caravan road between " + a.name() + " and " + b.name()
                                    + " has fallen silent.");
                }
                continue;
            }
            double capacity = 25.0
                    + a.tech().tradeVolumeBonus() * 0.5 + b.tech().tradeVolumeBonus() * 0.5;
            route.resetVolume(capacity);
            moveGoods(engine, a, b, route, relation);
            moveGoods(engine, b, a, route, relation);
        }
    }

    /** One caravan load per direction, chasing scarcity across the road. */
    private static void moveGoods(SocietyEngine engine, Settlement from, Settlement to,
                                  TradeRoute route, DiplomaticRelation relation) {
        Good best = null;
        double bestScore = 0.0;
        for (Good good : Good.values()) {
            double surplus = from.stock(good) - from.desiredStock(good) * 1.3;
            double gap = to.desiredStock(good) - to.stock(good);
            if (surplus <= 0.5 || gap <= 0.5) continue;
            double score = Math.min(surplus, gap) * to.priceOf(good);
            if (score > bestScore) {
                bestScore = score;
                best = good;
            }
        }
        if (best == null) return;

        double avgPrice = (from.priceOf(best) + to.priceOf(best)) / 2.0;
        double surplus = from.stock(best) - from.desiredStock(best) * 1.3;
        double gap = to.desiredStock(best) - to.stock(best);
        double wantedValue = Math.min(surplus * 0.4, gap * 0.4) * avgPrice;
        double movedValue = route.move(wantedValue);
        if (movedValue <= 0.01) return;

        double qty = movedValue / avgPrice;
        from.addStock(best, -qty);
        to.addStock(best, qty);

        // Payment: hard coin if the buyer's treasury allows, barter otherwise.
        double price = movedValue;
        if (to.treasury() >= price) {
            to.addTreasury(-price);
            from.addTreasury(price);
        } else {
            // Barter terms: no coin, but gratitude and a smaller margin.
            price = 0.0;
        }
        relation.addTrade(movedValue);
        relation.addScore(Math.min(0.3, movedValue * 0.015));
    }

    /**
     * How many goods one settlement is hungry for that the other has to
     * spare, capped at two - complementary economies gravitate together.
     */
    private static int complementaryEconomies(Settlement a, Settlement b) {
        int pairs = 0;
        pairs += oneWayComplement(a, b);
        pairs += oneWayComplement(b, a);
        return Math.min(2, pairs);
    }

    private static int oneWayComplement(Settlement hungry, Settlement stocked) {
        int matches = 0;
        for (Good good : Good.values()) {
            boolean needs = hungry.stock(good) < hungry.desiredStock(good) * 0.6;
            boolean hasSpare = stocked.stock(good) > stocked.desiredStock(good) * 1.4;
            if (needs && hasSpare) matches++;
        }
        return Math.min(1, matches);
    }

    private static String dominantExport(Settlement s) {
        Good best = null;
        double bestRatio = 0.0;
        for (Good good : Good.values()) {
            double ratio = s.stock(good) / Math.max(1.0, s.desiredStock(good));
            if (ratio > bestRatio) {
                bestRatio = ratio;
                best = good;
            }
        }
        return best == null || bestRatio < 1.5 ? null : best.name();
    }
}
