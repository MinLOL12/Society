package io.github.minlol12.society;

import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.ChronicleEntry;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Government;
import io.github.minlol12.society.core.data.Household;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.data.TradeRoute;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.Law;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;
import io.github.minlol12.society.core.types.TechNode;
import io.github.minlol12.society.core.types.Trait;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Every way the mod speaks to a player: the chronicle's pages, the citizen
 * card you get by tapping a villager, and the renders backing the
 * {@code /society} commands. Pure formatting; no simulation logic lives here.
 */
public final class SocietyText {

    private SocietyText() {
    }

    // =====================================================================
    // The chronicle item
    // =====================================================================

    /**
     * Right-click page: the settlement near the reader, or the world
     * chronicle when no settlement claims this ground.
     */
    public static void printSettlementPage(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement) {
        if (settlement == null) {
            send(player, "No settlement holds this land. The chronicle speaks of the wider world:",
                    Formatting.GRAY);
            printChronicleTail(player, engine.worldChronicle(), 5);
            return;
        }
        printSettlementHeader(player, engine, settlement);

        List<Citizen> people = engine.liveCitizensOf(settlement);
        int manifested = 0;
        for (Citizen c : people) {
            if (c.isManifested()) {
                manifested++;
            }
        }
        send(player, people.size() + " souls dwell here (" + manifested + " walk the streets); "
                + "morale " + Math.round(settlement.morale()) + ", treasury "
                + Math.round(settlement.treasury()) + ".", Formatting.WHITE);

        int standing = settlement.completedBuildings().size();
        int rising = settlement.sitesUnderConstruction().size();
        send(player, standing + (standing == 1 ? " building stands" : " buildings stand")
                + (rising > 0 ? ", " + rising + " going up" : "")
                + "; " + settlement.bedCapacity() + " beds.", Formatting.WHITE);

        Government government = settlement.government();
        if (government != null) {
            send(player, governmentBlurb(engine, settlement), Formatting.WHITE);
        }

        List<ChronicleEntry> chronicle = settlement.chronicle();
        if (!chronicle.isEmpty()) {
            send(player, "Latest in its chronicle:", Formatting.GRAY);
            printChronicleTail(player, chronicle, 4);
        }
    }

    /** Right-click on a villager: their personal page. */
    public static void printCitizenCard(ServerPlayerEntity player,
            SocietyEngine engine, Citizen citizen, String vanillaProfessionId) {
        if (citizen == null) {
            send(player, "A stranger - the chronicle holds no page for this villager.",
                    Formatting.GRAY);
            return;
        }
        send(player, "--- " + citizen.fullName() + " ---", Formatting.GOLD);

        Settlement home = engine.settlements().get(citizen.homeSettlementId());
        StringBuilder line = new StringBuilder();
        line.append("Age ").append(citizen.ageYears(engine.day()));
        line.append(" - ").append(citizen.profession().display());
        if (!vanillaProfessionId.isEmpty() && !vanillaProfessionId.equals("minecraft:none")) {
            line.append(" (in the world: ").append(prettifyVanillaProfession(vanillaProfessionId))
                    .append(")");
        }
        if (home != null) {
            line.append(" of ").append(home.name());
        } else {
            line.append(", without a home");
        }
        send(player, line.toString(), Formatting.WHITE);

        List<Trait> traits = citizen.personality().topTraits(3);
        StringBuilder traitLine = new StringBuilder();
        for (int i = 0; i < traits.size(); i++) {
            if (i > 0) {
                traitLine.append(", ");
            }
            traitLine.append(traits.get(i).display());
        }
        send(player, "A " + citizen.personality().archetype().display().toLowerCase()
                + " at heart - " + traitLine + ".", Formatting.YELLOW);

        Skill best = citizen.bestSkill();
        send(player, "Reputation " + citizen.reputation()
                + (best == null ? "" : ", finest at " + best.display().toLowerCase()
                        + " (" + citizen.bestSkillLevel() + ")")
                + ", " + fmt(citizen.personalWealth()) + " emeralds to their name.",
                Formatting.WHITE);

        Household household = engine.households().get(citizen.householdId());
        if (household != null) {
            StringBuilder family = new StringBuilder("House ").append(household.familyName());
            Citizen spouse = engine.citizens().get(citizen.spouseId());
            if (spouse != null && spouse.isAlive()) {
                family.append(", wed to ").append(spouse.firstName());
            }
            int children = citizen.childrenIds().size();
            if (children > 0) {
                family.append(", ").append(children).append(children == 1 ? " child" : " children");
            }
            String tradition = household.tradition();
            if (!tradition.isEmpty()) {
                family.append(". ").append(tradition);
            }
            send(player, family.toString(), Formatting.AQUA);
        }

        List<String> memories = citizen.memories();
        if (!memories.isEmpty()) {
            send(player, "They remember:", Formatting.GRAY);
            int from = Math.max(0, memories.size() - 3);
            for (int i = from; i < memories.size(); i++) {
                send(player, "  " + memories.get(i), Formatting.DARK_GRAY);
            }
        }
    }

