package io.github.minlol12.society.core.headless;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.minlol12.society.core.build.Blueprint;
import io.github.minlol12.society.core.build.Blueprints;
import io.github.minlol12.society.core.build.Mat;
import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SettlementTier;

/**
 * Checks every blueprint in the catalogue without touching Minecraft:
 * that each one fits its declared plot, that buildings people live in
 * actually contain the beds they promise, that anything with walls has a
 * door and some light, and that the planner can eventually want one of
 * everything.
 *
 * <p>Run with:
 * {@code java io.github.minlol12.society.core.headless.BlueprintAudit}</p>
 */
public final class BlueprintAudit {

    private BlueprintAudit() { }

    public static void main(String[] args) {
        System.out.println("=== Society blueprint audit ===");
        int failures = 0;
        int total = 0;

        Map<StructureType.Category, Integer> byCategory =
                new EnumMap<StructureType.Category, Integer>(StructureType.Category.class);

        for (StructureType type : StructureType.values()) {
            total++;
            Blueprint bp = Blueprints.of(type);
            List<String> problems = audit(type, bp);
            byCategory.merge(type.category(), Integer.valueOf(1), (a, b) ->
                    Integer.valueOf(a.intValue() + b.intValue()));
            if (problems.isEmpty()) {
                System.out.printf("  PASS  %-22s %2dx%-2d h%-2d  %4d blocks, %5.1f effort%n",
                        type.display(), bp.width(), bp.depth(), bp.height(),
                        bp.solidCells(), bp.totalEffort());
            } else {
                failures++;
                System.out.println("  FAIL  " + type.display());
                for (String problem : problems) {
                    System.out.println("          " + problem);
                }
            }
        }

        System.out.println();
        System.out.println("Structures by purpose:");
        for (Map.Entry<StructureType.Category, Integer> e : byCategory.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue());
        }

        failures += checkPlannerReach();
        failures += checkBedMath();

