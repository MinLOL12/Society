package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.Announcement;
import io.github.minlol12.society.core.SocietyEngine;
import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.Good;

/**
 * The world's player-managed currency. Anyone can print new notes (or burn
 * old ones), so the money supply is whatever the players make of it - but
 * the <em>value</em> of a note is not. It is anchored to the real wealth of
 * every settlement on the map:
 *
 * <pre>
 *   value per note = backing / supply * confidence
 * </pre>
 *
 * where {@code confidence} collapses when the supply balloons far past what
 * the world can back (hyperinflation - print too much and the currency
 * becomes worthless) and strengthens when notes grow scarce (deflation -
 * print too little and a single note buys the town). The ledger's treasuries
 * are denominated in this currency, so inflation quietly erodes what every
 * settlement has saved.
 */
public final class Currency {

    /** Name of the currency, renameable by players. */
    private String name = "Coins";
    /** Total notes in circulation. A fresh world starts modest; players grow it. */
    private double supply = 200.0;
    /** Lifetime total minted (for the report). */
    private double mintedTotal;
    /** Lifetime total burned (for the report). */
    private double burnedTotal;

    private double backing;
    private double value = 1.0;
    private double previousValue = 1.0;
    /** -1 deflating, 0 stable, +1 inflating. */
    private int trend;
    private int lastWarningDay = -1000;

    public String name() { return name; }

    public void setName(String name) {
        this.name = name == null || name.isEmpty() ? "Coins" : name;
    }

    public double supply() { return supply; }

    public double mintedTotal() { return mintedTotal; }

    public double burnedTotal() { return burnedTotal; }

    public double backing() { return backing; }

    public double value() { return value; }

    public double previousValue() { return previousValue; }

    public int trend() { return trend; }

    public String trendWord() {
        return trend > 0 ? "inflating" : trend < 0 ? "deflating" : "stable";
    }

    /**
     * The real wealth backing the notes: everything the settlements hold,
     * plus every recorded soul's modest worth.
     */
    public double computeBacking(SocietyEngine engine) {
        double total = 0.0;
        for (Settlement s : engine.settlements().values()) {
            if (s.isDestroyed()) continue;
            total += s.treasury() * 0.5;
            total += s.cachedPopulation() * 8.0;
            for (Good good : Good.values()) {
                total += s.stock(good) * good.baseValue() * 0.4;
            }
        }
        return Math.max(1.0, total);
    }

    /**
     * How much real wealth one note buys today. Backing is split evenly
     * over the notes in circulation, then adjusted for trust: a bloated
     * supply is distrusted (worthless money), a starved supply is prized
     * (extremely valuable money).
     */
    public double valuePerUnit(SocietyEngine engine) {
        double backingNow = computeBacking(engine);
        double par = Math.max(1.0, backingNow * 0.5);
        double deviation = (supply - par) / par;
        double confidence = Math.exp(-2.2 * Math.max(0.0, deviation))
                * Math.exp(0.9 * Math.max(0.0, -deviation));
        return Math.max(0.0001, (backingNow / Math.max(0.0001, supply)) * confidence);
    }

    /** The same good, priced in notes of this currency rather than emeralds. */
    public double priceInCurrency(Good good, double emeraldPrice, SocietyEngine engine) {
        return emeraldPrice / Math.max(0.0001, valuePerUnit(engine));
    }

    /** Called once per in-game day, from the engine's daily tick. */
    public void dailyTick(SocietyEngine engine) {
        previousValue = value;
        backing = computeBacking(engine);
        value = valuePerUnit(engine);
        double drift = (value - previousValue) / Math.max(0.0001, previousValue);
        // A falling value is inflation (each note buys less); a rising value
        // is deflation (notes grow more precious).
        trend = drift < -0.015 ? 1 : drift > 0.015 ? -1 : 0;

        double par = Math.max(1.0, backing * 0.5);
        double ratio = supply / par;
        if (engine.day() - lastWarningDay >= 6) {
            if (ratio >= 3.0) {
                lastWarningDay = engine.day();
                engine.announce(Announcement.Severity.GLOBAL, null,
                        "The " + name.toLowerCase() + " are becoming worthless - "
                                + "too much money has been printed for the world to back.");
            } else if (ratio <= 0.25) {
                lastWarningDay = engine.day();
                engine.announce(Announcement.Severity.GLOBAL, null,
                        "The " + name.toLowerCase() + " grow scarcer than the wealth behind them - "
                                + "each note is worth a small fortune.");
            }
        }
    }

    /** Prints new notes into circulation (inflation). */
    public double print(double amount) {
        double safe = Math.max(0.0, amount);
        supply += safe;
        mintedTotal += safe;
        return safe;
    }

    /** Destroys notes from circulation (deflation). */
    public double burn(double amount) {
        double safe = Math.min(Math.max(0.0, amount), supply);
        supply -= safe;
        burnedTotal += safe;
        return safe;
    }

    public Compound save() {
        return new Compound()
                .put("name", name)
                .put("supply", supply)
                .put("minted", mintedTotal)
                .put("burned", burnedTotal)
                .put("backing", backing)
                .put("value", value)
                .put("prevValue", previousValue)
                .put("trend", trend);
    }

    public void load(Compound c) {
        name = c.getString("name", "Coins");
        supply = c.getDouble("supply", 200.0);
        mintedTotal = c.getDouble("minted", 0.0);
        burnedTotal = c.getDouble("burned", 0.0);
        backing = c.getDouble("backing", 0.0);
        value = c.getDouble("value", 1.0);
        previousValue = c.getDouble("prevValue", value);
        trend = c.getInt("trend", 0);
    }
}
