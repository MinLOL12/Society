package io.github.minlol12.society.world.build;

import java.util.EnumMap;
import java.util.Map;

import io.github.minlol12.society.core.build.Mat;
import io.github.minlol12.society.core.types.CultureOrigin;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/**
 * Turns a blueprint's material <em>roles</em> into the blocks of a
 * particular people. One cottage blueprint becomes a mossy log lodge among
 * the Woodfolk, a granite hall among the Stonefolk and a sandstone house
 * among the Sandfolk - same drawing, different hands.
 */
public final class CulturePalette {

    private static final Map<CultureOrigin, CulturePalette> CACHE =
            new EnumMap<CultureOrigin, CulturePalette>(CultureOrigin.class);

    private final Map<Mat, Block> blocks = new EnumMap<Mat, Block>(Mat.class);

    private CulturePalette() { }

    public static synchronized CulturePalette of(CultureOrigin origin) {
        CulturePalette cached = CACHE.get(origin);
        if (cached != null) return cached;
        CulturePalette palette = build(origin == null ? CultureOrigin.PLAINS : origin);
        CACHE.put(origin, palette);
        return palette;
    }

    public Block block(Mat mat) {
        Block block = blocks.get(mat);
        return block == null ? Blocks.OAK_PLANKS : block;
    }

    private CulturePalette put(Mat mat, Block block) {
        blocks.put(mat, block);
        return this;
    }

    // =====================================================================
    // The palettes
    // =====================================================================

    private static CulturePalette build(CultureOrigin origin) {
        CulturePalette p = new CulturePalette();
        common(p);
        switch (origin) {
            case PLAINS: plains(p); break;
            case FOREST: forest(p); break;
            case MOUNTAIN: mountain(p); break;
            case COASTAL: coastal(p); break;
            case DESERT: desert(p); break;
            case SNOWY: snowy(p); break;
            case JUNGLE: jungle(p); break;
            case SWAMP: swamp(p); break;
            default: plains(p); break;
        }
        return p;
    }

    /** Everything that doesn't change with the land. */
    private static void common(CulturePalette p) {
        p.put(Mat.GLASS, Blocks.GLASS)
         .put(Mat.WINDOW, Blocks.GLASS_PANE)
         .put(Mat.BARS, Blocks.IRON_BARS)
         .put(Mat.TORCH, Blocks.WALL_TORCH)
         .put(Mat.LANTERN, Blocks.LANTERN)
         .put(Mat.HANGING_LANTERN, Blocks.LANTERN)
         .put(Mat.CAMPFIRE, Blocks.CAMPFIRE)
         .put(Mat.WATER, Blocks.WATER)
         .put(Mat.DIRT, Blocks.DIRT)
         .put(Mat.GRASS, Blocks.GRASS_BLOCK)
         .put(Mat.FARMLAND, Blocks.FARMLAND)
         .put(Mat.CROP, Blocks.WHEAT)
         .put(Mat.HAY, Blocks.HAY_BLOCK)
         .put(Mat.GRAVEL, Blocks.GRAVEL)
         .put(Mat.ORE_STONE, Blocks.COAL_ORE)
         .put(Mat.FLOWER, Blocks.POPPY)
         .put(Mat.FLOWER_POT, Blocks.POTTED_POPPY)
         .put(Mat.CARPET, Blocks.RED_CARPET)
         .put(Mat.BED, Blocks.RED_BED)
         .put(Mat.GRAVESTONE, Blocks.STONE_BRICK_WALL)
         .put(Mat.SCARECROW, Blocks.CARVED_PUMPKIN)
         // Workstations - these are what make villagers take up trades.
         .put(Mat.BELL, Blocks.BELL)
         .put(Mat.CHEST, Blocks.CHEST)
         .put(Mat.BARREL, Blocks.BARREL)
         .put(Mat.CRAFTING_TABLE, Blocks.CRAFTING_TABLE)
         .put(Mat.FURNACE, Blocks.FURNACE)
         .put(Mat.BLAST_FURNACE, Blocks.BLAST_FURNACE)
         .put(Mat.SMOKER, Blocks.SMOKER)
         .put(Mat.ANVIL, Blocks.ANVIL)
         .put(Mat.GRINDSTONE, Blocks.GRINDSTONE)
         .put(Mat.SMITHING_TABLE, Blocks.SMITHING_TABLE)
         .put(Mat.STONECUTTER, Blocks.STONECUTTER)
         .put(Mat.LOOM, Blocks.LOOM)
         .put(Mat.CARTOGRAPHY_TABLE, Blocks.CARTOGRAPHY_TABLE)
         .put(Mat.FLETCHING_TABLE, Blocks.FLETCHING_TABLE)
         .put(Mat.LECTERN, Blocks.LECTERN)
         .put(Mat.BOOKSHELF, Blocks.BOOKSHELF)
         .put(Mat.COMPOSTER, Blocks.COMPOSTER)
         .put(Mat.BREWING_STAND, Blocks.BREWING_STAND)
         .put(Mat.CAULDRON, Blocks.WATER_CAULDRON)
         .put(Mat.JUKEBOX, Blocks.JUKEBOX)
         .put(Mat.BEEHIVE, Blocks.BEEHIVE)
         .put(Mat.PAINTING, Blocks.AIR); // Paintings are entities, placed dynamically
    }

