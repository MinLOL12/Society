package io.github.minlol12.society.core.build;

import java.util.EnumMap;
import java.util.Map;

/**
 * The architecture itself: one hand-drawn blueprint per {@link StructureType}.
 * Everything is expressed in material <em>roles</em>, so the same drawing
 * becomes a log lodge among the Woodfolk and a sandstone hall among the
 * Sandfolk once the adapter resolves it through a culture palette.
 *
 * <p>Every building here has a floor, walls that meet, a real roof, a door
 * you can walk through, light, and furniture that suits its purpose - the
 * point being that a grown village should look built, not generated.</p>
 */
public final class Blueprints {

    private static final Map<StructureType, Blueprint> CACHE =
            new EnumMap<StructureType, Blueprint>(StructureType.class);

    private Blueprints() { }

    public static synchronized Blueprint of(StructureType type) {
        Blueprint cached = CACHE.get(type);
        if (cached != null) return cached;
        Blueprint built = draw(type);
        CACHE.put(type, built);
        return built;
    }

    private static Blueprint draw(StructureType type) {
        switch (type) {
            case TOWN_WELL: return well();
            case BELL_PLAZA: return bellPlaza();
            case MEETING_HALL: return meetingHall();
            case TOWN_HALL: return townHall();
            case NOTICE_BOARD: return noticeBoard();
            case FOUNTAIN: return fountain();
            case GARDEN: return garden();
            case GRAVEYARD: return graveyard();

            case SHELTER: return shelter();
            case COTTAGE: return cottage();
            case FAMILY_HOUSE: return familyHouse();
            case LONGHOUSE: return longhouse();
            case TOWNHOUSE: return townhouse();
            case MANOR: return manor();

            case FARM_PLOT: return farmPlot();
            case GREAT_FIELD: return greatField();
            case ORCHARD: return orchard();
            case ANIMAL_PEN: return animalPen();
            case BARN: return barn();
            case GRANARY: return granary();
            case WINDMILL: return windmill();
            case BAKERY: return bakery();
            case APIARY: return apiary();
            case FISHING_HUT: return fishingHut();

            case LUMBER_CAMP: return lumberCamp();
            case SAWMILL: return sawmill();
            case MINE_HEAD: return mineHead();
            case QUARRY: return quarry();
            case SMITHY: return smithy();
            case FOUNDRY: return foundry();
            case CARPENTER: return carpenter();
            case MASON_YARD: return masonYard();
            case WEAVER: return weaver();
            case TANNERY: return tannery();
            case POTTERY: return pottery();
            case BREWERY: return brewery();

            case MARKET_STALL: return marketStall();
            case MARKETPLACE: return marketplace();
            case WAREHOUSE: return warehouse();
            case TRADING_POST: return tradingPost();
            case INN: return inn();
            case STABLE: return stable();
            case DOCK: return dock();

            case SHRINE: return shrine();
            case LIBRARY: return library();
            case SCHOOL: return school();
            case APOTHECARY: return apothecary();
            case INFIRMARY: return infirmary();
            case OBSERVATORY: return observatory();
            case BATHHOUSE: return bathhouse();

            case GUARD_POST: return guardPost();
            case WATCHTOWER: return watchtower();
            case BARRACKS: return barracks();
            case GATEHOUSE: return gatehouse();
            case WALL_SEGMENT: return wallSegment();
            default: return cottage();
        }
    }

    // =====================================================================
    // Shared shapes
    // =====================================================================

    /**
     * The common bones of a building: foundation, floor, hollow walls with
     * corner posts, a door in the middle of the south face, windows, and a
     * pitched roof. Individual blueprints then furnish the inside.
     */
    private static Blueprint house(StructureType type, int w, int d, int wallHeight,
                                   boolean stoneBase, boolean gable) {
        int roofRise = gable ? (d + 1) / 2 : 1;
        Blueprint bp = new Blueprint(type, w, wallHeight + roofRise + 2, d);
        int maxX = w - 1;
        int maxZ = d - 1;

        // Foundation and floor.
        bp.fill(0, 0, 0, maxX, 0, maxZ, stoneBase ? Mat.FOUNDATION : Mat.FLOOR);
        bp.fill(1, 1, 1, maxX - 1, 1, maxZ - 1, Mat.FLOOR);

        // Walls, hollow inside, with corner pillars.
        bp.walls(0, 0, maxX, maxZ, 1, wallHeight, Mat.WALL);
        bp.hollow(0, 2, 0, maxX, wallHeight, maxZ);
        bp.corners(0, 0, maxX, maxZ, 1, wallHeight, Mat.PILLAR);

        // Door on the south face (-Z), centred.
        int doorX = w / 2;
        bp.set(doorX, 1, 0, Mat.DOOR);
        bp.set(doorX, 2, 0, Mat.AIR);
        bp.set(doorX, wallHeight, 0, Mat.WALL_ACCENT);

        // Windows: skip the corners and the doorway.
        int windowY = wallHeight >= 4 ? 2 : wallHeight;
        for (int x = 2; x <= maxX - 2; x += 2) {
            if (x != doorX) bp.set(x, windowY, 0, Mat.WINDOW);
            bp.set(x, windowY, maxZ, Mat.WINDOW);
        }
        for (int z = 2; z <= maxZ - 2; z += 2) {
            bp.set(0, windowY, z, Mat.WINDOW);
            bp.set(maxX, windowY, z, Mat.WINDOW);
        }

        // Roof.
        if (gable) {
            bp.gableRoofX(0, 0, maxX, maxZ, wallHeight + 1, Mat.ROOF, Mat.ROOF_BACK, Mat.ROOF_BLOCK);
        } else {
            bp.flatRoof(0, 0, maxX, maxZ, wallHeight + 1, Mat.ROOF_BLOCK, Mat.ROOF_SLAB);
        }

        // A lantern by the door and one inside.
        bp.set(doorX, wallHeight, 1, Mat.HANGING_LANTERN);
        return bp;
    }

    /** A doorstep: path in front of the entrance so buildings connect. */
    private static void doorstep(Blueprint bp, int doorX) {
        bp.set(doorX, 0, 0, Mat.FOUNDATION);
    }

    // =====================================================================
    // Civic
    // =====================================================================

    private static Blueprint well() {
        Blueprint bp = new Blueprint(StructureType.TOWN_WELL, 5, 6, 5);
        bp.fill(0, 0, 0, 4, 0, 4, Mat.PATH);
        // Stone rim around a water shaft.
        bp.outline(1, 1, 3, 3, 1, Mat.WALL);
        bp.set(2, 0, 2, Mat.WATER);
        bp.set(2, 1, 2, Mat.WATER);
        // Four posts and a little roof over the water.
        for (int y = 2; y <= 3; y++) {
            bp.set(1, y, 1, Mat.PILLAR);
            bp.set(3, y, 1, Mat.PILLAR);
            bp.set(1, y, 3, Mat.PILLAR);
            bp.set(3, y, 3, Mat.PILLAR);
        }
        bp.fill(1, 4, 1, 3, 4, 3, Mat.ROOF_SLAB);
        bp.set(2, 4, 2, Mat.ROOF_BLOCK);
        bp.set(2, 3, 2, Mat.HANGING_LANTERN);
        return bp;
    }

    private static Blueprint bellPlaza() {
        Blueprint bp = new Blueprint(StructureType.BELL_PLAZA, 7, 6, 7);
        bp.fill(0, 0, 0, 6, 0, 6, Mat.PATH);
        bp.fill(2, 0, 2, 4, 0, 4, Mat.FLOOR_ACCENT);
        // Bell on a raised plinth with four corner posts.
        bp.set(3, 1, 3, Mat.WALL);
        bp.set(3, 2, 3, Mat.BELL);
        for (int y = 1; y <= 3; y++) {
            bp.set(2, y, 2, Mat.PILLAR);
            bp.set(4, y, 2, Mat.PILLAR);
            bp.set(2, y, 4, Mat.PILLAR);
            bp.set(4, y, 4, Mat.PILLAR);
        }
        bp.fill(2, 4, 2, 4, 4, 4, Mat.ROOF_SLAB);
        bp.set(2, 3, 3, Mat.LANTERN);
        bp.set(4, 3, 3, Mat.LANTERN);
        // Benches around the square.
        bp.set(1, 1, 1, Mat.SLAB);
        bp.set(5, 1, 1, Mat.SLAB);
        bp.set(1, 1, 5, Mat.SLAB);
        bp.set(5, 1, 5, Mat.SLAB);
        bp.set(0, 1, 3, Mat.FLOWER);
        bp.set(6, 1, 3, Mat.FLOWER);
        return bp;
    }

