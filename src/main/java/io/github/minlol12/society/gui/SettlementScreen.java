package io.github.minlol12.society.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.ChronicleEntry;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.Law;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.TechNode;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The settlement page: what the town is, what it has built, what it is
 * building now, and what it is short of. Opened by right-clicking with the
 * Society Chronicle inside a settlement's land.
 */
public final class SettlementScreen {

    private SettlementScreen() { }

    public static void open(net.minecraft.server.network.ServerPlayerEntity player,
                            SocietyEngine engine, Settlement settlement) {
        ReadOnlyScreen.open(player, title(settlement), build(engine, settlement));
        player.getWorld().playSound(null, player.getBlockPos(),
                net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.0f);
    }

    public static Text title(Settlement settlement) {
        if (settlement == null) {
            return Text.literal("The Wilds").formatted(Formatting.DARK_GRAY);
        }
        return Text.literal(settlement.name()).formatted(Formatting.DARK_GREEN)
                .append(Text.literal(" - " + settlement.tier().display())
                        .formatted(Formatting.DARK_GRAY));
    }

    public static List<ItemStack> build(SocietyEngine engine, Settlement s) {
        List<ItemStack> slots = new ArrayList<ItemStack>(54);
        for (int i = 0; i < 54; i++) slots.add(ItemStack.EMPTY);
        if (s == null) {
            slots.set(22, wilds(engine));
            return slots;
        }

        ItemStack pane = pane();
        for (int i = 0; i < 9; i++) slots.set(i, pane.copy());
        for (int i = 45; i < 54; i++) slots.set(i, pane.copy());

        slots.set(4, banner(engine, s));
        slots.set(10, people(engine, s));
        slots.set(12, stocks(s));
        slots.set(14, buildingsCard(s));
        slots.set(16, worksCard(engine, s));
        slots.set(19, government(engine, s));
        slots.set(21, cultureCard(s));
        slots.set(23, techCard(s));
        slots.set(25, diplomacyCard(engine, s));
        slots.set(30, workforce(s));
        slots.set(32, historyCard(s));
        slots.set(34, playersCard(engine, s));
        slots.set(36, playerStructuresCard(engine, s));

        // A row of the town's most notable buildings.
        List<StructureType> notable = notableBuildings(s);
        for (int i = 0; i < notable.size() && i < 7; i++) {
            slots.set(37 + i, buildingStack(s, notable.get(i)));
        }
        return slots;
    }

    // =====================================================================
    // Cards
    // =====================================================================

