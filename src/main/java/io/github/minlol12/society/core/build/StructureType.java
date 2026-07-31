package io.github.minlol12.society.core.build;

import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;
import io.github.minlol12.society.core.types.TechNode;

/**
 * The catalogue of everything a settlement can actually build. Each entry
 * knows what it costs, when a town is ready for it, how many of it a town
 * of a given size wants, and what it gives back - beds to sleep in,
 * storage, defence, jobs, or simply somewhere pleasant to stand.
 *
 * <p>Nothing here is decoration for its own sake: the planner picks the
 * next building from unmet need, so a hungry village really does raise a
 * granary before it raises a bathhouse.</p>
 */
public enum StructureType {

    // =====================================================================
    // Civic heart
    // =====================================================================

    TOWN_WELL("Well", Category.CIVIC, SettlementTier.CAMP, null,
            5, 5, 0, 6, 14, 0),
    BELL_PLAZA("Bell Plaza", Category.CIVIC, SettlementTier.CAMP, null,
            7, 7, 0, 10, 22, 0),
    MEETING_HALL("Meeting Hall", Category.CIVIC, SettlementTier.HAMLET, null,
            11, 9, 0, 68, 40, 0),
    TOWN_HALL("Town Hall", Category.CIVIC, SettlementTier.TOWN, TechNode.ARCHITECTURE,
            13, 11, 0, 110, 130, 0),
    NOTICE_BOARD("Notice Board", Category.CIVIC, SettlementTier.HAMLET, null,
            3, 2, 0, 8, 2, 0),
    FOUNTAIN("Fountain", Category.CIVIC, SettlementTier.TOWN, TechNode.STONEMASONRY,
            7, 7, 0, 4, 60, 0),
    GARDEN("Public Garden", Category.CIVIC, SettlementTier.VILLAGE, null,
            9, 7, 0, 18, 12, 0),
    GRAVEYARD("Graveyard", Category.CIVIC, SettlementTier.HAMLET, null,
            9, 7, 0, 12, 24, 0),
    JAIL("Jail", Category.CIVIC, SettlementTier.HAMLET, null,
            9, 8, 4, 40, 60, 10),

    // =====================================================================
    // Housing
    // =====================================================================

    SHELTER("Lean-to Shelter", Category.HOUSING, SettlementTier.CAMP, null,
            5, 5, 1, 14, 2, 0),
    COTTAGE("Cottage", Category.HOUSING, SettlementTier.CAMP, null,
            7, 7, 2, 42, 16, 0),
    FAMILY_HOUSE("Family House", Category.HOUSING, SettlementTier.HAMLET, null,
            9, 8, 4, 74, 34, 0),
    LONGHOUSE("Longhouse", Category.HOUSING, SettlementTier.VILLAGE, TechNode.CARPENTRY,
            13, 7, 10, 140, 52, 0),
    TOWNHOUSE("Townhouse", Category.HOUSING, SettlementTier.TOWN, TechNode.STONEMASONRY,
            8, 8, 5, 82, 96, 0),
    MANOR("Manor", Category.HOUSING, SettlementTier.CITY, TechNode.ARCHITECTURE,
            13, 12, 8, 150, 190, 0),

    // =====================================================================
    // Food and land
    // =====================================================================

    FARM_PLOT("Farm Plot", Category.FOOD, SettlementTier.CAMP, null,
            9, 9, 0, 12, 0, 0),
    GREAT_FIELD("Great Field", Category.FOOD, SettlementTier.VILLAGE, TechNode.CROP_ROTATION,
            15, 13, 0, 26, 4, 0),
    ORCHARD("Orchard", Category.FOOD, SettlementTier.HAMLET, null,
            11, 11, 0, 14, 2, 0),
    ANIMAL_PEN("Animal Pen", Category.FOOD, SettlementTier.CAMP, null,
            11, 9, 0, 30, 0, 0),
    BARN("Barn", Category.FOOD, SettlementTier.HAMLET, null,
            11, 9, 0, 78, 18, 0),
    GRANARY("Granary", Category.FOOD, SettlementTier.HAMLET, null,
            7, 7, 0, 54, 30, 0),
    WINDMILL("Windmill", Category.FOOD, SettlementTier.VILLAGE, TechNode.CARPENTRY,
            9, 9, 0, 96, 46, 0),
    BAKERY("Bakery", Category.FOOD, SettlementTier.VILLAGE, null,
            7, 7, 0, 44, 40, 0),
    APIARY("Apiary", Category.FOOD, SettlementTier.VILLAGE, null,
            7, 7, 0, 26, 6, 0),
    FISHING_HUT("Fishing Hut", Category.FOOD, SettlementTier.HAMLET, null,
            9, 8, 0, 48, 10, 0),

