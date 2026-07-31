package io.github.minlol12.society.core.types;

/** Growth stages of a settlement, from campfire circle to walled city. */
public enum SettlementTier {

    CAMP("Camp", 2, 2),
    HAMLET("Hamlet", 4, 4),
    VILLAGE("Village", 8, 8),
    TOWN("Town", 16, 14),
    CITY("City", 32, 22);

    private final String display;
    private final int minPopulation;
    private final int housingBase;

    SettlementTier(String display, int minPopulation, int housingBase) {
        this.display = display;
        this.minPopulation = minPopulation;
        this.housingBase = housingBase;
    }

    public String display() { return display; }

    /** Minimum population required to hold this tier. */
    public int minPopulation() { return minPopulation; }

    /** How many beds exist before extra building work. */
    public int housingBase() { return housingBase; }

    /** Stock storage capacity per citizen at this tier. */
    public int storagePerCapita() {
        return 30 + 20 * ordinal();
    }

    public SettlementTier next() {
        int i = ordinal();
        return i + 1 < values().length ? values()[i + 1] : this;
    }

    public SettlementTier previous() {
        int i = ordinal();
        return i > 0 ? values()[i - 1] : this;
    }

    public static SettlementTier forPopulation(int population) {
        SettlementTier best = CAMP;
        for (SettlementTier t : values()) {
            if (population >= t.minPopulation) best = t;
        }
        return best;
    }
}
