package io.github.minlol12.society.core.system;

import java.util.List;
import java.util.Random;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.Law;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.TechNode;

/**
 * The world's weather of fortune: seasonal festivals, fires, plague,
 * bandits at the treasury door, wandering bards carrying names across the
 * map, and the occasional stroke of scholarly genius.
 */
public final class EventSystem {

    private EventSystem() { }

    public static void tick(SocietyEngine engine, Settlement s, boolean raining, int seasonIndex) {
        int day = engine.day();
        tickFestival(engine, s, raining, seasonIndex);
        tickFire(engine, s);
        tickPlague(engine, s);
        tickBandits(engine, s);
        tickBard(engine, s);
        tickInsight(engine, s);
        // A long peace quietly heals old fears.
        if (day % 7 == 0 && !engine.isAtWar(s.id())) {
            s.addThreat(-0.5);
        }
    }

    // =====================================================================
    // Festivals
    // =====================================================================

    private static void tickFestival(SocietyEngine engine, Settlement s, boolean raining, int seasonIndex) {
        if (seasonIndex == s.lastFestivalSeasonIndex()) return;
        s.setLastFestivalSeasonIndex(seasonIndex);
        if (raining || s.morale() < 38.0) {
            return; // nobody dances in the rain after a hungry year
        }
        int population = s.cachedPopulation();
        if (population < 2) return;

        s.addStock(Good.FOOD, -Math.min(s.stock(Good.FOOD), population * 0.4));
        s.addMorale(4.0);
        for (Citizen c : engine.liveCitizensOf(s)) {
            c.addReputation(1);
        }
        engine.record(EventType.FESTIVAL, s,
                s.name() + " celebrates " + s.culture().origin().festivalName()
                        + " - " + s.culture().origin().dressStyle()
                        + " crown the dancers as the seasons turn.");
        if (engine.random().nextDouble() < 0.25) {
            String fact = festivalFact(engine, s);
            if (fact != null) s.culture().addFact(fact);
        }
    }

    private static String festivalFact(SocietyEngine engine, Settlement s) {
        if (s.culture().faminesSurvived() > 0 && engine.random().nextBoolean()) {
            return "honours those lost to famine at every feast";
        }
        if (s.culture().warsFought() > 0 && engine.random().nextBoolean()) {
            return "sings of " + s.culture().warsFought() + " wars around the festival fire";
        }
        if (s.culture().discoveriesMade() > 0) {
            return "teaches children the " + s.tech().unlocked().get(0).display()
                    + " of their elders at festival";
        }
        return null;
    }

    // =====================================================================
    // Disasters
    // =====================================================================

    private static void tickFire(SocietyEngine engine, Settlement s) {
        if (s.tier().ordinal() < SettlementTier.HAMLET.ordinal()) return;
        double chance = 0.004;
        if (s.government().hasLaw(Law.VIGIL_CHARTER)) chance *= 0.4;
        if (s.tech().isUnlocked(TechNode.STONEMASONRY)) chance *= 0.7;
        if (engine.random().nextDouble() >= chance) return;

        double foodLoss = s.stock(Good.FOOD) * (0.1 + engine.random().nextDouble() * 0.2);
        double woodLoss = s.stock(Good.WOOD) * (0.1 + engine.random().nextDouble() * 0.2);
        s.addStock(Good.FOOD, -foodLoss);
        s.addStock(Good.WOOD, -woodLoss);

        // The fire takes a real building: it is left a burnt-out shell the
        // builders must come back and raise again.
        Building burnt = pickBurnable(engine, s);
        if (burnt != null) {
            burnt.damage(burnt.totalWork() * (0.35 + engine.random().nextDouble() * 0.4));
            burnt.setPlacedCells(0);
        }
        s.addThreat(2.0);
        s.addMorale(-3.0);
        s.culture().noteDisasterSurvived();
        s.culture().addFact("rebuilt after the fire of Day " + engine.day());
        engine.record(EventType.FIRE, s,
                "Fire sweeps through " + s.name() + "!"
                        + (burnt == null ? "" : " The " + burnt.type().display().toLowerCase()
                                + " is gutted;")
                        + " buckets pass hand to hand, and the "
                        + s.culture().origin().buildingStyle() + " will be rebuilt.");
    }

    /** The fire prefers finished timber buildings over bare sites. */
    private static Building pickBurnable(SocietyEngine engine, Settlement s) {
        List<Building> candidates = s.completedBuildings();
        if (candidates.isEmpty()) return null;
        return candidates.get(engine.random().nextInt(candidates.size()));
    }

