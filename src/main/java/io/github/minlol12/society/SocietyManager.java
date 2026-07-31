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
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.Skill;
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
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
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
    /** Guards stationed at watchtowers who will shoot arrows at enemies. */
    private final Map<UUID, String> watchtowerArchers = new HashMap<UUID, String>();
    private final Map<UUID, Integer> lastArcherShotTick = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> watchtowerGuardHome = new HashMap<UUID, Integer>(); // building index

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

        // --- Watchtower Archers: guards on towers shoot bows at enemies ---
        processWatchtowerArchers(currentTick);

        // --- Village Herald: announces events with particles/sounds ---
        processVillageHerald(currentTick);

        // --- Wandering Traders: trader entities walk between settlements ---
        processWanderingTraders(currentTick);

        // --- Festival Fireworks: happy settlements celebrate with fireworks ---
        processFestivalFireworks(currentTick);

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
    // Watchtower Archers: guards stationed at watchtowers use bows
    // =====================================================================

    /**
     * Guards at watchtowers scan for hostile mobs in a wide radius and fire
     * arrows at them. A guard with a bow can pick off threats from the safety
     * of the tower, giving the settlement real ranged defence. Guards return
     * to their tower after each volley.
     */
    private void processWatchtowerArchers(int currentTick) {
        if (currentTick % 40 != 0) return; // fire every 2 seconds

        for (Settlement s : engine.settlements().values()) {
            if (s.isDestroyed()) continue;

            // Find all watchtowers for this settlement
            List<Building> towers = new ArrayList<Building>();
            for (Building b : s.buildings()) {
                if (b.type() == StructureType.WATCHTOWER && b.isComplete() && !b.isRuined()) {
                    towers.add(b);
                }
            }
            if (towers.isEmpty()) continue;

            // Find hostile mobs near the settlement
            List<MobEntity> hostiles = overworld.getEntitiesByClass(MobEntity.class,
                    new Box(s.centerX() - 48, s.centerY() - 16, s.centerZ() - 48,
                            s.centerX() + 48, s.centerY() + 16, s.centerZ() + 48),
                    e -> e.isAlive() && e instanceof HostileEntity);
            if (hostiles.isEmpty()) continue;

            // Find guard villagers near watchtowers who have bows
            for (UUID uuid : loadedVillagers) {
                Entity entity = overworld.getEntity(uuid);
                if (!(entity instanceof VillagerEntity) || !entity.isAlive()) continue;
                VillagerEntity villager = (VillagerEntity) entity;

                Citizen c = engine.citizenForEntity(uuid.toString());
                if (c == null || c.profession() != SimProfession.GUARD) continue;

                // Check if this guard is near a watchtower (within 16 blocks)
                Building nearestTower = null;
                double nearestDist = Double.MAX_VALUE;
                for (Building tower : towers) {
                    double dist = tower.distanceTo((int) villager.getX(), (int) villager.getZ());
                    if (dist < nearestDist && dist < 16.0) {
                        nearestDist = dist;
                        nearestTower = tower;
                    }
                }
                if (nearestTower == null) continue;

                // Guard must have a bow (settlement has bows in stock)
                if (s.stock(Good.BOWS) < 0.1) continue;

                // Find the nearest hostile target
                MobEntity target = null;
                double targetDist = Double.MAX_VALUE;
                for (MobEntity hostile : hostiles) {
                    double dist = villager.squaredDistanceTo(hostile);
                    if (dist < targetDist) {
                        targetDist = dist;
                        target = hostile;
                    }
                }
                if (target == null || targetDist > 900.0) continue; // ~30 blocks range

                // Face the target and look down from the tower
                villager.getLookControl().lookAt(target, 30.0F, 60.0F);

                // Shoot an arrow
                Integer lastShot = lastArcherShotTick.getOrDefault(uuid, -60);
                if (currentTick - lastShot.intValue() < 60) continue; // 3-second cooldown

                lastArcherShotTick.put(uuid, Integer.valueOf(currentTick));

                // Create and shoot the arrow from above
                ArrowEntity arrow = new ArrowEntity(overworld, villager);
                double dx = target.getX() - villager.getX();
                double dy = (target.getY() + target.getHeight() * 0.5) - (villager.getY() + 1.5);
                double dz = target.getZ() - villager.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                // Launch with a high arc for the tower elevation advantage
                arrow.setVelocity(dx, dy + dist * 0.15, dz, 1.6F, 8.0F);
                arrow.setDamage(4.0 + c.skillLevel(Skill.COMBAT) * 0.05);
                arrow.setPierceLevel((byte) 1);
                overworld.spawnEntity(arrow);

                overworld.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                        SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F);

                // Guards sometimes shout when they fire
                if (engine.random().nextDouble() < 0.25) {
                    engine.record(EventType.DEFENCE, s,
                            "A watchtower guard of " + s.name()
                                    + " fires upon " + target.getType().getName().getString() + "s!");
                }
            }
        }
    }

    // =====================================================================
    // Village Herald: announces settlement events with visible effects
    // =====================================================================

    /** Last day each settlement had a herald announcement. */
    private final Map<String, Integer> lastHeraldDay = new HashMap<String, Integer>();

    /**
     * The village herald stands at the bell plaza or notice board and
     * broadcasts announcements with sound and visible effects so players
     * notice something important happened. Births, marriages, discoveries,
     * and construction are all proclaimed.
     */
    private void processVillageHerald(int currentTick) {
        if (currentTick % 600 != 0) return; // check every 30 seconds

        for (Settlement s : engine.settlements().values()) {
            if (s.isDestroyed()) continue;

            // Herald needs a notice board, meeting hall, or bell plaza
            boolean hasHeraldSpot = s.has(StructureType.NOTICE_BOARD)
                    || s.has(StructureType.BELL_PLAZA)
                    || s.has(StructureType.MEETING_HALL);
            if (!hasHeraldSpot) continue;

            Integer lastDay = lastHeraldDay.getOrDefault(s.id(), -5);
            if (engine.day() - lastDay.intValue() < 1) continue;

            // Check recent chronicle entries for something to announce
            List<io.github.minlol12.society.core.data.ChronicleEntry> entries = s.chronicle();
            if (entries.isEmpty()) continue;

            io.github.minlol12.society.core.data.ChronicleEntry latest =
                    entries.get(entries.size() - 1);
            if (latest.day() <= lastDay.intValue()) continue;

            lastHeraldDay.put(s.id(), Integer.valueOf(engine.day()));

            // Find the bell or plaza location for the announcement
            int heraldX = s.centerX();
            int heraldY = s.centerY();
            int heraldZ = s.centerZ();
            for (Building b : s.buildings()) {
                if (b.type() == StructureType.BELL_PLAZA && b.isComplete()) {
                    heraldX = b.x();
                    heraldY = b.y();
                    heraldZ = b.z();
                    break;
                }
                if (b.type() == StructureType.NOTICE_BOARD && b.isComplete()) {
                    heraldX = b.x();
                    heraldY = b.y();
                    heraldZ = b.z();
                }
            }

            // Ring the bell for important events
            if (latest.type().level() == io.github.minlol12.society.core.types.EventType.Level.GLOBAL
                    || latest.type() == io.github.minlol12.society.core.types.EventType.MARRIAGE
                    || latest.type() == io.github.minlol12.society.core.types.EventType.DISCOVERY) {
                overworld.playSound(null, heraldX, heraldY, heraldZ,
                        SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2.0F, 1.0F);
            }

            // Spawn a note particle effect at the herald's position
            Vec3d heraldPos = new Vec3d(heraldX + 0.5, heraldY + 2.5, heraldZ + 0.5);
            if (!PlayerLookup.around(overworld, heraldPos, 64.0).isEmpty()) {
                overworld.spawnParticles(ParticleTypes.POOF,
                        heraldPos.x, heraldPos.y, heraldPos.z, 5, 0.5, 0.5, 0.5, 0.02);
            }
        }
    }

    // =====================================================================
    // Wandering Traders: trader entities walk between settlements
    // =====================================================================

    /** Map of active wandering trader entity UUID -> source settlement ID. */
    private final Map<UUID, String> wanderingTraders = new HashMap<UUID, String>();
    /** Track last spawn day per settlement to limit frequency. */
    private final Map<String, Integer> lastTraderSpawnDay = new HashMap<String, Integer>();

    /**
     * Periodically spawns wandering trader entities that walk from one
     * settlement to another carrying goods. Players can see these traders
     * on the road, creating a living, breathing world with commerce in motion.
     */
    private void processWanderingTraders(int currentTick) {
        if (currentTick % 400 != 0) return; // check every 20 seconds

        // Remove dead/missing traders
        for (UUID traderId : new ArrayList<UUID>(wanderingTraders.keySet())) {
            Entity e = overworld.getEntity(traderId);
            if (e == null || !e.isAlive()) {
                wanderingTraders.remove(traderId);
            }
        }

        // Limit total active traders
        if (wanderingTraders.size() >= 6) return;

        List<Settlement> alive = new ArrayList<Settlement>();
        for (Settlement s : engine.settlements().values()) {
            if (!s.isDestroyed() && s.cachedPopulation() > 3) alive.add(s);
        }
        if (alive.size() < 2) return;

        // Pick a settlement to spawn from
        for (Settlement source : alive) {
            Integer lastSpawn = lastTraderSpawnDay.getOrDefault(source.id(), -15);
            if (engine.day() - lastSpawn.intValue() < 8) continue;
            if (engine.random().nextDouble() >= 0.25) continue;

            // Find a destination settlement (different from source)
            Settlement dest = null;
            double bestScore = 0;
            for (Settlement candidate : alive) {
                if (candidate.id().equals(source.id())) continue;
                double distance = source.distanceTo(candidate);
                if (distance < 50 || distance > 400) continue;
                double score = candidate.morale() / distance;
                if (engine.findRoute(source.id(), candidate.id()) != null) score *= 2.0;
                if (score > bestScore) {
                    bestScore = score;
                    dest = candidate;
                }
            }
            if (dest == null) continue;

            lastTraderSpawnDay.put(source.id(), Integer.valueOf(engine.day()));

            // Spawn the trader villager at the source settlement
            VillagerEntity trader = EntityType.VILLAGER.create(overworld);
            if (trader == null) continue;

            trader.refreshPositionAndAngles(
                    source.centerX() + engine.random().nextInt(6) - 3,
                    source.centerY() + 1,
                    source.centerZ() + engine.random().nextInt(6) - 3,
                    engine.random().nextFloat() * 360.0f, 0.0f);
            trader.setPersistent();
            trader.setCustomName(Text.literal("Wandering Trader from " + source.name()));
            trader.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.EMERALD));

            // Give the trader a walking target toward the destination
            overworld.spawnEntity(trader);
            trader.getNavigation().startMovingTo(
                    dest.centerX(), dest.centerY(), dest.centerZ(), 0.6D);

            wanderingTraders.put(trader.getUuid(), source.id());
            engine.record(EventType.TRADE_ROUTE, source,
                    "A trader sets out from " + source.name()
                            + " heading for " + dest.name() + ".");
            break; // one trader per pass
        }
    }

    // =====================================================================
    // Festival Fireworks: happy settlements celebrate with fireworks
    // =====================================================================

    private final Map<String, Integer> lastFireworksDay = new HashMap<String, Integer>();

    /**
     * Settlements with high morale and enough population occasionally
     * launch firework rockets during the evening, celebrating their
     * prosperity. The fireworks go off at the bell plaza or the town
     * centre and can be heard and seen from a distance.
     */
    private void processFestivalFireworks(int currentTick) {
        // Only during evening hours (12000-18000 game ticks)
        long timeOfDay = overworld.getTimeOfDay() % 24000;
        if (timeOfDay < 12000 || timeOfDay > 18000) return;
        if (currentTick % 60 != 0) return; // check every 3 seconds

        for (Settlement s : engine.settlements().values()) {
            if (s.isDestroyed()) continue;
            if (s.morale() < 70.0) continue;
            if (s.cachedPopulation() < 8) continue;

            Integer lastDay = lastFireworksDay.getOrDefault(s.id(), -3);
            if (engine.day() - lastDay.intValue() < 2) continue;
            if (engine.random().nextDouble() >= 0.15) continue;

            lastFireworksDay.put(s.id(), Integer.valueOf(engine.day()));

            // Find the celebration spot
            int fx = s.centerX();
            int fy = s.centerY();
            int fz = s.centerZ();
            for (Building b : s.buildings()) {
                if (b.type() == StructureType.BELL_PLAZA && b.isComplete()) {
                    fx = b.x(); fy = b.y(); fz = b.z(); break;
                }
                if (b.type() == StructureType.FOUNTAIN && b.isComplete()) {
                    fx = b.x(); fy = b.y(); fz = b.z(); break;
                }
            }

            // Launch 3-5 fireworks with random colors
            int count = 3 + engine.random().nextInt(3);
            ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
            // Set flight duration and explosion colors via NBT
            net.minecraft.nbt.NbtCompound fireworks = new net.minecraft.nbt.NbtCompound();
            fireworks.putByte("Flight", (byte) (1 + engine.random().nextInt(2)));
            net.minecraft.nbt.NbtList explosions = new net.minecraft.nbt.NbtList();
            net.minecraft.nbt.NbtCompound explosion = new net.minecraft.nbt.NbtCompound();
            int[] colors = new int[]{0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFFA500};
            explosion.putIntArray("Colors", new int[]{
                    colors[engine.random().nextInt(colors.length)],
                    colors[engine.random().nextInt(colors.length)]
            });
            explosion.putByte("Type", (byte) engine.random().nextInt(4));
            explosions.add(explosion);
            fireworks.put("Explosions", explosions);
            fireworkStack.getOrCreateNbt().put("Fireworks", fireworks);

            for (int i = 0; i < count; i++) {
                double offsetX = (engine.random().nextDouble() - 0.5) * 8;
                double offsetZ = (engine.random().nextDouble() - 0.5) * 8;
                FireworkRocketEntity firework = new FireworkRocketEntity(
                        overworld,
                        fx + 0.5 + offsetX, fy + 3, fz + 0.5 + offsetZ,
                        fireworkStack.copy());
                overworld.spawnEntity(firework);
            }

            overworld.playSound(null, fx, fy + 3, fz,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT,
                    1.5F, 0.8F + engine.random().nextFloat() * 0.4F);

            engine.record(EventType.FESTIVAL, s,
                    s.name() + " celebrates with fireworks! The sky lights up"
                            + " over the " + s.culture().origin().festivalName().toLowerCase() + ".");
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
