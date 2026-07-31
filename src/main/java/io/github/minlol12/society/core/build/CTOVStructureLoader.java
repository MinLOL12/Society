package io.github.minlol12.society.core.build;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.minlol12.society.SocietyMod;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.JigsawReplacementStructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Loads village structures directly from
 * ChoiceTheorem's Overhauled Village (CurseForge 6459787)
 * using its NBT files.
 */
public final class CTOVStructureLoader {

    private static final String CTOV_NAMESPACE = "ctov";
    private static final Set<String> REPORTED_MISSING = new HashSet<String>();

    private CTOVStructureLoader() {}

    /**
     * Returns the StructureTemplate for a given CTOV structure ID.
     * Example IDs: "village/plains/house/small_1", "village/desert/town_center", etc.
     */
    public static StructureTemplate load(ServerWorld world, String structurePath) {
        Identifier id = new Identifier(CTOV_NAMESPACE, structurePath);
        StructureTemplateManager manager = world.getStructureTemplateManager();
        return manager.getTemplate(id).orElse(null);
    }

    /**
     * Places the first available CTOV template from {@code structurePaths}.
     * Returns true only when a template was found and placed.
     */
    public static boolean placeAny(ServerWorld world, List<String> structurePaths,
                                   BlockPos pos, int rotation) {
        if (structurePaths == null || structurePaths.isEmpty()) return false;
        for (String path : structurePaths) {
            if (place(world, path, pos, rotation)) {
                return true;
            }
        }
        reportMissing(structurePaths);
        return false;
    }

    /**
     * Places a CTOV structure at the given position.
     * Returns false when CTOV is not installed or the requested NBT id does
     * not exist, so callers do not silently mark an invisible building as done.
     */
    public static boolean place(ServerWorld world, String structurePath, BlockPos pos, int rotation) {
        StructureTemplate template = load(world, structurePath);
        if (template == null) {
            return false;
        }

        StructurePlacementData data = new StructurePlacementData()
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
                .addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
        BlockRotation rot = BlockRotation.NONE;
        switch (rotation % 4) {
            case 1: rot = BlockRotation.CLOCKWISE_90; break;
            case 2: rot = BlockRotation.CLOCKWISE_180; break;
            case 3: rot = BlockRotation.COUNTERCLOCKWISE_90; break;
            default: break;
        }
        if (rot != BlockRotation.NONE) {
            data.setRotation(rot);
        }
        template.place(world, pos, pos, data, world.random, 2);
        return true;
    }

    private static void reportMissing(List<String> structurePaths) {
        String primary = structurePaths.get(0);
        if (REPORTED_MISSING.add(primary)) {
            SocietyMod.LOGGER.warn("[Society] Could not find any CTOV NBT template for {}. "
                    + "Tried {} candidate(s); is ChoiceTheorem's Overhauled Village file 6459787 installed?",
                    primary, Integer.valueOf(structurePaths.size()));
        }
    }
}