    /**
     * Fills in every wood-derived role from one wood family, so a palette
     * only has to name its timber once.
     */
    private static void woodSet(CulturePalette p, Block planks, Block log, Block stairs,
                                Block slab, Block fence, Block gate, Block door,
                                Block trapdoor, Block sign, Block leaves, Block sapling) {
        p.put(Mat.FLOOR, planks)
         .put(Mat.WALL, planks)
         .put(Mat.BEAM, log)
         .put(Mat.PILLAR, log)
         .put(Mat.LOG, log)
         .put(Mat.STAIR, stairs)
         .put(Mat.SLAB, slab)
         .put(Mat.TABLE, slab)
         .put(Mat.FENCE, fence)
         .put(Mat.GATE, gate)
         .put(Mat.DOOR, door)
         .put(Mat.TRAPDOOR, trapdoor)
         .put(Mat.SIGN, sign)
         .put(Mat.BANNER, Blocks.WHITE_WALL_BANNER)
         .put(Mat.LEAVES, leaves)
         .put(Mat.SAPLING, sapling)
         .put(Mat.LADDER, Blocks.LADDER);
    }

    private static void plains(CulturePalette p) {
        woodSet(p, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_STAIRS, Blocks.OAK_SLAB,
                Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE, Blocks.OAK_DOOR, Blocks.OAK_TRAPDOOR,
                Blocks.OAK_WALL_SIGN, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING);
        p.put(Mat.FOUNDATION, Blocks.COBBLESTONE)
         .put(Mat.FLOOR_ACCENT, Blocks.STRIPPED_OAK_WOOD)
         .put(Mat.WALL_ACCENT, Blocks.STRIPPED_OAK_LOG)
         .put(Mat.ROOF, Blocks.SPRUCE_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.SPRUCE_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.SPRUCE_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.SPRUCE_PLANKS)
         .put(Mat.LOW_WALL, Blocks.COBBLESTONE_WALL)
         .put(Mat.PATH, Blocks.DIRT_PATH);
    }

    private static void forest(CulturePalette p) {
        woodSet(p, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
                Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_WALL_SIGN, Blocks.SPRUCE_LEAVES,
                Blocks.SPRUCE_SAPLING);
        p.put(Mat.FOUNDATION, Blocks.MOSSY_COBBLESTONE)
         .put(Mat.FLOOR_ACCENT, Blocks.STRIPPED_SPRUCE_WOOD)
         .put(Mat.WALL_ACCENT, Blocks.STRIPPED_SPRUCE_LOG)
         .put(Mat.ROOF, Blocks.DARK_OAK_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.DARK_OAK_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.DARK_OAK_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.DARK_OAK_PLANKS)
         .put(Mat.LOW_WALL, Blocks.MOSSY_COBBLESTONE_WALL)
         .put(Mat.PATH, Blocks.DIRT_PATH)
         .put(Mat.FLOWER, Blocks.LILY_OF_THE_VALLEY)
         .put(Mat.FLOWER_POT, Blocks.POTTED_LILY_OF_THE_VALLEY);
    }

