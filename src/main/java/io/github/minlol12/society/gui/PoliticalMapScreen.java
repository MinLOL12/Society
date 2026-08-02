package io.github.minlol12.society.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.DiplomaticRelation;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.Treaty;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The political map of the city-states: every state's land painted in its
 * own colour, white borders where one state's ground meets another's, and
 * the wilderness between them black. Hover any province to read who holds
 * it; the legend below names every power, its government and its treaties.
 * Opened with {@code /society map}.
 */
public final class PoliticalMapScreen {

    /** The map itself is a 9x5 grid of cells inside the 9x6 page. */
    private static final int MAP_COLS = 9;
    private static final int MAP_ROWS = 5;
    private static final int TOTAL_SLOTS = 54;
    private static final int LEGEND_START = MAP_COLS * MAP_ROWS;

    /** Province colours: black is wilderness and white is border, so neither
     *  is used for a state itself. */
    private static final Item[] PROVINCE_PANES = {
            Items.RED_STAINED_GLASS_PANE,
            Items.ORANGE_STAINED_GLASS_PANE,
            Items.YELLOW_STAINED_GLASS_PANE,
            Items.LIME_STAINED_GLASS_PANE,
            Items.GREEN_STAINED_GLASS_PANE,
            Items.CYAN_STAINED_GLASS_PANE,
            Items.LIGHT_BLUE_STAINED_GLASS_PANE,
            Items.BLUE_STAINED_GLASS_PANE,
            Items.PURPLE_STAINED_GLASS_PANE,
            Items.MAGENTA_STAINED_GLASS_PANE,
            Items.PINK_STAINED_GLASS_PANE,
            Items.BROWN_STAINED_GLASS_PANE,
            Items.GRAY_STAINED_GLASS_PANE,
            Items.LIGHT_GRAY_STAINED_GLASS_PANE
    };

    /** The legend swatches match the province colours index for index. */
    private static final Item[] LEGEND_WOOLS = {
            Items.RED_WOOL,
            Items.ORANGE_WOOL,
            Items.YELLOW_WOOL,
            Items.LIME_WOOL,
            Items.GREEN_WOOL,
            Items.CYAN_WOOL,
            Items.LIGHT_BLUE_WOOL,
            Items.BLUE_WOOL,
            Items.PURPLE_WOOL,
            Items.MAGENTA_WOOL,
            Items.PINK_WOOL,
            Items.BROWN_WOOL,
            Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL
    };

    private PoliticalMapScreen() { }

    public static void open(net.minecraft.server.network.ServerPlayerEntity player,
                            SocietyEngine engine, String homeSettlementId) {
        ReadOnlyScreen.open(player, title(engine), build(engine, homeSettlementId));
        player.getWorld().playSound(null, player.getBlockPos(),
                net.minecraft.sound.SoundEvents.ITEM_BOOK_PAGE_TURN,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.0f);
    }

    public static Text title(SocietyEngine engine) {
        return Text.literal("Political Map of the City-States")
                .formatted(Formatting.GOLD)
                .append(Text.literal(" - Day " + engine.day() + ", " + engine.season().display())
                        .formatted(Formatting.DARK_GRAY));
    }

    public static List<ItemStack> build(SocietyEngine engine, String homeSettlementId) {
        List<ItemStack> slots = new ArrayList<ItemStack>(TOTAL_SLOTS);
        for (int i = 0; i < TOTAL_SLOTS; i++) slots.add(ItemStack.EMPTY);

        List<Settlement> alive = new ArrayList<Settlement>();
        for (Settlement s : engine.settlements().values()) {
            if (!s.isDestroyed()) alive.add(s);
        }
        if (alive.isEmpty()) {
            slots.set(22, emptyWorld(engine));
            return slots;
        }

        Map<String, Integer> colours = assignColours(alive);

        if (alive.size() == 1) {
            // A single power rules everything the map can see.
            Settlement only = alive.get(0);
            for (int row = 0; row < MAP_ROWS; row++) {
                for (int col = 0; col < MAP_COLS; col++) {
                    slots.set(row * MAP_COLS + col, province(only, 0));
                }
            }
            slots.set(4, capital(engine, only, homeSettlementId));
            legend(slots, engine, alive, colours, homeSettlementId);
            return slots;
        }

        // Map bounds: the extremes of every capital, padded so the map breathes.
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Settlement s : alive) {
            minX = Math.min(minX, s.centerX());
            maxX = Math.max(maxX, s.centerX());
            minZ = Math.min(minZ, s.centerZ());
            maxZ = Math.max(maxZ, s.centerZ());
        }
        double spanX = Math.max(1.0, maxX - minX);
        double spanZ = Math.max(1.0, maxZ - minZ);
        double cellSize = Math.max(spanX / MAP_COLS, spanZ / MAP_ROWS) * 1.2;
        double originX = (minX + maxX) / 2.0 - cellSize * MAP_COLS / 2.0;
        double originZ = (minZ + maxZ) / 2.0 - cellSize * MAP_ROWS / 2.0;

