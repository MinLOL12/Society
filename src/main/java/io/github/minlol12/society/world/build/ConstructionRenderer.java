package io.github.minlol12.society.world.build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.build.Blueprint;
import io.github.minlol12.society.core.build.Blueprints;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.Settlement;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Keeps the world's blocks in step with the ledger's building sites.
 * Each pass looks at settlements near players and lays down however much
 * of each building the settlement has actually paid for in labour, so a
 * player standing in a growing village sees walls rise course by course.
 *
 * <p>Work is spread over ticks and skipped entirely for chunks nobody has
 * loaded, so the world never stalls raising a city nobody is looking at.</p>
 */
public final class ConstructionRenderer {

    /** Only render building work this close to a player. */
    private static final double RENDER_RADIUS = 160.0;
    /** Buildings advanced per pass, across all settlements. Settlements pour
     *  their energy into expansion, so several rise at once. */
    private static final int BUILDINGS_PER_PASS = 8;
    /** Ticks between passes; building is meant to be watchable, not instant,
     *  but towns are eager and the work moves along briskly. */
    private static final int TICK_INTERVAL = 20;

    private final ServerWorld world;
    private final SocietyEngine engine;
    /** Buildings fully laid into the world already. */
    private final Set<String> rendered = new HashSet<String>();
    private int tickCounter;
    private int cursor;

    public ConstructionRenderer(ServerWorld world, SocietyEngine engine) {
        this.world = world;
        this.engine = engine;
    }

    /** Called every server tick; does real work only occasionally. */
    public void tick() {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        List<Building> queue = new ArrayList<Building>();
        List<Settlement> owners = new ArrayList<Settlement>();
        for (Settlement s : engine.settlements().values()) {
            if (s.isDestroyed()) continue;
            if (!hasWitness(s)) continue;
            for (Building b : s.buildings()) {
                if (b.isRuined()) continue;
                if (rendered.contains(b.id()) && b.isComplete()) continue;
                queue.add(b);
                owners.add(s);
            }
        }
        if (queue.isEmpty()) return;

        // Round-robin so no one settlement hogs the builder.
        int done = 0;
        for (int i = 0; i < queue.size() && done < BUILDINGS_PER_PASS; i++) {
            int index = (cursor + i) % queue.size();
            Building building = queue.get(index);
            Settlement owner = owners.get(index);
            if (!chunkReady(building)) continue;
            VillagerEntity builder = findBuilderEntity(building, owner);
            boolean finished = StructureBuilder.placeNextSlice(world, building,
                    owner.culture().origin(), building.isComplete(), builder);
            if (finished && building.isComplete()) {
                rendered.add(building.id());
                // Decorate completed buildings with paintings
                decorateWithPaintings(world, building);
            }
            done++;
        }
        cursor = queue.isEmpty() ? 0 : (cursor + done + 1) % queue.size();
    }

