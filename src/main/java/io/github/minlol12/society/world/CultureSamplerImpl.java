package io.github.minlol12.society.world;

import io.github.minlol12.society.core.types.CultureOrigin;
import io.github.minlol12.society.core.CultureSampler;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Reads the biome at a position and judges what kind of folk would settle
 * there: coast makes Saltfolk, snow makes Frostfolk, jungle makes Leaffolk.
 */
public final class CultureSamplerImpl implements CultureSampler {

    private final World world;

    public CultureSamplerImpl(World world) {
        this.world = world;
    }

    @Override
    public CultureOrigin sample(int x, int z) {
        BlockPos pos = new BlockPos(x, Math.max(world.getBottomY() + 1, Math.min(world.getTopY() - 1, 64)), z);
        RegistryEntry<Biome> entry = world.getBiome(pos);
        if (entry.isIn(BiomeTags.IS_OCEAN) || entry.isIn(BiomeTags.IS_DEEP_OCEAN)
                || entry.isIn(BiomeTags.IS_BEACH) || entry.isIn(BiomeTags.IS_RIVER)) {
            return CultureOrigin.COASTAL;
        }
        if (entry.isIn(BiomeTags.IS_MOUNTAIN) || entry.isIn(BiomeTags.IS_HILL)) {
            return CultureOrigin.MOUNTAIN;
        }
        if (entry.isIn(BiomeTags.IS_JUNGLE)) {
            return CultureOrigin.JUNGLE;
        }
        if (entry.matchesKey(BiomeKeys.SWAMP) || entry.matchesKey(BiomeKeys.MANGROVE_SWAMP)) {
            return CultureOrigin.SWAMP;
        }
        if (entry.isIn(BiomeTags.IS_TAIGA)) {
            return entry.value().getTemperature() <= 0.2f ? CultureOrigin.SNOWY : CultureOrigin.FOREST;
        }
        if (entry.isIn(BiomeTags.IS_BADLANDS)) {
            return CultureOrigin.DESERT;
        }
        if (entry.isIn(BiomeTags.IS_FOREST)) {
            return CultureOrigin.FOREST;
        }
        if (entry.isIn(BiomeTags.IS_SAVANNA)) {
            return CultureOrigin.PLAINS;
        }
        float temperature = entry.value().getTemperature();
        if (temperature <= 0.2f) {
            return CultureOrigin.SNOWY;
        }
        if (temperature >= 0.95f) {
            return CultureOrigin.DESERT;
        }
        return CultureOrigin.PLAINS;
    }
}