        // Who owns each cell? The nearest state within reach of its claim.
        Settlement[][] owners = new Settlement[MAP_ROWS][MAP_COLS];
        for (int row = 0; row < MAP_ROWS; row++) {
            for (int col = 0; col < MAP_COLS; col++) {
                double wx = originX + (col + 0.5) * cellSize;
                double wz = originZ + (row + 0.5) * cellSize;
                owners[row][col] = ownerAt(alive, wx, wz);
            }
        }
        for (int row = 0; row < MAP_ROWS; row++) {
            for (int col = 0; col < MAP_COLS; col++) {
                Settlement owner = owners[row][col];
                if (owner == null) {
                    slots.set(row * MAP_COLS + col, wilderness(originX, originZ, cellSize, col, row));
                } else if (isBorder(owners, row, col)) {
                    slots.set(row * MAP_COLS + col, border(owner));
                } else {
                    slots.set(row * MAP_COLS + col, province(owner, colourOf(colours, owner.id())));
                }
            }
        }

        // A capital marker stands on each state's own cell.
        for (Settlement s : alive) {
            int col = clamp((int) Math.floor((s.centerX() - originX) / cellSize), MAP_COLS - 1);
            int row = clamp((int) Math.floor((s.centerZ() - originZ) / cellSize), MAP_ROWS - 1);
            slots.set(row * MAP_COLS + col, capital(engine, s, homeSettlementId));
        }

