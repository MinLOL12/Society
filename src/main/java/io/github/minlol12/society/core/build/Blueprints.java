package io.github.minlol12.society.core.build;

import java.util.EnumMap;
import java.util.Map;

/**
 * Village structures come from ChoiceTheorem's Overhauled Village
 * (CurseForge file 6459787) via its NBT structure system.
 *
 * This class now acts as a thin wrapper that can load real
 * CTOV NBT structures when a ServerWorld is available.
 */
public final class Blueprints {

    private static final Map<StructureType, String> CTOV_PATHS = new EnumMap<>(StructureType.class);

    static {
        // Map our StructureTypes to CTOV NBT paths (examples - adjust as needed)
        CTOV_PATHS.put(StructureType.COTTAGE, "village/plains/house");
        CTOV_PATHS.put(StructureType.FAMILY_HOUSE, "village/plains/house_large");
        CTOV_PATHS.put(StructureType.BARN, "village/plains/barn");
        CTOV_PATHS.put(StructureType.BLACKSMITH, "village/plains/smithy");
        CTOV_PATHS.put(StructureType.TOWN_HALL, "village/plains/temple");
        // Add more mappings for other StructureTypes as needed
    }

    private Blueprints() {}

    public static synchronized Blueprint of(StructureType type) {
        // Return a minimal placeholder - real NBT loading happens in CTOVStructureLoader
        return new Blueprint(type, 1, 1, 1);
    }

    /**
     * Returns the CTOV NBT path for a given StructureType, or null if not mapped.
     */
    public static String getCTOVPath(StructureType type) {
        return CTOV_PATHS.get(type);
    }
}