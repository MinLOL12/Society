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
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
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
    /** Buildings advanced per pass, across all settlements. */
    private static final int BUILDINGS_PER_PASS = 4;
    /** Ticks between passes; building is meant to be watchable, not instant. */
    private static final int TICK_INTERVAL = 40;

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