    private static void tickPlague(SocietyEngine engine, Settlement s) {
        if (s.tier().ordinal() < SettlementTier.TOWN.ordinal()) return;
        Integer lastPlague = engine.lastPlagueDays().get(s.id());
        if (lastPlague != null && engine.day() - lastPlague.intValue() < 30) return;
        double chance = 0.0025;
        if (s.professionCount(SimProfession.HEALER) > 0) chance *= 0.6;
        if (s.stock(Good.MEDICINE) < 2.0) chance *= 1.4;
        if (engine.random().nextDouble() >= chance) return;

        engine.lastPlagueDays().put(s.id(), Integer.valueOf(engine.day()));
        int victims = 0;
        if (engine.cfg().plagueCasualties) {
            double deathMod = s.tech().deathModifier() * s.buildingHealthModifier();
            List<Citizen> people = engine.liveCitizensOf(s);
            for (Citizen c : people) {
                if (c.isManifested()) continue; // real entities live by Minecraft rules
                if (engine.random().nextDouble() < 0.03 * deathMod) {
                    engine.handleCitizenDeath(c, "succumbed to the grey rot", false);
                    victims++;
                }
            }
        }
        s.addThreat(3.0);
        s.addMorale(-6.0);
        s.culture().noteDisasterSurvived();
        s.culture().addFact("endured the grey rot of Day " + engine.day());
        engine.record(EventType.PLAGUE, s,
                "The grey rot creeps through " + s.name()
                        + (victims > 0 ? "; " + victims + " souls are lost to it." : ".")
                        + " The healers go door to door with bitter herbs.");
    }

    private static void tickBandits(SocietyEngine engine, Settlement s) {
        if (s.treasury() < 300.0 || s.cachedSecurity() >= 4.0) return;
        if (s.tier().ordinal() < SettlementTier.VILLAGE.ordinal()) return;
        if (engine.random().nextDouble() >= 0.006) return;

        double gold = Math.min(s.treasury() * 0.12, 220.0);
        boolean repelled = s.professionCount(SimProfession.GUARD) >= 3
                && engine.random().nextDouble() < 0.5;
        if (repelled) {
            s.addTreasury(-gold * 0.3);
            engine.record(EventType.RAID, s,
                    "Bandits came for the vaults of " + s.name()
                            + " in the night - the guards drove them howling into the dark.");
            s.culture().addFact("beat back bandits on Day " + engine.day());
        } else {
            s.addTreasury(-gold);
            engine.record(EventType.RAID, s,
                    "Bandits emptied part of the treasury of " + s.name()
                            + " in the night. The watch found only footprints by morning.");
            s.culture().addFact("was robbed by bandits on Day " + engine.day());
        }
        s.addThreat(3.0);
        s.addMorale(-4.0);
    }

    // =====================================================================
    // Travellers
    // =====================================================================

    private static void tickBard(SocietyEngine engine, Settlement s) {
        if (engine.random().nextDouble() >= 0.012) return;
        List<DiplomaticRelation> relations = engine.relationsInvolving(s.id());
        if (relations.isEmpty()) return;
        for (DiplomaticRelation relation : relations) {
            relation.addScore(3.0);
        }
        engine.record(EventType.CULTURE, s,
                "A wandering bard plays in the square of " + s.name()
                        + ", and carries its name down every road.");
        if (engine.random().nextDouble() < 0.4) {
            s.culture().addFact("was sung about by a travelling bard");
        }
    }

    private static void tickInsight(SocietyEngine engine, Settlement s) {
        if (s.professionCount(SimProfession.SCHOLAR) <= 0) return;
        if (engine.random().nextDouble() >= 0.012) return;
        TechNode focus = EconomySystem.chooseResearchFocus(engine, s);
        if (focus == null) return;
        if (s.tech().addResearch(focus, 50.0)) {
            engine.onDiscovery(s, null, focus);
        } else if (engine.random().nextDouble() < 0.5) {
            int percent = (int) Math.min(99, s.tech().progressOf(focus) / focus.cost() * 100.0);
            engine.announce(io.github.minlol12.society.core.Announcement.Severity.LOCAL, s,
                    "The scholars of " + s.name() + " have advanced " + focus.display()
                            + " (" + percent + "%).");
        }
    }
}
