package io.github.minlol12.society.world.build;

import java.util.List;

import io.github.minlol12.society.core.build.Blueprint;
import io.github.minlol12.society.core.build.Blueprints;
import io.github.minlol12.society.core.build.Mat;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.types.CultureOrigin;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

/**
 * Lays a blueprint into the world one course at a time, so a player
 * standing in a village genuinely watches the walls go up over several
 * days rather than finding a finished building popped into place.
 *
 * <p>Each call places at most a slice of the remaining cells; the building
 * remembers how far it has got in {@link Building#placedCells()}.</p>
 */
public final class StructureBuilder {

    /** Cells laid per building per tick of the builder (one in-game day). */
    private static final int CELLS_PER_PASS = 600;

    private StructureBuilder() { }

    /**
     * Places the next slice of a building. Returns true when every cell of
     * the blueprint has been laid.
     */
    public static boolean placeNextSlice(ServerWorld world, Building building,
                                         CultureOrigin origin, boolean finishAll) {
        return placeNextSlice(world, building, origin, finishAll, null);
    }

    public static boolean placeNextSlice(ServerWorld world, Building building,
                                         CultureOrigin origin, boolean finishAll,
                                         VillagerEntity builder) {
        Blueprint blueprint = Blueprints.of(building.type());
        // NBT structures are authored templates, not synthetic Society block
        // blueprints.  Never put down stakes/scaffolding as a substitute: the
        // finished CTOV/AdoraBuild NBT is the only thing this path can place.
        if (blueprint.solidCells() == 0 && Blueprints.usesCTOV(building.type())) {
            if (!isValidBuildSite(world, building, blueprint)) return false;
            if (!finishAll) return false;
            BlockPos anchor = groundAnchor(world, building, blueprint);
            boolean placed = io.github.minlol12.society.core.build.CTOVStructureLoader.placeAny(
                    world, Blueprints.ctovCandidates(building.type(), origin), anchor, building.rotation());
            if (placed) {
                building.setPlacedCells(Integer.MAX_VALUE);
                return true;
            }
            return false;
        }
        List<Blueprint.Cell> cells = blueprint.orderedCells();
        if (cells.isEmpty()) return true;

        CulturePalette palette = CulturePalette.of(origin);
        int from = Math.min(building.placedCells(), cells.size());

        int target = finishAll
                ? cells.size()
                : (int) Math.floor(cells.size() * building.fraction());
        target = Math.min(cells.size(), Math.max(target, from));
        int limit = Math.min(target, from + CELLS_PER_PASS);

        BlockPos anchor = groundAnchor(world, building, blueprint);

        if (builder != null && from < cells.size() && !finishAll) {
            Blueprint.Cell c = cells.get(from);
            BlockPos targetPos = anchor.add(rotateX(c, blueprint, building.rotation()),
                    c.y + blueprint.baseOffset(),
                    rotateZ(c, blueprint, building.rotation()));
            builder.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.15D);
            builder.getLookControl().lookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
            builder.swingHand(Hand.MAIN_HAND, true);
        }