    // =====================================================================
    // Industry
    // =====================================================================

    LUMBER_CAMP("Lumber Camp", Category.INDUSTRY, SettlementTier.CAMP, null,
            9, 8, 0, 34, 6, 0),
    SAWMILL("Sawmill", Category.INDUSTRY, SettlementTier.VILLAGE, TechNode.CARPENTRY,
            11, 9, 0, 88, 30, 0),
    MINE_HEAD("Mine Head", Category.INDUSTRY, SettlementTier.HAMLET, TechNode.MINING,
            9, 9, 0, 46, 40, 0),
    QUARRY("Quarry", Category.INDUSTRY, SettlementTier.VILLAGE, TechNode.STONEMASONRY,
            11, 11, 0, 22, 20, 0),
    SMITHY("Smithy", Category.INDUSTRY, SettlementTier.HAMLET, null,
            9, 8, 0, 48, 62, 0),
    FOUNDRY("Foundry", Category.INDUSTRY, SettlementTier.TOWN, TechNode.SMELTING,
            11, 9, 0, 60, 120, 8),
    CARPENTER("Carpenter's Shop", Category.INDUSTRY, SettlementTier.HAMLET, null,
            9, 8, 0, 62, 16, 0),
    MASON_YARD("Mason's Yard", Category.INDUSTRY, SettlementTier.VILLAGE, TechNode.STONEMASONRY,
            9, 9, 0, 30, 74, 0),
    WEAVER("Weaver's Shop", Category.INDUSTRY, SettlementTier.HAMLET, null,
            8, 8, 0, 52, 18, 0),
    TANNERY("Tannery", Category.INDUSTRY, SettlementTier.VILLAGE, null,
            9, 8, 0, 46, 24, 0),
    POTTERY("Pottery Kiln", Category.INDUSTRY, SettlementTier.VILLAGE, null,
            8, 8, 0, 34, 48, 0),
    BREWERY("Brewery", Category.INDUSTRY, SettlementTier.TOWN, null,
            9, 9, 0, 66, 44, 0),

    // =====================================================================
    // Trade
    // =====================================================================

    MARKET_STALL("Market Stall", Category.TRADE, SettlementTier.HAMLET, null,
            5, 5, 0, 22, 4, 0),
    MARKETPLACE("Marketplace", Category.TRADE, SettlementTier.VILLAGE, null,
            13, 13, 0, 62, 48, 0),
    WAREHOUSE("Warehouse", Category.TRADE, SettlementTier.VILLAGE, null,
            11, 9, 0, 84, 46, 0),
    TRADING_POST("Trading Post", Category.TRADE, SettlementTier.VILLAGE, TechNode.COINAGE,
            9, 9, 2, 66, 40, 0),
    INN("Inn", Category.TRADE, SettlementTier.VILLAGE, null,
            11, 10, 4, 92, 52, 0),
    STABLE("Stable", Category.TRADE, SettlementTier.VILLAGE, null,
            11, 9, 0, 68, 14, 0),
    DOCK("Dock", Category.TRADE, SettlementTier.VILLAGE, TechNode.NAVIGATION,
            11, 11, 0, 74, 12, 0),

    // =====================================================================
    // Knowledge and care
    // =====================================================================

