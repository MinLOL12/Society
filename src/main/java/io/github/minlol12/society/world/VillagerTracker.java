package io.github.minlol12.society.world;

import java.util.Optional;

import io.github.minlol12.society.core.VillagerSnapshot;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

/** Builds engine snapshots out of live villager brains and bodies. */
public final class VillagerTracker {

    private VillagerTracker() { }

    public static VillagerSnapshot snapshot(VillagerEntity villager, ServerWorld world) {
        RegistryKey<World> dimension = world.getRegistryKey();

        Optional<GlobalPos> home = villager.getBrain().getOptionalMemory(MemoryModuleType.HOME);
        Optional<GlobalPos> meeting = villager.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT);
        Optional<GlobalPos> job = villager.getBrain().getOptionalMemory(MemoryModuleType.JOB_SITE);

        BlockPos homePos = home.isPresent() && home.get().getDimension() == dimension
                ? home.get().getPos() : null;
        BlockPos meetPos = meeting.isPresent() && meeting.get().getDimension() == dimension
                ? meeting.get().getPos() : null;
        BlockPos jobPos = job.isPresent() && job.get().getDimension() == dimension
                ? job.get().getPos() : null;

        String professionId = "minecraft:none";
        Identifier id = Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
        if (id != null) {
            professionId = id.toString();
        }

        return new VillagerSnapshot(
                villager.getUuidAsString(),
                villager.getX(), villager.getY(), villager.getZ(),
                homePos != null,
                homePos == null ? 0 : homePos.getX(),
                homePos == null ? 0 : homePos.getY(),
                homePos == null ? 0 : homePos.getZ(),
                meetPos != null,
                meetPos == null ? 0 : meetPos.getX(),
                meetPos == null ? 0 : meetPos.getY(),
                meetPos == null ? 0 : meetPos.getZ(),
                jobPos != null,
                jobPos == null ? 0 : jobPos.getX(),
                jobPos == null ? 0 : jobPos.getY(),
                jobPos == null ? 0 : jobPos.getZ(),
                professionId,
                villager.isBaby());
    }
}
