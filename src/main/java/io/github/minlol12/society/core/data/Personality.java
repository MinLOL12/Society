package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.Archetype;
import io.github.minlol12.society.core.types.Trait;

/**
 * The whole inner life of a citizen: eight trait values between 0 and 100
 * that together determine an {@link Archetype}, colour every social
 * decision, and drift slightly toward their culture's admired virtue when a
 * child is raised inside a settlement.
 */
public final class Personality {

    private final EnumMap<Trait, Integer> values = new EnumMap<Trait, Integer>(Trait.class);

    public Personality() {
        for (Trait t : Trait.values()) {
            values.put(t, Integer.valueOf(50));
        }
    }

    public static Personality random(Random random, Trait culturalVirtue) {
        Personality p = new Personality();
        for (Trait t : Trait.values()) {
            int v = 15 + random.nextInt(71); // 15..85
            if (t == culturalVirtue) {
                v = Math.min(100, v + 14);
            }
            p.values.put(t, Integer.valueOf(v));
        }
        return p;
    }

    /** Child personality: blend of both parents plus fresh noise and a cultural pull. */
    public static Personality inherit(Personality mother, Personality father, Random random, Trait culturalVirtue) {
        Personality p = new Personality();
        for (Trait t : Trait.values()) {
            double base = (mother.get(t) + father.get(t)) / 2.0;
            int v = (int) Math.round(base + (random.nextGaussian() * 14.0));
            if (t == culturalVirtue) {
                v += 8;
            }
            p.values.put(t, Integer.valueOf(Math.max(0, Math.min(100, v))));
        }
        return p;
    }

    public int get(Trait trait) {
        Integer v = values.get(trait);
        return v == null ? 50 : v.intValue();
    }

    /** Names of the {@code n} traits with the highest values, strongest first. */
    public List<Trait> topTraits(int n) {
        List<Trait> sorted = new ArrayList<Trait>();
        for (Trait t : Trait.values()) sorted.add(t);
        sorted.sort((a, b) -> Integer.compare(get(b), get(a)));
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    public Trait dominantTrait() {
        return topTraits(1).get(0);
    }

    /** Weighted archetype fit; the highest-scoring archetype wins. */
    public Archetype archetype() {
        int industry = get(Trait.INDUSTRY);
        int sociability = get(Trait.SOCIABILITY);
        int ambition = get(Trait.AMBITION);
        int curiosity = get(Trait.CURIOSITY);
        int aggression = get(Trait.AGGRESSION);
        int caution = get(Trait.CAUTION);
        int generosity = get(Trait.GENEROSITY);
        int wisdom = get(Trait.WISDOM);

        EnumMap<Archetype, Integer> score = new EnumMap<Archetype, Integer>(Archetype.class);
        score.put(Archetype.WORKER, industry);
        score.put(Archetype.ARTISAN, (int) (industry * 0.6 + curiosity * 0.5));
        score.put(Archetype.MERCHANT, (int) (ambition * 0.75 + sociability * 0.55));
        score.put(Archetype.EXPLORER, (int) (curiosity * 0.9 + (100 - caution) * 0.5));
        score.put(Archetype.WARRIOR, (int) (aggression * 0.95 + caution * 0.25));
        score.put(Archetype.SAGE, (int) (wisdom * 0.8 + curiosity * 0.6));
        score.put(Archetype.LEADER, (int) (sociability * 0.55 + ambition * 0.5 + wisdom * 0.5));
        score.put(Archetype.CARETAKER, (int) (generosity * 0.85 + wisdom * 0.35));

        Archetype best = Archetype.WORKER;
        int bestScore = -1;
        for (Map.Entry<Archetype, Integer> e : score.entrySet()) {
            if (e.getValue().intValue() > bestScore) {
                bestScore = e.getValue().intValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** Similarity 0..100 used for courtship and diplomacy flavour. */
    public static double compatibility(Personality a, Personality b) {
        double diff = 0;
        for (Trait t : Trait.values()) {
            diff += Math.abs(a.get(t) - b.get(t));
        }
        double similarity = 100.0 - diff / Trait.values().length;
        // Complementary ambition/industry pairings are famously productive.
        similarity += Math.min(a.get(Trait.INDUSTRY), b.get(Trait.AMBITION)) * 0.05;
        similarity += Math.min(a.get(Trait.GENEROSITY), b.get(Trait.SOCIABILITY)) * 0.05;
        return Math.max(0.0, Math.min(100.0, similarity));
    }

    public Compound save() {
        Compound c = new Compound();
        for (Trait t : Trait.values()) {
            c.put(t.name(), get(t));
        }
        return c;
    }

    public static Personality load(Compound c) {
        Personality p = new Personality();
        for (Trait t : Trait.values()) {
            p.values.put(t, Integer.valueOf(c.getInt(t.name(), 50)));
        }
        return p;
    }
}