    SHRINE("Shrine", Category.KNOWLEDGE, SettlementTier.HAMLET, null,
            5, 5, 0, 16, 22, 0),
    LIBRARY("Library", Category.KNOWLEDGE, SettlementTier.VILLAGE, TechNode.WRITING,
            11, 9, 0, 78, 62, 0),
    SCHOOL("School", Category.KNOWLEDGE, SettlementTier.TOWN, TechNode.WRITING,
            11, 9, 0, 74, 56, 0),
    APOTHECARY("Apothecary", Category.KNOWLEDGE, SettlementTier.VILLAGE, TechNode.MEDICINE,
            9, 8, 0, 52, 34, 0),
    INFIRMARY("Infirmary", Category.KNOWLEDGE, SettlementTier.TOWN, TechNode.MEDICINE,
            11, 9, 6, 70, 70, 0),
    OBSERVATORY("Observatory", Category.KNOWLEDGE, SettlementTier.CITY, TechNode.ARCHITECTURE,
            9, 15, 0, 60, 160, 0),
    BATHHOUSE("Bathhouse", Category.KNOWLEDGE, SettlementTier.CITY, TechNode.STONEMASONRY,
            11, 9, 0, 40, 130, 0),

    // =====================================================================
    // Defence
    // =====================================================================

    GUARD_POST("Guard Post", Category.DEFENCE, SettlementTier.HAMLET, null,
            5, 7, 1, 30, 14, 0),
    WATCHTOWER("Watchtower", Category.DEFENCE, SettlementTier.VILLAGE, null,
            7, 14, 0, 54, 78, 0),
    BARRACKS("Barracks", Category.DEFENCE, SettlementTier.TOWN, TechNode.MILITARY_DRILL,
            13, 8, 6, 86, 90, 0),
    GATEHOUSE("Gatehouse", Category.DEFENCE, SettlementTier.TOWN, TechNode.STONEMASONRY,
            9, 10, 0, 40, 140, 0),
    WALL_SEGMENT("Wall", Category.DEFENCE, SettlementTier.CITY, TechNode.STONEMASONRY,
            11, 7, 0, 12, 120, 0),
    MILITARY_BASE("Military Base", Category.DEFENCE, SettlementTier.HAMLET, null,
            15, 8, 8, 80, 80, 20);

    /** Broad purpose, used by the planner and by the settlement report. */
    public enum Category { CIVIC, HOUSING, FOOD, INDUSTRY, TRADE, KNOWLEDGE, DEFENCE }

    private final String display;
    private final Category category;
    private final SettlementTier minTier;
    private final TechNode requiredTech;
    private final int footprint;
    private final int height;
    private final int beds;
    private final double woodCost;
    private final double stoneCost;
    private final double ironCost;

    StructureType(String display, Category category, SettlementTier minTier, TechNode requiredTech,
                  int footprint, int height, int beds,
                  double woodCost, double stoneCost, double ironCost) {
        this.display = display;
        this.category = category;
        this.minTier = minTier;
        this.requiredTech = requiredTech;
        this.footprint = footprint;
        this.height = height;
        this.beds = beds;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
    }

    public String display() { return display; }

    public Category category() { return category; }

    public SettlementTier minTier() { return minTier; }

    public TechNode requiredTech() { return requiredTech; }

    /** Side length of the square plot this building needs. */
    public int footprint() { return footprint; }

    public int height() { return height; }

    /** Real beds - these, not an abstract number, are a town's housing. */
    public int beds() { return beds; }

    public double cost(Good good) {
        switch (good) {
            case WOOD: return woodCost;
            case STONE: return stoneCost;
            case IRON: return ironCost;
            default: return 0.0;
        }
    }

    /** Total labour to raise this, in builder-days at skill 0. */
    public double labour() {
        return (woodCost + stoneCost + ironCost * 4.0) * 0.22 + 3.0;
    }

    /** Extra storage capacity per citizen this building adds. */
    public double storageBonus() {
        switch (this) {
            case GRANARY: return 9.0;
            case BARN: return 6.0;
            case WAREHOUSE: return 14.0;
            default: return 0.0;
        }
    }

    /** How much this building adds to the settlement's defence rating. */
    public double defenceBonus() {
        switch (this) {
            case GUARD_POST: return 1.0;
            case WATCHTOWER: return 2.5;
            case BARRACKS: return 4.0;
            case GATEHOUSE: return 3.0;
            case WALL_SEGMENT: return 1.5;
            case MILITARY_BASE: return 6.0;
            case JAIL: return 0.5;
            default: return 0.0;
        }
    }

