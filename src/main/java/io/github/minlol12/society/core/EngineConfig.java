package io.github.minlol12.society.core;

/** Tunables for the simulation, populated from the mod's JSON config. */
public final class EngineConfig {

    /** Hard caps so worlds never melt down. */
    public int maxSettlements = 24;
    public int maxCitizensPerSettlement = 160;
    public int seasonLengthDays = 30;

    /** Citizens may emigrate between settlements when unhappy. */
    public boolean enableMigration = true;

    /** Unmanifested citizens become real villagers when a player is near. */
    public boolean enableManifestSpawns = true;

    /** Wars may kill unmanifested citizens (manifested villagers are never harmed by the ledger). */
    public boolean warCasualties = false;

    /** Plagues may kill unmanifested citizens. */
    public boolean plagueCasualties = true;

    /** Famines may kill unmanifested citizens. */
    public boolean famineCasualties = true;

    /** Society sends chat announcements. */
    public boolean announcements = true;

    /** Radius around a settlement in which local announcements are heard. */
    public int announcementRadius = 160;

    /** Maximum non-major announcements from one settlement per day. */
    public int dailyAnnouncementBudget = 4;
}
