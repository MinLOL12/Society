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
import net.minecraft.state.property.Properties;
import net.minecraft.server.world.ServerWorld;
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
    private static final int CELLS_PER_PASS = 220;

    private StructureBuilder() { }

    /**
     * Places the next slice of a building. Returns true when every cell of
     * the blueprint has been laid.
     */
    public static boolean placeNextSlice(ServerWorld world, Building building,
                                         CultureOrigin origin, boolean finishAll) {
        Blueprint blueprint = Blueprints.of(building.type());
        List<Blueprint.Cell> cells = blueprint.orderedCells();
        if (cells.isEmpty()) return true;

        CulturePalette palette = CulturePalette.of(origin);
        int from = Math.min(building.placedCells(), cells.size());

        // How much of the building should stand by now: the world never
        // runs ahead of the labour the settlement has actually put in.
        int target = finishAll
                ? cells.size()
                : (int) Math.floor(cells.size() * building.fraction());
        target = Math.min(cells.size(), Math.max(target, from));
        int limit = Math.min(target, from + CELLS_PER_PASS);

        BlockPos anchor = groundAnchor(world, building, blueprint);

        for (int i = from; i < limit; i++) {
            Blueprint.Cell cell = cells.get(i);
            BlockPos pos = anchor.add(rotateX(cell, blueprint, building.rotation()),
                    cell.y + blueprint.baseOffset(),
                    rotateZ(cell, blueprint, building.rotation()));
            place(world, pos, cell.mat, palette, building.rotation());
        }
        building.setPlacedCells(limit);
        return limit >= cells.size();
    }

    /** Removes a building's blocks - used when a site is abandoned. */
    public static void clear(ServerWorld world, Building building) {
        Blueprint blueprint = Blueprints.of(building.type());
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
        int surface = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, building.x(), building.z());
        return new BlockPos(cornerX, surface, cornerZ);
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
                              CulturePalette palette, int rotation) {
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
                return;
            case BED:
                placeBed(world, pos, block, front);
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
            case WATER:
                world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
                return;
            default:
                break;
        }

        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
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
}