    /** Daily morale this building gives back once it stands. */
    public double moraleBonus() {
        switch (this) {
            case FOUNTAIN: return 0.30;
            case GARDEN: return 0.25;
            case BATHHOUSE: return 0.30;
            case INN: return 0.25;
            case SHRINE: return 0.15;
            case BELL_PLAZA: return 0.10;
            case MARKETPLACE: return 0.15;
            case GRAVEYARD: return 0.05;
            case BAKERY: return 0.10;
            case BREWERY: return 0.15;
            case SCHOOL: return 0.10;
            default: return 0.0;
        }
    }

    /**
     * Multiplier this building gives to the daily output of one good.
     * Workshops are why a settlement's economy visibly improves as it
     * builds: a sawmill really does mean more planks.
     */
    public double productionBonus(Good good) {
        switch (this) {
            case FARM_PLOT: return good == Good.FOOD ? 0.06 : 0.0;
            case GREAT_FIELD: return good == Good.FOOD ? 0.14 : 0.0;
            case ORCHARD: return good == Good.FOOD ? 0.07 : 0.0;
            case ANIMAL_PEN: return good == Good.FOOD ? 0.05 : 0.0;
            case APIARY: return good == Good.FOOD ? 0.04 : 0.0;
            case FISHING_HUT: return good == Good.FOOD ? 0.09 : 0.0;
            case WINDMILL: return good == Good.FOOD ? 0.12 : 0.0;
            case BAKERY: return good == Good.FOOD ? 0.08 : 0.0;
            case LUMBER_CAMP: return good == Good.WOOD ? 0.10 : 0.0;
            case SAWMILL: return good == Good.WOOD ? 0.20 : 0.0;
            case MINE_HEAD: return good == Good.IRON ? 0.16 : good == Good.STONE ? 0.08 : 0.0;
            case QUARRY: return good == Good.STONE ? 0.22 : 0.0;
            case SMITHY: return good == Good.TOOLS ? 0.16 : 0.0;
            case FOUNDRY: return good == Good.TOOLS ? 0.18 : good == Good.IRON ? 0.12 : 0.0;
            case CARPENTER: return good == Good.TOOLS ? 0.10 : 0.0;
            case MASON_YARD: return good == Good.STONE ? 0.12 : 0.0;
            case WEAVER: return good == Good.CLOTH ? 0.22 : 0.0;
            case TANNERY: return good == Good.CLOTH ? 0.12 : 0.0;
            case POTTERY: return good == Good.LUXURY ? 0.14 : 0.0;
            case BREWERY: return good == Good.LUXURY ? 0.16 : 0.0;
            case APOTHECARY: return good == Good.MEDICINE ? 0.22 : 0.0;
            case INFIRMARY: return good == Good.MEDICINE ? 0.14 : 0.0;
            default: return 0.0;
        }
    }

    /** Research speed this building adds. */
    public double researchBonus() {
        switch (this) {
            case LIBRARY: return 0.22;
            case SCHOOL: return 0.15;
            case OBSERVATORY: return 0.25;
            case SHRINE: return 0.03;
            default: return 0.0;
        }
    }

    /** Trade income this building adds. */
    public double tradeBonus() {
        switch (this) {
            case MARKET_STALL: return 0.06;
            case MARKETPLACE: return 0.18;
            case TRADING_POST: return 0.20;
            case WAREHOUSE: return 0.08;
            case INN: return 0.10;
            case STABLE: return 0.07;
            case DOCK: return 0.16;
            default: return 0.0;
        }
    }

    /** Reduction in death chance from age, plague and famine. */
    public double healthBonus() {
        switch (this) {
            case APOTHECARY: return 0.10;
            case INFIRMARY: return 0.18;
            case BATHHOUSE: return 0.08;
            case TOWN_WELL: return 0.05;
            default: return 0.0;
        }
    }

