package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.PlayerRole;

/**
 * What the ledger knows about a <em>player</em>: the role they have taken,
 * the settlement they belong to, and their purse in the world's player
 * currency. Purely optional spice on top of the villager simulation - a
 * world with no players simply has no entries here.
 */
public final class PlayerData {

    private final String uuid;
    private String playerName;
    private PlayerRole role = PlayerRole.NONE;
    private String homeSettlementId = "";
    private double currencyBalance;
    private int roleSinceDay = -1;
    private int lastSeenDay = -1;

    public PlayerData(String uuid) {
        this.uuid = uuid == null ? "" : uuid;
    }

    public String uuid() { return uuid; }

    public String playerName() { return playerName == null ? "" : playerName; }

    public void setPlayerName(String name) { this.playerName = name == null ? "" : name; }

    public PlayerRole role() { return role; }

    public void setRole(PlayerRole role, int day) {
        this.role = role == null ? PlayerRole.NONE : role;
        this.roleSinceDay = this.role == PlayerRole.NONE ? -1 : day;
    }

    public int roleSinceDay() { return roleSinceDay; }

    public String homeSettlementId() { return homeSettlementId; }

    public void setHomeSettlementId(String id) { this.homeSettlementId = id == null ? "" : id; }

    public double currencyBalance() { return currencyBalance; }

    public void addCurrency(double delta) {
        currencyBalance = Math.max(0.0, currencyBalance + delta);
    }

    public void setCurrencyBalance(double balance) {
        currencyBalance = Math.max(0.0, balance);
    }

    public int lastSeenDay() { return lastSeenDay; }

    public void noteSeen(int day) { this.lastSeenDay = day; }

    public Compound save() {
        return new Compound()
                .put("uuid", uuid)
                .put("name", playerName == null ? "" : playerName)
                .put("role", role.name())
                .put("home", homeSettlementId)
                .put("balance", currencyBalance)
                .put("roleSince", roleSinceDay)
                .put("lastSeen", lastSeenDay);
    }

    public static PlayerData load(Compound c) {
        PlayerData d = new PlayerData(c.getString("uuid", ""));
        d.playerName = c.getString("name", "");
        try {
            d.role = PlayerRole.valueOf(c.getString("role", "NONE"));
        } catch (IllegalArgumentException ignored) {
            d.role = PlayerRole.NONE;
        }
        d.homeSettlementId = c.getString("home", "");
        d.currencyBalance = c.getDouble("balance", 0.0);
        d.roleSinceDay = c.getInt("roleSince", -1);
        d.lastSeenDay = c.getInt("lastSeen", -1);
        return d;
    }
}
