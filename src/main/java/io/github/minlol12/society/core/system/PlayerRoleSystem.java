package io.github.minlol12.society.core.system;

import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.data.PlayerData;
import io.github.minlol12.society.core.data.Settlement;
import io.github.minlol12.society.core.types.EventType;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.PlayerRole;

/**
 * What the players' chosen roles do to the simulation. Every day each
 * settlement adds the crafts of its online, home-assigned players - a
 * blacksmith's tools, a farmer's food, a scholar's research - and a
 * sovereign's presence steadies the whole town.
 *
 * <p>This is deliberately a light touch: the villager economy stands on
 * its own, and a handful of playing friends makes a town a little more
 * alive, never a different game.</p>
 */
public final class PlayerRoleSystem {

    private PlayerRoleSystem() { }

    /** Called by the economy for each settlement once a day. */
    public static void applySettlementBonuses(SocietyEngine engine, Settlement s) {
        if (engine.playerData().isEmpty()) return;

        for (PlayerData data : engine.playerData().values()) {
            if (!s.id().equals(data.homeSettlementId())) continue;
            PlayerRole role = data.role();
            if (role == null || role == PlayerRole.NONE) continue;
            applyRole(engine, s, role);
        }

        // A crowned sovereign steadies the town they rule.
        String rulerUuid = engine.rulerPlayers().get(s.id());
        if (rulerUuid != null) {
            PlayerData ruler = engine.playerData().get(rulerUuid);
            if (ruler != null && ruler.role() != null && ruler.role().isSovereign()) {
                s.addMorale(1.5);
                s.addTreasury(0.4); // the crown's cut of the day's work
                if (engine.random().nextDouble() < 0.02) {
                    s.culture().addFact("is ruled by a player sovereign, "
                            + ruler.playerName());
                }
            }
        }
    }

    private static void applyRole(SocietyEngine engine, Settlement s, PlayerRole role) {
        switch (role) {
            case WORKER:
                s.addStock(Good.FOOD, 0.15);
                s.addStock(Good.WOOD, 0.15);
                break;
            case FARMER:
                s.addStock(Good.FOOD, 0.45);
                break;
            case BLACKSMITH:
                s.addStock(Good.TOOLS, 0.06);
                s.addStock(Good.WEAPONS, 0.02);
                break;
            case MINER:
                s.addStock(Good.STONE, 0.2);
                s.addStock(Good.IRON, 0.06);
                break;
            case BUILDER:
                s.addStock(Good.WOOD, 0.3);
                s.addMorale(0.1);
                break;
            case CRAFTER:
                s.addStock(Good.CLOTH, 0.1);
                s.addStock(Good.TOOLS, 0.03);
                break;
            case TRADER:
                s.addTreasury(0.5);
                break;
            case SCHOLAR: {
                io.github.minlol12.society.core.types.TechNode focus =
                        chooseResearch(engine, s);
                if (focus != null) {
                    s.tech().addResearch(focus, 0.18);
                }
                break;
            }
            case HEALER:
                s.addStock(Good.MEDICINE, 0.05);
                s.addStock(Good.POTIONS, 0.02);
                break;
            case GUARD:
                s.addThreat(-0.05);
                s.addStock(Good.BOWS, 0.02);
                break;
            case STEWARD:
                s.addTreasury(0.35);
                break;
            default:
                break;
        }
    }

    private static io.github.minlol12.society.core.types.TechNode chooseResearch(
            SocietyEngine engine, Settlement s) {
        java.util.List<io.github.minlol12.society.core.types.TechNode> available =
                s.tech().available();
        if (available.isEmpty()) return null;
        return available.get(engine.random().nextInt(available.size()));
    }

    /**
     * Crowns a player as sovereign of a settlement (KING or QUEEN). Only one
     * player may hold a settlement's throne at a time. Returns false when the
     * throne is already taken.
     */
    public static boolean crown(SocietyEngine engine, String settlementId,
                                String playerUuid, PlayerRole role, int day) {
        if (settlementId == null || playerUuid == null) return false;
        String current = engine.rulerPlayers().get(settlementId);
        if (current != null && !current.equals(playerUuid)) return false;

        Settlement s = engine.settlements().get(settlementId);
        if (s == null || s.isDestroyed()) return false;

        PlayerData data = engine.playerData().get(playerUuid);
        if (data == null) return false;

        engine.rulerPlayers().put(settlementId, playerUuid);
        data.setRole(role, day);
        data.setHomeSettlementId(settlementId);
        s.culture().addFact("was crowned under the player sovereign "
                + data.playerName());
        engine.record(EventType.GOVERNMENT_CHANGE, s,
                data.playerName() + " is crowned " + role.display() + " of "
                        + s.name() + " at its government building.");
        engine.markDirty();
        return true;
    }

    /** Steps down from a settlement's throne, if the player held it. */
    public static boolean abdicate(SocietyEngine engine, String settlementId,
                                   String playerUuid, int day) {
        String current = engine.rulerPlayers().get(settlementId);
        if (current == null || !current.equals(playerUuid)) return false;

        PlayerData data = engine.playerData().get(playerUuid);
        engine.rulerPlayers().remove(settlementId);
        if (data != null && data.role() != null && data.role().isSovereign()) {
            data.setRole(PlayerRole.NONE, day);
        }
        Settlement s = engine.settlements().get(settlementId);
        if (s != null) {
            s.culture().addFact("its player sovereign stepped down");
            engine.record(EventType.GOVERNMENT_CHANGE, s,
                    (data == null ? "A ruler" : data.playerName())
                            + " has abdicated the throne of " + s.name() + ".");
        }
        engine.markDirty();
        return true;
    }
}
