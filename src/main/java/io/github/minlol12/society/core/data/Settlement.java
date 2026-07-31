package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.Good;
import io.github.minlol12.society.core.types.SettlementTier;
import io.github.minlol12.society.core.types.SimProfession;

/**
 * A community: its land, people, stock, treasury, knowledge, culture and
 * government, plus the rolling needs profile that the economy recomputes
 * daily (and which drives profession evolution).
 */
public final class Settlement {

    private final String id;
    private String name;
    private int foundedDay;
    private String founderId = "";
    private int centerX;
    private int centerY = 64;
    private int centerZ;
    private Culture culture;
    private Government government;
    private SettlementTier tier = SettlementTier.CAMP;
    private final EnumMap<Good, Double> stockpile = new EnumMap<Good, Double>(Good.class);
    private double treasury;
    private final TechState tech = new TechState();
    private double housingBuilt;
    private final List<String> citizenIds = new ArrayList<String>();
    private final List<ChronicleEntry> chronicle = new ArrayList<ChronicleEntry>();
    private int lastFestivalSeasonIndex = -1;
    private int famineDays;
    private double morale = 60.0;
    private double threatLevel;
    private boolean destroyed;

    // --- Rolling daily cache, recomputed by the economy each morning. ---
    private int cachedPopulation;
    private double cachedFoodBalance;
    private double cachedSecurity;
    private final EnumMap<SimProfession, Integer> professionCounts = new EnumMap<SimProfession, Integer>(SimProfession.class);
    private final EnumMap<SimProfession, Double> professionDemand = new EnumMap<SimProfession, Double>(SimProfession.class);

    public Settlement(String id, String name, int foundedDay, Culture culture, int x, int y, int z) {
        this.id = id;
        this.name = name;
        this.foundedDay = foundedDay;
        this.culture = culture;
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
    }

    public String id() { return id; }

    public String name() { return name; }

    public void setName(String name) { this.name = name; }

    public int foundedDay() { return foundedDay; }

    public String founderId() { return founderId; }

    public void setFounderId(String id) { this.founderId = id == null ? "" : id; }

    public int centerX() { return centerX; }

    public int centerY() { return centerY; }

    public int centerZ() { return centerZ; }