    /** The profession that works here, or NONE for pure infrastructure. */
    public SimProfession worksite() {
        switch (this) {
            case FARM_PLOT:
            case GREAT_FIELD:
            case ORCHARD:
            case ANIMAL_PEN:
            case BARN:
            case APIARY:
            case FISHING_HUT:
            case WINDMILL:
            case BAKERY:
                return SimProfession.FARMER;
            case LUMBER_CAMP:
            case SAWMILL:
                return SimProfession.LUMBERJACK;
            case MINE_HEAD:
            case QUARRY:
                return SimProfession.MINER;
            case SMITHY:
            case FOUNDRY:
            case CARPENTER:
            case MASON_YARD:
            case WEAVER:
            case TANNERY:
            case POTTERY:
            case BREWERY:
                return SimProfession.CRAFTER;
            case MARKET_STALL:
            case MARKETPLACE:
            case TRADING_POST:
            case WAREHOUSE:
            case INN:
            case STABLE:
            case DOCK:
                return SimProfession.TRADER;
            case LIBRARY:
            case SCHOOL:
            case OBSERVATORY:
                return SimProfession.SCHOLAR;
            case APOTHECARY:
            case INFIRMARY:
                return SimProfession.HEALER;
            case GUARD_POST:
            case WATCHTOWER:
            case BARRACKS:
            case GATEHOUSE:
            case MILITARY_BASE:
            case JAIL:
                return SimProfession.GUARD;
            case TOWN_HALL:
            case MEETING_HALL:
                return SimProfession.STEWARD;
            default:
                return SimProfession.NONE;
        }
    }

    /** True when this building should sit on the settlement's edge. */
    public boolean outskirts() {
        switch (this) {
            case FARM_PLOT:
            case GREAT_FIELD:
            case ORCHARD:
            case ANIMAL_PEN:
            case LUMBER_CAMP:
            case MINE_HEAD:
            case QUARRY:
            case GRAVEYARD:
            case WATCHTOWER:
            case GATEHOUSE:
            case WALL_SEGMENT:
            case MILITARY_BASE:
            case TANNERY:
            case DOCK:
            case FISHING_HUT:
                return true;
            default:
                return false;
        }
    }

    /** Buildings that must stand beside water. */
    public boolean needsWater() {
        return this == DOCK || this == FISHING_HUT;
    }

    /** Only ever one of these per settlement. */
    public boolean unique() {
        switch (this) {
            case BELL_PLAZA:
            case TOWN_WELL:
            case MEETING_HALL:
            case TOWN_HALL:
            case FOUNTAIN:
            case GRAVEYARD:
            case NOTICE_BOARD:
            case WINDMILL:
            case SAWMILL:
            case FOUNDRY:
            case MARKETPLACE:
            case TRADING_POST:
            case LIBRARY:
            case SCHOOL:
            case INFIRMARY:
            case OBSERVATORY:
            case BATHHOUSE:
            case BARRACKS:
            case BREWERY:
            case INN:
            case DOCK:
            case JAIL:
            case MILITARY_BASE:
                return true;
            default:
                return false;
        }
    }

