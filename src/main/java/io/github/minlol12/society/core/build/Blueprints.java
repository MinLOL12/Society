package io.github.minlol12.society.core.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.types.CultureOrigin;

/**
 * Village structures come from ChoiceTheorem's Overhauled Village
 * (CurseForge file 6459787) via its NBT structure system.
 *
 * <p>The in-memory {@link Blueprint} is only a dimensional placeholder used
 * by the pure simulation and the chunk/plot checks. The Minecraft adapter
 * asks this class for CTOV template ids and then loads the real NBT from the
 * {@code ctov} namespace at render time.</p>
 */
public final class Blueprints {

    /** CTOV village-piece paths after {@code village/<style>/}. */
    private static final Map<StructureType, List<String>> CTOV_SUFFIXES =
            new EnumMap<StructureType, List<String>>(StructureType.class);

    static {
        // -----------------------------------------------------------------
        // Civic heart
        // -----------------------------------------------------------------
        map(StructureType.TOWN_WELL, "deco/well", "decos/well", "town_center");
        map(StructureType.BELL_PLAZA, "town_center", "deco/well", "decos/well");
        map(StructureType.MEETING_HALL, "town_center", "jobsite/sanctuary", "jobsite/restaurant");
        map(StructureType.TOWN_HALL, "town_center", "jobsite/sanctuary", "jobsite/vault");
        map(StructureType.NOTICE_BOARD, "deco/lamp_1", "decos/lamp_1", "town_center");
        map(StructureType.FOUNTAIN, "deco/fountain", "deco/well", "decos/well", "town_center");
        map(StructureType.GARDEN, "jobsite/garden", "deco/picnic_mat", "deco/flowerbed", "deco/goddess_statue");
        map(StructureType.GRAVEYARD, "decos/cemetary", "decos/grave_0", "decos/grave_1", "deco/goddess_statue");
        map(StructureType.JAIL, "decos/cage", "jobsite/vault", "jobsite/barracks");

        // -----------------------------------------------------------------
        // Housing
        // -----------------------------------------------------------------
        map(StructureType.SHELTER, "house/small_1", "house/small_2", "house/med_1");
        map(StructureType.COTTAGE, "house/small_2", "house/small_1", "house/small_3", "house/small_4", "house/small_5");
        map(StructureType.FAMILY_HOUSE, "house/med_1", "house/mid_1", "house/med_2", "house/mid_2", "house/big_1");
        map(StructureType.LONGHOUSE, "house/longhouse_1", "house/longhouse_2", "house/big_1", "house/med_3");
        map(StructureType.TOWNHOUSE, "house/med_2", "house/mid_2", "house/big_1", "house/big_2");
        map(StructureType.MANOR, "house/big_2", "house/big_3", "house/big_1", "town_center");

        // -----------------------------------------------------------------
        // Food and land
        // -----------------------------------------------------------------
        map(StructureType.FARM_PLOT, "jobsite/farm", "jobsite/fd_farm");
        map(StructureType.GREAT_FIELD, "jobsite/fd_farm", "jobsite/farm", "jobsite/flower_farm");
        map(StructureType.ORCHARD, "jobsite/orchard", "jobsite/flower_farm", "jobsite/farm");
        map(StructureType.ANIMAL_PEN, "jobsite/pen", "jobsite/chicken_coop", "jobsite/shepherd");
        map(StructureType.BARN, "jobsite/shepherd", "jobsite/pen", "jobsite/chicken_coop");
        map(StructureType.GRANARY, "jobsite/warehouse", "jobsite/farm", "house/small_3");
        map(StructureType.WINDMILL, "jobsite/windmill", "jobsite/water_mill", "jobsite/farm");
        map(StructureType.BAKERY, "jobsite/bakery", "jobsite/restaurant");
        map(StructureType.APIARY, "deco/beekeeper", "jobsite/flower_farm", "jobsite/farm");
        map(StructureType.FISHING_HUT, "jobsite/fishing_hut", "jobsite/fishing_oasis", "jobsite/fishing_house");

        // -----------------------------------------------------------------
        // Industry
        // -----------------------------------------------------------------
        map(StructureType.LUMBER_CAMP, "jobsite/woodworker", "jobsite/sawmill", "deco/shed");
        map(StructureType.SAWMILL, "jobsite/sawmill", "jobsite/woodworker");
        map(StructureType.MINE_HEAD, "jobsite/mason", "jobsite/smith", "jobsite/factory");
        map(StructureType.QUARRY, "jobsite/mason", "jobsite/farm", "deco/garbage_heaps");
        map(StructureType.SMITHY, "jobsite/smith", "jobsite/gunsmith", "jobsite/weaponsmith", "jobsite/armoursmith");
        map(StructureType.FOUNDRY, "jobsite/smith", "jobsite/factory", "jobsite/gunsmith");
        map(StructureType.CARPENTER, "jobsite/woodworker", "jobsite/sawmill");
        map(StructureType.MASON_YARD, "jobsite/mason", "jobsite/factory");
        map(StructureType.WEAVER, "jobsite/loomhouse", "jobsite/shepherd", "jobsite/artist_studio");
        map(StructureType.TANNERY, "jobsite/tannery", "jobsite/leatherworker", "jobsite/leather");
        map(StructureType.POTTERY, "jobsite/mason", "jobsite/artist_studio", "jobsite/factory");
        map(StructureType.BREWERY, "jobsite/restaurant", "jobsite/vineyard", "jobsite/tavern");

        // -----------------------------------------------------------------
        // Trade
        // -----------------------------------------------------------------
        map(StructureType.MARKET_STALL, "deco/stall", "deco/stall_2", "jobsite/restaurant", "town_center");
        map(StructureType.MARKETPLACE, "town_center", "jobsite/restaurant", "jobsite/gazebo");
        map(StructureType.WAREHOUSE, "jobsite/warehouse", "jobsite/vault");
        map(StructureType.TRADING_POST, "jobsite/warehouse", "town_center", "jobsite/restaurant");
        map(StructureType.INN, "jobsite/restaurant", "jobsite/tavern", "house/big_1");
        map(StructureType.STABLE, "jobsite/shepherd", "jobsite/pen", "jobsite/horse_trainer");
        map(StructureType.DOCK, "deco/boat", "jobsite/fishing_hut", "jobsite/fishing_oasis", "jobsite/water_mill");

        // -----------------------------------------------------------------
        // Knowledge and care
        // -----------------------------------------------------------------
        map(StructureType.SHRINE, "jobsite/sanctuary", "jobsite/priest_tower", "deco/goddess_statue");
        map(StructureType.LIBRARY, "jobsite/library", "jobsite/scriber");
        map(StructureType.SCHOOL, "jobsite/scriber", "jobsite/library", "jobsite/cartography");
        map(StructureType.APOTHECARY, "jobsite/alchemist", "jobsite/chemist", "jobsite/botanist");
        map(StructureType.INFIRMARY, "jobsite/alchemist", "jobsite/chemist", "jobsite/sanctuary");
        map(StructureType.OBSERVATORY, "jobsite/observatory", "jobsite/wizard_tower", "jobsite/priest_tower");
        map(StructureType.BATHHOUSE, "deco/fountain", "deco/well", "decos/well", "jobsite/sanctuary");

        // -----------------------------------------------------------------
        // Defence
        // -----------------------------------------------------------------
        map(StructureType.GUARD_POST, "jobsite/archery_range", "jobsite/hunter", "jobsite/gunsmith");
        map(StructureType.WATCHTOWER, "jobsite/priest_tower", "jobsite/wizard_tower", "jobsite/archery_range");
        map(StructureType.BARRACKS, "jobsite/barracks", "jobsite/archery_range", "jobsite/gunsmith");
        map(StructureType.GATEHOUSE, "jobsite/barracks", "town_center", "jobsite/archery_range");
        map(StructureType.WALL_SEGMENT, "jobsite/barracks", "deco/wall", "town_center");
        map(StructureType.MILITARY_BASE, "jobsite/barracks", "jobsite/archery_range", "jobsite/gunsmith");
    }

