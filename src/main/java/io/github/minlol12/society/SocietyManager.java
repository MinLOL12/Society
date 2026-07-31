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
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.data.Building;
import io.github.minlol12.society.core.data.Citizen;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Trait;
import io.github.minlol12.society.gui.CitizenScreen;
import io.github.minlol12.society.gui.SettlementScreen;
import io.github.minlol12.society.item.SocietyItems;
import io.github.minlol12.society.state.SocietyPersistentState;
import io.github.minlol12.society.world.CultureSamplerImpl;
import io.github.minlol12.society.world.DeathCauses;
import io.github.minlol12.society.world.VillagerTracker;
import io.github.minlol12.society.world.build.ConstructionRenderer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
    /** Villagers may only land an unarmed hit from roughly one block away. */
    private static final double VILLAGER_MELEE_RANGE_SQUARED = 2.25D;
    /** Food is handed out by a Chef/Cook at most once per villager per 10 seconds. */
    private static final int CHEF_FEED_INTERVAL_TICKS = 200;

    private static SocietyManager instance;

    private final MinecraftServer server;
    private final ServerWorld overworld;
    private final SocietyEngine engine;
    private final SocietyPersistentState state;
    private final CultureSamplerImpl sampler;
    private final Set<UUID> loadedVillagers = new HashSet<UUID>();
    /** Entity uuid -> citizen id for villagers the engine asked us to manifest. */
    private final Map<UUID, String> pendingManifests = new HashMap<UUID, String>();
    /** Lays the ledger's building sites into real blocks, a course at a time. */
    private final ConstructionRenderer construction;
    private int lastProcessedDay = -1;

    private final Map<UUID, Integer> lastVillagerAttackTick = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> lastChefMealTick = new HashMap<UUID, Integer>();
    private final Set<UUID> aggressiveVillagers = new HashSet<UUID>();
    private final List<ArrestEscort> activeArrests = new ArrayList<ArrestEscort>();
    private final Map<String, Integer> lastArmyDeployDay = new HashMap<String, Integer>();

    private static class ArrestEscort {
        UUID copId;
        UUID prisonerId;
        BlockPos jailPos;
        int timer;
        String settlementId;
    }

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
        this.construction = new ConstructionRenderer(overworld, engine);
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
            if (!(entity instanceof VillagerEntity)) {
                return ActionResult.PASS;
            }
            // Sneaking with an empty hand, or holding the Chronicle, opens
            // the villager's page. Ordinary right-clicks still trade.
            boolean chronicle = player.getStackInHand(hand).isOf(SocietyItems.SOCIETY_CHRONICLE);
            boolean inspectGesture = chronicle
                    || (player.isSneaking() && player.getStackInHand(hand).isEmpty()
                        && SocietyMod.config().sneakToInspect);
            if (!inspectGesture) {
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
        // Blocks go up continuously, not only on the day boundary: this is
        // what makes growth something you watch rather than something you
        // come back to find finished.
        if (SocietyMod.config().buildStructures) {
            construction.tick();
        }

        processLiveWorldInteractions();

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

        for (UUID uuid : new ArrayList<UUID>(loadedVillagers)) {
            Citizen c = engine.citizenForEntity(uuid.toString());
            Entity entity = overworld.getEntity(uuid);
            if (c != null && !c.isAlive() && entity != null) {
                entity.discard();
                loadedVillagers.remove(uuid);
            }
        }

        state.setData(engine.save());
        engine.clearDirty();
    }

    private void feedVillagersFromChefs(int currentTick) {
        List<VillagerEntity> chefs = new ArrayList<VillagerEntity>();
        for (UUID id : loadedVillagers) {
            Entity entity = overworld.getEntity(id);
            if (entity instanceof VillagerEntity && entity.isAlive() && isCookingVillager((VillagerEntity) entity)) {
                chefs.add((VillagerEntity) entity);
            }
        }
        for (VillagerEntity chef : chefs) {
            List<VillagerEntity> diners = overworld.getEntitiesByClass(VillagerEntity.class,
                    chef.getBoundingBox().expand(3.0D),
                    villager -> villager.isAlive() && villager != chef && villager.getHealth() < villager.getMaxHealth());
            for (VillagerEntity diner : diners) {
                UUID dinerId = diner.getUuid();
                if (currentTick - lastChefMealTick.getOrDefault(dinerId, Integer.valueOf(-CHEF_FEED_INTERVAL_TICKS)).intValue()
                        < CHEF_FEED_INTERVAL_TICKS) continue;
                lastChefMealTick.put(dinerId, Integer.valueOf(currentTick));
                chef.getLookControl().lookAt(diner, 30.0F, 30.0F);
                chef.swingHand(Hand.MAIN_HAND, true);
                diner.heal(2.0F);
                overworld.playSound(null, diner.getX(), diner.getY(), diner.getZ(),
                        SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 0.7F, 1.0F);
                break; // one serving per chef per interaction pass
            }
        }
    }

    private static boolean isCookingVillager(VillagerEntity villager) {
        String profession = villager.getVillagerData().getProfession().toString().toLowerCase(java.util.Locale.ROOT);
        return profession.contains("chef") || profession.contains("cook");
    }

    /**
     * Steers a villager directly away from a threat, so the timid and the
     * young survive an encounter instead of every last one charging in.
     */
    private static void fleeFrom(VillagerEntity villager, MobEntity threat) {
        double dx = villager.getX() - threat.getX();
        double dz = villager.getZ() - threat.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4D) {
            // Standing on top of one another: bolt in a random direction.
            dx = villager.getRandom().nextDouble() - 0.5D;
            dz = villager.getRandom().nextDouble() - 0.5D;
            len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4D) {
                dx = 1.0D;
                dz = 0.0D;
                len = 1.0D;
            }
        }
        double fleeDistance = 14.0D;
        double destX = villager.getX() + (dx / len) * fleeDistance;
        double destZ = villager.getZ() + (dz / len) * fleeDistance;
        villager.getNavigation().startMovingTo(destX, villager.getY(), destZ, 1.2D);
    }

    /**
     * Teleports a player to a settlement's most recently finished building -
     * or to its heart when nothing has been completed yet - so a builder's
     * work can be admired the moment it is done.
     */
    public void teleportToConstruction(ServerPlayerEntity player, Settlement settlement) {
        Building latest = null;
        for (Building b : settlement.buildings()) {
            if (b.isRuined() || !b.isComplete()) continue;
            if (latest == null || b.completedDay() > latest.completedDay()) {
                latest = b;
            }
        }
        double destX = (latest != null ? latest.x() : settlement.centerX()) + 0.5D;
        double destY = (latest != null ? latest.y() : settlement.centerY()) + 1.0D;
        double destZ = (latest != null ? latest.z() : settlement.centerZ()) + 0.5D;
        player.teleport(overworld, destX, destY, destZ,
                java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                player.getYaw(), player.getPitch());
        String where = latest != null
                ? "the newly finished " + latest.type().display().toLowerCase()
                : settlement.name();
        player.sendMessage(Text.literal("[Society] Teleported to " + where + ".")
                .formatted(Formatting.GOLD), false);
    }

    private void processLiveWorldInteractions() {
        int currentTick = server.getTicks();

        // 1. Villagers react to threats: the brave stand and fight, the timid
        //    and the young turn and run. Not everyone charges a monster.
        for (UUID uuid : new ArrayList<UUID>(loadedVillagers)) {
            Entity entity = overworld.getEntity(uuid);
            if (!(entity instanceof VillagerEntity) || !entity.isAlive()) continue;
            VillagerEntity villager = (VillagerEntity) entity;

            List<MobEntity> hostiles = overworld.getEntitiesByClass(MobEntity.class,
                    villager.getBoundingBox().expand(16.0),
                    e -> e.isAlive() && (e instanceof HostileEntity || e.getTarget() == villager));
            if (hostiles.isEmpty()) continue;
            MobEntity target = hostiles.get(0);

            int aggression = 50;
            int caution = 50;
            Citizen c = engine.citizenForEntity(uuid.toString());
            if (c != null) {
                aggression = c.personality().get(Trait.AGGRESSION);
                caution = c.personality().get(Trait.CAUTION);
            }

            // Children never engage a monster - they run for safety.
            if (villager.isBaby()) {
                fleeFrom(villager, target);
                continue;
            }
            // The cautious and the unaggressive flee instead of fighting; the
            // fraction that stands its ground is decided by temperament, so a
            // town does not throw every soul at a single zombie.
            int fleeChance = Math.max(5, Math.min(95, 35 + caution - aggression));
            if (overworld.getRandom().nextInt(100) < fleeChance) {
                fleeFrom(villager, target);
                continue;
            }

            // The brave close to melee range first; this deliberately remains
            // an unarmed, one-block strike.
            double distanceSquared = villager.squaredDistanceTo(target);
            if (distanceSquared > VILLAGER_MELEE_RANGE_SQUARED) {
                villager.getNavigation().startMovingTo(target, 1.15D);
                villager.getLookControl().lookAt(target, 30.0F, 30.0F);
                continue;
            }
            if (currentTick - lastVillagerAttackTick.getOrDefault(uuid, Integer.valueOf(-100)).intValue() >= 20) {
                lastVillagerAttackTick.put(uuid, Integer.valueOf(currentTick));
                target.damage(overworld.getDamageSources().mobAttack(villager),
                        (float) (2.0 + aggression * 0.05));
                villager.swingHand(Hand.MAIN_HAND, true);
                overworld.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }

        // Chef's Delight's Chef/Cook professions keep nearby villagers fed.
        // This stays registry-id based, so Society never needs to compile against
        // the optional mod and works with either of its cooking professions.
        feedVillagersFromChefs(currentTick);

        // 2. Military Base Army Deployment when >= 3 hostile mobs attacking villagers
        if (currentTick % 20 == 0) {
            for (Settlement s : engine.settlements().values()) {
                if (s.isDestroyed()) continue;
                List<MobEntity> settlementHostiles = overworld.getEntitiesByClass(MobEntity.class,
                        new Box(s.centerX() - 64, s.centerY() - 32, s.centerZ() - 64,
                                s.centerX() + 64, s.centerY() + 32, s.centerZ() + 64),
                        e -> e.isAlive() && (e instanceof HostileEntity || e.getTarget() instanceof VillagerEntity));
                if (settlementHostiles.size() >= 3) {
                    if (engine.day() > lastArmyDeployDay.getOrDefault(s.id(), Integer.valueOf(-10)).intValue()) {
                        lastArmyDeployDay.put(s.id(), Integer.valueOf(engine.day()));
                        Building base = null;
                        for (Building b : s.buildings()) {
                            if (b.type() == StructureType.MILITARY_BASE && !b.isRuined()) {
                                base = b;
                                break;
                            }
                        }
                        int deployX = base != null ? base.x() : s.centerX();
                        int deployY = base != null ? base.y() : s.centerY();
                        int deployZ = base != null ? base.z() : s.centerZ();

                        for (int i = 0; i < 3; i++) {
                            VillagerEntity soldier = EntityType.VILLAGER.create(overworld);
                            if (soldier != null) {
                                soldier.refreshPositionAndAngles(deployX + (i - 1) * 2, deployY + 1, deployZ, 0, 0);
                                soldier.setCustomName(Text.literal(s.name() + " Guard"));
                                // Give the guard a sword for visual defense
                                soldier.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                                soldier.setTarget(settlementHostiles.get(i % settlementHostiles.size()));
                                overworld.spawnEntity(soldier);
                            }
                        }
                        engine.record(EventType.DEFENCE, s,
                                "An army deploys from the Military Base of " + s.name()
                                + " to defend against " + settlementHostiles.size() + " hostile invaders!");
                    }
                }
            }
        }

        // 3. Rare brawl -> Cops call -> Cops arrest & escort to Jail
        if (currentTick % 100 == 0 && loadedVillagers.size() >= 2) {
            if (overworld.getRandom().nextInt(6) == 0 && aggressiveVillagers.isEmpty()) {
                List<UUID> allLoaded = new ArrayList<UUID>(loadedVillagers);
                int count = Math.min(allLoaded.size(), 2 + overworld.getRandom().nextInt(2));
                for (int i = 0; i < count; i++) {
                    UUID id = allLoaded.get(i);
                    aggressiveVillagers.add(id);
                    Entity e = overworld.getEntity(id);
                    if (e instanceof VillagerEntity ve) {
                        ve.swingHand(Hand.MAIN_HAND, true);
                        overworld.playSound(null, ve.getX(), ve.getY(), ve.getZ(),
                                SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    }
                }
                if (!aggressiveVillagers.isEmpty()) {
                    UUID firstId = aggressiveVillagers.iterator().next();
                    Entity firstEntity = overworld.getEntity(firstId);
                    if (firstEntity instanceof VillagerEntity ve) {
                        overworld.playSound(null, ve.getX(), ve.getY(), ve.getZ(),
                                SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.5F, 1.0F);
                        Citizen c = engine.citizenForEntity(firstId.toString());
                        Settlement s = c != null ? engine.settlements().get(c.homeSettlementId()) : null;
                        if (s != null) {
                            engine.record(EventType.POLICE, s,
                                    "Villagers call the cops on aggressive brawlers in " + s.name() + "!");
                            Building jail = null;
                            for (Building b : s.buildings()) {
                                if (b.type() == StructureType.JAIL && !b.isRuined()) {
                                    jail = b;
                                    break;
                                }
                            }
                            BlockPos jailPos = jail != null
                                    ? new BlockPos(jail.x(), jail.y(), jail.z())
                                    : new BlockPos(s.centerX(), s.centerY(), s.centerZ());
                            for (UUID brawlerId : new ArrayList<UUID>(aggressiveVillagers)) {
                                Entity brawler = overworld.getEntity(brawlerId);
                                if (brawler instanceof VillagerEntity bVe && bVe.isAlive()) {
                                    VillagerEntity cop = EntityType.VILLAGER.create(overworld);
                                    if (cop != null) {
                                        cop.refreshPositionAndAngles(bVe.getX() + 1, bVe.getY(), bVe.getZ() + 1, 0, 0);
                                        cop.setCustomName(Text.literal("Guard"));
                                        cop.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                                        overworld.spawnEntity(cop);
                                        ArrestEscort escort = new ArrestEscort();
                                        escort.copId = cop.getUuid();
                                        escort.prisonerId = brawlerId;
                                        escort.jailPos = jailPos;
                                        escort.timer = 0;
                                        escort.settlementId = s.id();
                                        activeArrests.add(escort);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Process active police escorts
        for (int i = activeArrests.size() - 1; i >= 0; i--) {
            ArrestEscort escort = activeArrests.get(i);
            escort.timer++;
            Entity copEnt = overworld.getEntity(escort.copId);
            Entity prisonerEnt = overworld.getEntity(escort.prisonerId);
            boolean copAlive = copEnt instanceof IronGolemEntity && copEnt.isAlive();
            boolean prisonerAlive = prisonerEnt instanceof VillagerEntity && prisonerEnt.isAlive();
            if (!copAlive || !prisonerAlive || escort.timer > 160
                    || (copAlive && copEnt.squaredDistanceTo(escort.jailPos.getX(), escort.jailPos.getY(), escort.jailPos.getZ()) < 25.0D)) {
                if (prisonerAlive) {
                    prisonerEnt.refreshPositionAndAngles(escort.jailPos.getX(), escort.jailPos.getY() + 1, escort.jailPos.getZ(), 0, 0);
                }
                if (copAlive) {
                    copEnt.discard();
                }
                aggressiveVillagers.remove(escort.prisonerId);
                activeArrests.remove(i);
                Settlement s = engine.settlements().get(escort.settlementId);
                if (s != null) {
                    engine.record(EventType.POLICE, s,
                            "Cops arrested aggressive villagers and escorted them to the Jail in " + s.name() + ".");
                }
            } else if (copEnt instanceof IronGolemEntity c && prisonerEnt instanceof VillagerEntity p) {
                c.getNavigation().startMovingTo(escort.jailPos.getX(), escort.jailPos.getY(), escort.jailPos.getZ(), 1.25D);
                p.getNavigation().startMovingTo(c.getX(), c.getY(), c.getZ(), 1.1D);
            }
        }
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
            MutableText text = Text.literal("[Society] ").formatted(Formatting.GOLD)
                    .append(Text.literal(announcement.text).formatted(Formatting.GRAY));
            // A settlement the player can walk to in person can also be reached
            // in a click - most usefully to admire a freshly finished building.
            if (!announcement.settlementName.isEmpty()) {
                text.append(buildTeleportLink(announcement.settlementName));
            }
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

    /** A clickable "[Teleport]" that runs the visit command for a settlement. */
    private static MutableText buildTeleportLink(String settlementName) {
        net.minecraft.text.Style style = net.minecraft.text.Style.EMPTY
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(new net.minecraft.text.ClickEvent(
                        net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                        "/society visit " + settlementName))
                .withHoverEvent(new net.minecraft.text.HoverEvent(
                        net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Teleport to " + settlementName)));
        return Text.literal(" [Teleport]").setStyle(style);
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
        if (SocietyMod.config().villagerScreen) {
            SettlementScreen.open(player, engine, settlement);
        } else {
            SocietyText.printSettlementPage(player, engine, settlement);
        }
    }

    /**
     * Clicking a villager: opens their stat page. Any citizen the ledger
     * has not met yet is written into it first, so the screen is never
     * empty just because a villager wandered in from the wild.
     */
    public void inspectCitizen(ServerPlayerEntity player, VillagerEntity villager) {
        Citizen citizen = engine.citizenForEntity(villager.getUuidAsString());
        if (citizen == null && villager.isAlive()) {
            citizen = engine.citizenForSnapshot(
                    VillagerTracker.snapshot(villager, overworld), sampler);
            loadedVillagers.add(villager.getUuid());
        }
        String profession = "";
        net.minecraft.util.Identifier id = net.minecraft.registry.Registries.VILLAGER_PROFESSION
                .getId(villager.getVillagerData().getProfession());
        if (id != null) {
            profession = id.toString();
        }
        if (SocietyMod.config().villagerScreen) {
            CitizenScreen.open(player, engine, citizen, profession);
        } else {
            SocietyText.printCitizenCard(player, engine, citizen, profession);
        }
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