    private static void mountain(CulturePalette p) {
        woodSet(p, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
                Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_WALL_SIGN, Blocks.SPRUCE_LEAVES,
                Blocks.SPRUCE_SAPLING);
        // Stonefolk build in stone: walls and pillars are masonry, not timber.
        p.put(Mat.FOUNDATION, Blocks.STONE_BRICKS)
         .put(Mat.FLOOR, Blocks.POLISHED_ANDESITE)
         .put(Mat.FLOOR_ACCENT, Blocks.POLISHED_DIORITE)
         .put(Mat.WALL, Blocks.STONE_BRICKS)
         .put(Mat.WALL_ACCENT, Blocks.CHISELED_STONE_BRICKS)
         .put(Mat.PILLAR, Blocks.POLISHED_ANDESITE)
         .put(Mat.BEAM, Blocks.SPRUCE_LOG)
         .put(Mat.STAIR, Blocks.STONE_BRICK_STAIRS)
         .put(Mat.SLAB, Blocks.STONE_BRICK_SLAB)
         .put(Mat.ROOF, Blocks.DEEPSLATE_TILE_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.DEEPSLATE_TILE_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.DEEPSLATE_TILE_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.DEEPSLATE_TILES)
         .put(Mat.LOW_WALL, Blocks.STONE_BRICK_WALL)
         .put(Mat.PATH, Blocks.COBBLESTONE)
         .put(Mat.ORE_STONE, Blocks.IRON_ORE)
         .put(Mat.CARPET, Blocks.GRAY_CARPET);
    }

    private static void coastal(CulturePalette p) {
        woodSet(p, Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_STAIRS, Blocks.OAK_SLAB,
                Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE, Blocks.OAK_DOOR, Blocks.OAK_TRAPDOOR,
                Blocks.OAK_WALL_SIGN, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING);
        p.put(Mat.FOUNDATION, Blocks.COBBLESTONE)
         .put(Mat.FLOOR, Blocks.BIRCH_PLANKS)
         .put(Mat.FLOOR_ACCENT, Blocks.STRIPPED_BIRCH_WOOD)
         .put(Mat.WALL, Blocks.BIRCH_PLANKS)
         .put(Mat.WALL_ACCENT, Blocks.STRIPPED_BIRCH_LOG)
         .put(Mat.STAIR, Blocks.BIRCH_STAIRS)
         .put(Mat.SLAB, Blocks.BIRCH_SLAB)
         .put(Mat.TABLE, Blocks.BIRCH_SLAB)
         .put(Mat.ROOF, Blocks.PRISMARINE_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.PRISMARINE_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.PRISMARINE_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.PRISMARINE)
         .put(Mat.LOW_WALL, Blocks.COBBLESTONE_WALL)
         .put(Mat.PATH, Blocks.SAND)
         .put(Mat.CARPET, Blocks.LIGHT_BLUE_CARPET)
         .put(Mat.BED, Blocks.LIGHT_BLUE_BED)
         .put(Mat.FLOWER, Blocks.CORNFLOWER)
         .put(Mat.FLOWER_POT, Blocks.POTTED_CORNFLOWER)
         .put(Mat.BANNER, Blocks.LIGHT_BLUE_WALL_BANNER);
    }

    private static void desert(CulturePalette p) {
        woodSet(p, Blocks.ACACIA_PLANKS, Blocks.ACACIA_LOG, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB,
                Blocks.ACACIA_FENCE, Blocks.ACACIA_FENCE_GATE, Blocks.ACACIA_DOOR,
                Blocks.ACACIA_TRAPDOOR, Blocks.ACACIA_WALL_SIGN, Blocks.ACACIA_LEAVES,
                Blocks.ACACIA_SAPLING);
        // Sandfolk raise thick sandstone walls against the long sun.
        p.put(Mat.FOUNDATION, Blocks.SMOOTH_SANDSTONE)
         .put(Mat.FLOOR, Blocks.SMOOTH_SANDSTONE)
         .put(Mat.FLOOR_ACCENT, Blocks.CUT_SANDSTONE)
         .put(Mat.WALL, Blocks.SANDSTONE)
         .put(Mat.WALL_ACCENT, Blocks.CHISELED_SANDSTONE)
         .put(Mat.PILLAR, Blocks.CUT_SANDSTONE)
         .put(Mat.STAIR, Blocks.SANDSTONE_STAIRS)
         .put(Mat.SLAB, Blocks.SANDSTONE_SLAB)
         .put(Mat.TABLE, Blocks.SANDSTONE_SLAB)
         // Flat roofs: nothing to shed in the desert but heat.
         .put(Mat.ROOF, Blocks.SMOOTH_SANDSTONE_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.SMOOTH_SANDSTONE_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.SMOOTH_SANDSTONE)
         .put(Mat.LOW_WALL, Blocks.SANDSTONE_WALL)
         .put(Mat.PATH, Blocks.SMOOTH_SANDSTONE)
         .put(Mat.CARPET, Blocks.ORANGE_CARPET)
         .put(Mat.BED, Blocks.ORANGE_BED)
         .put(Mat.BANNER, Blocks.RED_WALL_BANNER)
         .put(Mat.FLOWER, Blocks.DEAD_BUSH)
         .put(Mat.FLOWER_POT, Blocks.POTTED_DEAD_BUSH)
         .put(Mat.SAPLING, Blocks.ACACIA_SAPLING);
    }

