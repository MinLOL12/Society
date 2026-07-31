package io.github.minlol12.society.core.system;

import java.util.ArrayList;
import java.util.List;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Government;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.GovernmentType;
import io.github.minlol12.society.core.types.Law;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;
import io.github.minlol12.society.core.types.Trait;

/**
 * Order out of crowds: who leads, under what form of government, with which
 * laws. Follows population tier, dominant cultural values, the profession
 * mix and - when chiefs die - occasionally the eldest child.
 */
public final class PoliticsSystem {

    private PoliticsSystem() { }

    public static void tick(SocietyEngine engine, Settlement s) {
        Government government = s.government();
        if (government == null) {
            // Abolished on load or a brand-new settlement: the elder steps up.
            Citizen elder = oldestCitizen(engine, s);
            government = new Government(GovernmentType.ELDER_COUNCIL,
                    elder == null ? "" : elder.id(), engine.day());
            s.setGovernment(government);
            refreshLaws(engine, s);
            return;
        }

        // --- Laws set up once -------------------------------------------------
        if (government.laws().isEmpty()) {
            refreshLaws(engine, s);
        }

        // --- Vacant throne? ---------------------------------------------------
        Citizen leader = government.leaderId().isEmpty()
                ? null : engine.citizens().get(government.leaderId());
        if (leader == null || !leader.isAlive()
                || !s.id().equals(leader.homeSettlementId())) {
            elect(engine, s, leader);
        }

        // --- Form of government follows size and character -------------------
        GovernmentType target = bestGovernmentForm(engine, s);
        if (target != government.type()) {
            GovernmentType old = government.type();
            government.setType(target, engine.day());
            Citizen ruler = elect(engine, s, null);
            refreshLaws(engine, s);
            refreshCouncil(engine, s);
            Citizen findRuler = ruler;
            engine.record(EventType.GOVERNMENT_CHANGE, s,
                    s.name() + " moves from " + old.display() + " to " + target.display()
                            + (findRuler == null ? "." : " under " + target.leaderTitle()
                                    + " " + findRuler.fullName() + "."));
            s.culture().addFact("abandoned " + old.display().toLowerCase()
                    + " for " + target.display().toLowerCase());
        } else {
            refreshCouncil(engine, s);
        }

        // --- Deposition of the hated -----------------------------------------
        Citizen current = government.leaderId().isEmpty()
                ? null : engine.citizens().get(government.leaderId());
        Integer lastDeposition = engine.lastDepositionDays().get(s.id());
        boolean depositionCooldown = lastDeposition != null
                && engine.day() - lastDeposition.intValue() < 25;
        if (current != null && s.morale() < 22.0 && !depositionCooldown
                && engine.day() - government.leaderSinceDay() > 10
                && engine.random().nextDouble() < 0.1) {
            engine.lastDepositionDays().put(s.id(), Integer.valueOf(engine.day()));
            engine.record(EventType.LEADER_CHANGE, s,
                    current.fullName() + " was deposed after days of grumbling in " + s.name() + ".");
            current.addMemory(engine.day(), "was driven from office");
            Citizen successor = elect(engine, s, current);
            s.setMorale(45.0);
            s.addThreat(1.0);
            if (successor != null) {
                predecessorNote(engine, successor, "rose to power after " + current.firstName()
                        + " was deposed");
            }
            refreshLaws(engine, s);
        }
    }

    // =====================================================================
    // Choosing rulers
    // =====================================================================

    private static Citizen oldestCitizen(SocietyEngine engine, Settlement s) {
        Citizen oldest = null;
        for (Citizen c : engine.liveCitizensOf(s)) {
            if (oldest == null || c.birthDay() < oldest.birthDay()) oldest = c;
        }
        return oldest;
    }

