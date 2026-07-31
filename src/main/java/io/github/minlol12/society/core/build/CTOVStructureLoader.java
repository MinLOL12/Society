package io.github.minlol12.society.core.build;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Loads village structures directly from
 * ChoiceTheorem's Overhauled Village (CurseForge 6459787)
 * using its NBT files.
 */
public final class CTOVStructureLoader {

    private static final String CTOV_NAMESPACE = "ctov";

    private CTOVStructureLoader() {}

    /**
     * Returns the StructureTemplate for a given CTOV structure ID.
     * Example IDs: "village/plains/house", "village/desert/temple", etc.
     */
    public static StructureTemplate load(ServerWorld world, String structurePath) {
        Identifier id = new Identifier(CTOV_NAMESPACE, structurePath);
        StructureTemplateManager manager = world.getStructureTemplateManager();
        return manager.getTemplateOrThrow(id);
    }

    /**
     * Places a CTOV structure at the given position.
     */
    public static void place(ServerWorld world, String structurePath, BlockPos pos) {
        StructureTemplate template = load(world, structurePath);
        if (template != null) {
            template.place(world, pos, pos, 
                new net.minecraft.structure.StructurePlacementData(), 
                world.random, 2);
        }
    }
}