    private static ItemStack wilds(SocietyEngine engine) {
        ItemStack stack = new ItemStack(Items.GRASS_BLOCK);
        name(stack, "Unclaimed Land", Formatting.GRAY);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("No settlement holds this ground."));
        lore.add(blank());
        lore.add(grey("Lead three or more villagers together"));
        lore.add(grey("and they will raise their first fires here."));
        lore.add(blank());
        lore.add(pair("Day", String.valueOf(engine.day())));
        lore.add(pair("Season", engine.season().display()));
        lore.add(pair("Settlements alive", String.valueOf(engine.settlementsAlive())));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack banner(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.WHITE_BANNER);
        name(stack, s.name(), Formatting.GOLD);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Rank", s.tier().display()));
        lore.add(pair("Folk", s.culture().origin().folkName()));
        lore.add(pair("Founded", "day " + s.foundedDay()));
        Citizen founder = engine.citizens().get(s.founderId());
        lore.add(pair("Founder", founder == null ? "unknown hands" : founder.fullName()));
        lore.add(pair("Standing at", "(" + s.centerX() + ", " + s.centerZ() + ")"));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack people(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        name(stack, "People", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        List<Citizen> people = engine.liveCitizensOf(s);
        int walking = 0;
        int children = 0;
        for (Citizen c : people) {
            if (c.isManifested()) walking++;
            if (!c.isAdult(engine.day())) children++;
        }
        lore.add(pair("Population", String.valueOf(people.size())));
        lore.add(pair("Out and about", walking + " walking the streets"));
        lore.add(pair("Children", String.valueOf(children)));
        lore.add(pair("Beds", s.bedCapacity() + " (housing "
                + (s.bedCapacity() >= people.size() ? "sufficient" : "short") + ")"));
        lore.add(pair("Morale", bar((int) s.morale()) + " " + Math.round(s.morale())));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack stocks(Settlement s) {
        ItemStack stack = new ItemStack(Items.CHEST);
        name(stack, "Stores", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Treasury", Math.round(s.treasury()) + " emeralds"));
        lore.add(blank());
        for (Good good : Good.values()) {
            double stock = s.stock(good);
            if (stock < 0.5) continue;
            double want = s.desiredStock(good);
            Formatting colour = stock < want * 0.4 ? Formatting.RED
                    : stock < want ? Formatting.YELLOW : Formatting.GREEN;
            lore.add(Text.literal(good.display() + ": ").formatted(Formatting.GRAY)
                    .append(Text.literal((int) stock + " / " + (int) want).formatted(colour))
                    .styled(style -> style.withItalic(Boolean.FALSE)));
        }
        if (s.famineDays() > 0) {
            lore.add(blank());
            lore.add(line("Famine, day " + s.famineDays() + ".", Formatting.RED));
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack buildingsCard(Settlement s) {
        ItemStack stack = new ItemStack(Items.BRICKS);
        name(stack, "Buildings", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        List<Building> done = s.completedBuildings();
        List<Building> rising = s.sitesUnderConstruction();
        lore.add(pair("Standing", String.valueOf(done.size())));
        lore.add(pair("Under construction", String.valueOf(rising.size())));
        if (!rising.isEmpty()) {
            lore.add(blank());
            lore.add(grey("Rising now:"));
            int shown = 0;
            for (Building b : rising) {
                int percent = (int) Math.round(b.fraction() * 100);
                lore.add(Text.literal("  " + b.type().display() + " ")
                        .formatted(Formatting.WHITE)
                        .append(Text.literal(bar(percent) + " " + percent + "%")
                                .formatted(Formatting.AQUA))
                        .styled(style -> style.withItalic(Boolean.FALSE)));
                if (++shown >= 4) break;
            }
        }
        StructureType blocked = s.blockedBuild();
        if (blocked != null) {
            lore.add(blank());
            lore.add(line("Waiting to start: " + blocked.display(), Formatting.GOLD));
            lore.add(grey("  needs " + materialLine(s, blocked)));
        }
        lore(stack, lore);
        return stack;
    }

    private static String materialLine(Settlement s, StructureType type) {
        StringBuilder sb = new StringBuilder();
        appendShort(sb, s, type, Good.WOOD);
        appendShort(sb, s, type, Good.STONE);
        appendShort(sb, s, type, Good.IRON);
        return sb.length() == 0 ? "only hands" : sb.toString();
    }

    private static void appendShort(StringBuilder sb, Settlement s, StructureType type, Good good) {
        double missing = type.cost(good) - s.stock(good);
        if (missing <= 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append((int) Math.ceil(missing)).append(' ').append(good.display().toLowerCase());
    }

    private static ItemStack worksCard(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        name(stack, "Industry", Formatting.YELLOW);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("What the town's buildings give it:"));
        lore.add(blank());
        addBonus(lore, "Food", s.buildingProductionModifier(Good.FOOD));
        addBonus(lore, "Timber", s.buildingProductionModifier(Good.WOOD));
        addBonus(lore, "Stone", s.buildingProductionModifier(Good.STONE));
        addBonus(lore, "Tools", s.buildingProductionModifier(Good.TOOLS));
        addBonus(lore, "Cloth", s.buildingProductionModifier(Good.CLOTH));
        addBonus(lore, "Medicine", s.buildingProductionModifier(Good.MEDICINE));
        addBonus(lore, "Research", s.buildingResearchModifier());
        addBonus(lore, "Trade", s.buildingTradeModifier());
        if (s.buildingDefence() > 0) {
            lore.add(pair("Defences", "+" + fmt(s.buildingDefence())));
        }
        lore(stack, lore);
        return stack;
    }

    private static void addBonus(List<Text> lore, String label, double modifier) {
        if (modifier <= 1.001) return;
        int percent = (int) Math.round((modifier - 1.0) * 100);
        lore.add(Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal("+" + percent + "%").formatted(Formatting.GREEN))
                .styled(style -> style.withItalic(Boolean.FALSE)));
    }

    private static ItemStack government(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.GOLDEN_HELMET);
        name(stack, "Government", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        if (s.government() == null) {
            lore.add(grey("The hearths govern themselves."));
            lore(stack, lore);
            return stack;
        }
        Citizen leader = engine.leaderOf(s);
        lore.add(pair("Form", s.government().type().display()));
        lore.add(pair(s.government().type().leaderTitle(),
                leader == null ? "vacant seat" : leader.fullName()));
        lore.add(pair("Since", "day " + s.government().leaderSinceDay()));
        if (!s.government().laws().isEmpty()) {
            lore.add(blank());
            lore.add(grey("Laws:"));
            for (Law law : s.government().laws()) {
                lore.add(grey("  " + law.display() + " - " + law.summary()));
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack cultureCard(Settlement s) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        name(stack, "Culture", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Folk", s.culture().origin().folkName()));
        lore.add(pair("They build", s.culture().origin().buildingStyle()));
        lore.add(pair("They wear", s.culture().origin().dressStyle()));
        lore.add(pair("Festival", s.culture().origin().festivalName()));
        List<String> facts = s.culture().facts();
        if (!facts.isEmpty()) {
            lore.add(blank());
            int from = Math.max(0, facts.size() - 4);
            for (int i = from; i < facts.size(); i++) {
                lore.add(grey("  " + facts.get(i)));
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack techCard(Settlement s) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        name(stack, "Known Arts", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        List<TechNode> unlocked = s.tech().unlocked();
        if (unlocked.isEmpty()) {
            lore.add(grey("Everything here is still learned the old way."));
        } else {
            for (TechNode node : unlocked) {
                lore.add(line("  " + node.display(), Formatting.GREEN));
            }
        }
        List<TechNode> available = s.tech().available();
        if (!available.isEmpty()) {
            lore.add(blank());
            lore.add(grey("Being studied:"));
            int shown = 0;
            for (TechNode node : available) {
                int percent = (int) (s.tech().progressOf(node) / node.cost() * 100);
                if (percent <= 0) continue;
                lore.add(grey("  " + node.display() + " " + percent + "%"));
                if (++shown >= 3) break;
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack diplomacyCard(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.MAP);
        name(stack, "Neighbours", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        List<DiplomaticRelation> relations = engine.relationsInvolving(s.id());
        if (relations.isEmpty()) {
            lore.add(grey("They have met no one else yet."));
        } else {
            for (DiplomaticRelation relation : relations) {
                String otherId = relation.other(s.id());
                Settlement other = otherId == null ? null : engine.settlements().get(otherId);
                if (other == null) continue;
                Formatting colour = relation.treaty().atWar() ? Formatting.RED
                        : relation.score() > 40 ? Formatting.GREEN : Formatting.GRAY;
                lore.add(Text.literal("  " + other.name() + ": ").formatted(Formatting.WHITE)
                        .append(Text.literal(relation.treaty().description()).formatted(colour))
                        .styled(style -> style.withItalic(Boolean.FALSE)));
            }
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack workforce(Settlement s) {
        ItemStack stack = new ItemStack(Items.WHEAT);
        name(stack, "Workforce", Formatting.AQUA);
        List<Text> lore = new ArrayList<Text>();
        for (Map.Entry<SimProfession, Integer> e : s.professionCountsMap().entrySet()) {
            if (e.getValue().intValue() <= 0) continue;
            lore.add(pair(e.getKey().display(), String.valueOf(e.getValue())));
        }
        if (lore.isEmpty()) lore.add(grey("Everyone is still drifting."));
        boolean anyNeed = false;
        for (Map.Entry<SimProfession, Double> e : s.professionDemandMap().entrySet()) {
            if (e.getValue().doubleValue() < 1.0) continue;
            if (!anyNeed) {
                lore.add(blank());
                lore.add(grey("Hands wanted:"));
                anyNeed = true;
            }
            lore.add(line("  " + e.getKey().display(), Formatting.YELLOW));
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack historyCard(Settlement s) {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        name(stack, "Chronicle", Formatting.AQUA);
        List<Text> lore = new ArrayList<Text>();
        List<ChronicleEntry> chronicle = s.chronicle();
        if (chronicle.isEmpty()) {
            lore.add(grey("Its pages are still blank."));
        } else {
            int from = Math.max(0, chronicle.size() - 6);
            for (int i = from; i < chronicle.size(); i++) {
                ChronicleEntry entry = chronicle.get(i);
                lore.add(grey("Day " + entry.day() + ": " + entry.text()));
            }
        }
        lore(stack, lore);
        return stack;
    }

    /** The players who belong to this settlement and what they do here. */
    private static ItemStack playersCard(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.NAME_TAG);
        name(stack, "Players & Roles", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        boolean any = false;
        for (io.github.minlol12.society.core.data.PlayerData data
                : engine.playerData().values()) {
            if (!s.id().equals(data.homeSettlementId())) continue;
            if (data.role() == null
                    || data.role() == io.github.minlol12.society.core.types.PlayerRole.NONE) {
                continue;
            }
            any = true;
            lore.add(pair(data.playerName(), data.role().display()));
        }
        String rulerUuid = engine.rulerPlayers().get(s.id());
        if (rulerUuid != null) {
            io.github.minlol12.society.core.data.PlayerData ruler =
                    engine.playerData().get(rulerUuid);
            if (ruler != null) {
                lore.add(blank());
                lore.add(line(ruler.role().display() + " " + ruler.playerName()
                        + " rules here.", Formatting.RED));
            }
        }
        if (!any && rulerUuid == null) {
            lore.add(grey("No players belong to this town yet."));
            lore.add(grey("Take a role with /society role set <role>."));
        }
        lore(stack, lore);
        return stack;
    }

    /** Structures the players themselves claimed on this settlement's land. */
    private static ItemStack playerStructuresCard(SocietyEngine engine, Settlement s) {
        ItemStack stack = new ItemStack(Items.BELL);
        name(stack, "Player Structures", Formatting.LIGHT_PURPLE);
        List<Text> lore = new ArrayList<Text>();
        boolean any = false;
        for (io.github.minlol12.society.core.data.PlayerStructure p
                : engine.playerStructures()) {
            if (!s.id().equals(p.settlementId())) continue;
            any = true;
            lore.add(Text.literal("  " + p.label() + " (").formatted(Formatting.WHITE)
                    .append(Text.literal(p.kind().display().toLowerCase())
                            .formatted(Formatting.AQUA))
                    .append(Text.literal(" by " + p.ownerName() + ")")
                            .formatted(Formatting.GRAY))
                    .styled(style -> style.withItalic(Boolean.FALSE)));
        }
        if (!any) {
            lore.add(grey("Nothing claimed here yet."));
            lore.add(grey("Use the Setter Stick or /society structure place."));
        }
        lore(stack, lore);
        return stack;
    }

    /** The buildings most worth showing off, biggest investment first. */
    private static List<StructureType> notableBuildings(Settlement s) {
        Map<StructureType, Integer> counts = new EnumMap<StructureType, Integer>(StructureType.class);
        for (Building b : s.completedBuildings()) {
            Integer v = counts.get(b.type());
            counts.put(b.type(), Integer.valueOf(v == null ? 1 : v.intValue() + 1));
        }
        List<StructureType> types = new ArrayList<StructureType>(counts.keySet());
        types.sort((a, b) -> {
            int cmp = Double.compare(b.labour(), a.labour());
            return cmp != 0 ? cmp : a.name().compareTo(b.name());
        });
        return types;
    }

    private static ItemStack buildingStack(Settlement s, StructureType type) {
        ItemStack stack = new ItemStack(buildingIcon(type));
        int count = s.countBuildings(type);
        stack.setCount(Math.max(1, Math.min(64, count)));
        MutableText title = Text.literal(type.display()).formatted(Formatting.WHITE);
        if (count > 1) {
            title.append(Text.literal(" x" + count).formatted(Formatting.GRAY));
        }
        stack.setCustomName(title.styled(style -> style.withItalic(Boolean.FALSE)));
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey(type.category().name().charAt(0)
                + type.category().name().substring(1).toLowerCase()));
        if (type.beds() > 0) lore.add(pair("Beds", String.valueOf(type.beds() * count)));
        if (type.worksite() != SimProfession.NONE) {
            lore.add(pair("Worked by", type.worksite().display() + "s"));
        }
        lore(stack, lore);
        return stack;
    }

    private static Item buildingIcon(StructureType type) {
        switch (type.category()) {
            case CIVIC: return Items.BELL;
            case HOUSING: return Items.OAK_DOOR;
            case FOOD: return Items.WHEAT;
            case INDUSTRY: return Items.ANVIL;
            case TRADE: return Items.EMERALD;
            case KNOWLEDGE: return Items.BOOKSHELF;
            case DEFENCE: return Items.SHIELD;
            default: return Items.BRICKS;
        }
    }

    // =====================================================================
    // Formatting
    // =====================================================================

    private static String bar(int value) {
        int filled = Math.max(0, Math.min(10, (value + 5) / 10));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(i < filled ? '\u25a0' : '\u25a1');
        return sb.toString();
    }

    private static ItemStack pane() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setCustomName(Text.literal(" "));
        return stack;
    }

    private static void name(ItemStack stack, String text, Formatting colour) {
        stack.setCustomName(Text.literal(text).formatted(colour)
                .styled(style -> style.withItalic(Boolean.FALSE)));
    }

    private static void lore(ItemStack stack, List<Text> lines) {
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (Text line : lines) {
            list.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(line)));
        }
        stack.getOrCreateSubNbt("display").put("Lore", list);
    }

    private static Text pair(String label, String value) {
        return Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(value).formatted(Formatting.WHITE))
                .styled(style -> style.withItalic(Boolean.FALSE));
    }

    private static Text grey(String text) {
        return Text.literal(text).formatted(Formatting.GRAY)
                .styled(style -> style.withItalic(Boolean.FALSE));
    }

    private static Text line(String text, Formatting colour) {
        return Text.literal(text).formatted(colour)
                .styled(style -> style.withItalic(Boolean.FALSE));
    }

    private static Text blank() {
        return Text.literal("");
    }

    private static String fmt(double value) {
        long rounded = Math.round(value * 10.0);
        return (rounded / 10) + "." + Math.abs(rounded % 10);
    }
}
