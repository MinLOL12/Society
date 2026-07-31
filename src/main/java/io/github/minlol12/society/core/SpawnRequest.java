package io.github.minlol12.society.core;

/**
 * The engine asking the world to make a citizen flesh: when a player is
 * near, the Minecraft side answers this by spawning a real villager bound
 * to {@link #citizenId}.
 */
public final class SpawnRequest {

    public final String settlementId;
    public final String citizenId;
    public final double x;
    public final double y;
    public final double z;
    public final boolean baby;

    public SpawnRequest(String settlementId, String citizenId, double x, double y, double z, boolean baby) {
        this.settlementId = settlementId;
        this.citizenId = citizenId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.baby = baby;
    }
}
