package io.github.minlol12.society.world;

import io.github.minlol12.society.core.build.Blueprints;
import io.github.minlol12.society.core.build.CTOVStructureLoader;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.PlayerStructure;
import io.github.minlol12.society.core.types.CultureOrigin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.JigsawReplacementStructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;

/**
 * Puts player-claimed structures into the world. Premade structures are
 * stamped from the NBT catalogue (CTOV, and any custom
 * {@code namespace:path} the player names); hand-built claims get a bell
 * marker rung onto their corner so everyone knows the ground is spoken for.
 */
public final class PlayerStructurePlacer {

    private PlayerStructurePlacer() { }

    /**
     * Stamps a premade structure so its centre lands on the player's spot.
     * Returns false when no template could be found or placed.
     */
    public static boolean stampPremade(ServerWorld world, StructureType type,
                                       CultureOrigin origin, int centerX, int centerZ,
                                       int rotation) {
        StructureTemplate template = firstTemplate(world, type, origin);
        if (template == null) {
            // Fall back to the loader's candidate sweep; it reports misses.
            BlockPos anchor = groundAnchor(world, centerX, centerZ);
            return CTOVStructureLoader.placeAny(world,
                    Blueprints.ctovCandidates(type, origin), anchor, rotation);
        }
        Vec3i size = template.getSize();
        int halfX = size.getX() / 2;
        int halfZ = size.getZ() / 2;
        BlockPos anchor = groundAnchor(world, centerX - halfX, centerZ - halfZ);
        return placeTemplate(world, template, anchor, rotation);
    }

    /**
     * Stamps a named NBT template ({@code namespace:path} or a bare path in
     * the {@code ctov} namespace) centred on the given spot.
     */
    public static boolean stampNbt(ServerWorld world, String nbtPath,
                                   int centerX, int centerZ, int rotation) {
        Identifier id;
        try {
            id = parseIdentifier(nbtPath);
        } catch (IllegalArgumentException e) {
            return false;
        }
        StructureTemplateManager manager = world.getStructureTemplateManager();
        StructureTemplate template = manager.getTemplate(id).orElse(null);
        if (template == null) {
            return false;
        }
        Vec3i size = template.getSize();
        BlockPos anchor = groundAnchor(world,
                centerX - size.getX() / 2, centerZ - size.getZ() / 2);
        return placeTemplate(world, template, anchor, rotation);
    }

    /** Rings a bell onto the claimed box so the claim is visible in-world. */
    public static void markClaim(ServerWorld world, PlayerStructure structure) {
        int x = structure.centerX();
        int z = structure.centerZ();
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) + 1;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.isReplaceable()) {
            world.setBlockState(pos, Blocks.BELL.getDefaultState(), 3);
        }
        world.playSound(null, x, y, z, SoundEvents.BLOCK_BELL_USE,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    // =====================================================================
    // Plumbing
    // =====================================================================

    private static StructureTemplate firstTemplate(ServerWorld world, StructureType type,
                                                   CultureOrigin origin) {
        StructureTemplateManager manager = world.getStructureTemplateManager();
        for (String path : Blueprints.ctovCandidates(type, origin)) {
            StructureTemplate template = manager.getTemplate(
                    new Identifier("ctov", path)).orElse(null);
            if (template != null) {
                return template;
            }
        }
        return null;
    }

    private static boolean placeTemplate(ServerWorld world, StructureTemplate template,
                                         BlockPos anchor, int rotation) {
        StructurePlacementData data = new StructurePlacementData()
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
                .addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
        switch (((rotation % 4) + 4) % 4) {
            case 1: data.setRotation(BlockRotation.CLOCKWISE_90); break;
            case 2: data.setRotation(BlockRotation.CLOCKWISE_180); break;
            case 3: data.setRotation(BlockRotation.COUNTERCLOCKWISE_90); break;
            default: break;
        }
        template.place(world, anchor, anchor, data, world.random, 2);
        return true;
    }

    private static BlockPos groundAnchor(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        while (y > world.getBottomY() + 5) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && !state.isReplaceable() && state.isOpaqueFullCube(world, new BlockPos(x, y, z))) {
                return new BlockPos(x, y + 1, z);
            }
            y--;
        }
        return new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    private static Identifier parseIdentifier(String path) {
        int colon = path.indexOf(':');
        if (colon > 0) {
            return new Identifier(path.substring(0, colon), path.substring(colon + 1));
        }
        return new Identifier("ctov", path);
    }
}
