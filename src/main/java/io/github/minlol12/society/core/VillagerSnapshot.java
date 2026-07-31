package io.github.minlol12.society.core;

/**
 * A Minecraft-free description of a loaded villager entity on a given day,
 * produced by the adapter layer. The engine uses the <em>anchor</em>
 * (meeting point, then home, then body) to group villagers into
 * settlements.
 */
public final class VillagerSnapshot {

    public final String entityUuid;
    public final double x;
    public final double y;
    public final double z;
    public final boolean hasHome;
    public final double homeX;
    public final double homeY;
    public final double homeZ;
    public final boolean hasMeetingPoint;
    public final double meetX;
    public final double meetY;
    public final double meetZ;
    public final boolean hasJobSite;
    public final double jobX;
    public final double jobY;
    public final double jobZ;
    public final String professionId;
    public final boolean baby;

    public VillagerSnapshot(String entityUuid, double x, double y, double z,
                            boolean hasHome, double homeX, double homeY, double homeZ,
                            boolean hasMeetingPoint, double meetX, double meetY, double meetZ,
                            boolean hasJobSite, double jobX, double jobY, double jobZ,
                            String professionId, boolean baby) {
        this.entityUuid = entityUuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasHome = hasHome;
        this.homeX = homeX;
        this.homeY = homeY;
        this.homeZ = homeZ;
        this.hasMeetingPoint = hasMeetingPoint;
        this.meetX = meetX;
        this.meetY = meetY;
        this.meetZ = meetZ;
        this.hasJobSite = hasJobSite;
        this.jobX = jobX;
        this.jobY = jobY;
        this.jobZ = jobZ;
        this.professionId = professionId;
        this.baby = baby;
    }

    public boolean hasAnchor() {
        return hasMeetingPoint || hasHome;
    }

    public double anchorX() {
        return hasMeetingPoint ? meetX : hasHome ? homeX : x;
    }

    public double anchorY() {
        return hasMeetingPoint ? meetY : hasHome ? homeY : y;
    }

    public double anchorZ() {
        return hasMeetingPoint ? meetZ : hasHome ? homeZ : z;
    }
}
