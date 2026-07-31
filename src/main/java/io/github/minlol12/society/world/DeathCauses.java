package io.github.minlol12.society.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.text.Text;

/** Turns a server-side damage source into an obituary line. */
public final class DeathCauses {

    private DeathCauses() { }

    /** Obits read better with the killer's name when there is one. */
    public static String describe(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker != null) {
            Text name = attacker.getDisplayName();
            return "was slain by " + (name == null ? attacker.getType().toString() : name.getString());
        }
        String id = source.getType().msgId();
        switch (id) {
            case "inFire":
            case "onFire": return "burned to death";
            case "lava": return "burned in lava";
            case "inWall": return "suffocated in a wall";
            case "drown": return "drowned";
            case "starve": return "starved";
            case "cactus": return "was pricked to death";
            case "fall": return "fell to their death";
            case "flyIntoWall": return "flew into a wall";
            case "outOfWorld": return "fell out of the world";
            case "magic": return "died by magic";
            case "wither": return "withered away";
            case "anvil":
            case "fallingBlock": return "was crushed";
            case "lightningBolt": return "was struck by lightning";
            case "freeze": return "froze to death";
            case "hotFloor": return "burned on hot ground";
            case "explosion":
            case "player_explosion": return "was blown up";
            case "genericKill": return "was killed";
            case "generic": return "died";
            default: return "died (" + id + ")";
        }
    }

    /** Was this a violent death, for threat bookkeeping? */
    public static boolean isViolent(DamageSource source) {
        if (source.getAttacker() != null) return true;
        String id = source.getType().msgId();
        return id.equals("explosion") || id.equals("player_explosion")
                || id.equals("lava") || id.equals("lightningBolt");
    }
}