    private static Blueprint meetingHall() {
        Blueprint bp = house(StructureType.MEETING_HALL, 11, 9, 5, true, true);
        // A long table down the middle with seats.
        for (int z = 3; z <= 6; z++) {
            bp.set(5, 2, z, Mat.TABLE);
            bp.set(4, 2, z, Mat.STAIR);
            bp.set(6, 2, z, Mat.STAIR);
        }
        bp.set(5, 2, 2, Mat.LECTERN);
        bp.set(2, 2, 7, Mat.BOOKSHELF);
        bp.set(8, 2, 7, Mat.BOOKSHELF);
        bp.set(1, 2, 1, Mat.BANNER);
        bp.set(9, 2, 1, Mat.BANNER);
        bp.set(2, 4, 2, Mat.LANTERN);
        bp.set(8, 4, 2, Mat.LANTERN);
        bp.set(2, 4, 6, Mat.LANTERN);
        bp.set(8, 4, 6, Mat.LANTERN);
        bp.set(1, 2, 4, Mat.BARREL);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint townHall() {
        Blueprint bp = house(StructureType.TOWN_HALL, 13, 11, 7, true, true);
        // Grand entrance: a porch of pillars.
        for (int x = 4; x <= 8; x += 2) {
            bp.set(x, 1, 0, Mat.PILLAR);
            bp.set(x, 2, 0, Mat.PILLAR);
            bp.set(x, 3, 0, Mat.PILLAR);
        }
        bp.set(6, 1, 0, Mat.DOOR);
        bp.set(6, 2, 0, Mat.AIR);
        // Council chamber: a ring of seats around a speaker's lectern.
        bp.set(6, 2, 5, Mat.LECTERN);
        for (int x = 3; x <= 9; x += 2) {
            bp.set(x, 2, 3, Mat.STAIR);
            bp.set(x, 2, 7, Mat.STAIR);
        }
        bp.set(2, 2, 5, Mat.STAIR);
        bp.set(10, 2, 5, Mat.STAIR);
        // Archives along the back wall.
        for (int x = 3; x <= 9; x++) {
            bp.set(x, 2, 9, Mat.BOOKSHELF);
            bp.set(x, 3, 9, Mat.BOOKSHELF);
        }
        bp.set(2, 2, 9, Mat.CHEST);
        bp.set(10, 2, 9, Mat.CHEST);
        bp.set(1, 2, 1, Mat.BANNER);
        bp.set(11, 2, 1, Mat.BANNER);
        for (int x = 3; x <= 9; x += 3) {
            bp.set(x, 6, 2, Mat.LANTERN);
            bp.set(x, 6, 8, Mat.LANTERN);
        }
        bp.set(6, 8, 5, Mat.BELL);
        doorstep(bp, 6);
        return bp;
    }

    private static Blueprint noticeBoard() {
        Blueprint bp = new Blueprint(StructureType.NOTICE_BOARD, 3, 3, 2);
        bp.fill(0, 0, 0, 2, 0, 1, Mat.PATH);
        bp.set(0, 1, 1, Mat.FENCE);
        bp.set(2, 1, 1, Mat.FENCE);
        bp.fill(0, 2, 1, 2, 2, 1, Mat.SIGN);
        bp.set(1, 1, 1, Mat.LECTERN);
        return bp;
    }

    private static Blueprint fountain() {
        Blueprint bp = new Blueprint(StructureType.FOUNTAIN, 7, 5, 7);
        bp.fill(0, 0, 0, 6, 0, 6, Mat.FLOOR_ACCENT);
        // Sunken basin.
        bp.fill(1, 0, 1, 5, 0, 5, Mat.WATER);
        bp.outline(1, 1, 5, 5, 1, Mat.LOW_WALL);
        // Tiered centre.
        bp.fill(2, 1, 2, 4, 1, 4, Mat.WALL);
        bp.fill(3, 2, 3, 3, 2, 3, Mat.WALL);
        bp.set(3, 3, 3, Mat.WATER);
        bp.set(0, 1, 0, Mat.LANTERN);
        bp.set(6, 1, 0, Mat.LANTERN);
        bp.set(0, 1, 6, Mat.LANTERN);
        bp.set(6, 1, 6, Mat.LANTERN);
        return bp;
    }

    private static Blueprint garden() {
        Blueprint bp = new Blueprint(StructureType.GARDEN, 9, 4, 7);
        bp.fill(0, 0, 0, 8, 0, 6, Mat.GRASS);
        // Gravel paths in a cross.
        bp.fill(4, 0, 0, 4, 0, 6, Mat.PATH);
        bp.fill(0, 0, 3, 8, 0, 3, Mat.PATH);
        bp.outline(0, 0, 8, 6, 1, Mat.FENCE);
        bp.set(4, 1, 0, Mat.GATE);
        // Flower beds in each quarter.
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 5; z++) {
                if (x == 4 || z == 3) continue;
                if ((x + z) % 2 == 0) bp.set(x, 1, z, Mat.FLOWER);
            }
        }
        bp.set(2, 1, 1, Mat.SAPLING);
        bp.set(6, 1, 5, Mat.SAPLING);
        bp.set(3, 1, 3, Mat.SLAB);
        bp.set(5, 1, 3, Mat.SLAB);
        bp.set(1, 2, 3, Mat.LANTERN);
        bp.set(7, 2, 3, Mat.LANTERN);
        return bp;
    }

    private static Blueprint graveyard() {
        Blueprint bp = new Blueprint(StructureType.GRAVEYARD, 9, 4, 7);
        bp.fill(0, 0, 0, 8, 0, 6, Mat.GRASS);
        bp.outline(0, 0, 8, 6, 1, Mat.LOW_WALL);
        bp.set(4, 1, 0, Mat.GATE);
        // Rows of headstones with a path between them.
        for (int x = 1; x <= 7; x += 2) {
            for (int z = 2; z <= 5; z += 2) {
                bp.set(x, 1, z, Mat.GRAVESTONE);
                bp.set(x, 0, z, Mat.DIRT);
            }
        }
        bp.fill(4, 0, 1, 4, 0, 6, Mat.PATH);
        bp.set(1, 1, 1, Mat.SAPLING);
        bp.set(7, 1, 1, Mat.SAPLING);
        bp.set(4, 1, 5, Mat.LANTERN);
        return bp;
    }

    // =====================================================================
    // Housing
    // =====================================================================

    private static Blueprint shelter() {
        Blueprint bp = new Blueprint(StructureType.SHELTER, 5, 5, 5);
        bp.fill(0, 0, 0, 4, 0, 4, Mat.FLOOR);
        // Three walls and an open front.
        bp.fill(0, 1, 4, 4, 2, 4, Mat.WALL);
        bp.fill(0, 1, 1, 0, 2, 4, Mat.WALL);
        bp.fill(4, 1, 1, 4, 2, 4, Mat.WALL);
        bp.set(0, 1, 0, Mat.PILLAR);
        bp.set(4, 1, 0, Mat.PILLAR);
        bp.set(0, 2, 0, Mat.PILLAR);
        bp.set(4, 2, 0, Mat.PILLAR);
        // Lean-to roof sloping to the front.
        bp.fill(0, 3, 1, 4, 3, 4, Mat.ROOF_SLAB);
        bp.fill(0, 3, 0, 4, 3, 0, Mat.ROOF);
        bp.set(1, 1, 3, Mat.BED);
        bp.set(3, 1, 3, Mat.CAMPFIRE);
        bp.set(3, 1, 1, Mat.BARREL);
        bp.set(2, 2, 4, Mat.TORCH);
        return bp;
    }

    private static Blueprint cottage() {
        Blueprint bp = house(StructureType.COTTAGE, 7, 7, 4, false, true);
        // Two beds, a hearth, a table and storage - a whole small life.
        bp.set(1, 2, 5, Mat.BED);
        bp.set(3, 2, 5, Mat.BED);
        bp.set(5, 2, 5, Mat.FURNACE);
        bp.set(5, 2, 4, Mat.SMOKER);
        bp.set(2, 2, 2, Mat.TABLE);
        bp.set(1, 2, 2, Mat.STAIR);
        bp.set(3, 2, 2, Mat.STAIR);
        bp.set(5, 2, 2, Mat.CHEST);
        bp.set(5, 2, 1, Mat.CRAFTING_TABLE);
        bp.set(1, 2, 1, Mat.FLOWER_POT);
        bp.set(2, 3, 6, Mat.TORCH);
        bp.fill(1, 1, 1, 5, 1, 5, Mat.FLOOR_ACCENT);
        doorstep(bp, 3);
        return bp;
    }

    private static Blueprint familyHouse() {
        Blueprint bp = house(StructureType.FAMILY_HOUSE, 9, 8, 5, true, true);
        // Ground floor: hearth, kitchen and a table for the family.
        bp.set(7, 2, 6, Mat.FURNACE);
        bp.set(7, 2, 5, Mat.SMOKER);
        bp.set(7, 2, 4, Mat.BARREL);
        bp.set(3, 2, 3, Mat.TABLE);
        bp.set(2, 2, 3, Mat.STAIR);
        bp.set(4, 2, 3, Mat.STAIR);
        bp.set(3, 2, 2, Mat.STAIR);
        bp.set(1, 2, 6, Mat.CRAFTING_TABLE);
        bp.set(1, 2, 1, Mat.CHEST);
        // Upper floor: four beds under the eaves, reached by a ladder.
        bp.fill(1, 4, 1, 7, 4, 6, Mat.FLOOR);
        bp.set(1, 4, 6, Mat.AIR);
        bp.set(1, 2, 6, Mat.LADDER);
        bp.set(1, 3, 6, Mat.LADDER);
        bp.set(1, 4, 6, Mat.AIR);
        bp.set(2, 5, 6, Mat.BED);
        bp.set(4, 5, 6, Mat.BED);
        bp.set(2, 5, 2, Mat.BED);
        bp.set(4, 5, 2, Mat.BED);
        bp.set(6, 5, 4, Mat.CHEST);
        bp.set(6, 5, 2, Mat.CARPET);
        bp.set(3, 5, 4, Mat.CARPET);
        bp.set(7, 4, 1, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint longhouse() {
        Blueprint bp = house(StructureType.LONGHOUSE, 13, 7, 5, false, true);
        // A central hearth running the length of the hall.
        for (int x = 3; x <= 9; x += 3) {
            bp.set(x, 2, 3, Mat.CAMPFIRE);
        }
        // Sleeping benches along both long walls.
        for (int x = 2; x <= 10; x += 2) {
            bp.set(x, 2, 1, Mat.BED);
            bp.set(x, 2, 5, Mat.BED);
        }
        bp.set(1, 2, 3, Mat.BARREL);
        bp.set(11, 2, 3, Mat.CHEST);
        bp.set(11, 2, 1, Mat.CRAFTING_TABLE);
        bp.set(1, 2, 1, Mat.LOOM);
        for (int x = 2; x <= 10; x += 4) {
            bp.set(x, 4, 2, Mat.LANTERN);
            bp.set(x, 4, 4, Mat.LANTERN);
        }
        bp.fill(1, 1, 1, 11, 1, 5, Mat.FLOOR_ACCENT);
        doorstep(bp, 6);
        return bp;
    }

    private static Blueprint townhouse() {
        // Narrow, tall, built shoulder to shoulder with its neighbours.
        Blueprint bp = house(StructureType.TOWNHOUSE, 8, 8, 7, true, false);
        // Ground floor: shopfront and hearth.
        bp.set(6, 2, 6, Mat.FURNACE);
        bp.set(6, 2, 5, Mat.BARREL);
        bp.set(2, 2, 2, Mat.TABLE);
        bp.set(1, 2, 2, Mat.STAIR);
        bp.set(3, 2, 2, Mat.STAIR);
        bp.set(1, 2, 6, Mat.CRAFTING_TABLE);
        // Ladder to the first floor.
        bp.set(6, 2, 1, Mat.LADDER);
        bp.set(6, 3, 1, Mat.LADDER);
        bp.fill(1, 4, 1, 6, 4, 6, Mat.FLOOR);
        bp.set(6, 4, 1, Mat.AIR);
        // First floor: bedrooms.
        bp.set(2, 5, 5, Mat.BED);
        bp.set(4, 5, 5, Mat.BED);
        bp.set(2, 5, 2, Mat.BED);
        bp.set(4, 5, 2, Mat.BED);
        bp.set(6, 5, 4, Mat.BED);
        bp.set(1, 5, 4, Mat.CHEST);
        bp.set(3, 5, 3, Mat.CARPET);
        bp.set(2, 6, 6, Mat.LANTERN);
        // Roof terrace rail.
        bp.outline(0, 0, 7, 7, 8, Mat.FENCE);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint manor() {
        Blueprint bp = house(StructureType.MANOR, 13, 12, 8, true, true);
        // Pillared entrance and a wide hall.
        for (int x = 4; x <= 8; x += 2) {
            bp.fill(x, 1, 0, x, 4, 0, Mat.PILLAR);
        }
        bp.set(6, 1, 0, Mat.DOOR);
        bp.set(6, 2, 0, Mat.AIR);
        // Great hall: long table, hearths at both ends.
        for (int z = 4; z <= 7; z++) {
            bp.set(6, 2, z, Mat.TABLE);
            bp.set(5, 2, z, Mat.STAIR);
            bp.set(7, 2, z, Mat.STAIR);
        }
        bp.set(2, 2, 10, Mat.FURNACE);
        bp.set(10, 2, 10, Mat.FURNACE);
        bp.set(1, 2, 1, Mat.BANNER);
        bp.set(11, 2, 1, Mat.BANNER);
        // Upper floor with eight beds.
        bp.fill(1, 5, 1, 11, 5, 10, Mat.FLOOR);
        bp.set(1, 5, 10, Mat.AIR);
        bp.fill(1, 2, 10, 1, 4, 10, Mat.LADDER);
        for (int x = 2; x <= 10; x += 3) {
            bp.set(x, 6, 2, Mat.BED);
            bp.set(x, 6, 9, Mat.BED);
        }
        bp.set(2, 6, 5, Mat.BED);
        bp.set(10, 6, 5, Mat.BED);
        bp.set(6, 6, 5, Mat.CARPET);
        bp.set(6, 6, 6, Mat.CARPET);
        bp.set(3, 6, 6, Mat.CHEST);
        bp.set(9, 6, 6, Mat.CHEST);
        for (int x = 3; x <= 9; x += 3) {
            bp.set(x, 7, 1, Mat.LANTERN);
            bp.set(x, 7, 10, Mat.LANTERN);
        }
        doorstep(bp, 6);
        return bp;
    }

    // =====================================================================
    // Food and land
    // =====================================================================

    private static Blueprint farmPlot() {
        Blueprint bp = new Blueprint(StructureType.FARM_PLOT, 9, 3, 9);
        bp.fill(0, 0, 0, 8, 0, 8, Mat.DIRT);
        // Four beds of crops around a watered cross.
        bp.fill(4, 0, 0, 4, 0, 8, Mat.WATER);
        bp.fill(0, 0, 4, 8, 0, 4, Mat.WATER);
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                if (x == 4 || z == 4) continue;
                bp.set(x, 0, z, Mat.FARMLAND);
                bp.set(x, 1, z, Mat.CROP);
            }
        }
        bp.set(0, 1, 0, Mat.FENCE);
        bp.set(8, 1, 0, Mat.FENCE);
        bp.set(0, 1, 8, Mat.FENCE);
        bp.set(8, 1, 8, Mat.FENCE);
        bp.set(2, 1, 2, Mat.SCARECROW);
        bp.set(6, 1, 6, Mat.COMPOSTER);
        return bp;
    }

    private static Blueprint greatField() {
        Blueprint bp = new Blueprint(StructureType.GREAT_FIELD, 15, 4, 13);
        bp.fill(0, 0, 0, 14, 0, 12, Mat.DIRT);
        // Irrigation channels every fourth row.
        for (int z = 2; z <= 10; z += 4) {
            bp.fill(0, 0, z, 14, 0, z, Mat.WATER);
        }
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 12; z++) {
                if ((z - 2) % 4 == 0 && z >= 2 && z <= 10) continue;
                bp.set(x, 0, z, Mat.FARMLAND);
                bp.set(x, 1, z, Mat.CROP);
            }
        }
        // A cart track down the middle and a work shed at the head.
        bp.fill(7, 0, 0, 7, 0, 12, Mat.PATH);
        bp.fill(7, 1, 0, 7, 1, 12, Mat.AIR);
        bp.fill(0, 1, 0, 0, 1, 0, Mat.FENCE);
        bp.set(1, 1, 1, Mat.SCARECROW);
        bp.set(13, 1, 11, Mat.SCARECROW);
        bp.set(12, 1, 1, Mat.COMPOSTER);
        bp.set(13, 1, 1, Mat.BARREL);
        bp.set(11, 1, 1, Mat.HAY);
        bp.set(11, 2, 1, Mat.HAY);
        return bp;
    }

    private static Blueprint orchard() {
        Blueprint bp = new Blueprint(StructureType.ORCHARD, 11, 8, 11);
        bp.fill(0, 0, 0, 10, 0, 10, Mat.GRASS);
        // A grid of trees with grass walks between them.
        for (int x = 1; x <= 9; x += 4) {
            for (int z = 1; z <= 9; z += 4) {
                bp.fill(x, 1, z, x, 4, z, Mat.LOG);
                bp.fill(x - 1, 4, z - 1, x + 1, 5, z + 1, Mat.LEAVES);
                bp.set(x, 4, z, Mat.LOG);
                bp.set(x, 5, z, Mat.LEAVES);
            }
        }
        bp.outline(0, 0, 10, 10, 1, Mat.FENCE);
        bp.set(5, 1, 0, Mat.GATE);
        bp.set(5, 1, 5, Mat.COMPOSTER);
        bp.set(6, 1, 5, Mat.BARREL);
        return bp;
    }

    private static Blueprint animalPen() {
        Blueprint bp = new Blueprint(StructureType.ANIMAL_PEN, 11, 5, 9);
        bp.fill(0, 0, 0, 10, 0, 8, Mat.GRASS);
        bp.outline(0, 0, 10, 8, 1, Mat.FENCE);
        bp.set(5, 1, 0, Mat.GATE);
        // A three-sided shed in the corner for shelter.
        bp.fill(1, 0, 6, 4, 0, 8, Mat.FLOOR);
        bp.fill(1, 1, 8, 4, 2, 8, Mat.WALL);
        bp.fill(1, 1, 6, 1, 2, 8, Mat.WALL);
        bp.fill(4, 1, 6, 4, 2, 8, Mat.WALL);
        bp.fill(1, 3, 6, 4, 3, 8, Mat.ROOF_SLAB);
        bp.set(2, 1, 7, Mat.HAY);
        bp.set(3, 1, 7, Mat.HAY);
        bp.set(8, 1, 2, Mat.WATER);
        bp.set(8, 0, 2, Mat.FOUNDATION);
        bp.set(7, 1, 6, Mat.HAY);
        return bp;
    }

    private static Blueprint barn() {
        Blueprint bp = house(StructureType.BARN, 11, 9, 6, false, true);
        // Wide double doors instead of one.
        bp.set(5, 1, 0, Mat.DOOR);
        bp.set(6, 1, 0, Mat.DOOR);
        bp.set(5, 2, 0, Mat.AIR);
        bp.set(6, 2, 0, Mat.AIR);
        // Stalls down one side, hay loft above.
        for (int z = 2; z <= 6; z += 2) {
            bp.set(1, 2, z, Mat.FENCE);
            bp.set(2, 2, z, Mat.FENCE);
            bp.set(1, 2, z - 1, Mat.HAY);
        }
        bp.fill(6, 2, 6, 9, 3, 7, Mat.HAY);
        bp.set(9, 2, 1, Mat.BARREL);
        bp.set(8, 2, 1, Mat.COMPOSTER);
        bp.set(4, 2, 7, Mat.CHEST);
        bp.set(5, 5, 4, Mat.HANGING_LANTERN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint granary() {
        // Raised on staddle stones to keep the grain dry and safe.
        Blueprint bp = new Blueprint(StructureType.GRANARY, 7, 8, 7);
        bp.fill(0, 0, 0, 6, 0, 6, Mat.FOUNDATION);
        for (int x = 1; x <= 5; x += 2) {
            for (int z = 1; z <= 5; z += 2) {
                bp.set(x, 1, z, Mat.PILLAR);
            }
        }
        bp.fill(1, 2, 1, 5, 2, 5, Mat.FLOOR);
        bp.walls(1, 1, 5, 5, 3, 5, Mat.WALL);
        bp.hollow(1, 3, 1, 5, 5, 5);
        bp.corners(1, 1, 5, 5, 3, 5, Mat.PILLAR);
        bp.set(3, 3, 1, Mat.DOOR);
        bp.set(3, 4, 1, Mat.AIR);
        bp.set(3, 1, 1, Mat.LADDER);
        bp.set(3, 2, 1, Mat.LADDER);
        bp.set(2, 4, 5, Mat.WINDOW);
        bp.set(4, 4, 5, Mat.WINDOW);
        // Grain within.
        bp.fill(2, 3, 3, 4, 4, 4, Mat.HAY);
        bp.set(2, 3, 2, Mat.BARREL);
        bp.set(4, 3, 2, Mat.BARREL);
        bp.gableRoofX(1, 1, 5, 5, 6, Mat.ROOF, Mat.ROOF_BACK, Mat.ROOF_BLOCK);
        bp.set(3, 5, 2, Mat.HANGING_LANTERN);
        return bp;
    }

    private static Blueprint windmill() {
        Blueprint bp = new Blueprint(StructureType.WINDMILL, 9, 9, 9);
        // Round-ish stone tower.
        bp.fill(2, 0, 2, 6, 0, 6, Mat.FOUNDATION);
        for (int y = 1; y <= 5; y++) {
            bp.outline(2, 2, 6, 6, y, Mat.WALL);
            bp.set(2, y, 2, Mat.SKIP);
            bp.set(6, y, 2, Mat.SKIP);
            bp.set(2, y, 6, Mat.SKIP);
            bp.set(6, y, 6, Mat.SKIP);
        }
        bp.fill(3, 1, 3, 5, 5, 5, Mat.AIR);
        bp.set(4, 1, 2, Mat.DOOR);
        bp.set(4, 2, 2, Mat.AIR);
        bp.set(3, 3, 2, Mat.WINDOW);
        bp.set(5, 3, 2, Mat.WINDOW);
        bp.set(4, 3, 6, Mat.WINDOW);
        // Millstone and sacks inside.
        bp.set(4, 1, 4, Mat.FLOOR_ACCENT);
        bp.set(3, 1, 4, Mat.BARREL);
        bp.set(5, 1, 4, Mat.HAY);
        bp.fill(3, 1, 5, 5, 1, 5, Mat.FLOOR_ACCENT);
        bp.set(3, 2, 3, Mat.LADDER);
        bp.set(3, 3, 3, Mat.LADDER);
        bp.set(3, 4, 3, Mat.LADDER);
        // Cap and sails.
        bp.fill(2, 6, 2, 6, 6, 6, Mat.ROOF_SLAB);
        bp.fill(3, 7, 3, 5, 7, 5, Mat.ROOF_BLOCK);
        bp.fill(4, 3, 1, 4, 3, 1, Mat.BEAM);
        for (int y = 1; y <= 6; y++) {
            bp.set(4, y, 0, Mat.FENCE);
        }
        bp.fill(1, 5, 0, 7, 5, 0, Mat.FENCE);
        bp.set(4, 4, 5, Mat.LANTERN);
        return bp;
    }

    private static Blueprint bakery() {
        Blueprint bp = house(StructureType.BAKERY, 7, 7, 4, true, true);
        // Ovens along the back wall, counter at the front.
        bp.set(4, 2, 5, Mat.SMOKER);
        bp.set(5, 2, 5, Mat.FURNACE);
        bp.set(3, 2, 5, Mat.FURNACE);
        bp.fill(1, 2, 2, 5, 2, 2, Mat.SLAB);
        bp.set(1, 2, 4, Mat.BARREL);
        bp.set(1, 2, 5, Mat.COMPOSTER);
        bp.set(5, 2, 1, Mat.CHEST);
        bp.set(2, 2, 1, Mat.HAY);
        bp.set(3, 3, 6, Mat.HANGING_LANTERN);
        bp.set(1, 3, 1, Mat.LANTERN);
        doorstep(bp, 3);
        return bp;
    }

    private static Blueprint apiary() {
        Blueprint bp = new Blueprint(StructureType.APIARY, 7, 5, 7);
        bp.fill(0, 0, 0, 6, 0, 6, Mat.GRASS);
        bp.outline(0, 0, 6, 6, 1, Mat.FENCE);
        bp.set(3, 1, 0, Mat.GATE);
        // Hives on low plinths, flowers between them.
        for (int x = 1; x <= 5; x += 2) {
            bp.set(x, 1, 4, Mat.SLAB);
            bp.set(x, 2, 4, Mat.BEEHIVE);
        }
        for (int x = 1; x <= 5; x++) {
            bp.set(x, 1, 2, Mat.FLOWER);
        }
        bp.set(1, 1, 1, Mat.FLOWER);
        bp.set(5, 1, 1, Mat.FLOWER);
        bp.set(3, 1, 5, Mat.BARREL);
        return bp;
    }

    private static Blueprint fishingHut() {
        Blueprint bp = house(StructureType.FISHING_HUT, 9, 8, 4, false, true);
        // A jetty reaching out the back, over the water.
        for (int z = 8; z <= 8; z++) {
            bp.fill(3, 1, z, 5, 1, z, Mat.SLAB);
        }
        bp.set(4, 1, 7, Mat.DOOR);
        bp.set(4, 2, 7, Mat.AIR);
        bp.set(1, 2, 6, Mat.BARREL);
        bp.set(2, 2, 6, Mat.BARREL);
        bp.set(7, 2, 6, Mat.SMOKER);
        bp.set(7, 2, 5, Mat.CHEST);
        bp.set(1, 2, 1, Mat.CRAFTING_TABLE);
        bp.set(6, 2, 1, Mat.CAULDRON);
        bp.set(3, 3, 1, Mat.LANTERN);
        bp.set(4, 3, 8, Mat.HANGING_LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    // =====================================================================
    // Industry
    // =====================================================================

    private static Blueprint lumberCamp() {
        Blueprint bp = new Blueprint(StructureType.LUMBER_CAMP, 9, 8, 8);
        bp.fill(0, 0, 0, 8, 0, 7, Mat.PATH);
        // Open-sided work shed.
        bp.fill(1, 0, 4, 7, 0, 7, Mat.FLOOR);
        bp.fill(1, 1, 7, 7, 3, 7, Mat.WALL);
        bp.fill(1, 1, 4, 1, 3, 7, Mat.WALL);
        bp.fill(7, 1, 4, 7, 3, 7, Mat.WALL);
        bp.fill(1, 4, 4, 7, 4, 7, Mat.ROOF_SLAB);
        bp.set(3, 1, 4, Mat.PILLAR);
        bp.set(5, 1, 4, Mat.PILLAR);
        bp.set(3, 2, 4, Mat.PILLAR);
        bp.set(5, 2, 4, Mat.PILLAR);
        bp.set(2, 1, 6, Mat.CRAFTING_TABLE);
        bp.set(3, 1, 6, Mat.CHEST);
        bp.set(6, 1, 6, Mat.FLETCHING_TABLE);
        bp.set(4, 3, 7, Mat.LANTERN);
        // Log piles and stumps outside.
        bp.fill(1, 1, 1, 2, 2, 2, Mat.LOG);
        bp.set(6, 1, 1, Mat.LOG);
        bp.set(7, 1, 2, Mat.LOG);
        bp.set(4, 1, 1, Mat.CAMPFIRE);
        return bp;
    }

    private static Blueprint sawmill() {
        Blueprint bp = house(StructureType.SAWMILL, 11, 9, 5, false, true);
        // Big working doorway.
        bp.set(4, 1, 0, Mat.DOOR);
        bp.set(6, 1, 0, Mat.DOOR);
        bp.set(4, 2, 0, Mat.AIR);
        bp.set(6, 2, 0, Mat.AIR);
        // The saw bench down the centre, timber stacked either side.
        for (int z = 3; z <= 6; z++) {
            bp.set(5, 2, z, Mat.SLAB);
        }
        bp.set(5, 2, 7, Mat.STONECUTTER);
        bp.set(4, 2, 7, Mat.CRAFTING_TABLE);
        bp.fill(1, 2, 5, 2, 3, 7, Mat.LOG);
        bp.fill(8, 2, 5, 9, 3, 7, Mat.LOG);
        bp.set(1, 2, 1, Mat.CHEST);
        bp.set(9, 2, 1, Mat.BARREL);
        bp.set(2, 2, 3, Mat.FLETCHING_TABLE);
        bp.set(3, 4, 4, Mat.LANTERN);
        bp.set(7, 4, 4, Mat.LANTERN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint mineHead() {
        Blueprint bp = new Blueprint(StructureType.MINE_HEAD, 9, 9, 9);
        bp.fill(0, 0, 0, 8, 0, 8, Mat.GRAVEL);
        // The shaft: a fenced hole with a ladder going down.
        bp.fill(3, 0, 3, 5, 0, 5, Mat.AIR);
        bp.outline(2, 2, 6, 6, 1, Mat.LOW_WALL);
        bp.set(4, 1, 2, Mat.AIR);
        bp.set(4, 0, 4, Mat.LADDER);
        // Headframe over the shaft.
        for (int y = 1; y <= 4; y++) {
            bp.set(2, y, 2, Mat.PILLAR);
            bp.set(6, y, 2, Mat.PILLAR);
            bp.set(2, y, 6, Mat.PILLAR);
            bp.set(6, y, 6, Mat.PILLAR);
        }
        bp.fill(2, 5, 2, 6, 5, 6, Mat.ROOF_SLAB);
        bp.fill(3, 5, 3, 5, 5, 5, Mat.AIR);
        bp.set(4, 4, 4, Mat.HANGING_LANTERN);
        // Tool shed and ore heaps.
        bp.set(1, 1, 7, Mat.CHEST);
        bp.set(2, 1, 7, Mat.CRAFTING_TABLE);
        bp.set(7, 1, 7, Mat.FURNACE);
        bp.fill(7, 1, 1, 7, 2, 2, Mat.ORE_STONE);
        bp.set(1, 1, 1, Mat.CAMPFIRE);
        bp.set(4, 1, 8, Mat.BARREL);
        return bp;
    }

    private static Blueprint quarry() {
        Blueprint bp = new Blueprint(StructureType.QUARRY, 11, 6, 11);
        bp.fill(0, 0, 0, 10, 0, 10, Mat.FOUNDATION);
        // Stepped pit cut into the ground.
        bp.fill(2, 0, 2, 8, 0, 8, Mat.AIR);
        bp.outline(2, 2, 8, 8, 0, Mat.STAIR);
        bp.fill(3, 0, 3, 7, 0, 7, Mat.AIR);
        // Working level with cut blocks stacked ready.
        bp.set(1, 1, 1, Mat.STONECUTTER);
        bp.set(2, 1, 1, Mat.CHEST);
        bp.fill(8, 1, 1, 9, 2, 2, Mat.WALL_ACCENT);
        bp.set(1, 1, 9, Mat.CRAFTING_TABLE);
        bp.set(9, 1, 9, Mat.BARREL);
        bp.set(5, 1, 0, Mat.LANTERN);
        bp.outline(0, 0, 10, 10, 1, Mat.LOW_WALL);
        bp.set(5, 1, 0, Mat.AIR);
        return bp;
    }

    private static Blueprint smithy() {
        Blueprint bp = house(StructureType.SMITHY, 9, 8, 4, true, true);
        // The forge: furnaces, anvil, quench trough, tool racks.
        bp.set(6, 2, 6, Mat.FURNACE);
        bp.set(7, 2, 6, Mat.BLAST_FURNACE);
        bp.set(7, 2, 5, Mat.FURNACE);
        bp.set(4, 2, 6, Mat.ANVIL);
        bp.set(2, 2, 6, Mat.CAULDRON);
        bp.set(1, 2, 5, Mat.SMITHING_TABLE);
        bp.set(1, 2, 3, Mat.GRINDSTONE);
        bp.set(7, 2, 2, Mat.CHEST);
        bp.set(2, 2, 1, Mat.CRAFTING_TABLE);
        bp.set(4, 2, 3, Mat.SLAB);
        bp.set(4, 3, 6, Mat.LANTERN);
        bp.set(4, 3, 1, Mat.HANGING_LANTERN);
        // Chimney over the forge.
        bp.fill(7, 5, 6, 7, 7, 6, Mat.WALL_ACCENT);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint foundry() {
        Blueprint bp = house(StructureType.FOUNDRY, 11, 9, 5, true, false);
        // A row of blast furnaces along the back, chimneys above each.
        for (int x = 2; x <= 8; x += 3) {
            bp.set(x, 2, 7, Mat.BLAST_FURNACE);
            bp.fill(x, 6, 7, x, 8, 7, Mat.WALL_ACCENT);
        }
        bp.set(5, 2, 5, Mat.ANVIL);
        bp.set(4, 2, 5, Mat.SMITHING_TABLE);
        bp.set(6, 2, 5, Mat.GRINDSTONE);
        bp.set(1, 2, 2, Mat.CHEST);
        bp.set(9, 2, 2, Mat.BARREL);
        bp.set(1, 2, 6, Mat.CAULDRON);
        bp.set(9, 2, 6, Mat.CRAFTING_TABLE);
        bp.fill(2, 2, 2, 3, 2, 2, Mat.ORE_STONE);
        bp.set(3, 5, 4, Mat.LANTERN);
        bp.set(7, 5, 4, Mat.LANTERN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint carpenter() {
        Blueprint bp = house(StructureType.CARPENTER, 9, 8, 4, false, true);
        bp.set(2, 2, 6, Mat.CRAFTING_TABLE);
        bp.set(3, 2, 6, Mat.STONECUTTER);
        bp.set(4, 2, 6, Mat.FLETCHING_TABLE);
        bp.set(6, 2, 6, Mat.CHEST);
        bp.fill(1, 2, 3, 1, 3, 5, Mat.LOG);
        bp.fill(7, 2, 3, 7, 3, 5, Mat.LOG);
        bp.set(4, 2, 3, Mat.TABLE);
        bp.set(3, 2, 1, Mat.BARREL);
        bp.set(5, 2, 1, Mat.SLAB);
        bp.set(4, 3, 6, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint masonYard() {
        Blueprint bp = new Blueprint(StructureType.MASON_YARD, 9, 6, 9);
        bp.fill(0, 0, 0, 8, 0, 8, Mat.FOUNDATION);
        // Covered working area at the back.
        bp.fill(1, 1, 7, 7, 3, 7, Mat.WALL);
        bp.fill(1, 1, 5, 1, 3, 7, Mat.WALL);
        bp.fill(7, 1, 5, 7, 3, 7, Mat.WALL);
        bp.fill(1, 4, 5, 7, 4, 7, Mat.ROOF_SLAB);
        bp.set(3, 1, 5, Mat.PILLAR);
        bp.set(5, 1, 5, Mat.PILLAR);
        bp.fill(3, 2, 5, 3, 3, 5, Mat.PILLAR);
        bp.fill(5, 2, 5, 5, 3, 5, Mat.PILLAR);
        bp.set(2, 1, 6, Mat.STONECUTTER);
        bp.set(4, 1, 6, Mat.STONECUTTER);
        bp.set(6, 1, 6, Mat.CHEST);
        bp.set(4, 3, 7, Mat.LANTERN);
        // A door in the shed's back wall so the yard is properly enclosed.
        bp.set(4, 1, 7, Mat.DOOR);
        bp.set(4, 2, 7, Mat.AIR);
        // Stacks of dressed stone in the yard.
        bp.fill(1, 1, 1, 2, 2, 2, Mat.WALL_ACCENT);
        bp.fill(6, 1, 1, 7, 1, 2, Mat.WALL_ACCENT);
        bp.set(4, 1, 2, Mat.SLAB);
        bp.outline(0, 0, 8, 8, 1, Mat.LOW_WALL);
        bp.set(4, 1, 0, Mat.AIR);
        return bp;
    }

    private static Blueprint weaver() {
        Blueprint bp = house(StructureType.WEAVER, 8, 8, 4, false, true);
        bp.set(2, 2, 6, Mat.LOOM);
        bp.set(4, 2, 6, Mat.LOOM);
        bp.set(6, 2, 6, Mat.CAULDRON);
        bp.set(1, 2, 4, Mat.CHEST);
        bp.set(6, 2, 4, Mat.BARREL);
        bp.set(3, 2, 2, Mat.TABLE);
        bp.set(1, 2, 1, Mat.CARPET);
        bp.set(5, 2, 1, Mat.CARPET);
        bp.set(6, 2, 2, Mat.CRAFTING_TABLE);
        bp.set(3, 3, 6, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint tannery() {
        Blueprint bp = house(StructureType.TANNERY, 9, 8, 4, false, true);
        // Soaking vats out back, drying racks inside.
        bp.set(2, 2, 6, Mat.CAULDRON);
        bp.set(3, 2, 6, Mat.CAULDRON);
        bp.set(4, 2, 6, Mat.CAULDRON);
        bp.set(6, 2, 6, Mat.COMPOSTER);
        bp.set(1, 2, 4, Mat.BARREL);
        bp.set(7, 2, 4, Mat.CHEST);
        bp.fill(2, 3, 2, 6, 3, 2, Mat.BEAM);
        bp.set(3, 2, 2, Mat.CARPET);
        bp.set(5, 2, 2, Mat.CARPET);
        bp.set(7, 2, 1, Mat.SMITHING_TABLE);
        bp.set(4, 3, 5, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint pottery() {
        Blueprint bp = house(StructureType.POTTERY, 8, 8, 4, true, true);
        // Kilns and wheels.
        bp.set(5, 2, 6, Mat.FURNACE);
        bp.set(6, 2, 6, Mat.FURNACE);
        bp.fill(6, 5, 6, 6, 7, 6, Mat.WALL_ACCENT);
        bp.set(2, 2, 6, Mat.CAULDRON);
        bp.set(2, 2, 4, Mat.SLAB);
        bp.set(3, 2, 4, Mat.SLAB);
        bp.set(1, 2, 2, Mat.CHEST);
        bp.set(6, 2, 2, Mat.BARREL);
        bp.set(4, 2, 2, Mat.FLOWER_POT);
        bp.set(3, 2, 1, Mat.FLOWER_POT);
        bp.set(3, 3, 5, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint brewery() {
        Blueprint bp = house(StructureType.BREWERY, 9, 9, 5, true, true);
        // Mash tuns, a brewing stand, and a cellar of barrels.
        bp.set(2, 2, 7, Mat.CAULDRON);
        bp.set(3, 2, 7, Mat.CAULDRON);
        bp.set(5, 2, 7, Mat.BREWING_STAND);
        bp.set(6, 2, 7, Mat.FURNACE);
        bp.fill(1, 2, 4, 1, 3, 6, Mat.BARREL);
        bp.fill(7, 2, 4, 7, 3, 6, Mat.BARREL);
        bp.set(4, 2, 4, Mat.TABLE);
        bp.set(3, 2, 4, Mat.STAIR);
        bp.set(5, 2, 4, Mat.STAIR);
        bp.set(2, 2, 1, Mat.CHEST);
        bp.set(6, 2, 1, Mat.HAY);
        bp.set(4, 4, 6, Mat.LANTERN);
        bp.set(4, 4, 2, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    // =====================================================================
    // Trade
    // =====================================================================

    private static Blueprint marketStall() {
        Blueprint bp = new Blueprint(StructureType.MARKET_STALL, 5, 5, 5);
        bp.fill(0, 0, 0, 4, 0, 4, Mat.PATH);
        // Four posts, an awning, a counter and goods behind it.
        for (int y = 1; y <= 2; y++) {
            bp.set(0, y, 0, Mat.FENCE);
            bp.set(4, y, 0, Mat.FENCE);
            bp.set(0, y, 4, Mat.FENCE);
            bp.set(4, y, 4, Mat.FENCE);
        }
        bp.fill(0, 3, 0, 4, 3, 4, Mat.ROOF_SLAB);
        bp.fill(1, 1, 1, 3, 1, 1, Mat.SLAB);
        bp.set(1, 1, 3, Mat.BARREL);
        bp.set(3, 1, 3, Mat.CHEST);
        bp.set(2, 1, 3, Mat.CRAFTING_TABLE);
        bp.set(2, 2, 4, Mat.SIGN);
        bp.set(0, 3, 2, Mat.HANGING_LANTERN);
        return bp;
    }

    private static Blueprint marketplace() {
        Blueprint bp = new Blueprint(StructureType.MARKETPLACE, 13, 6, 13);
        bp.fill(0, 0, 0, 12, 0, 12, Mat.PATH);
        bp.fill(5, 0, 5, 7, 0, 7, Mat.FLOOR_ACCENT);
        // Four stalls around a central space.
        int[][] stalls = {{1, 1}, {9, 1}, {1, 9}, {9, 9}};
        for (int[] s : stalls) {
            int x = s[0];
            int z = s[1];
            for (int y = 1; y <= 2; y++) {
                bp.set(x, y, z, Mat.FENCE);
                bp.set(x + 2, y, z, Mat.FENCE);
                bp.set(x, y, z + 2, Mat.FENCE);
                bp.set(x + 2, y, z + 2, Mat.FENCE);
            }
            bp.fill(x, 3, z, x + 2, 3, z + 2, Mat.ROOF_SLAB);
            bp.set(x + 1, 1, z + 2, Mat.SLAB);
            bp.set(x + 1, 1, z + 1, Mat.BARREL);
            bp.set(x, 1, z + 1, Mat.CHEST);
        }
        // A well-lit centre with benches.
        bp.set(6, 1, 6, Mat.LANTERN);
        bp.set(5, 1, 5, Mat.SLAB);
        bp.set(7, 1, 7, Mat.SLAB);
        bp.set(5, 1, 7, Mat.FLOWER_POT);
        bp.set(7, 1, 5, Mat.FLOWER_POT);
        bp.set(6, 1, 0, Mat.SIGN);
        return bp;
    }

    private static Blueprint warehouse() {
        Blueprint bp = house(StructureType.WAREHOUSE, 11, 9, 6, true, false);
        bp.set(4, 1, 0, Mat.DOOR);
        bp.set(6, 1, 0, Mat.DOOR);
        bp.set(4, 2, 0, Mat.AIR);
        bp.set(6, 2, 0, Mat.AIR);
        // Aisles of crates two high, with a walkway down the middle.
        for (int x = 1; x <= 9; x++) {
            if (x == 5) continue;
            for (int z = 3; z <= 7; z += 2) {
                bp.set(x, 2, z, Mat.BARREL);
                if (x % 2 == 0) bp.set(x, 3, z, Mat.CHEST);
            }
        }
        bp.set(5, 2, 7, Mat.CHEST);
        bp.set(1, 2, 1, Mat.LECTERN);
        bp.set(9, 2, 1, Mat.CHEST);
        bp.set(3, 5, 4, Mat.HANGING_LANTERN);
        bp.set(7, 5, 4, Mat.HANGING_LANTERN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint tradingPost() {
        Blueprint bp = house(StructureType.TRADING_POST, 9, 9, 5, true, true);
        // Counter facing the door, strongboxes behind it.
        bp.fill(2, 2, 3, 6, 2, 3, Mat.SLAB);
        bp.set(4, 2, 3, Mat.AIR);
        bp.set(2, 2, 6, Mat.CHEST);
        bp.set(3, 2, 6, Mat.CHEST);
        bp.set(5, 2, 6, Mat.BARREL);
        bp.set(6, 2, 6, Mat.CARTOGRAPHY_TABLE);
        bp.set(1, 2, 5, Mat.LECTERN);
        bp.set(7, 2, 4, Mat.BARREL);
        // Guest beds for travelling merchants.
        bp.set(1, 2, 1, Mat.BED);
        bp.set(7, 2, 1, Mat.BED);
        bp.set(4, 2, 1, Mat.CARPET);
        bp.set(4, 4, 6, Mat.LANTERN);
        bp.set(4, 2, 0, Mat.DOOR);
        bp.set(2, 4, 0, Mat.SIGN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint inn() {
        Blueprint bp = house(StructureType.INN, 11, 10, 5, true, true);
        // Taproom: bar, tables, hearth.
        bp.fill(6, 2, 7, 9, 2, 7, Mat.SLAB);
        bp.set(9, 2, 6, Mat.BARREL);
        bp.set(8, 2, 6, Mat.BARREL);
        bp.set(6, 2, 6, Mat.BREWING_STAND);
        bp.set(1, 2, 7, Mat.FURNACE);
        bp.set(2, 2, 7, Mat.SMOKER);
        bp.set(3, 2, 4, Mat.TABLE);
        bp.set(2, 2, 4, Mat.STAIR);
        bp.set(4, 2, 4, Mat.STAIR);
        bp.set(7, 2, 4, Mat.TABLE);
        bp.set(6, 2, 4, Mat.STAIR);
        bp.set(8, 2, 4, Mat.STAIR);
        bp.set(1, 2, 1, Mat.JUKEBOX);
        // Guest rooms upstairs.
        bp.set(9, 2, 1, Mat.LADDER);
        bp.set(9, 3, 1, Mat.LADDER);
        bp.fill(1, 4, 1, 9, 4, 8, Mat.FLOOR);
        bp.set(9, 4, 1, Mat.AIR);
        bp.set(2, 5, 7, Mat.BED);
        bp.set(4, 5, 7, Mat.BED);
        bp.set(6, 5, 7, Mat.BED);
        bp.set(8, 5, 7, Mat.BED);
        bp.set(2, 5, 3, Mat.CARPET);
        bp.set(5, 5, 3, Mat.CHEST);
        bp.set(3, 6, 8, Mat.LANTERN);
        bp.set(7, 6, 8, Mat.LANTERN);
        bp.set(2, 4, 0, Mat.SIGN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint stable() {
        Blueprint bp = new Blueprint(StructureType.STABLE, 11, 7, 9);
        bp.fill(0, 0, 0, 10, 0, 8, Mat.PATH);
        // Row of open stalls under one long roof.
        bp.fill(1, 0, 4, 9, 0, 8, Mat.FLOOR);
        bp.fill(1, 1, 8, 9, 3, 8, Mat.WALL);
        bp.fill(1, 1, 4, 1, 3, 8, Mat.WALL);
        bp.fill(9, 1, 4, 9, 3, 8, Mat.WALL);
        for (int x = 3; x <= 7; x += 2) {
            bp.fill(x, 1, 5, x, 2, 7, Mat.FENCE);
            bp.fill(x, 1, 4, x, 3, 4, Mat.PILLAR);
        }
        bp.fill(1, 4, 4, 9, 4, 8, Mat.ROOF_SLAB);
        bp.set(2, 1, 7, Mat.HAY);
        bp.set(4, 1, 7, Mat.HAY);
        bp.set(6, 1, 7, Mat.HAY);
        bp.set(8, 1, 7, Mat.HAY);
        bp.set(2, 1, 5, Mat.WATER);
        bp.set(8, 1, 5, Mat.CHEST);
        bp.set(5, 3, 8, Mat.LANTERN);
        // Tack-room door at the end of the range.
        bp.set(9, 1, 6, Mat.DOOR);
        bp.set(9, 2, 6, Mat.AIR);
        // Paddock in front.
        bp.outline(0, 0, 10, 3, 1, Mat.FENCE);
        bp.set(5, 1, 0, Mat.GATE);
        return bp;
    }

    private static Blueprint dock() {
        Blueprint bp = new Blueprint(StructureType.DOCK, 11, 6, 11);
        bp.fill(0, 0, 0, 10, 0, 3, Mat.PATH);
        // A pier of planks on posts reaching into the water.
        bp.fill(4, 1, 4, 6, 1, 10, Mat.FLOOR);
        for (int z = 4; z <= 10; z += 2) {
            bp.set(4, 0, z, Mat.PILLAR);
            bp.set(6, 0, z, Mat.PILLAR);
        }
        // Side jetties.
        bp.fill(1, 1, 6, 3, 1, 6, Mat.FLOOR);
        bp.fill(7, 1, 6, 9, 1, 6, Mat.FLOOR);
        bp.set(3, 2, 5, Mat.FENCE);
        bp.set(7, 2, 5, Mat.FENCE);
        // Crane post and cargo.
        bp.fill(3, 1, 3, 3, 4, 3, Mat.PILLAR);
        bp.fill(3, 4, 3, 3, 4, 5, Mat.BEAM);
        bp.set(1, 1, 1, Mat.BARREL);
        bp.set(2, 1, 1, Mat.CHEST);
        bp.set(8, 1, 1, Mat.BARREL);
        bp.set(9, 1, 2, Mat.CRAFTING_TABLE);
        bp.set(5, 2, 10, Mat.LANTERN);
        bp.set(5, 2, 4, Mat.LANTERN);
        return bp;
    }

    // =====================================================================
    // Knowledge and care
    // =====================================================================

    private static Blueprint shrine() {
        Blueprint bp = new Blueprint(StructureType.SHRINE, 5, 5, 5);
        bp.fill(0, 0, 0, 4, 0, 4, Mat.FOUNDATION);
        // Open canopy on four pillars over an altar.
        for (int y = 1; y <= 3; y++) {
            bp.set(0, y, 0, Mat.PILLAR);
            bp.set(4, y, 0, Mat.PILLAR);
            bp.set(0, y, 4, Mat.PILLAR);
            bp.set(4, y, 4, Mat.PILLAR);
        }
        bp.fill(0, 4, 0, 4, 4, 4, Mat.ROOF_SLAB);
        bp.set(2, 4, 2, Mat.ROOF_BLOCK);
        bp.set(2, 1, 3, Mat.WALL);
        bp.set(2, 2, 3, Mat.SLAB);
        bp.set(1, 1, 3, Mat.LANTERN);
        bp.set(3, 1, 3, Mat.LANTERN);
        bp.set(2, 1, 1, Mat.CARPET);
        bp.set(1, 1, 1, Mat.FLOWER_POT);
        bp.set(3, 1, 1, Mat.FLOWER_POT);
        return bp;
    }

    private static Blueprint library() {
        Blueprint bp = house(StructureType.LIBRARY, 11, 9, 6, true, true);
        // Shelves lining the walls, lecterns and reading desks in the middle.
        for (int x = 1; x <= 9; x++) {
            if (x == 5) continue;
            bp.set(x, 2, 7, Mat.BOOKSHELF);
            bp.set(x, 3, 7, Mat.BOOKSHELF);
        }
        bp.fill(1, 2, 3, 1, 4, 5, Mat.BOOKSHELF);
        bp.fill(9, 2, 3, 9, 4, 5, Mat.BOOKSHELF);
        bp.set(3, 2, 4, Mat.LECTERN);
        bp.set(7, 2, 4, Mat.LECTERN);
        bp.set(5, 2, 4, Mat.TABLE);
        bp.set(5, 2, 3, Mat.STAIR);
        bp.set(5, 2, 5, Mat.STAIR);
        bp.set(5, 2, 7, Mat.CARTOGRAPHY_TABLE);
        bp.set(2, 2, 1, Mat.CHEST);
        bp.set(8, 2, 1, Mat.CARPET);
        bp.set(3, 5, 2, Mat.LANTERN);
        bp.set(7, 5, 2, Mat.LANTERN);
        bp.set(3, 5, 6, Mat.LANTERN);
        bp.set(7, 5, 6, Mat.LANTERN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint school() {
        Blueprint bp = house(StructureType.SCHOOL, 11, 9, 5, true, true);
        // Rows of desks facing a teaching lectern.
        for (int z = 3; z <= 6; z += 2) {
            for (int x = 2; x <= 8; x += 2) {
                bp.set(x, 2, z, Mat.STAIR);
                bp.set(x, 2, z + 1 <= 7 ? z + 1 : z, Mat.SKIP);
            }
        }
        for (int z = 4; z <= 7; z += 2) {
            for (int x = 2; x <= 8; x += 2) {
                bp.set(x, 2, z, Mat.TABLE);
            }
        }
        bp.set(5, 2, 1, Mat.LECTERN);
        bp.fill(2, 3, 1, 8, 3, 1, Mat.WALL_ACCENT);
        bp.set(1, 2, 7, Mat.BOOKSHELF);
        bp.set(9, 2, 7, Mat.BOOKSHELF);
        bp.set(1, 2, 1, Mat.CHEST);
        bp.set(3, 4, 4, Mat.LANTERN);
        bp.set(7, 4, 4, Mat.LANTERN);
        bp.set(9, 2, 1, Mat.BELL);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint apothecary() {
        Blueprint bp = house(StructureType.APOTHECARY, 9, 8, 4, false, true);
        bp.set(2, 2, 6, Mat.BREWING_STAND);
        bp.set(3, 2, 6, Mat.CAULDRON);
        bp.set(5, 2, 6, Mat.COMPOSTER);
        bp.set(6, 2, 6, Mat.FURNACE);
        bp.fill(1, 2, 3, 1, 3, 5, Mat.BOOKSHELF);
        bp.set(7, 2, 4, Mat.CHEST);
        bp.set(7, 2, 3, Mat.BARREL);
        bp.set(4, 2, 3, Mat.TABLE);
        bp.set(2, 2, 1, Mat.FLOWER_POT);
        bp.set(6, 2, 1, Mat.FLOWER_POT);
        bp.set(4, 2, 1, Mat.LECTERN);
        bp.set(4, 3, 5, Mat.LANTERN);
        doorstep(bp, 4);
        return bp;
    }

    private static Blueprint infirmary() {
        Blueprint bp = house(StructureType.INFIRMARY, 11, 9, 5, true, true);
        // Beds down both walls with a nurse's station between.
        for (int z = 3; z <= 7; z += 2) {
            bp.set(1, 2, z, Mat.BED);
            bp.set(9, 2, z, Mat.BED);
        }
        bp.set(5, 2, 7, Mat.BREWING_STAND);
        bp.set(4, 2, 7, Mat.CAULDRON);
        bp.set(6, 2, 7, Mat.CHEST);
        bp.set(5, 2, 4, Mat.TABLE);
        bp.set(3, 2, 1, Mat.BOOKSHELF);
        bp.set(7, 2, 1, Mat.COMPOSTER);
        bp.set(3, 2, 4, Mat.CARPET);
        bp.set(7, 2, 4, Mat.CARPET);
        bp.set(3, 4, 5, Mat.LANTERN);
        bp.set(7, 4, 5, Mat.LANTERN);
        doorstep(bp, 5);
        return bp;
    }

    private static Blueprint observatory() {
        Blueprint bp = new Blueprint(StructureType.OBSERVATORY, 9, 15, 9);
        bp.fill(0, 0, 0, 8, 0, 8, Mat.FOUNDATION);
        // Square base storey.
        bp.walls(0, 0, 8, 8, 1, 4, Mat.WALL);
        bp.hollow(0, 1, 0, 8, 4, 8);
        bp.fill(1, 1, 1, 7, 1, 7, Mat.FLOOR);
        bp.corners(0, 0, 8, 8, 1, 4, Mat.PILLAR);
        bp.set(4, 1, 0, Mat.DOOR);
        bp.set(4, 2, 0, Mat.AIR);
        bp.set(2, 3, 0, Mat.WINDOW);
        bp.set(6, 3, 0, Mat.WINDOW);
        bp.set(2, 2, 6, Mat.BOOKSHELF);
        bp.set(6, 2, 6, Mat.BOOKSHELF);
        bp.set(4, 2, 6, Mat.LECTERN);
        bp.set(2, 2, 2, Mat.CARTOGRAPHY_TABLE);
        // The tower.
        bp.fill(2, 5, 2, 6, 5, 6, Mat.FLOOR);
        bp.fill(3, 5, 3, 5, 5, 5, Mat.AIR);
        for (int y = 5; y <= 10; y++) {
            bp.outline(3, 3, 5, 5, y, Mat.WALL);
        }
        bp.fill(4, 5, 4, 4, 10, 4, Mat.AIR);
        for (int y = 1; y <= 10; y++) {
            bp.set(3, y, 4, Mat.LADDER);
        }
        // Open observation deck.
        bp.fill(2, 11, 2, 6, 11, 6, Mat.FLOOR);
        bp.outline(2, 2, 6, 6, 12, Mat.FENCE);
        bp.set(4, 12, 4, Mat.LECTERN);
        bp.set(2, 12, 2, Mat.LANTERN);
        bp.set(6, 12, 6, Mat.LANTERN);
        bp.set(4, 13, 2, Mat.BEAM);
        // Roof over the base storey, and a small cupola above the deck so
        // the astronomers have somewhere dry to keep their charts.
        bp.flatRoof(0, 0, 8, 8, 5, Mat.ROOF_BLOCK, Mat.ROOF_SLAB);
        bp.fill(2, 5, 2, 6, 5, 6, Mat.AIR);
        bp.fill(2, 11, 2, 6, 11, 6, Mat.FLOOR);
        bp.fill(3, 13, 3, 5, 13, 5, Mat.ROOF_SLAB);
        bp.set(4, 14, 4, Mat.ROOF_BLOCK);
        return bp;
    }

    private static Blueprint bathhouse() {
        Blueprint bp = house(StructureType.BATHHOUSE, 11, 9, 5, true, false);
        // A warm pool sunk into the floor, benches around it.
        bp.fill(3, 1, 3, 7, 1, 6, Mat.WATER);
        bp.fill(3, 0, 3, 7, 0, 6, Mat.FOUNDATION);
        bp.outline(2, 2, 8, 7, 2, Mat.SLAB);
        bp.set(1, 2, 2, Mat.CAULDRON);
        bp.set(9, 2, 2, Mat.CAULDRON);
        bp.set(1, 2, 7, Mat.FURNACE);
        bp.set(9, 2, 7, Mat.FURNACE);
        bp.set(2, 2, 1, Mat.FLOWER_POT);
        bp.set(8, 2, 1, Mat.FLOWER_POT);
        bp.set(5, 2, 7, Mat.CARPET);
        for (int x = 2; x <= 8; x += 3) {
            bp.set(x, 5, 2, Mat.LANTERN);
            bp.set(x, 5, 7, Mat.LANTERN);
        }
        // Skylight down the middle of the flat roof.
        bp.fill(4, 6, 3, 6, 6, 6, Mat.GLASS);
        doorstep(bp, 5);
        return bp;
    }

    // =====================================================================
    // Defence
    // =====================================================================

    private static Blueprint guardPost() {
        Blueprint bp = new Blueprint(StructureType.GUARD_POST, 5, 7, 5);
        bp.fill(0, 0, 0, 4, 0, 4, Mat.FOUNDATION);
        bp.walls(0, 0, 4, 4, 1, 3, Mat.WALL);
        bp.hollow(0, 1, 0, 4, 3, 4);
        bp.corners(0, 0, 4, 4, 1, 3, Mat.PILLAR);
        bp.set(2, 1, 0, Mat.DOOR);
        bp.set(2, 2, 0, Mat.AIR);
        bp.set(0, 2, 2, Mat.BARS);
        bp.set(4, 2, 2, Mat.BARS);
        bp.set(2, 2, 4, Mat.BARS);
        bp.set(1, 1, 3, Mat.BED);
        bp.set(3, 1, 3, Mat.CHEST);
        bp.set(3, 1, 1, Mat.CRAFTING_TABLE);
        // Fighting top with a rail and a small shingled canopy, so the
        // watch can stand a wet night without the roof leaking.
        bp.fill(0, 4, 0, 4, 4, 4, Mat.FLOOR);
        bp.outline(0, 0, 4, 4, 5, Mat.LOW_WALL);
        bp.set(0, 5, 0, Mat.PILLAR);
        bp.set(4, 5, 0, Mat.PILLAR);
        bp.set(0, 5, 4, Mat.PILLAR);
        bp.set(4, 5, 4, Mat.PILLAR);
        bp.fill(0, 6, 0, 4, 6, 4, Mat.ROOF_SLAB);
        bp.set(2, 5, 2, Mat.LANTERN);
        bp.set(1, 1, 1, Mat.LADDER);
        bp.set(1, 2, 1, Mat.LADDER);
        bp.set(1, 3, 1, Mat.LADDER);
        bp.set(1, 4, 1, Mat.AIR);
        return bp;
    }

    private static Blueprint watchtower() {
        Blueprint bp = new Blueprint(StructureType.WATCHTOWER, 7, 14, 7);
        bp.fill(0, 0, 0, 6, 0, 6, Mat.FOUNDATION);
        // Wide base tapering to a shaft.
        bp.walls(1, 1, 5, 5, 1, 3, Mat.WALL);
        bp.hollow(1, 1, 1, 5, 3, 5);
        bp.fill(2, 1, 2, 4, 1, 4, Mat.FLOOR);
        bp.set(3, 1, 1, Mat.DOOR);
        bp.set(3, 2, 1, Mat.AIR);
        bp.corners(1, 1, 5, 5, 1, 3, Mat.PILLAR);
        // Shaft.
        for (int y = 4; y <= 9; y++) {
            bp.outline(2, 2, 4, 4, y, Mat.WALL);
        }
        bp.fill(3, 4, 3, 3, 9, 3, Mat.AIR);
        for (int y = 1; y <= 9; y++) {
            bp.set(2, y, 3, Mat.LADDER);
        }
        bp.set(2, 6, 2, Mat.BARS);
        bp.set(4, 6, 4, Mat.BARS);
        // Overhanging crown.
        bp.fill(1, 10, 1, 5, 10, 5, Mat.FLOOR);
        bp.fill(2, 10, 2, 4, 10, 4, Mat.AIR);
        bp.fill(3, 10, 3, 3, 10, 3, Mat.AIR);
        bp.outline(1, 1, 5, 5, 11, Mat.LOW_WALL);
        bp.set(1, 12, 1, Mat.PILLAR);
        bp.set(5, 12, 1, Mat.PILLAR);
        bp.set(1, 12, 5, Mat.PILLAR);
        bp.set(5, 12, 5, Mat.PILLAR);
        bp.fill(1, 13, 1, 5, 13, 5, Mat.ROOF_SLAB);
        bp.set(3, 11, 3, Mat.CAMPFIRE);
        bp.set(3, 12, 1, Mat.BANNER);
        bp.set(1, 2, 1, Mat.LANTERN);
        // The watch's own kit at the foot of the tower.
        bp.set(2, 1, 4, Mat.CHEST);
        bp.set(4, 1, 4, Mat.FLETCHING_TABLE);
        bp.set(4, 1, 2, Mat.BARREL);
        return bp;
    }

    private static Blueprint barracks() {
        Blueprint bp = house(StructureType.BARRACKS, 13, 8, 5, true, true);
        // Two rows of bunks with an arms rack and a drill floor.
        for (int x = 2; x <= 10; x += 2) {
            bp.set(x, 2, 6, Mat.BED);
        }
        bp.set(2, 2, 1, Mat.BED);
        bp.set(10, 2, 1, Mat.BED);
        bp.set(6, 2, 6, Mat.CHEST);
        bp.set(1, 2, 3, Mat.SMITHING_TABLE);
        bp.set(1, 2, 4, Mat.GRINDSTONE);
        bp.set(11, 2, 3, Mat.ANVIL);
        bp.set(11, 2, 4, Mat.CHEST);
        bp.set(6, 2, 3, Mat.TABLE);
        bp.set(5, 2, 3, Mat.STAIR);
        bp.set(7, 2, 3, Mat.STAIR);
        bp.set(4, 2, 1, Mat.BANNER);
        bp.set(8, 2, 1, Mat.BANNER);
        for (int x = 3; x <= 9; x += 3) {
            bp.set(x, 4, 5, Mat.LANTERN);
        }
        doorstep(bp, 6);
        return bp;
    }

    private static Blueprint gatehouse() {
        Blueprint bp = new Blueprint(StructureType.GATEHOUSE, 9, 10, 5);
        bp.fill(0, 0, 0, 8, 0, 4, Mat.FOUNDATION);
        // Two towers flanking a road-wide arch.
        for (int y = 1; y <= 6; y++) {
            bp.outline(0, 0, 2, 4, y, Mat.WALL);
            bp.outline(6, 0, 8, 4, y, Mat.WALL);
        }
        bp.fill(1, 1, 1, 1, 5, 3, Mat.AIR);
        bp.fill(7, 1, 1, 7, 5, 3, Mat.AIR);
        bp.set(1, 1, 0, Mat.DOOR);
        bp.set(7, 1, 0, Mat.DOOR);
        bp.set(1, 2, 0, Mat.AIR);
        bp.set(7, 2, 0, Mat.AIR);
        for (int y = 1; y <= 4; y++) {
            bp.set(1, y, 2, Mat.LADDER);
            bp.set(7, y, 2, Mat.LADDER);
        }
        bp.set(1, 3, 0, Mat.BARS);
        bp.set(7, 3, 0, Mat.BARS);
        // The arch over the road.
        bp.fill(3, 1, 0, 5, 4, 4, Mat.AIR);
        bp.fill(3, 5, 0, 5, 5, 4, Mat.WALL);
        bp.fill(3, 4, 0, 5, 4, 0, Mat.WALL_ACCENT);
        bp.fill(3, 4, 4, 5, 4, 4, Mat.WALL_ACCENT);
        bp.set(4, 4, 2, Mat.HANGING_LANTERN);
        // Battlements on top.
        bp.fill(0, 6, 0, 8, 6, 4, Mat.FLOOR);
        bp.fill(1, 6, 1, 1, 6, 3, Mat.AIR);
        bp.fill(7, 6, 1, 7, 6, 3, Mat.AIR);
        for (int x = 0; x <= 8; x += 2) {
            bp.set(x, 7, 0, Mat.LOW_WALL);
            bp.set(x, 7, 4, Mat.LOW_WALL);
        }
        bp.set(0, 7, 2, Mat.LOW_WALL);
        bp.set(8, 7, 2, Mat.LOW_WALL);
        bp.set(2, 7, 2, Mat.BANNER);
        bp.set(6, 7, 2, Mat.BANNER);
        bp.set(4, 7, 1, Mat.LANTERN);
        // Tile caps over both towers.
        bp.fill(0, 8, 0, 2, 8, 4, Mat.ROOF_SLAB);
        bp.fill(6, 8, 0, 8, 8, 4, Mat.ROOF_SLAB);
        // The gate watch keeps a chest and a bench in the west tower.
        bp.set(1, 1, 3, Mat.CHEST);
        bp.set(7, 1, 3, Mat.CRAFTING_TABLE);
        return bp;
    }

    private static Blueprint wallSegment() {
        Blueprint bp = new Blueprint(StructureType.WALL_SEGMENT, 11, 7, 3);
        bp.fill(0, 0, 0, 10, 0, 2, Mat.FOUNDATION);
        // A thick curtain wall with a walkway on top.
        bp.fill(0, 1, 0, 10, 4, 0, Mat.WALL);
        bp.fill(0, 1, 2, 10, 4, 2, Mat.WALL);
        bp.fill(0, 1, 1, 10, 3, 1, Mat.WALL);
        bp.fill(0, 4, 1, 10, 4, 1, Mat.FLOOR);
        // Crenellations.
        for (int x = 0; x <= 10; x += 2) {
            bp.set(x, 5, 0, Mat.LOW_WALL);
            bp.set(x, 5, 2, Mat.LOW_WALL);
        }
        bp.set(5, 5, 1, Mat.LANTERN);
        // Stair up at one end.
        bp.set(1, 1, 1, Mat.LADDER);
        bp.set(1, 2, 1, Mat.LADDER);
        bp.set(1, 3, 1, Mat.LADDER);
        bp.set(1, 3, 1, Mat.LADDER);
        return bp;
    }
}