        legend(slots, engine, alive, colours, homeSettlementId);
        return slots;
    }

    // =====================================================================
    // Map cells
    // =====================================================================

    /** The nearest settlement, if it reaches this spot; wilderness otherwise. */
    private static Settlement ownerAt(List<Settlement> alive, double x, double z) {
        Settlement best = null;
        double bestDist = Double.MAX_VALUE;
        for (Settlement s : alive) {
            double dx = s.centerX() - x;
            double dz = s.centerZ() - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        if (best == null) return null;
        return bestDist <= claimRadius(best) ? best : null;
    }

    /** How far a state's writ runs: the bigger the town, the wider its land. */
    private static double claimRadius(Settlement s) {
        switch (s.tier()) {
            case CITY: return 520.0;
            case TOWN: return 360.0;
            case VILLAGE: return 240.0;
            case HAMLET: return 160.0;
            default: return 100.0;
        }
    }

    /** White wherever two states' ground meet (or the map's own edge). */
    private static boolean isBorder(Settlement[][] owners, int row, int col) {
        Settlement owner = owners[row][col];
        if (col > 0 && owners[row][col - 1] != owner) return true;
        if (col < MAP_COLS - 1 && owners[row][col + 1] != owner) return true;
        if (row > 0 && owners[row - 1][col] != owner) return true;
        if (row < MAP_ROWS - 1 && owners[row + 1][col] != owner) return true;
        return false;
    }

    private static ItemStack province(Settlement owner, int colourIndex) {
        ItemStack stack = new ItemStack(PROVINCE_PANES[colourIndex]);
        name(stack, "Land of " + owner.name(), Formatting.WHITE);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("Part of the city-state of " + owner.name() + "."));
        lore.add(pair("Tier", owner.tier().display()));
        lore.add(pair("Population", String.valueOf(owner.cachedPopulation())));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack border(Settlement owner) {
        ItemStack stack = new ItemStack(Items.WHITE_STAINED_GLASS_PANE);
        name(stack, "Border", Formatting.WHITE);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("Where the ground of " + owner.name() + " ends."));
        lore(stack, lore);
        return stack;
    }

    private static ItemStack wilderness(double originX, double originZ, double cellSize, int col, int row) {
        ItemStack stack = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        name(stack, "Wilderness", Formatting.DARK_GRAY);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("No city-state holds this ground."));
        lore.add(blank());
        int wx = (int) (originX + (col + 0.5) * cellSize);
        int wz = (int) (originZ + (row + 0.5) * cellSize);
        lore.add(pair("At", "(" + wx + ", " + wz + ")"));
        lore(stack, lore);
        return stack;
    }

    /** A banner planted on a state's own cell: its name and its standing. */
    private static ItemStack capital(SocietyEngine engine, Settlement s, String homeSettlementId) {
        ItemStack stack = new ItemStack(Items.WHITE_BANNER);
        MutableText title = Text.literal(s.name()).formatted(Formatting.GOLD);
        if (s.id().equals(homeSettlementId)) {
            title.append(Text.literal(" (home)").formatted(Formatting.AQUA));
        }
        stack.setCustomName(title.styled(style -> style.withItalic(Boolean.FALSE)));
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Tier", s.tier().display()));
        lore.add(pair("Population", String.valueOf(s.cachedPopulation())));
        Citizen leader = engine.leaderOf(s);
        lore.add(pair("Government", s.government() == null ? "hearths rule"
                : s.government().type().display()));
        lore.add(pair(s.government() == null ? "Leader" : s.government().type().leaderTitle(),
                leader == null ? "vacant seat" : leader.fullName()));
        lore.add(pair("Standing at", "(" + s.centerX() + ", " + s.centerZ() + ")"));
        String standing = standingSummary(engine, s);
        if (!standing.isEmpty()) {
            lore.add(blank());
            lore.add(grey(standing));
        }
        lore(stack, lore);
        return stack;
    }

    private static String standingSummary(SocietyEngine engine, Settlement s) {
        int wars = 0, allies = 0, pacts = 0;
        for (DiplomaticRelation r : engine.relationsInvolving(s.id())) {
            if (r.treaty().atWar()) wars++;
            else if (r.treaty() == Treaty.ALLIANCE) allies++;
            else if (r.treaty() == Treaty.TRADE_PACT) pacts++;
        }
        StringBuilder sb = new StringBuilder();
        if (wars > 0) sb.append("at war with ").append(wars).append(" state").append(wars > 1 ? "s" : "");
        if (allies > 0) sb.append(sb.length() > 0 ? ", " : "").append("allied with ").append(allies);
        if (pacts > 0) sb.append(sb.length() > 0 ? ", " : "").append(pacts).append(" trade pact").append(pacts > 1 ? "s" : "");
        return sb.length() == 0 ? "" : sb.toString();
    }

    // =====================================================================
    // Legend
    // =====================================================================

    private static void legend(List<ItemStack> slots, SocietyEngine engine, List<Settlement> alive,
                               Map<String, Integer> colours, String homeSettlementId) {
        int shown = 0;
        for (Settlement s : alive) {
            int slot = LEGEND_START + shown;
            if (slot >= TOTAL_SLOTS) break;
            // One slot left but several states remain: summarise instead.
            if (slot == TOTAL_SLOTS - 1 && shown < alive.size() - 1) {
                slots.set(slot, moreStack(alive.size() - shown));
                break;
            }
            slots.set(slot, legendStack(engine, s, colourOf(colours, s.id()), shown + 1, homeSettlementId));
            shown++;
        }
    }

    private static ItemStack legendStack(SocietyEngine engine, Settlement s, int colourIndex,
                                         int number, String homeSettlementId) {
        ItemStack stack = new ItemStack(LEGEND_WOOLS[colourIndex]);
        MutableText title = Text.literal(number + ". " + s.name()).formatted(Formatting.WHITE);
        if (s.id().equals(homeSettlementId)) {
            title.append(Text.literal(" (home)").formatted(Formatting.AQUA));
        }
        stack.setCustomName(title.styled(style -> style.withItalic(Boolean.FALSE)));
        List<Text> lore = new ArrayList<Text>();
        lore.add(pair("Tier", s.tier().display()));
        lore.add(pair("Population", String.valueOf(s.cachedPopulation())));
        lore.add(pair("Government", s.government() == null ? "hearths rule"
                : s.government().type().display()));
        String standing = standingSummary(engine, s);
        if (!standing.isEmpty()) {
            lore.add(blank());
            lore.add(grey(standing));
        }
        lore(stack, lore);
        return stack;
    }

    private static ItemStack moreStack(int more) {
        ItemStack stack = new ItemStack(Items.BARRIER);
        name(stack, "+ " + more + " more states", Formatting.GRAY);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("The map can only list nine powers at once."));
        lore(stack, lore);
        return stack;
    }

    /** A stable, distinct colour per settlement, handed out in a fixed order. */
    private static Map<String, Integer> assignColours(List<Settlement> alive) {
        Map<String, Integer> colours = new LinkedHashMap<String, Integer>();
        boolean[] used = new boolean[PROVINCE_PANES.length];
        for (Settlement s : alive) {
            int start = Math.floorMod(s.id().hashCode(), PROVINCE_PANES.length);
            int idx = start;
            for (int probe = 0; probe < PROVINCE_PANES.length; probe++) {
                if (!used[idx]) break;
                idx = (idx + 1) % PROVINCE_PANES.length;
            }
            used[idx] = true;
            colours.put(s.id(), Integer.valueOf(idx));
        }
        return colours;
    }

    private static int colourOf(Map<String, Integer> colours, String settlementId) {
        Integer c = colours.get(settlementId);
        return c == null ? 0 : c.intValue();
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    // =====================================================================
    // No states yet
    // =====================================================================

    private static ItemStack emptyWorld(SocietyEngine engine) {
        ItemStack stack = new ItemStack(Items.GRASS_BLOCK);
        name(stack, "Unclaimed Land", Formatting.GRAY);
        List<Text> lore = new ArrayList<Text>();
        lore.add(grey("No city-state has been founded yet."));
        lore.add(grey("The map waits for its first colours."));
        lore.add(blank());
        lore.add(pair("Day", String.valueOf(engine.day())));
        lore.add(pair("Season", engine.season().display()));
        lore(stack, lore);
        return stack;
    }

    // =====================================================================
    // Formatting (mirrors SettlementScreen)
    // =====================================================================

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

    private static Text blank() {
        return Text.literal("");
    }
}