    /**
     * How many of these a settlement of this size and shape wants. This is
     * the whole planner in one method: unmet need here becomes a real
     * building on the ground.
     */
    public int desiredCount(int population, SettlementTier tier, int workers) {
        int t = tier.ordinal();
        switch (this) {
            // --- Civic ---
            case TOWN_WELL: return 1;
            case BELL_PLAZA: return 1;
            case NOTICE_BOARD: return t >= SettlementTier.HAMLET.ordinal() ? 1 : 0;
            case MEETING_HALL: return t >= SettlementTier.HAMLET.ordinal() ? 1 : 0;
            case TOWN_HALL: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;
            case FOUNTAIN: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;
            case GARDEN: return Math.min(2, population / 24);
            case GRAVEYARD: return t >= SettlementTier.HAMLET.ordinal() ? 1 : 0;
            case JAIL: return t >= SettlementTier.HAMLET.ordinal() ? 1 : 0;

            // --- Housing: enough beds for everyone, plus a little room ---
            // Cottages are the workhorse and are never tier-gated: a crowded
            // camp must be able to build its way up to being a hamlet.
            case SHELTER: return t <= SettlementTier.HAMLET.ordinal() ? 2 : 0;
            case COTTAGE: return Math.max(1, (int) Math.ceil(population / 2.0));
            case FAMILY_HOUSE: return t >= SettlementTier.HAMLET.ordinal() ? population / 5 : 0;
            case LONGHOUSE: return t >= SettlementTier.VILLAGE.ordinal() ? population / 12 : 0;
            case TOWNHOUSE: return t >= SettlementTier.TOWN.ordinal() ? population / 8 : 0;
            case MANOR: return t >= SettlementTier.CITY.ordinal() ? population / 30 : 0;

            // --- Food: fields scale with mouths to feed ---
            case FARM_PLOT: return Math.max(1, population / 4);
            case GREAT_FIELD: return t >= SettlementTier.VILLAGE.ordinal() ? population / 14 : 0;
            case ORCHARD: return Math.min(3, population / 10);
            case ANIMAL_PEN: return Math.min(3, 1 + population / 14);
            case BARN: return Math.min(2, population / 12);
            case GRANARY: return Math.max(1, population / 18);
            case WINDMILL: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;
            case BAKERY: return t >= SettlementTier.VILLAGE.ordinal() ? Math.min(2, population / 20) : 0;
            case APIARY: return Math.min(2, population / 22);
            case FISHING_HUT: return Math.min(2, population / 16);

            // --- Industry: one worksite per few workers of that trade ---
            case LUMBER_CAMP: return Math.min(3, 1 + workers / 6);
            case SAWMILL: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;
            case MINE_HEAD: return Math.min(2, 1 + workers / 8);
            case QUARRY: return Math.min(2, workers / 6);
            case SMITHY: return Math.max(1, workers / 5);
            case FOUNDRY: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;
            case CARPENTER: return Math.min(2, 1 + workers / 8);
            case MASON_YARD: return Math.min(2, workers / 7);
            case WEAVER: return Math.min(2, 1 + workers / 9);
            case TANNERY: return Math.min(1, workers / 6);
            case POTTERY: return Math.min(2, workers / 8);
            case BREWERY: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;

            // --- Trade ---
            case MARKET_STALL: return Math.min(6, 1 + population / 8);
            case MARKETPLACE: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;
            case WAREHOUSE: return t >= SettlementTier.VILLAGE.ordinal() ? Math.min(2, population / 22) : 0;
            case TRADING_POST: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;
            case INN: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;
            case STABLE: return t >= SettlementTier.VILLAGE.ordinal() ? Math.min(2, population / 20) : 0;
            case DOCK: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;

            // --- Knowledge and care ---
            case SHRINE: return Math.min(2, 1 + population / 26);
            case LIBRARY: return t >= SettlementTier.VILLAGE.ordinal() ? 1 : 0;
            case SCHOOL: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;
            case APOTHECARY: return Math.min(2, 1 + population / 30);
            case INFIRMARY: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;
            case OBSERVATORY: return t >= SettlementTier.CITY.ordinal() ? 1 : 0;
            case BATHHOUSE: return t >= SettlementTier.CITY.ordinal() ? 1 : 0;

            // --- Defence ---
            case GUARD_POST: return Math.min(4, 1 + population / 12);
            case WATCHTOWER: return t >= SettlementTier.VILLAGE.ordinal() ? Math.min(3, 1 + population / 20) : 0;
            case BARRACKS: return t >= SettlementTier.TOWN.ordinal() ? 1 : 0;
            case GATEHOUSE: return t >= SettlementTier.TOWN.ordinal() ? Math.min(2, 1 + population / 40) : 0;
            case WALL_SEGMENT: return t >= SettlementTier.CITY.ordinal() ? Math.min(8, 4 + population / 20) : 0;
            case MILITARY_BASE: return t >= SettlementTier.HAMLET.ordinal() ? 1 : 0;

            default: return 0;
        }
    }

    public static StructureType byName(String name) {
        if (name == null) return null;
        for (StructureType t : values()) {
            if (t.name().equalsIgnoreCase(name) || t.display.equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
