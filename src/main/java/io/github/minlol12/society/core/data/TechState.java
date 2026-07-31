package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.TechNode;

/** A settlement's accumulated knowledge: progress bars plus unlocked nodes. */
public final class TechState {

    private final EnumMap<TechNode, Double> progress = new EnumMap<TechNode, Double>(TechNode.class);
    private final List<TechNode> unlocked = new ArrayList<TechNode>();

    /** Replaces this state with another's contents (used when loading). */
    public void copyFrom(TechState other) {
        progress.clear();
        progress.putAll(other.progress);
        unlocked.clear();
        unlocked.addAll(other.unlocked);
    }

    public boolean isUnlocked(TechNode node) {
        return unlocked.contains(node);
    }

    public List<TechNode> unlocked() {
        return unlocked;
    }

    public double progressOf(TechNode node) {
        Double p = progress.get(node);
        return p == null ? 0.0 : p.doubleValue();
    }

    /** Nodes whose parents are unlocked but which are not yet known. */
    public List<TechNode> available() {
        List<TechNode> out = new ArrayList<TechNode>();
        for (TechNode node : TechNode.values()) {
            if (unlocked.contains(node)) continue;
            TechNode parent = node.parent();
            if (parent == null || unlocked.contains(parent)) {
                out.add(node);
            }
        }
        return out;
    }

    /**
     * Pours research points into a node. Returns true if this call completed
     * the discovery.
     */
    public boolean addResearch(TechNode node, double points) {
        if (unlocked.contains(node)) return false;
        double value = progressOf(node) + points;
        if (value >= node.cost()) {
            progress.put(node, Double.valueOf(node.cost()));
            unlocked.add(node);
            return true;
        }
        progress.put(node, Double.valueOf(value));
        return false;
    }

    /** Combined production multiplier for a good from all unlocked tech. */
    public double goodModifier(Good good) {
        double mod = 1.0;
        for (TechNode n : unlocked) {
            mod *= n.goodModifier(good);
        }
        return mod;
    }

    public double researchModifier() {
        return unlocked.contains(TechNode.WRITING) ? 1.25 : 1.0;
    }

    public double deathModifier() {
        return unlocked.contains(TechNode.MEDICINE) ? 0.65 : 1.0;
    }

    public double buildModifier() {
        double m = unlocked.contains(TechNode.CARPENTRY) ? 1.3 : 1.0;
        if (unlocked.contains(TechNode.ARCHITECTURE)) m *= 1.3;
        return m;
    }

    public double tradeVolumeBonus() {
        return unlocked.contains(TechNode.NAVIGATION) ? 10.0 : 0.0;
    }

    public double coinageModifier() {
        return unlocked.contains(TechNode.COINAGE) ? 1.25 : 1.0;
    }

    public double guardModifier() {
        return unlocked.contains(TechNode.MILITARY_DRILL) ? 1.3 : 1.0;
    }

    public boolean cityAllowed() {
        return unlocked.contains(TechNode.ARCHITECTURE);
    }

    public Compound save() {
        Compound c = new Compound();
        for (TechNode n : TechNode.values()) {
            double p = progressOf(n);
            if (p > 0.0) c.put("p_" + n.name(), p);
        }
        List<String> names = new ArrayList<String>();
        for (TechNode n : unlocked) names.add(n.name());
        c.putStringList("unlocked", names);
        return c;
    }

    public static TechState load(Compound c) {
        TechState state = new TechState();
        for (TechNode n : TechNode.values()) {
            double p = c.getDouble("p_" + n.name(), 0.0);
            if (p > 0.0) state.progress.put(n, Double.valueOf(p));
        }
        for (String name : c.getStringList("unlocked")) {
            try {
                TechNode n = TechNode.valueOf(name);
                if (!state.unlocked.contains(n)) state.unlocked.add(n);
            } catch (IllegalArgumentException ignored) {
                // Unknown node from a newer/older version; skip.
            }
        }
        return state;
    }
}