    /** Runs an election/succession and returns the new leader (also saved). */
    private static Citizen elect(SocietyEngine engine, Settlement s, Citizen predecessor) {
        Government government = s.government();
        List<Citizen> candidates = engine.liveCitizensOf(s);
        int day = engine.day();

        Citizen best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Citizen c : candidates) {
            if (!c.isAdult(day)) continue;
            if (predecessor != null && c.id().equals(predecessor.id())) continue;
            double score;
            switch (government.type()) {
                case CHIEFDOM:
                    score = c.skillLevel(Skill.COMBAT) * 0.8
                            + c.personality().get(Trait.AGGRESSION) * 0.4
                            + c.reputation() * 0.3;
                    // Dynasties matter in chiefdoms: an adult child of the
                    // fallen chief leads the mourning band.
                    if (predecessor != null && !predecessor.isAlive()
                            && isChildOf(c, predecessor)) {
                        score += 25.0;
                    }
                    break;
                case MERCHANT_LEAGUE:
                    score = c.skillLevel(Skill.TRADING) * 0.9
                            + (c.profession() == SimProfession.TRADER ? 20.0 : 0.0)
                            + c.reputation() * 0.25;
                    break;
                default:
                    score = (c.personality().get(Trait.SOCIABILITY)
                            + c.personality().get(Trait.AMBITION)
                            + c.personality().get(Trait.WISDOM)) / 3.0 * 0.8
                            + c.skillLevel(Skill.STEWARDSHIP) * 0.4
                            + c.reputation() * 0.4;
                    break;
            }
            score += engine.random().nextDouble() * 5.0;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        if (best != null) {
            government.setLeader(best.id(), day);
            best.addReputation(10);
            String title = government.type().leaderTitle();
            if (predecessor == null) {
                if (best.isAlive()) {
                    // Silent first installation - the founding covers it.
                }
            } else {
                String verb;
                if (!predecessor.isAlive()) {
                    verb = isChildOf(best, predecessor)
                            ? "succeeded their parent " + predecessor.fullName()
                            : "succeeded " + predecessor.fullName() + ", who " + predecessor.deathCause();
                } else {
                    verb = "took office after " + predecessor.fullName();
                }
                engine.record(EventType.LEADER_CHANGE, s,
                        title + " " + best.fullName() + " " + verb
                                + " to lead " + s.name() + ".");
                best.addMemory(day, "became " + title.toLowerCase() + " of " + s.name());
            }
        }
        refreshCouncil(engine, s);
        return best;
    }

    private static boolean isChildOf(Citizen child, Citizen parent) {
        return parent.id().equals(child.motherId()) || parent.id().equals(child.fatherId());
    }

    private static void predecessorNote(SocietyEngine engine, Citizen citizen, String note) {
        citizen.addMemory(engine.day(), note);
    }

    // =====================================================================
    // Forms and laws
    // =====================================================================

    private static GovernmentType bestGovernmentForm(SocietyEngine engine, Settlement s) {
        SettlementTier tier = s.tier();
        if (tier.ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return GovernmentType.ELDER_COUNCIL;
        }
        double aggression = averageTrait(engine, s, Trait.AGGRESSION);
        int traders = s.professionCount(SimProfession.TRADER);
        int population = s.cachedPopulation();
        // Hysteresis: a chiefdom keeps its chief while the folk remain
        // broadly fierce, but one only *forms* among truly fierce folk.
        if (aggression >= (s.government().type() == GovernmentType.CHIEFDOM ? 50.0 : 62.0)) {
            return GovernmentType.CHIEFDOM;
        }
        if (traders >= (s.government().type() == GovernmentType.MERCHANT_LEAGUE
                ? 2 : Math.max(2, population / 5))) {
            return GovernmentType.MERCHANT_LEAGUE;
        }
        if (tier.ordinal() >= SettlementTier.TOWN.ordinal()) {
            // Market towns keep their guildmasters; the rest elect councils.
            if (s.government().type() == GovernmentType.MERCHANT_LEAGUE && traders >= 2) {
                return GovernmentType.MERCHANT_LEAGUE;
            }
            return GovernmentType.FREE_COUNCIL;
        }
        return GovernmentType.MAYORALTY;
    }