    // =====================================================================
    // /society commands
    // =====================================================================

    /** One line per living settlement: name, tier, population, place. */
    public static void printSettlementList(ServerPlayerEntity player, SocietyEngine engine) {
        boolean any = false;
        for (Settlement s : engine.settlements().values()) {
            if (s.isDestroyed()) {
                continue;
            }
            any = true;
            send(player, "- " + s.name() + " [" + s.tier().display() + "] pop "
                    + s.cachedPopulation() + "/" + s.housingCapacity() + " at ("
                    + s.centerX() + ", " + s.centerZ() + ")", Formatting.YELLOW);
        }
        if (!any) {
            send(player, "The world holds no settlement yet. Lead villagers together and one will rise.",
                    Formatting.GRAY);
        }
    }

    /** Sectioned settlement render for {@code /society settlement <name> [section]}. */
    public static void printSettlementSection(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement, String section) {
        if (section == null || section.equals("info")) {
            printSettlementPage(player, engine, settlement);
        } else if (section.equals("economy")) {
            printEconomySection(player, engine, settlement);
        } else if (section.equals("tech")) {
            printTechSection(player, settlement);
        } else if (section.equals("culture")) {
            printCultureSection(player, settlement);
        } else if (section.equals("diplomacy")) {
            printDiplomacySection(player, engine, settlement);
        } else if (section.equals("government")) {
            printGovernmentSection(player, engine, settlement);
        } else if (section.equals("buildings")) {
            printBuildingsSection(player, engine, settlement);
        } else {
            send(player, "Unknown section '" + section
                    + "'. Try: info, economy, tech, culture, diplomacy, government, buildings.",
                    Formatting.RED);
        }
    }

    /** The last {@code count} entries of the world chronicle. */
    public static void printWorldHistory(ServerPlayerEntity player, SocietyEngine engine, int count) {
        send(player, "--- The Chronicle of This World ---", Formatting.GOLD);
        printChronicleTail(player, engine.worldChronicle(), count);
    }

    /** Where the ledger's calendar stands. */
    public static void printDayLine(ServerPlayerEntity player, SocietyEngine engine) {
        send(player, "It is day " + engine.day() + ", "
                + engine.season().display().toLowerCase() + ". "
                + engine.settlementsAlive() + " settlements and "
                + engine.citizens().size() + " recorded souls stand in the ledger.",
                Formatting.WHITE);
    }

    // =====================================================================
    // Sections
    // =====================================================================

    private static void printSettlementHeader(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement) {
        send(player, "=== " + settlement.name() + " - " + settlement.tier().display()
                + " ===", Formatting.GOLD);
        Citizen founderCitizen = engine == null || settlement.founderId().isEmpty()
                ? null
                : engine.citizens().get(settlement.founderId());
        String founder = founderCitizen == null ? "unknown hands" : founderCitizen.fullName();
        send(player, settlement.culture().origin().folkName() + " - founded day "
                + settlement.foundedDay() + " by " + founder + ".", Formatting.GRAY);
    }

    private static void printEconomySection(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement) {
        printSettlementHeader(player, engine, settlement);
        send(player, "Treasury " + Math.round(settlement.treasury())
                + " - stockpiles: " + settlement.stockSummary(), Formatting.WHITE);

        StringBuilder prices = new StringBuilder("Prices:");
        for (Good good : Good.values()) {
            prices.append(' ').append(good.display().toLowerCase()).append(' ')
                    .append(fmt(settlement.priceOf(good))).append(';');
        }
        send(player, prices.toString(), Formatting.GRAY);

        StringBuilder work = new StringBuilder("Workforce:");
        boolean anyJob = false;
        for (Map.Entry<SimProfession, Integer> e : settlement.professionCountsMap().entrySet()) {
            if (e.getValue().intValue() <= 0) {
                continue;
            }
            anyJob = true;
            work.append(' ').append(e.getValue()).append(' ')
                    .append(Citizen.pluralProfession(e.getKey()).toLowerCase()).append(',');
        }
        send(player, anyJob ? work.substring(0, work.length() - 1) : "Workforce: all drifting.",
                Formatting.WHITE);

        StringBuilder wants = new StringBuilder("Needed hands:");
        boolean anyWant = false;
        for (Map.Entry<SimProfession, Double> e : settlement.professionDemandMap().entrySet()) {
            if (e.getValue().doubleValue() < 1.0) {
                continue;
            }
            anyWant = true;
            wants.append(' ').append(e.getKey().display().toLowerCase()).append(',');
        }
        if (anyWant) {
            send(player, wants.substring(0, wants.length() - 1), Formatting.YELLOW);
        }

        int routes = engine.routesInvolving(settlement.id());
        if (routes > 0) {
            send(player, routes + (routes == 1 ? " trade route feeds" : " trade routes feed")
                    + " its markets.", Formatting.GREEN);
        }
    }