    private static void snowy(CulturePalette p) {
        woodSet(p, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
                Blocks.SPRUCE_FENCE, Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_DOOR,
                Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_WALL_SIGN, Blocks.SPRUCE_LEAVES,
                Blocks.SPRUCE_SAPLING);
        p.put(Mat.FOUNDATION, Blocks.COBBLESTONE)
         .put(Mat.FLOOR_ACCENT, Blocks.STRIPPED_SPRUCE_WOOD)
         .put(Mat.WALL_ACCENT, Blocks.STRIPPED_SPRUCE_LOG)
         // Steep roofs so the snow slides off.
         .put(Mat.ROOF, Blocks.DARK_OAK_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.DARK_OAK_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.DARK_OAK_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.DARK_OAK_PLANKS)
         .put(Mat.LOW_WALL, Blocks.COBBLESTONE_WALL)
         .put(Mat.PATH, Blocks.GRAVEL)
         .put(Mat.CARPET, Blocks.WHITE_CARPET)
         .put(Mat.BED, Blocks.WHITE_BED)
         .put(Mat.FLOWER, Blocks.POPPY)
         .put(Mat.LEAVES, Blocks.SPRUCE_LEAVES);
    }

    private static void jungle(CulturePalette p) {
        woodSet(p, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_LOG, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB,
                Blocks.JUNGLE_FENCE, Blocks.JUNGLE_FENCE_GATE, Blocks.JUNGLE_DOOR,
                Blocks.JUNGLE_TRAPDOOR, Blocks.JUNGLE_WALL_SIGN, Blocks.JUNGLE_LEAVES,
                Blocks.JUNGLE_SAPLING);
        p.put(Mat.FOUNDATION, Blocks.MOSSY_COBBLESTONE)
         .put(Mat.FLOOR_ACCENT, Blocks.STRIPPED_JUNGLE_WOOD)
         .put(Mat.WALL_ACCENT, Blocks.STRIPPED_JUNGLE_LOG)
         // Thatch: green and leafy, like everything else here.
         .put(Mat.ROOF, Blocks.JUNGLE_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.JUNGLE_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.JUNGLE_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.JUNGLE_LEAVES)
         .put(Mat.LOW_WALL, Blocks.MOSSY_COBBLESTONE_WALL)
         .put(Mat.PATH, Blocks.MOSS_BLOCK)
         .put(Mat.CARPET, Blocks.LIME_CARPET)
         .put(Mat.FLOWER, Blocks.ORANGE_TULIP)
         .put(Mat.FLOWER_POT, Blocks.POTTED_ORANGE_TULIP)
         .put(Mat.BANNER, Blocks.GREEN_WALL_BANNER);
    }

    private static void swamp(CulturePalette p) {
        woodSet(p, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_STAIRS,
                Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_FENCE_GATE,
                Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.DARK_OAK_WALL_SIGN,
                Blocks.OAK_LEAVES, Blocks.OAK_SAPLING);
        p.put(Mat.FOUNDATION, Blocks.MOSSY_COBBLESTONE)
         .put(Mat.FLOOR_ACCENT, Blocks.STRIPPED_DARK_OAK_WOOD)
         .put(Mat.WALL_ACCENT, Blocks.STRIPPED_DARK_OAK_LOG)
         // Peat and turf roofs.
         .put(Mat.ROOF, Blocks.SPRUCE_STAIRS)
         .put(Mat.ROOF_BACK, Blocks.SPRUCE_STAIRS)
         .put(Mat.ROOF_SLAB, Blocks.SPRUCE_SLAB)
         .put(Mat.ROOF_BLOCK, Blocks.MOSS_BLOCK)
         .put(Mat.LOW_WALL, Blocks.MOSSY_COBBLESTONE_WALL)
         .put(Mat.PATH, Blocks.PODZOL)
         .put(Mat.CARPET, Blocks.BROWN_CARPET)
         .put(Mat.FLOWER, Blocks.BROWN_MUSHROOM)
         .put(Mat.FLOWER_POT, Blocks.POTTED_BROWN_MUSHROOM)
         .put(Mat.BANNER, Blocks.BLACK_WALL_BANNER);
    }
}
