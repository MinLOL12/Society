package io.github.minlol12.society.core.build;

/**
 * A <em>material role</em> in a blueprint - what a block is <em>for</em>,
 * never which block it actually is. The Minecraft adapter resolves each
 * role through a culture palette, so the same cottage blueprint becomes
 * mossy logs among the Woodfolk and sandstone among the Sandfolk.
 *
 * <p>Keeping blueprints in roles rather than block ids is what lets the
 * whole construction system live in the Minecraft-free simulation core.</p>
 */
public enum Mat {

    /** Leave whatever the world already has here. */
    SKIP(Phase.CLEAR),
    /** Carve out to air (the inside of a room, the space over a roof). */
    AIR(Phase.CLEAR),

    // --- Shell ---------------------------------------------------------
    FOUNDATION(Phase.STRUCTURE),
    FLOOR(Phase.STRUCTURE),
    FLOOR_ACCENT(Phase.STRUCTURE),
    WALL(Phase.STRUCTURE),
    WALL_ACCENT(Phase.STRUCTURE),
    PILLAR(Phase.STRUCTURE),
    BEAM(Phase.STRUCTURE),
    ROOF(Phase.STRUCTURE),
    /** The far slope of a gable; laid so its stairs face the other way. */
    ROOF_BACK(Phase.STRUCTURE),
    ROOF_SLAB(Phase.STRUCTURE),
    ROOF_BLOCK(Phase.STRUCTURE),
    STAIR(Phase.STRUCTURE),
    SLAB(Phase.STRUCTURE),
    LOW_WALL(Phase.STRUCTURE),
    FENCE(Phase.STRUCTURE),
    GATE(Phase.STRUCTURE),
    GLASS(Phase.STRUCTURE),
    PATH(Phase.STRUCTURE),
    GRAVEL(Phase.STRUCTURE),
    DIRT(Phase.STRUCTURE),
    GRASS(Phase.STRUCTURE),
    LOG(Phase.STRUCTURE),
    LEAVES(Phase.STRUCTURE),
    HAY(Phase.STRUCTURE),
    WATER(Phase.STRUCTURE),
    ORE_STONE(Phase.STRUCTURE),

    // --- Fixtures ------------------------------------------------------
    WINDOW(Phase.FIXTURE),
    BARS(Phase.FIXTURE),
    DOOR(Phase.FIXTURE),
    TRAPDOOR(Phase.FIXTURE),
    LADDER(Phase.FIXTURE),
    TORCH(Phase.FIXTURE),
    LANTERN(Phase.FIXTURE),
    HANGING_LANTERN(Phase.FIXTURE),
    CAMPFIRE(Phase.FIXTURE),
    BED(Phase.FIXTURE),
    CARPET(Phase.FIXTURE),
    FLOWER(Phase.FIXTURE),
    FLOWER_POT(Phase.FIXTURE),
    SAPLING(Phase.FIXTURE),
    CROP(Phase.FIXTURE),
    FARMLAND(Phase.STRUCTURE),
    SIGN(Phase.FIXTURE),
    BANNER(Phase.FIXTURE),
    SCARECROW(Phase.FIXTURE),
    GRAVESTONE(Phase.FIXTURE),

    /** Wall painting, adds culture and beauty to interiors. */
    PAINTING(Phase.FIXTURE),

    // --- Workstations and furniture -------------------------------------
    BELL(Phase.FIXTURE),
    CHEST(Phase.FIXTURE),
    BARREL(Phase.FIXTURE),
    CRAFTING_TABLE(Phase.FIXTURE),
    FURNACE(Phase.FIXTURE),
    BLAST_FURNACE(Phase.FIXTURE),
    SMOKER(Phase.FIXTURE),
    ANVIL(Phase.FIXTURE),
    GRINDSTONE(Phase.FIXTURE),
    SMITHING_TABLE(Phase.FIXTURE),
    STONECUTTER(Phase.FIXTURE),
    LOOM(Phase.FIXTURE),
    CARTOGRAPHY_TABLE(Phase.FIXTURE),
    FLETCHING_TABLE(Phase.FIXTURE),
    LECTERN(Phase.FIXTURE),
    BOOKSHELF(Phase.STRUCTURE),
    COMPOSTER(Phase.FIXTURE),
    BREWING_STAND(Phase.FIXTURE),
    CAULDRON(Phase.FIXTURE),
    JUKEBOX(Phase.FIXTURE),
    BEEHIVE(Phase.FIXTURE),
    /** Slab-on-fence table top; the adapter places the post itself. */
    TABLE(Phase.FIXTURE);

    /** Build order: everything is cleared, then raised, then furnished. */
    public enum Phase { CLEAR, STRUCTURE, FIXTURE }

    private final Phase phase;

    Mat(Phase phase) {
        this.phase = phase;
    }

    public Phase phase() {
        return phase;
    }

    /** True for roles whose placement depends on a facing direction. */
    public boolean directional() {
        switch (this) {
            case ROOF:
            case ROOF_BACK:
            case STAIR:
            case DOOR:
            case TRAPDOOR:
            case LADDER:
            case BED:
            case GATE:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case ANVIL:
            case GRINDSTONE:
            case LOOM:
            case LECTERN:
            case CHEST:
            case BARREL:
            case SIGN:
            case BANNER:
            case BEEHIVE:
            case TORCH:
            case BEAM:
            case PAINTING:
                return true;
            default:
                return false;
        }
    }

    /**
     * How much labour one cell of this role costs. Roofs and walls are the
     * bulk of a building; a flower costs almost nothing.
     */
    public double effort() {
        switch (this) {
            case SKIP: return 0.0;
            case AIR: return 0.05;
            case FLOWER:
            case FLOWER_POT:
            case CARPET:
            case TORCH:
            case CROP:
            case SAPLING:
            case SIGN:
            case PAINTING:
                return 0.1;
            case FOUNDATION:
            case WALL:
            case ROOF:
            case ROOF_BACK:
            case ROOF_BLOCK:
            case PILLAR:
                return 0.35;
            case BELL:
            case ANVIL:
            case BLAST_FURNACE:
            case BOOKSHELF:
                return 0.8;
            default:
                return 0.25;
        }
    }
}
