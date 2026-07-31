package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.build.StructureType;
import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.PlayerStructureKind;

/**
 * A structure claimed by a player, either stamped from the premade NBT
 * catalogue (CTOV templates - {@code nbtPath} or {@code structureType} set)
 * or carved out of the world by hand and labelled with the Setter Stick.
 *
 * <p>Structures labelled GOVERNMENT become government buildings: the
 * nearest settlement recognises them, sovereign players are crowned there,
 * and they show up on the settlement's pages.</p>
 */
public final class PlayerStructure {

    private final String id;
    private String ownerName;
    private String label;
    private PlayerStructureKind kind = PlayerStructureKind.CUSTOM;
    /** Premade catalogue type, when this was stamped from the list. */
    private StructureType structureType;
    /** NBT template path when stamped from a specific file (ctov/adorabuild). */
    private String nbtPath = "";
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int rotation;
    private int createdDay;
    private String settlementId = "";

    public PlayerStructure(String id) {
        this.id = id;
    }

    public String id() { return id; }

    public String ownerName() { return ownerName == null ? "" : ownerName; }

    public void setOwnerName(String ownerName) { this.ownerName = ownerName == null ? "" : ownerName; }

    public String label() { return label == null ? "" : label; }

    public void setLabel(String label) { this.label = label == null ? "" : label; }

    public PlayerStructureKind kind() { return kind; }

    public void setKind(PlayerStructureKind kind) { this.kind = kind == null ? PlayerStructureKind.CUSTOM : kind; }

    public StructureType structureType() { return structureType; }

    public void setStructureType(StructureType structureType) { this.structureType = structureType; }

    public String nbtPath() { return nbtPath; }

    public void setNbtPath(String nbtPath) { this.nbtPath = nbtPath == null ? "" : nbtPath; }

    public void setBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxY = Math.max(y1, y2);
        maxZ = Math.max(z1, z2);
    }

    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }

    public int centerX() { return (minX + maxX) / 2; }
    public int centerY() { return (minY + maxY) / 2; }
    public int centerZ() { return (minZ + maxZ) / 2; }

    public int sizeX() { return maxX - minX + 1; }
    public int sizeY() { return maxY - minY + 1; }
    public int sizeZ() { return maxZ - minZ + 1; }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public int rotation() { return rotation; }

    public void setRotation(int rotation) { this.rotation = ((rotation % 4) + 4) % 4; }

    public int createdDay() { return createdDay; }

    public void setCreatedDay(int day) { this.createdDay = day; }

    public String settlementId() { return settlementId; }

    public void setSettlementId(String id) { this.settlementId = id == null ? "" : id; }

    public boolean isGovernment() {
        return kind.isGovernment();
    }

    public Compound save() {
        return new Compound()
                .put("id", id)
                .put("owner", ownerName == null ? "" : ownerName)
                .put("label", label == null ? "" : label)
                .put("kind", kind.name())
                .put("type", structureType == null ? "" : structureType.name())
                .put("nbt", nbtPath)
                .put("minX", minX).put("minY", minY).put("minZ", minZ)
                .put("maxX", maxX).put("maxY", maxY).put("maxZ", maxZ)
                .put("rot", rotation)
                .put("created", createdDay)
                .put("settlement", settlementId);
    }

    public static PlayerStructure load(Compound c) {
        PlayerStructure p = new PlayerStructure(c.getString("id", ""));
        p.ownerName = c.getString("owner", "");
        p.label = c.getString("label", "");
        p.kind = PlayerStructureKind.byName(c.getString("kind", "CUSTOM"));
        String typeName = c.getString("type", "");
        if (!typeName.isEmpty()) {
            p.structureType = StructureType.byName(typeName);
        }
        p.nbtPath = c.getString("nbt", "");
        p.minX = c.getInt("minX", 0);
        p.minY = c.getInt("minY", 0);
        p.minZ = c.getInt("minZ", 0);
        p.maxX = c.getInt("maxX", 0);
        p.maxY = c.getInt("maxY", 0);
        p.maxZ = c.getInt("maxZ", 0);
        p.rotation = c.getInt("rot", 0);
        p.createdDay = c.getInt("created", 0);
        p.settlementId = c.getString("settlement", "");
        return p;
    }
}
