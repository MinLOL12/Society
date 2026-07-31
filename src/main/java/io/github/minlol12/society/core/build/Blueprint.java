package io.github.minlol12.society.core.build;

import java.util.ArrayList;
import java.util.List;

/**
 * A building described as a grid of {@link Mat} roles, oriented so that
 * +X is "right", +Z is "back" and the entrance faces -Z (north) before
 * rotation. The Minecraft adapter rotates, resolves roles through a
 * culture palette and places the blocks.
 *
 * <p>Blueprints are pure data with no Minecraft types, so the whole
 * catalogue can be validated headlessly.</p>
 */
public final class Blueprint {

    private final StructureType type;
    private final int width;
    private final int height;
    private final int depth;
    /** Indexed [y][z][x]. */
    private final Mat[][][] cells;
    /** Y offset applied to the whole build (-1 sinks the foundation). */
    private int baseOffset = -1;

    public Blueprint(StructureType type, int width, int height, int depth) {
        this.type = type;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.depth = Math.max(1, depth);
        this.cells = new Mat[this.height][this.depth][this.width];
        for (int y = 0; y < this.height; y++) {
            for (int z = 0; z < this.depth; z++) {
                for (int x = 0; x < this.width; x++) {
                    this.cells[y][z][x] = Mat.SKIP;
                }
            }
        }
    }

    public StructureType type() { return type; }

    public int width() { return width; }

    public int height() { return height; }

    public int depth() { return depth; }

    public int baseOffset() { return baseOffset; }

    public Blueprint baseOffset(int offset) {
        this.baseOffset = offset;
        return this;
    }

    public Mat at(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth) {
            return Mat.SKIP;
        }
        return cells[y][z][x];
    }

    public Blueprint set(int x, int y, int z, Mat mat) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth || mat == null) {
            return this;
        }
        cells[y][z][x] = mat;
        return this;
    }

    // =====================================================================
    // Drawing helpers - these read like the mason's own instructions
    // =====================================================================

    /** Fills an inclusive box. */
    public Blueprint fill(int x0, int y0, int z0, int x1, int y1, int z1, Mat mat) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                    set(x, y, z, mat);
                }
            }
        }
        return this;
    }

    /** Fills the outline of a rectangle on one Y layer (walls, not floor). */
    public Blueprint outline(int x0, int z0, int x1, int z1, int y, Mat mat) {
        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        int minZ = Math.min(z0, z1);
        int maxZ = Math.max(z0, z1);
        for (int x = minX; x <= maxX; x++) {
            set(x, y, minZ, mat);
            set(x, y, maxZ, mat);
        }
        for (int z = minZ; z <= maxZ; z++) {
            set(minX, y, z, mat);
            set(maxX, y, z, mat);
        }
        return this;
    }

    /** Walls of a room: an outline repeated over several layers. */
    public Blueprint walls(int x0, int z0, int x1, int z1, int yFrom, int yTo, Mat mat) {
        for (int y = yFrom; y <= yTo; y++) {
            outline(x0, z0, x1, z1, y, mat);
        }
        return this;
    }

    /** The four corner columns of a room. */
    public Blueprint corners(int x0, int z0, int x1, int z1, int yFrom, int yTo, Mat mat) {
        for (int y = yFrom; y <= yTo; y++) {
            set(x0, y, z0, mat);
            set(x1, y, z0, mat);
            set(x0, y, z1, mat);
            set(x1, y, z1, mat);
        }
        return this;
    }

    /**
     * A gabled roof running along the X axis: each course steps one block
     * inward and one block up, so the result is a properly pitched roof
     * rather than a flat lid.
     *
     * <p>The two slopes are laid with different roles ({@code edge} for the
     * near side, {@code edgeBack} for the far side) so the adapter can turn
     * the stairs to face down their own slope - otherwise both sides of the
     * roof would lean the same way.</p>
     */
    public Blueprint gableRoofX(int x0, int z0, int x1, int z1, int yStart,
                                Mat edge, Mat edgeBack, Mat solid) {
        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        int minZ = Math.min(z0, z1);
        int maxZ = Math.max(z0, z1);
        int span = maxZ - minZ + 1;
        int steps = span / 2;

        for (int i = 0; i < steps; i++) {
            int y = yStart + i;
            int near = minZ + i;
            int far = maxZ - i;
            for (int x = minX; x <= maxX; x++) {
                set(x, y, near, edge);
                set(x, y, far, edgeBack);
                for (int z = near + 1; z <= far - 1; z++) {
                    if (at(x, y, z) == Mat.SKIP || at(x, y, z) == Mat.AIR) {
                        set(x, y, z, (x == minX || x == maxX) ? Mat.WALL : Mat.AIR);
                    }
                }
            }
        }

        // Cap the ridge. An odd span meets on a single line; an even span
        // needs two, or the very top of the roof would be left open.
        if (span % 2 == 1) {
            int ridgeY = yStart + steps - 1;
            int z = minZ + steps;
            for (int x = minX; x <= maxX; x++) {
                set(x, ridgeY, z, solid);
            }
        } else {
            int ridgeY = yStart + steps;
            for (int x = minX; x <= maxX; x++) {
                set(x, ridgeY, minZ + steps - 1, solid);
                set(x, ridgeY, minZ + steps, solid);
            }
        }
        return this;
    }

    /** A flat roof of slabs with a solid rim. */
    public Blueprint flatRoof(int x0, int z0, int x1, int z1, int y, Mat rim, Mat inner) {
        fill(x0, y, z0, x1, y, z1, inner);
        outline(x0, z0, x1, z1, y, rim);
        return this;
    }

    /** Hollows out the inside of a box, leaving its shell. */
    public Blueprint hollow(int x0, int y0, int z0, int x1, int y1, int z1) {
        return fill(x0 + 1, y0, z0 + 1, x1 - 1, y1, z1 - 1, Mat.AIR);
    }

    // =====================================================================
    // Analysis
    // =====================================================================

    /** Total labour needed to raise this building. */
    public double totalEffort() {
        double sum = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    sum += cells[y][z][x].effort();
                }
            }
        }
        return sum;
    }

    /** Number of cells that actually place something. */
    public int solidCells() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    if (cells[y][z][x] != Mat.SKIP && cells[y][z][x] != Mat.AIR) count++;
                }
            }
        }
        return count;
    }

    /** How many beds this blueprint contains (drives real housing counts). */
    public int bedCount() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    if (cells[y][z][x] == Mat.BED) count++;
                }
            }
        }
        return count;
    }

    /** Every cell that is not SKIP, in build order (clear, structure, fixture). */
    public List<Cell> orderedCells() {
        List<Cell> out = new ArrayList<Cell>();
        for (Mat.Phase phase : Mat.Phase.values()) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    for (int x = 0; x < width; x++) {
                        Mat mat = cells[y][z][x];
                        if (mat == Mat.SKIP || mat.phase() != phase) continue;
                        out.add(new Cell(x, y, z, mat));
                    }
                }
            }
        }
        return out;
    }

    /** One placement instruction. */
    public static final class Cell {
        public final int x;
        public final int y;
        public final int z;
        public final Mat mat;

        Cell(int x, int y, int z, Mat mat) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.mat = mat;
        }
    }
}