        for (int i = from; i < limit; i++) {
            Blueprint.Cell cell = cells.get(i);
            BlockPos pos = anchor.add(rotateX(cell, blueprint, building.rotation()),
                    cell.y + blueprint.baseOffset(),
                    rotateZ(cell, blueprint, building.rotation()));
            if (cell.y == 0) {
                ensureFoundation(world, pos);
            }
            place(world, pos, cell.mat, palette, building.rotation(), builder);
        }
        building.setPlacedCells(limit);
        return limit >= cells.size();
    }

    /** Removes a building's blocks - used when a site is abandoned. */
    public static void clear(ServerWorld world, Building building) {
        Blueprint blueprint = Blueprints.of(building.type());
        // An unfinished NBT site has no synthetic placeholder blocks to clear.
        if (blueprint.solidCells() == 0 && Blueprints.usesCTOV(building.type())) return;
        BlockPos anchor = groundAnchor(world, building, blueprint);
        for (Blueprint.Cell cell : blueprint.orderedCells()) {
            BlockPos pos = anchor.add(rotateX(cell, blueprint, building.rotation()),
                    cell.y + blueprint.baseOffset(),
                    rotateZ(cell, blueprint, building.rotation()));
            if (!world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    // =====================================================================
    // Geometry
    // =====================================================================

    /**
     * The world position of the blueprint's (0,0,0) corner, dropped onto
     * the terrain so buildings sit on the ground instead of floating.
     */
    public static BlockPos groundAnchor(ServerWorld world, Building building, Blueprint blueprint) {
        int cornerX = building.x() - blueprint.width() / 2;
        int cornerZ = building.z() - blueprint.depth() / 2;
        int cY = findSolidGroundY(world, building.x(), building.z());
        int y1 = findSolidGroundY(world, cornerX, cornerZ);
        int y2 = findSolidGroundY(world, cornerX + blueprint.width() - 1, cornerZ);
        int y3 = findSolidGroundY(world, cornerX, cornerZ + blueprint.depth() - 1);
        int y4 = findSolidGroundY(world, cornerX + blueprint.width() - 1, cornerZ + blueprint.depth() - 1);
        int minSurface = Math.min(cY, Math.min(Math.min(y1, y2), Math.min(y3, y4)));
        return new BlockPos(cornerX, minSurface, cornerZ);
    }

    /**
     * A template may only be stamped onto genuinely level, dry, open land.
     * This is intentionally conservative: rejecting a plot is preferable to
     * filling a pond, shaving a hill, or embedding an NBT house in a cliff.
     */
    public static boolean isValidBuildSite(ServerWorld world, Building building, Blueprint blueprint) {
        BlockPos anchor = groundAnchor(world, building, blueprint);
        int width = (building.rotation() & 1) == 0 ? blueprint.width() : blueprint.depth();
        int depth = (building.rotation() & 1) == 0 ? blueprint.depth() : blueprint.width();
        int groundY = anchor.getY();

        for (int x = anchor.getX(); x < anchor.getX() + width; x++) {
            for (int z = anchor.getZ(); z < anchor.getZ() + depth; z++) {
                // Every column must have the same solid surface.  Water,
                // leaves, partial blocks and slopes are not valid foundations.
                if (findSolidGroundY(world, x, z) != groundY) return false;
                BlockPos foundation = new BlockPos(x, groundY - 1, z);
                BlockState foundationState = world.getBlockState(foundation);
                if (!foundationState.getFluidState().isEmpty()
                        || !foundationState.isOpaqueFullCube(world, foundation)) return false;

                // The complete logical template volume must already be open;
                // no terrain or another build is cleared to make room for it.
                for (int y = groundY; y < groundY + blueprint.height(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.isOutOfHeightLimit(pos)) return false;
                    BlockState state = world.getBlockState(pos);
                    if (!state.getFluidState().isEmpty() || (!state.isAir() && !state.isReplaceable())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int findSolidGroundY(ServerWorld world, int x, int z) {
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        while (y > world.getBottomY() + 5) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && !state.isReplaceable() && state.isOpaqueFullCube(world, new BlockPos(x, y, z))) {
                String name = state.getBlock().getTranslationKey().toLowerCase();
                if (!name.contains("leaves") && !name.contains("log") && !name.contains("wood")
                        && !name.contains("stair") && !name.contains("slab") && !name.contains("roof")
                        && !name.contains("planks") && !name.contains("door") && !name.contains("bed")) {
                    return y + 1;
                }
            }
            y--;
        }
        return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static void ensureFoundation(ServerWorld world, BlockPos pos) {
        BlockPos.Mutable cur = pos.down().mutableCopy();
        for (int step = 0; step < 12; step++) {
            if (world.isOutOfHeightLimit(cur)) break;
            BlockState state = world.getBlockState(cur);
            if (state.isOpaqueFullCube(world, cur) && !state.isReplaceable()) break;
            world.setBlockState(cur, Blocks.DIRT.getDefaultState(), Block.NOTIFY_LISTENERS);
            cur.move(Direction.DOWN);
        }
    }

    private static void ensureSolidContainer(ServerWorld world, BlockPos pos) {
        ensureFoundation(world, pos);
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos side = pos.offset(dir);
            BlockState state = world.getBlockState(side);
            if (state.isAir() || state.isReplaceable() || (!state.isOpaqueFullCube(world, side)
                    && !state.isOf(Blocks.FARMLAND) && !state.isOf(Blocks.WATER))) {
                world.setBlockState(side, Blocks.DIRT.getDefaultState(), Block.NOTIFY_LISTENERS);
                ensureFoundation(world, side);
            }
        }
    }

    private static int rotateX(Blueprint.Cell cell, Blueprint bp, int rotation) {
        switch (rotation) {
            case 1: return bp.depth() - 1 - cell.z;
            case 2: return bp.width() - 1 - cell.x;
            case 3: return cell.z;
            default: return cell.x;
        }
    }

    private static int rotateZ(Blueprint.Cell cell, Blueprint bp, int rotation) {
        switch (rotation) {
            case 1: return cell.x;
            case 2: return bp.depth() - 1 - cell.z;
            case 3: return bp.width() - 1 - cell.x;
            default: return cell.z;
        }
    }

    /** The blueprint's "front" (-Z) after rotation. */
    private static Direction facing(int rotation) {
        switch (rotation) {
            case 1: return Direction.EAST;
            case 2: return Direction.SOUTH;
            case 3: return Direction.WEST;
            default: return Direction.NORTH;
        }
    }

    // =====================================================================
    // Placement
    // =====================================================================

    private static void place(ServerWorld world, BlockPos pos, Mat mat,
                              CulturePalette palette, int rotation,
                              VillagerEntity builder) {
        if (mat == Mat.SKIP) return;
        if (world.isOutOfHeightLimit(pos)) return;

        if (mat == Mat.AIR) {
            if (!world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
            return;
        }

        Block block = palette.block(mat);
        BlockState state = block.getDefaultState();
        Direction front = facing(rotation);

        switch (mat) {
            case DOOR:
                placeDoor(world, pos, block, front);
                if (builder != null) {
                    world.playSound(null, pos, block.getDefaultState().getSoundGroup().getPlaceSound(),
                            SoundCategory.BLOCKS, 0.8F, 1.0F);
                }
                return;
            case BED:
                placeBed(world, pos, block, front);
                if (builder != null) {
                    world.playSound(null, pos, block.getDefaultState().getSoundGroup().getPlaceSound(),
                            SoundCategory.BLOCKS, 0.8F, 1.0F);
                }
                return;
            case STAIR:
            case ROOF:
            case ROOF_BACK:
                state = orientStairs(state, front, mat);
                break;
            case SLAB:
            case ROOF_SLAB:
                if (state.contains(Properties.SLAB_TYPE)) {
                    state = state.with(Properties.SLAB_TYPE, SlabType.BOTTOM);
                }
                break;
            case TABLE:
                if (state.contains(Properties.SLAB_TYPE)) {
                    state = state.with(Properties.SLAB_TYPE, SlabType.TOP);
                }
                break;
            case TORCH:
            case SIGN:
            case BANNER:
                // Wall-mounted things face out of the building.
                if (state.contains(Properties.HORIZONTAL_FACING)) {
                    state = state.with(Properties.HORIZONTAL_FACING, front);
                }
                break;
            case LADDER:
                if (state.contains(Properties.HORIZONTAL_FACING)) {
                    state = state.with(Properties.HORIZONTAL_FACING, front.getOpposite());
                }
                break;
            case HANGING_LANTERN:
                if (state.contains(Properties.HANGING)) {
                    state = state.with(Properties.HANGING, Boolean.TRUE);
                }
                break;
            case TRAPDOOR:
                if (state.contains(Properties.HORIZONTAL_FACING)) {
                    state = state.with(Properties.HORIZONTAL_FACING, front);
                }
                break;
            case CROP:
                if (state.contains(Properties.AGE_7)) {
                    state = state.with(Properties.AGE_7, Integer.valueOf(7));
                }
                break;
            case FARMLAND:
                if (state.contains(Properties.MOISTURE)) {
                    state = state.with(Properties.MOISTURE, Integer.valueOf(7));
                }
                break;
            case CAULDRON:
                if (state.contains(Properties.LEVEL_3)) {
                    state = state.with(Properties.LEVEL_3, Integer.valueOf(3));
                }
                break;
            case PILLAR:
            case LOG:
            case BEAM:
                if (state.contains(Properties.AXIS)) {
                    state = state.with(Properties.AXIS,
                            mat == Mat.BEAM ? front.getAxis() : Direction.Axis.Y);
                }
                break;
            case LEAVES:
                if (state.contains(Properties.PERSISTENT)) {
                    state = state.with(Properties.PERSISTENT, Boolean.TRUE);
                }
                break;
            case CHEST:
            case BARREL:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case LOOM:
            case LECTERN:
            case GRINDSTONE:
            case BEEHIVE:
            case GATE:
                if (state.contains(Properties.HORIZONTAL_FACING)) {
                    state = state.with(Properties.HORIZONTAL_FACING, front);
                }
                break;
            case ANVIL:
                if (state.contains(Properties.HORIZONTAL_FACING)) {
                    state = state.with(Properties.HORIZONTAL_FACING, front.rotateYClockwise());
                }
                break;
            case PAINTING:
                placePainting(world, pos, front);
                return;
            case WATER:
                ensureSolidContainer(world, pos);
                world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
                if (builder != null) {
                    world.playSound(null, pos, Blocks.WATER.getDefaultState().getSoundGroup().getPlaceSound(),
                            SoundCategory.BLOCKS, 0.8F, 1.0F);
                }
                return;
            default:
                break;
        }

        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        if (builder != null) {
            world.playSound(null, pos, state.getSoundGroup().getPlaceSound(),
                    SoundCategory.BLOCKS, 0.8F, 1.0F);
        }
    }

    /**
     * Roof stairs point down their own slope - the near side one way, the
     * far side the other - so a gable actually looks like a gable. Ordinary
     * stairs face into the room they furnish.
     */
    private static BlockState orientStairs(BlockState state, Direction front, Mat mat) {
        if (!state.contains(Properties.HORIZONTAL_FACING)) return state;
        Direction facing;
        if (mat == Mat.ROOF) {
            facing = front;
        } else if (mat == Mat.ROOF_BACK) {
            facing = front.getOpposite();
        } else {
            facing = front.getOpposite();
        }
        return state.with(Properties.HORIZONTAL_FACING, facing);
    }

    private static void placeDoor(ServerWorld world, BlockPos pos, Block block, Direction front) {
        if (!(block instanceof DoorBlock)) {
            world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
            return;
        }
        BlockState lower = block.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, front)
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upper = block.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, front)
                .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
        world.setBlockState(pos, lower, Block.NOTIFY_LISTENERS);
        world.setBlockState(pos.up(), upper, Block.NOTIFY_LISTENERS);
    }

    private static void placeBed(ServerWorld world, BlockPos pos, Block block, Direction front) {
        if (!(block instanceof BedBlock)) {
            world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
            return;
        }
        // Beds need two blocks; lay the foot into open space where we can.
        Direction bedDirection = front.getOpposite();
        BlockPos head = pos.offset(bedDirection);
        if (!world.getBlockState(head).isAir() && !world.getBlockState(head).isReplaceable()) {
            bedDirection = front;
            head = pos.offset(bedDirection);
        }
        BlockState foot = block.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, bedDirection)
                .with(Properties.BED_PART, BedPart.FOOT);
        BlockState headState = block.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, bedDirection)
                .with(Properties.BED_PART, BedPart.HEAD);
        world.setBlockState(pos, foot, Block.NOTIFY_LISTENERS);
        world.setBlockState(head, headState, Block.NOTIFY_LISTENERS);
    }

    /**
     * Spawns a painting entity on the wall at the given position. Paintings
     * are placed on the wall face indicated by the building's front direction,
     * adding culture and visual richness to interiors.
     */
    private static void placePainting(ServerWorld world, BlockPos pos, Direction front) {
        // Find a solid wall adjacent to this position to attach a painting to.
        // Try all horizontal directions to find a suitable wall face.
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos wallPos = pos.offset(dir);
            BlockState wallState = world.getBlockState(wallPos);
            if (!wallState.isOpaqueFullCube(world, wallPos)) continue;

            // Only place paintings sometimes (~40% chance) so interiors aren't
            // plastered floor-to-ceiling.
            if (world.random.nextInt(100) >= 40) continue;

            // Avoid placing if there's already a painting nearby.
            if (!world.getEntitiesByClass(PaintingEntity.class,
                    new net.minecraft.util.math.Box(
                            pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2,
                            pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2),
                    e -> true).isEmpty()) {
                continue;
            }

            RegistryEntry<PaintingVariant> variant = randomPaintingVariant(world);
            if (variant == null) continue;

            PaintingEntity painting = new PaintingEntity(world, wallPos, dir.getOpposite(), variant);
            if (painting.canStayAttached()) {
                world.spawnEntity(painting);
                return;
            }
        }
    }

    /** Picks a random painting variant from the registry. */
    private static RegistryEntry<PaintingVariant> randomPaintingVariant(ServerWorld world) {
        var registry = world.getRegistryManager()
                .get(net.minecraft.registry.RegistryKeys.PAINTING_VARIANT);
        var all = registry.streamEntries().toList();
        if (all.isEmpty()) return null;
        return all.get(world.random.nextInt(all.size()));
    }
}