    private VillagerEntity findBuilderEntity(Building building, Settlement owner) {
        if (!building.workerId().isEmpty()) {
            io.github.minlol12.society.core.data.Citizen c = engine.citizens().get(building.workerId());
            if (c != null && !c.entityUuid().isEmpty()) {
                try {
                    net.minecraft.entity.Entity e = world.getEntity(java.util.UUID.fromString(c.entityUuid()));
                    if (e instanceof VillagerEntity && e.isAlive()) {
                        return (VillagerEntity) e;
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        }
        List<VillagerEntity> nearby = world.getEntitiesByClass(VillagerEntity.class,
                new net.minecraft.util.math.Box(building.x() - 64, building.y() - 32, building.z() - 64,
                        building.x() + 64, building.y() + 32, building.z() + 64),
                e -> e.isAlive());
        for (VillagerEntity ve : nearby) {
            return ve;
        }
        return null;
    }

    /**
     * Scans the interior walls of a completed building and places random
     * paintings. This makes interiors feel lived-in and culturally rich.
     * Manors, town halls, libraries, and meeting halls get more paintings.
     */
    private void decorateWithPaintings(ServerWorld world, Building building) {
        Blueprint blueprint = Blueprints.of(building.type());
        int half = Math.max(blueprint.width(), blueprint.depth()) / 2 + 2;
        int searchRadius = Math.max(half, 6);

        // Determine how many paintings this building type deserves
        int maxPaintings;
        switch (building.type()) {
            case MANOR: maxPaintings = 5; break;
            case TOWN_HALL: maxPaintings = 4; break;
            case LIBRARY: maxPaintings = 3; break;
            case MEETING_HALL: maxPaintings = 3; break;
            case SCHOOL: maxPaintings = 2; break;
            case SHRINE: maxPaintings = 2; break;
            case INN: maxPaintings = 2; break;
            case FAMILY_HOUSE: maxPaintings = 1; break;
            case COTTAGE: maxPaintings = 1; break;
            default: maxPaintings = 0; break;
        }
        if (maxPaintings == 0) return;

        // Count existing paintings nearby
        List<PaintingEntity> existing = world.getEntitiesByClass(PaintingEntity.class,
                new Box(building.x() - searchRadius, building.y() - 4, building.z() - searchRadius,
                        building.x() + searchRadius, building.y() + 16, building.z() + searchRadius),
                e -> true);
        int toPlace = maxPaintings - existing.size();
        if (toPlace <= 0) return;

        // Scan for interior wall surfaces
        int placed = 0;
        for (int y = building.y() + 1; y < building.y() + 10 && placed < toPlace; y++) {
            for (int dx = -searchRadius; dx <= searchRadius && placed < toPlace; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius && placed < toPlace; dz++) {
                    if (world.random.nextInt(100) >= 25) continue; // sparse scan

                    int x = building.x() + dx;
                    int z = building.z() + dz;
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Must be air where painting goes
                    if (!state.isAir()) continue;

                    // Must have a solid wall behind it
                    for (Direction dir : Direction.Type.HORIZONTAL) {
                        BlockPos wallPos = pos.offset(dir);
                        BlockState wallState = world.getBlockState(wallPos);
                        if (wallState.isOpaqueFullCube(world, wallPos)) {
                            // Check no painting already too close
                            if (!world.getEntitiesByClass(PaintingEntity.class,
                                    new Box(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                                            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                                    e -> true).isEmpty()) continue;

                            PaintingEntity painting = new PaintingEntity(world, wallPos, dir.getOpposite());
                            RegistryEntry<PaintingVariant> variant = randomPaintingVariant(world);
                            if (variant != null) {
                                painting.setVariant(variant);
                                if (painting.canStayAttached()) {
                                    world.spawnEntity(painting);
                                    placed++;
                                }
                            }
                            break;
                        }
                    }
                }
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

    /** A building whose ruin was repaired must be laid down again. */
    public void forget(String buildingId) {
        rendered.remove(buildingId);
    }

    private boolean hasWitness(Settlement s) {
        Vec3d centre = new Vec3d(s.centerX(), s.centerY(), s.centerZ());
        return !PlayerLookup.around(world, centre, RENDER_RADIUS).isEmpty();
    }

    /** Never write blocks into chunks the server has not loaded. */
    private boolean chunkReady(Building building) {
        Blueprint blueprint = Blueprints.of(building.type());
        int half = Math.max(blueprint.width(), blueprint.depth()) / 2 + 1;
        // CTOV NBT pieces can be larger than Society's logical plot. Check a
        // safer area so template placement does not spill into unloaded chunks.
        if (Blueprints.usesCTOV(building.type())) {
            half = Math.max(half, 16);
        }
        int minChunkX = (building.x() - half) >> 4;
        int maxChunkX = (building.x() + half) >> 4;
        int minChunkZ = (building.z() - half) >> 4;
        int maxChunkZ = (building.z() + half) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) return false;
            }
        }
        return true;
    }
}