    private static void printTechSection(ServerPlayerEntity player, Settlement settlement) {
        printSettlementHeader(player, null, settlement);
        List<TechNode> unlocked = settlement.tech().unlocked();
        if (unlocked.isEmpty()) {
            send(player, "No discoveries yet - every craft here is learned the old way.",
                    Formatting.GRAY);
        } else {
            StringBuilder known = new StringBuilder("Known arts:");
            for (TechNode node : unlocked) {
                known.append(' ').append(node.display()).append(',');
            }
            send(player, known.substring(0, known.length() - 1), Formatting.GREEN);
        }
        List<TechNode> available = settlement.tech().available();
        if (!available.isEmpty()) {
            StringBuilder next = new StringBuilder("Within reach:");
            for (TechNode node : available) {
                next.append(' ').append(node.display())
                        .append(" (").append((int) settlement.tech().progressOf(node))
                        .append('/').append(node.cost()).append("),");
            }
            send(player, next.substring(0, next.length() - 1), Formatting.YELLOW);
        }
    }

    private static void printCultureSection(ServerPlayerEntity player, Settlement settlement) {
        printSettlementHeader(player, null, settlement);
        send(player, settlement.culture().describe(), Formatting.WHITE);
        send(player, "They build with " + settlement.culture().origin().buildingStyle()
                + ", dress in " + settlement.culture().origin().dressStyle()
                + ", and hold the " + settlement.culture().origin().festivalName()
                + " each year.", Formatting.AQUA);
        List<String> facts = settlement.culture().facts();
        if (!facts.isEmpty()) {
            int from = Math.max(0, facts.size() - 4);
            for (int i = from; i < facts.size(); i++) {
                send(player, "  " + facts.get(i), Formatting.DARK_GRAY);
            }
        }
    }

    private static void printDiplomacySection(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement) {
        printSettlementHeader(player, engine, settlement);
        List<DiplomaticRelation> relations = engine.relationsInvolving(settlement.id());
        if (relations.isEmpty()) {
            send(player, settlement.name() + " has met no other people yet.", Formatting.GRAY);
            return;
        }
        for (DiplomaticRelation relation : relations) {
            String otherId = relation.other(settlement.id());
            Settlement other = otherId == null ? null : engine.settlements().get(otherId);
            String otherName = other == null ? "a forgotten people" : other.name();
            TradeRoute route = other == null ? null : engine.findRoute(settlement.id(), other.id());
            StringBuilder line = new StringBuilder();
            line.append(otherName).append(": ").append(relation.treaty().description())
                    .append(" (regard ").append(Math.round(relation.score())).append(')');
            if (relation.totalTradeValue() > 0) {
                line.append(", ").append(Math.round(relation.totalTradeValue()))
                        .append(" emeralds traded");
            }
            if (route != null) {
                line.append(", caravans on the road");
            }
            send(player, "- " + line, Formatting.WHITE);
        }
    }

    private static void printGovernmentSection(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement) {
        printSettlementHeader(player, engine, settlement);
        Government government = settlement.government();
        if (government == null) {
            send(player, "No one rules here; the hearths govern themselves.", Formatting.GRAY);
            return;
        }
        send(player, governmentBlurb(engine, settlement), Formatting.WHITE);
        if (!government.laws().isEmpty()) {
            StringBuilder laws = new StringBuilder("Laws:");
            for (Law law : government.laws()) {
                laws.append(' ').append(law.display()).append(',');
            }
            send(player, laws.substring(0, laws.length() - 1), Formatting.YELLOW);
        }
        int councilSize = government.councilIds().size();
        if (councilSize > 0) {
            StringBuilder council = new StringBuilder("Council:");
            int shown = 0;
            for (String id : government.councilIds()) {
                Citizen c = engine.citizens().get(id);
                if (c == null || !c.isAlive()) {
                    continue;
                }
                council.append(' ').append(c.fullName());
                if (++shown >= 4) {
                    break;
                }
                council.append(',');
            }
            if (shown > 0) {
                send(player, council.toString(), Formatting.GRAY);
            }
        }
    }

