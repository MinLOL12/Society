package io.github.minlol12.society;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.minlol12.society.core.Announcement;
import io.github.minlol12.society.core.DayContext;
import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.SpawnRequest;
import io.github.minlol12.society.core.VillagerSnapshot;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.item.SocietyItems;
import io.github.minlol12.society.state.SocietyPersistentState;
import io.github.minlol12.society.world.CultureSamplerImpl;
import io.github.minlol12.society.world.DeathCauses;
import io.github.minlol12.society.world.VillagerTracker;

import net.fabricmc.fabric.api.event.entity.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * The bridge between the simulation ledger and the living game world.
 * Tracks loaded villagers, drives the simulation forward once per in-game
 * day, turns ledger announcements into chat, and lets ledger citizens put
 * on flesh as real villagers when players are around to see it.
 */
public final class SocietyManager {

    /** Players further than this from a spawn point won't witness manifestations. */
    private static final double MANIFEST_WITNESS_RADIUS = 96.0;

    private static SocietyManager instance;

    private final MinecraftServer server;
    private final ServerWorld overworld;
    private final SocietyEngine engine;
    private final SocietyPersistentState state;
    private final CultureSamplerImpl sampler;
    private final Set<UUID> loadedVillagers = new HashSet<UUID>();
    /** Entity uuid -> citizen id for villagers the engine asked us to manifest. */
    private final Map<UUID, String> pendingManifests = new HashMap<UUID, String>();
    private int lastProcessedDay = -1;

    private SocietyManager(MinecraftServer server) {
        this.server = server;
        this.overworld = server.getOverworld();
        this.engine = new SocietyEngine(overworld.getSeed(), SocietyMod.config().toEngineConfig());
        this.sampler = new CultureSamplerImpl(overworld);
        this.state = overworld.getPersistentStateManager().getOrCreate(
                SocietyPersistentState::loadFromNbt, SocietyPersistentState::new,
                SocietyPersistentState.KEY);
        if (state.hasData()) {
            engine.load(state.data());
        }
    }

    public static SocietyManager get() {
        return instance;
    }

    public SocietyEngine engine() {
        return engine;
    }

    // =====================================================================
    // Event wiring
    // =====================================================================

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            instance = new SocietyManager(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (instance != null) {
                instance.state.setData(instance.engine.save());
                instance = null;
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (instance != null) {
                instance.tick();
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (instance != null) {
                instance.onEntityLoad(entity, world);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (instance != null && entity instanceof VillagerEntity) {
                instance.loadedVillagers.remove(entity.getUuid());
                instance.engine.onVillagerUnloaded(entity.getUuidAsString());
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(SocietyManager::onAfterDeath);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof VillagerEntity)
                    || !player.getStackInHand(hand).isOf(SocietyItems.SOCIETY_CHRONICLE)) {
                return ActionResult.PASS;
            }
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            SocietyManager manager = instance;
            if (manager != null && player instanceof ServerPlayerEntity) {
                manager.inspectCitizen((ServerPlayerEntity) player, (VillagerEntity) entity);
            }
            return ActionResult.SUCCESS;
        });
    }

    // =====================================================================
    // Tick
    // =====================================================================

    private void tick() {
        int day = (int) (overworld.getTimeOfDay() / 24000L);
        if (day == lastProcessedDay) {
            return;
        }
        lastProcessedDay = day;

        List<VillagerSnapshot> crowd = new ArrayList<VillagerSnapshot>();
        for (UUID uuid : new ArrayList<UUID>(loadedVillagers)) {
            Entity entity = overworld.getEntity(uuid);
            if (entity instanceof VillagerEntity && entity.isAlive()) {
                crowd.add(VillagerTracker.snapshot((VillagerEntity) entity, overworld));
            } else {
                loadedVillagers.remove(uuid);
            }
        }

        engine.processDaily(crowd, sampler,
                new DayContext(day, overworld.isRaining(), overworld.getSeed()));
        deliverAnnouncements();
        handleSpawnRequests();

        state.setData(engine.save());
        engine.clearDirty();
    }

    // =====================================================================
    // Entity lifecycle
    // =====================================================================