    static double averageTrait(SocietyEngine engine, Settlement s, Trait trait) {
        List<Citizen> people = engine.liveCitizensOf(s);
        if (people.isEmpty()) return 50.0;
        double sum = 0;
        int count = 0;
        for (Citizen c : people) {
            if (!c.isAdult(engine.day())) continue;
            sum += c.personality().get(trait);
            count++;
        }
        return count == 0 ? 50.0 : sum / count;
    }

    /** Laws track culture: the admired virtue of the folk, plus the form. */
    private static void refreshLaws(SocietyEngine engine, Settlement s) {
        Government government = s.government();
        List<Law> wanted = new ArrayList<Law>();
        Law virtueLaw = lawForVirtue(s.culture().origin().virtue());
        if (virtueLaw != null) wanted.add(virtueLaw);
        switch (government.type()) {
            case CHIEFDOM: wanted.add(Law.MILITIA_EDICT); break;
            case MERCHANT_LEAGUE: wanted.add(Law.OPEN_MARKET); break;
            case FREE_COUNCIL: wanted.add(Law.OPEN_ARCHIVES); break;
            case MAYORALTY: wanted.add(Law.HEARTH_CHARTER); break;
            default: break;
        }
        if (s.tier().ordinal() >= SettlementTier.VILLAGE.ordinal()) {
            // A second statute reflecting the community's other passion.
            Trait strongest = dominantAverageTrait(engine, s);
            Law second = lawForVirtue(strongest);
            if (second != null && !wanted.contains(second)) wanted.add(second);
        }
        if (!government.laws().equals(wanted)) {
            government.laws().clear();
            government.laws().addAll(wanted);
            if (!wanted.isEmpty()) {
                StringBuilder text = new StringBuilder("The laws of ").append(s.name()).append(" now read: ");
                for (int i = 0; i < wanted.size(); i++) {
                    if (i > 0) text.append(", ");
                    text.append(wanted.get(i).display());
                }
                text.append('.');
                engine.record(EventType.LAW_CHANGE, s, text.toString());
            }
        }
    }

    private static Law lawForVirtue(Trait virtue) {
        switch (virtue) {
            case INDUSTRY: return Law.GRANARY_TITHE;
            case AMBITION: return Law.OPEN_MARKET;
            case AGGRESSION: return Law.MILITIA_EDICT;
            case CURIOSITY: return Law.OPEN_ARCHIVES;
            case GENEROSITY: return Law.HEARTH_CHARTER;
            case CAUTION: return Law.VIGIL_CHARTER;
            case WISDOM: return Law.OPEN_ARCHIVES;
            case SOCIABILITY: return Law.OPEN_MARKET;
            default: return null;
        }
    }

    private static Trait dominantAverageTrait(SocietyEngine engine, Settlement s) {
        Trait best = Trait.INDUSTRY;
        double bestValue = -1;
        for (Trait t : Trait.values()) {
            double avg = averageTrait(engine, s, t);
            if (avg > bestValue) {
                bestValue = avg;
                best = t;
            }
        }
        return best;
    }

    private static void refreshCouncil(SocietyEngine engine, Settlement s) {
        Government government = s.government();
        if (government.type() != GovernmentType.FREE_COUNCIL) {
            if (!government.councilIds().isEmpty() && government.type() != GovernmentType.ELDER_COUNCIL) {
                government.councilIds().clear();
            }
        }
        if (government.type() == GovernmentType.FREE_COUNCIL
                || government.type() == GovernmentType.ELDER_COUNCIL) {
            List<Citizen> people = engine.liveCitizensOf(s);
            List<Citizen> ranked = new ArrayList<Citizen>(people);
            ranked.sort((a, b) -> Integer.compare(b.reputation(), a.reputation()));
            List<String> council = new ArrayList<String>();
            for (Citizen c : ranked) {
                if (!c.isAdult(engine.day())) continue;
                council.add(c.id());
                if (council.size() >= (government.type() == GovernmentType.FREE_COUNCIL ? 5 : 3)) break;
            }
            if (!government.councilIds().equals(council)) {
                government.councilIds().clear();
                government.councilIds().addAll(council);
            }
        }
    }
}