    /** What the settlement has raised, and what it is raising now. */
    private static void printBuildingsSection(ServerPlayerEntity player,
            SocietyEngine engine, Settlement settlement) {
        printSettlementHeader(player, engine, settlement);

        List<Building> standing = settlement.completedBuildings();
        List<Building> rising = settlement.sitesUnderConstruction();
        send(player, standing.size() + " buildings stand; " + rising.size()
                + " are going up. " + settlement.bedCapacity() + " beds in all.",
                Formatting.WHITE);

        if (standing.isEmpty()) {
            send(player, "Nothing permanent yet - only fires and bedrolls.", Formatting.GRAY);
        } else {
            Map<StructureType, Integer> counts =
                    new java.util.EnumMap<StructureType, Integer>(StructureType.class);
            for (Building b : standing) {
                Integer v = counts.get(b.type());
                counts.put(b.type(), Integer.valueOf(v == null ? 1 : v.intValue() + 1));
            }
            StringBuilder line = new StringBuilder("Standing:");
            for (Map.Entry<StructureType, Integer> e : counts.entrySet()) {
                line.append(' ').append(e.getKey().display());
                if (e.getValue().intValue() > 1) line.append(" x").append(e.getValue());
                line.append(',');
            }
            send(player, line.substring(0, line.length() - 1), Formatting.GREEN);
        }

        for (Building b : rising) {
            int percent = (int) Math.round(b.fraction() * 100);
            Citizen worker = engine.citizens().get(b.workerId());
            send(player, "  " + b.type().display() + " - " + percent + "% raised at ("
                    + b.x() + ", " + b.z() + ")"
                    + (worker == null ? "" : ", worked by " + worker.fullName()),
                    Formatting.YELLOW);
        }

        StructureType blocked = settlement.blockedBuild();
        if (blocked != null) {
            StringBuilder shortfall = new StringBuilder();
            appendShortfall(shortfall, settlement, blocked, Good.WOOD);
            appendShortfall(shortfall, settlement, blocked, Good.STONE);
            appendShortfall(shortfall, settlement, blocked, Good.IRON);
            send(player, "They want a " + blocked.display().toLowerCase()
                    + " but lack " + (shortfall.length() == 0 ? "hands" : shortfall.toString())
                    + ".", Formatting.GOLD);
        }
    }

    private static void appendShortfall(StringBuilder sb, Settlement s,
            StructureType type, Good good) {
        double missing = type.cost(good) - s.stock(good);
        if (missing <= 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append((int) Math.ceil(missing)).append(' ').append(good.display().toLowerCase());
    }

    // =====================================================================
    // Bits and pieces
    // =====================================================================

    private static String governmentBlurb(SocietyEngine engine, Settlement settlement) {
        Government government = settlement.government();
        Citizen leader = engine.leaderOf(settlement);
        String leaderName = leader == null ? "a vacant seat" : leader.fullName();
        return government.type().display() + ", " + government.type().leaderTitle().toLowerCase()
                + " " + leaderName + " (since day " + government.leaderSinceDay() + ").";
    }

    private static void printChronicleTail(ServerPlayerEntity player,
            List<ChronicleEntry> chronicle, int count) {
        if (chronicle.isEmpty()) {
            send(player, "  ...its pages are still blank.", Formatting.DARK_GRAY);
            return;
        }
        int from = Math.max(0, chronicle.size() - Math.max(1, count));
        for (int i = from; i < chronicle.size(); i++) {
            ChronicleEntry entry = chronicle.get(i);
            send(player, "  [Day " + entry.day() + "] " + entry.text(), Formatting.GRAY);
        }
    }

    private static String prettifyVanillaProfession(String id) {
        String name = id;
        int colon = name.indexOf(':');
        if (colon >= 0) {
            name = name.substring(colon + 1);
        }
        return name.replace('_', ' ');
    }

    /** One decimal, never locale-dependent. */
    private static String fmt(double value) {
        long rounded = Math.round(value * 10.0);
        long whole = rounded / 10;
        long frac = Math.abs(rounded % 10);
        String sign = rounded < 0 && whole == 0 ? "-" : "";
        return sign + whole + "." + frac;
    }

    private static void send(ServerPlayerEntity player, String text, Formatting formatting) {
        player.sendMessage(Text.literal(text).formatted(formatting), false);
    }
}