    private Blueprints() {}

    public static synchronized Blueprint of(StructureType type) {
        // CTOV structures are loaded via NBT; return a dimensional placeholder
        // so chunk checks and anchoring work without defining every block cell.
        int footprint = Math.max(1, type.footprint());
        int height = Math.max(1, type.height());
        return new Blueprint(type, footprint, height, footprint).baseOffset(0);
    }

    /** True when this type is meant to be rendered from a CTOV NBT template. */
    public static boolean usesCTOV(StructureType type) {
        return type != null && CTOV_SUFFIXES.containsKey(type);
    }

    /**
     * Returns CTOV NBT template paths to try for this type and culture, best
     * first. Paths are relative to {@code data/ctov/structures/} and omit the
     * {@code .nbt} suffix, exactly as {@link net.minecraft.structure.StructureTemplateManager}
     * expects in an {@link net.minecraft.util.Identifier} path.
     */
    public static List<String> ctovCandidates(StructureType type, CultureOrigin origin) {
        List<String> suffixes = CTOV_SUFFIXES.get(type);
        if (suffixes == null || suffixes.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<String>();
        List<String> styles = stylesFor(origin, type);
        for (String style : styles) {
            for (String suffix : suffixes) {
                add(out, "village/" + style + "/" + suffix);
            }
        }

        // Theme-agnostic fallbacks for pieces that only exist in special CTOV variants.
        if (type == StructureType.GRAVEYARD) {
            add(out, "village/halloween/decos/cemetary");
            add(out, "village/halloween/decos/grave_0");
        }
        if (type == StructureType.JAIL) {
            add(out, "village/halloween/decos/cage");
        }
        if (type == StructureType.LONGHOUSE) {
            add(out, "village/savanna_na/house/longhouse_1");
            add(out, "village/savanna_na/house/longhouse_2");
        }
        if (type == StructureType.WALL_SEGMENT) {
            add(out, "village/underground/wall/small_wall");
            add(out, "village/underground/wall/large_wall");
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Legacy single-path API kept for older call sites and diagnostics.
     */
    public static String getCTOVPath(StructureType type) {
        List<String> candidates = ctovCandidates(type, CultureOrigin.PLAINS);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static void map(StructureType type, String... suffixes) {
        CTOV_SUFFIXES.put(type, Collections.unmodifiableList(Arrays.asList(suffixes)));
    }

    private static List<String> stylesFor(CultureOrigin origin, StructureType type) {
        String primary;
        switch (origin == null ? CultureOrigin.PLAINS : origin) {
            case FOREST:
                primary = "taiga";
                break;
            case MOUNTAIN:
                primary = "mountain";
                break;
            case COASTAL:
                primary = "beach";
                break;
            case DESERT:
                primary = "desert";
                break;
            case SNOWY:
                primary = "snowy_igloo";
                break;
            case JUNGLE:
                primary = "jungle";
                break;
            case SWAMP:
                primary = "swamp";
                break;
            default:
                primary = "plains";
                break;
        }

        List<String> styles = new ArrayList<String>();
        add(styles, primary);

        // Some building categories have excellent CTOV variants in themed villages.
        if (type == StructureType.GATEHOUSE || type == StructureType.WALL_SEGMENT
                || type == StructureType.BARRACKS || type == StructureType.MILITARY_BASE) {
            add(styles, fortifiedStyle(primary));
        }
        if (origin == CultureOrigin.DESERT) add(styles, "desert_oasis");
        if (origin == CultureOrigin.MOUNTAIN) add(styles, "mountain_alpine");
        if (origin == CultureOrigin.SWAMP) add(styles, "swamp_fortified");
        if (origin == CultureOrigin.FOREST) add(styles, "taiga_fortified");

        // Plains is the broadest CTOV set and is present in every supported version.
        add(styles, "plains");
        add(styles, "plains_fortified");
        return Collections.unmodifiableList(styles);
    }

    private static String fortifiedStyle(String primary) {
        if ("plains".equals(primary)) return "plains_fortified";
        if ("taiga".equals(primary)) return "taiga_fortified";
        if ("swamp".equals(primary)) return "swamp_fortified";
        if ("mesa".equals(primary)) return "mesa_fortified";
        return primary;
    }

    private static void add(List<String> list, String value) {
        if (value != null && !value.isEmpty() && !list.contains(value)) {
            list.add(value);
        }
    }
}
