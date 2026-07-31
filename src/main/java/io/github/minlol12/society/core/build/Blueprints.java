package io.github.minlol12.society.core.build;

import java.util.EnumMap;
import java.util.Map;

/**
 * Village structures are now provided exclusively by
 * ChoiceTheorem's Overhauled Village (CurseForge file 6459787)
 * via its NBT structure system.
 *
 * This class is kept only for API compatibility. All custom
 * blueprint generation has been removed.
 */
public final class Blueprints {

    private static final Map<StructureType, Blueprint> CACHE =
            new EnumMap<>(StructureType.class);

    private Blueprints() {}

    public static synchronized Blueprint of(StructureType type) {
        return CACHE.computeIfAbsent(type, t -> new Blueprint(t, 1, 1, 1));
    }
}