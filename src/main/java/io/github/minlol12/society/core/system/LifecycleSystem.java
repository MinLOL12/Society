package io.github.minlol12.society.core.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Household;
import io.github.minlol12.society.core.data.Personality;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Trait;

/**
 * The private lives of citizens: courtships, weddings, merged households,
 * old age. Two villagers with matching temperaments wed; their names,
 * roofs and histories fuse; and someday a child inherits what they built.
 */
public final class LifecycleSystem {

    /** Years (Minecraft days) at which citizens may wed. */
    private static final int MIN_WED_AGE = 18;
    private static final int MAX_WED_AGE = 64;

    private LifecycleSystem() { }

    public static void tick(SocietyEngine engine, Settlement s) {
        List<Citizen> people = engine.liveCitizensOf(s);
        if (people.isEmpty()) return;
        tickCourtship(engine, s, people);
        tickOldAge(engine, s, people);
        tickReputation(engine, people);
    }

    // =====================================================================
    // Courtship and marriage
    // =====================================================================

    private static void tickCourtship(SocietyEngine engine, Settlement s, List<Citizen> people) {
        Random random = engine.random();
        int day = engine.day();

        List<Citizen> eligible = new ArrayList<Citizen>();
        for (Citizen c : people) {
            if (!c.isAlive() || c.isMarried()) continue;
            int age = c.ageYears(day);
            if (age < MIN_WED_AGE || age > MAX_WED_AGE) continue;
            eligible.add(c);
        }
        if (eligible.size() < 2) return;

        // A few chance meetings each day; compatible couples tend to wed.
        int attempts = Math.min(3, eligible.size() / 2);
        for (int i = 0; i < attempts; i++) {
            Citizen a = eligible.get(random.nextInt(eligible.size()));
            Citizen b = eligible.get(random.nextInt(eligible.size()));
            if (a == b) continue;
            if (areSiblings(a, b)) continue;
            if (!a.homeSettlementId().equals(b.homeSettlementId())) continue;
            double compatibility = Personality.compatibility(a.personality(), b.personality());
            double charm = (a.personality().get(Trait.SOCIABILITY)
                    + b.personality().get(Trait.SOCIABILITY)) / 100.0;
            double moraleBonus = s.morale() / 200.0;
            double chance = compatibility > 48 ? 0.05 * (0.6 + charm + moraleBonus) : 0.002;
            if (random.nextDouble() < chance) {
                marry(engine, s, a, b);
                return; // one wedding a day is plenty for a village
            }
        }
    }

    private static boolean areSiblings(Citizen a, Citizen b) {
        if (!a.motherId().isEmpty() && a.motherId().equals(b.motherId())) return true;
        if (!a.fatherId().isEmpty() && a.fatherId().equals(b.fatherId())) return true;
        if (a.id().equals(b.fatherId()) || a.id().equals(b.motherId())) return true;
        if (b.id().equals(a.fatherId()) || b.id().equals(a.motherId())) return true;
        return false;
    }

    static void marry(SocietyEngine engine, Settlement s, Citizen a, Citizen b) {
        int day = engine.day();
        // Record names before the household fuse renames everyone.
        String nameA = a.fullName();
        String nameB = b.fullName();
        a.setSpouseId(b.id());
        b.setSpouseId(a.id());

        // Households fuse: the smaller family joins the older name. Both
        // partners share one roof and one surname from this day on.
        Household ha = engine.households().get(a.householdId());
        Household hb = engine.households().get(b.householdId());
        Household into = ha;
        Household from = hb;
        if (ha == null && hb == null) {
            into = engine.createHousehold(a.familyName(), a);
            from = null;
        } else if (ha == null) {
            into = hb;
            from = null;
        } else if (hb == null || hb == ha) {
            from = null;
        } else if (hb.memberIds().size() > ha.memberIds().size()) {
            into = hb;
            from = ha;
        }
        if (from != null) {
            for (String memberId : new ArrayList<String>(from.memberIds())) {
                Citizen member = engine.citizens().get(memberId);
                if (member == null) continue;
                member.setHouseholdId(into.id());
                member.setFamilyName(into.familyName());
                member.addMemory(day, "took the name " + into.familyName()
                        + " when " + a.firstName() + " wed " + b.firstName());
                into.addMember(member.id());
            }
            into.addWealth(from.wealth());
            engine.households().remove(from.id());
        }
        if (into != null) {
            into.addMember(a.id());
            into.addMember(b.id());
            if (a.profession() != SimProfession.NONE) into.noteProfession(a.profession());
            if (b.profession() != SimProfession.NONE) into.noteProfession(b.profession());
        }

        a.addMemory(day, "married " + b.fullName());
        b.addMemory(day, "married " + a.fullName());
        a.addReputation(3);
        b.addReputation(3);
        s.addMorale(1.5);
        engine.record(EventType.MARRIAGE, s,
                nameA + " and " + nameB + " were wed in " + s.name() + ".");
    }

    // =====================================================================
    // Old age
    // =====================================================================

    private static void tickOldAge(SocietyEngine engine, Settlement s, List<Citizen> people) {
        Random random = engine.random();
        int day = engine.day();
        double deathMod = s.tech().deathModifier() * s.buildingHealthModifier();
        boolean healerPresent = s.professionCount(SimProfession.HEALER) > 0;
        if (healerPresent) deathMod *= 0.85;

        for (Citizen c : new ArrayList<Citizen>(people)) {
            if (!c.isAlive()) continue;
            int age = c.ageYears(day);
            if (age < 55) continue;
            double chance = (age - 50) * 0.005 * deathMod;
            if (c.personalWealth() > 30) chance *= 0.85; // comfort softens the years
            if (age >= 80 || random.nextDouble() < chance) {
                engine.handleCitizenDeath(c,
                        "passed away in their sleep at the age of " + age, false);
            }
        }
    }

    // =====================================================================
    // Reputation
    // =====================================================================

    private static void tickReputation(SocietyEngine engine, List<Citizen> people) {
        for (Citizen c : people) {
            if (c.profession() != SimProfession.NONE && c.isAdult(engine.day())) {
                // Slow drift upward for honest work, bounded by daily life.
                if (c.reputation() < 70 && engine.random().nextDouble() < 0.1) {
                    c.addReputation(1);
                }
            }
        }
    }
}