    public void setCenter(int x, int y, int z) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
    }

    public double distanceTo(Settlement other) {
        double dx = centerX - other.centerX;
        double dz = centerZ - other.centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double distanceTo(int x, int z) {
        double dx = centerX - x;
        double dz = centerZ - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Culture culture() { return culture; }

    public void setCulture(Culture culture) { this.culture = culture; }

    public Government government() { return government; }

    public void setGovernment(Government government) { this.government = government; }

    public SettlementTier tier() { return tier; }

    public void setTier(SettlementTier tier) { this.tier = tier; }

    public double stock(Good good) {
        Double d = stockpile.get(good);
        return d == null ? 0.0 : d.doubleValue();
    }

    public void addStock(Good good, double delta) {
        stockpile.put(good, Double.valueOf(Math.max(0.0, stock(good) + delta)));
    }

    public void setStock(Good good, double value) {
        stockpile.put(good, Double.valueOf(Math.max(0.0, value)));
    }

    public double treasury() { return treasury; }

    public void addTreasury(double delta) { this.treasury = Math.max(0.0, treasury + delta); }

    public TechState tech() { return tech; }

    /** Extra housing raised by builders, on top of the tier baseline. */
    public double housingBuilt() { return housingBuilt; }

    public void addHousing(double delta) { this.housingBuilt = Math.max(0.0, housingBuilt + delta); }

    public int housingCapacity() {
        return tier.housingBase() + (int) housingBuilt;
    }

    public List<String> citizenIds() { return citizenIds; }

    public List<ChronicleEntry> chronicle() { return chronicle; }

    public void addChronicle(ChronicleEntry entry) {
        chronicle.add(entry);
        while (chronicle.size() > 300) {
            chronicle.remove(0);
        }
    }

    public int lastFestivalSeasonIndex() { return lastFestivalSeasonIndex; }

    public void setLastFestivalSeasonIndex(int index) { this.lastFestivalSeasonIndex = index; }

    public int famineDays() { return famineDays; }

    public void setFamineDays(int days) { this.famineDays = days; }

    public double morale() { return morale; }

    public void addMorale(double delta) {
        morale = Math.max(5.0, Math.min(100.0, morale + delta));
    }

    public void setMorale(double value) {
        morale = Math.max(5.0, Math.min(100.0, value));
    }

    public double threatLevel() { return threatLevel; }

    public void addThreat(double delta) {
        threatLevel = Math.max(0.0, Math.min(10.0, threatLevel + delta));
    }

    public boolean isDestroyed() { return destroyed; }

    public void setDestroyed(boolean destroyed) { this.destroyed = destroyed; }

    // --- Daily cache ---

    public int cachedPopulation() { return cachedPopulation; }

    public void setCachedPopulation(int population) { this.cachedPopulation = population; }

    public double cachedFoodBalance() { return cachedFoodBalance; }

    public void setCachedFoodBalance(double balance) { this.cachedFoodBalance = balance; }

    public double cachedSecurity() { return cachedSecurity; }

    public void setCachedSecurity(double security) { this.cachedSecurity = security; }

    public EnumMap<SimProfession, Integer> professionCountsMap() { return professionCounts; }

    public int professionCount(SimProfession p) {
        Integer v = professionCounts.get(p);
        return v == null ? 0 : v.intValue();
    }

    public void setProfessionCount(SimProfession p, int count) {
        professionCounts.put(p, Integer.valueOf(count));
    }

    public EnumMap<SimProfession, Double> professionDemandMap() { return professionDemand; }

    public double professionDemand(SimProfession p) {
        Double v = professionDemand.get(p);
        return v == null ? 0.0 : v.doubleValue();
    }

    public void setProfessionDemand(SimProfession p, double demand) {
        professionDemand.put(p, Double.valueOf(demand));
    }

    /** How much of a good the settlement tries to keep on hand. */
    public double desiredStock(Good good) {
        int pop = Math.max(1, cachedPopulation);
        switch (good) {
            case FOOD: return pop * 5.0;
            case WOOD:
            case STONE: return pop * 1.6;
            case IRON: return pop * 0.6;
            case GEMS: return pop * 0.15;
            case TOOLS: return pop * 0.8;
            case CLOTH: return pop * 0.7;
            case MEDICINE: return pop * 0.5;
            case LUXURY: return pop * 0.25;
            default: return pop;
        }
    }

    /** Hard capacity for one kind of good. */
    public double storageCap(Good good) {
        return Math.max(1, cachedPopulation) * tier().storagePerCapita();
    }

    /**
     * Local price of a good in emeralds, rising with scarcity. This is what
     * caravans use and what the economy report shows.
     */
    public double priceOf(Good good) {
        double stock = stock(good);
        double desired = desiredStock(good);
        double ratio = desired <= 0.0 ? 1.0 : (desired - stock) / desired;
        double factor = 1.0 + ratio * 0.9;
        factor = Math.max(0.35, Math.min(3.0, factor));
        return good.baseValue() * factor;
    }

    public String stockSummary() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Good g : Good.values()) {
            double stock = stock(g);
            if (stock < 0.05) continue;
            if (!first) sb.append(", ");
            sb.append(g.display()).append(' ').append((int) stock);
            first = false;
        }
        return first ? "nothing but dust" : sb.toString();
    }

    public Compound save() {
        Compound c = new Compound()
                .put("id", id)
                .put("name", name)
                .put("founded", foundedDay)
                .put("founder", founderId)
                .put("x", centerX)
                .put("y", centerY)
                .put("z", centerZ)
                .put("culture", culture.save())
                .put("tier", tier.name())
                .put("treasury", treasury)
                .put("tech", tech.save())
                .put("housing", housingBuilt)
                .put("lastFestival", lastFestivalSeasonIndex)
                .put("famineDays", famineDays)
                .put("morale", morale)
                .put("threat", threatLevel)
                .put("destroyed", destroyed)
                .putStringList("citizens", citizenIds);
        if (government != null) {
            c.put("government", government.save());
        }
        Compound stock = new Compound();
        for (Good g : Good.values()) {
            double v = stock(g);
            if (v > 0.0) stock.put(g.name(), v);
        }
        c.put("stock", stock);
        List<Compound> entries = new ArrayList<Compound>();
        for (ChronicleEntry e : chronicle) entries.add(e.save());
        c.putCompoundList("chronicle", entries);
        return c;
    }

    public static Settlement load(Compound c) {
        Culture culture = Culture.load(c.getCompound("culture"));
        Settlement s = new Settlement(
                c.getString("id", ""),
                c.getString("name", "Unnamed"),
                c.getInt("founded", 0),
                culture,
                c.getInt("x", 0),
                c.getInt("y", 64),
                c.getInt("z", 0));
        s.founderId = c.getString("founder", "");
        try {
            s.tier = SettlementTier.valueOf(c.getString("tier", "CAMP"));
        } catch (IllegalArgumentException ignored) { }
        s.treasury = c.getDouble("treasury", 0.0);
        s.tech.copyFrom(TechState.load(c.getCompound("tech")));
        s.housingBuilt = c.getDouble("housing", 0.0);
        s.lastFestivalSeasonIndex = c.getInt("lastFestival", -1);
        s.famineDays = c.getInt("famineDays", 0);
        s.morale = c.getDouble("morale", 60.0);
        s.threatLevel = c.getDouble("threat", 0.0);
        s.destroyed = c.getBool("destroyed", false);
        Government government = Government.load(c.getCompound("government"));
        s.government = government;
        for (String id : c.getStringList("citizens")) s.citizenIds.add(id);
        Compound stock = c.getCompound("stock");
        for (Good g : Good.values()) {
            double v = stock.getDouble(g.name(), 0.0);
            if (v > 0.0) s.stockpile.put(g, Double.valueOf(v));
        }
        for (Compound entry : c.getCompoundList("chronicle")) {
            s.chronicle.add(ChronicleEntry.load(entry));
        }
        return s;
    }
}