    private void onEntityLoad(Entity entity, ServerWorld world) {
        if (world != overworld || !(entity instanceof VillagerEntity)) {
            return;
        }
        VillagerEntity villager = (VillagerEntity) entity;
        if (!villager.isAlive()) {
            return;
        }
        loadedVillagers.add(entity.getUuid());
        VillagerSnapshot snapshot = VillagerTracker.snapshot(villager, world);

        String pendingCitizen = pendingManifests.remove(entity.getUuid());
        if (pendingCitizen != null) {
            Citizen bound = engine.onVillagerLoadedPending(pendingCitizen, snapshot, sampler);
            if (bound == null) {
                // The citizen died in the ledger before their body arrived.
                loadedVillagers.remove(entity.getUuid());
                entity.discard();
            }
        } else {
            engine.onVillagerLoaded(snapshot, sampler);
        }
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        SocietyManager manager = instance;
        if (manager == null || !(entity instanceof VillagerEntity)) {
            return;
        }
        manager.loadedVillagers.remove(entity.getUuid());
        manager.engine.onVillagerDied(entity.getUuidAsString(),
                DeathCauses.describe(source), DeathCauses.isViolent(source));
    }

    // =====================================================================
    // Announcements and manifestations
    // =====================================================================

    private void deliverAnnouncements() {
        for (Announcement announcement : engine.drainAnnouncements()) {
            if (announcement.severity == Announcement.Severity.NONE) {
                continue;
            }
            Text text = Text.literal("[Society] ").formatted(Formatting.GOLD)
                    .append(Text.literal(announcement.text).formatted(Formatting.GRAY));
            if (announcement.severity == Announcement.Severity.GLOBAL) {
                server.getPlayerManager().broadcast(text, false);
            } else {
                Vec3d pos = new Vec3d(announcement.x, 64, announcement.z);
                for (ServerPlayerEntity player : PlayerLookup.around(
                        overworld, pos, engine.cfg().announcementRadius)) {
                    player.sendMessage(text, false);
                }
            }
        }
    }

    private void handleSpawnRequests() {
        for (SpawnRequest request : engine.drainSpawns()) {
            if (!engine.cfg().enableManifestSpawns) {
                continue;
            }
            int blockX = (int) Math.floor(request.x);
            int blockZ = (int) Math.floor(request.z);
            // Only manifest where the world's chunks are live.
            if (!overworld.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                continue;
            }
            Vec3d pos = new Vec3d(request.x, request.y, request.z);
            if (PlayerLookup.around(overworld, pos, MANIFEST_WITNESS_RADIUS).isEmpty()) {
                continue; // manifesting unseen feels like cheating the stage
            }
            BlockPos surface = overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                    new BlockPos(blockX, (int) Math.floor(request.y), blockZ));
            VillagerEntity villager = EntityType.VILLAGER.create(overworld);
            if (villager == null) {
                continue;
            }
            villager.refreshPositionAndAngles(surface.getX() + 0.5, surface.getY(),
                    surface.getZ() + 0.5, java.util.concurrent.ThreadLocalRandom.current()
                            .nextFloat() * 360.0f, 0.0f);
            villager.setPersistent();
            if (request.baby) {
                villager.setBaby(true);
            }
            pendingManifests.put(villager.getUuid(), request.citizenId);
            if (!overworld.spawnEntity(villager)) {
                pendingManifests.remove(villager.getUuid());
            }
        }
    }

    // =====================================================================
    // Reading the world aloud
    // =====================================================================

    /** The chronicle's right-click: what's happening near the reader. */
    public void printLocalChronicle(ServerPlayerEntity player) {
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        Settlement settlement = engine.findSettlementNear(x, z, 96);
        SocietyText.printSettlementPage(player, engine, settlement);
    }

    /** The chronicle's right-click on a villager: their personal page. */
    public void inspectCitizen(ServerPlayerEntity player, VillagerEntity villager) {
        Citizen citizen = engine.citizenForEntity(villager.getUuidAsString());
        SocietyText.printCitizenCard(player, engine, citizen,
                villager.getVillagerData().getProfession().toString());
    }

    public ServerWorld overworld() {
        return overworld;
    }

    public MinecraftServer server() {
        return server;
    }

    public Set<UUID> loadedVillagers() {
        return loadedVillagers;
    }
}