        System.out.println();
        System.out.println(total + " structures audited.");
        if (failures == 0) {
            System.out.println("ALL BLUEPRINTS SOUND");
        } else {
            System.out.println("PROBLEMS: " + failures);
            System.exit(1);
        }
    }

    /** Structural sanity for one blueprint. */
    private static List<String> audit(StructureType type, Blueprint bp) {
        List<String> problems = new ArrayList<String>();

        // Fits the plot the planner reserves for it.
        if (bp.width() > type.footprint() || bp.depth() > type.footprint()) {
            problems.add("blueprint " + bp.width() + "x" + bp.depth()
                    + " exceeds declared footprint " + type.footprint());
        }
        if (bp.height() > type.height() + 4) {
            problems.add("blueprint height " + bp.height()
                    + " far exceeds declared height " + type.height());
        }
        if (bp.solidCells() == 0) {
            problems.add("places no blocks at all");
        }

        Map<Mat, Integer> counts = count(bp);

        // A building that promises beds must contain them.
        int beds = bp.bedCount();
        if (beds != type.beds()) {
            problems.add("declares " + type.beds() + " beds but the drawing has " + beds);
        }

        // Anything enclosed needs a way in, a roof and some light. Ramparts
        // are deliberately open to the sky and are judged differently.
        int walls = get(counts, Mat.WALL) + get(counts, Mat.WALL_ACCENT);
        if (walls > 20 && !isOpenAir(type)) {
            if (get(counts, Mat.DOOR) == 0) {
                problems.add("has " + walls + " wall blocks but no door");
            }
            int light = get(counts, Mat.LANTERN) + get(counts, Mat.HANGING_LANTERN)
                    + get(counts, Mat.TORCH) + get(counts, Mat.CAMPFIRE);
            if (light == 0) {
                problems.add("enclosed but unlit - mobs would spawn inside");
            }
            int roof = get(counts, Mat.ROOF) + get(counts, Mat.ROOF_SLAB)
                    + get(counts, Mat.ROOF_BLOCK);
            if (roof == 0) {
                problems.add("has walls but no roof");
            }
        }
        if (isOpenAir(type)) {
            // Open works still have to be lit, or they breed the very thing
            // they were built to keep out.
            int light = get(counts, Mat.LANTERN) + get(counts, Mat.HANGING_LANTERN)
                    + get(counts, Mat.TORCH) + get(counts, Mat.CAMPFIRE);
            if (light == 0) {
                problems.add("open defence work with no light along it");
            }
        }

        // Workshops need the workstation their trade implies.
        if (type.worksite() != io.github.minlol12.society.core.types.SimProfession.NONE) {
            boolean hasStation = false;
            for (Mat mat : counts.keySet()) {
                switch (mat) {
                    case CRAFTING_TABLE: case FURNACE: case BLAST_FURNACE: case SMOKER:
                    case ANVIL: case GRINDSTONE: case SMITHING_TABLE: case STONECUTTER:
                    case LOOM: case CARTOGRAPHY_TABLE: case FLETCHING_TABLE: case LECTERN:
                    case COMPOSTER: case BREWING_STAND: case CAULDRON: case BARREL:
                    case CHEST: case BEEHIVE: case BELL: case CROP: case HAY:
                        hasStation = true;
                        break;
                    default:
                        break;
                }
                if (hasStation) break;
            }
            if (!hasStation) {
                problems.add("is a " + type.worksite().display().toLowerCase()
                        + " worksite but has no workstation");
            }
        }

        // Costs should be in proportion to the thing being built.
        double material = type.cost(Good.WOOD) + type.cost(Good.STONE)
                + type.cost(Good.IRON) * 4.0;
        if (material <= 0.0 && bp.solidCells() > 20) {
            problems.add("costs nothing but places " + bp.solidCells() + " blocks");
        }
        if (type.labour() <= 0.0) {
            problems.add("takes no labour to build");
        }
        return problems;
    }

    /** Every structure must be reachable by some settlement, eventually. */
    private static int checkPlannerReach() {
        System.out.println();
        int problems = 0;
        for (StructureType type : StructureType.values()) {
            boolean everWanted = false;
            for (SettlementTier tier : SettlementTier.values()) {
                if (tier.ordinal() < type.minTier().ordinal()) continue;
                for (int pop = 1; pop <= 160; pop += 3) {
                    if (type.desiredCount(pop, tier, Math.max(1, pop * 2 / 3)) > 0) {
                        everWanted = true;
                        break;
                    }
                }
                if (everWanted) break;
            }
            if (!everWanted) {
                System.out.println("  FAIL  " + type.display()
                        + " is never wanted by any settlement - it would never be built");
                problems++;
            }
        }
        System.out.println(problems == 0
                ? "  PASS  every structure is reachable by the planner"
                : "  " + problems + " unreachable structures");
        return problems;
    }

    /** A full-grown town must be able to house its people in real beds. */
    private static int checkBedMath() {
        int problems = 0;
        for (SettlementTier tier : SettlementTier.values()) {
            int pop = Math.max(4, tier.minPopulation() * 2);
            int beds = 2;
            for (StructureType type : StructureType.values()) {
                if (type.beds() <= 0) continue;
                if (tier.ordinal() < type.minTier().ordinal()) continue;
                int wanted = type.desiredCount(pop, tier, pop * 2 / 3);
                if (type.unique()) wanted = Math.min(1, wanted);
                beds += wanted * type.beds();
            }
            boolean ok = beds >= pop;
            System.out.println((ok ? "  PASS  " : "  FAIL  ") + tier.display()
                    + " of " + pop + " can plan " + beds + " beds");
            if (!ok) problems++;
        }
        return problems;
    }

    /**
     * Curtain walls and their gate arches are meant to stand open to the
     * sky; demanding a roof and a front door of them would be wrong.
     */
    private static boolean isOpenAir(StructureType type) {
        switch (type) {
            // A curtain wall is meant to stand open to the sky.
            case WALL_SEGMENT:
            // Three-sided shelters and work sheds are open at the front on
            // purpose: you walk straight in off the yard.
            case SHELTER:
            case LUMBER_CAMP:
                return true;
            default:
                return false;
        }
    }

    private static Map<Mat, Integer> count(Blueprint bp) {
        Map<Mat, Integer> counts = new EnumMap<Mat, Integer>(Mat.class);
        for (Blueprint.Cell cell : bp.orderedCells()) {
            Integer v = counts.get(cell.mat);
            counts.put(cell.mat, Integer.valueOf(v == null ? 1 : v.intValue() + 1));
        }
        return counts;
    }

    private static int get(Map<Mat, Integer> counts, Mat mat) {
        Integer v = counts.get(mat);
        return v == null ? 0 : v.intValue();
    }
}
