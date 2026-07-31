package io.github.minlol12.society.core.data;

import java.util.ArrayList;
import java.util.List;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.CultureOrigin;
import io.github.minlol12.society.core.types.Good;

/**
 * The identity of a settlement. Starts as a seed from the land itself
 * ({@link CultureOrigin}) and keeps absorbing history: every disaster
 * survived, war fought and wonder discovered is folded into what the
 * culture becomes.
 */
public final class Culture {

    private final CultureOrigin origin;
    private final List<String> facts = new ArrayList<String>();
    private int warsFought;
    private int disastersSurvived;
    private int faminesSurvived;
    private int discoveriesMade;

    public Culture(CultureOrigin origin) {
        this.origin = origin;
    }

    public CultureOrigin origin() { return origin; }

    public List<String> facts() { return facts; }

    public void addFact(String fact) {
        if (facts.size() >= 24) {
            facts.remove(0);
        }
        facts.add(fact);
    }

    public int warsFought() { return warsFought; }

    public void noteWar() { warsFought++; }

    public int disastersSurvived() { return disastersSurvived; }

    public void noteDisasterSurvived() { disastersSurvived++; }

    public int faminesSurvived() { return faminesSurvived; }

    public void noteFamineSurvived() { faminesSurvived++; }

    public int discoveriesMade() { return discoveriesMade; }

    public void noteDiscovery() { discoveriesMade++; }

    public double productionModifier(Good good) {
        return origin.productionModifier(good);
    }

    /** Short prose description used by the chronicle and commands. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("The ").append(origin.folkName()).append(" of ").append(origin.display().toLowerCase());
        sb.append(" stock build ").append(origin.buildingStyle());
        sb.append(", wear ").append(origin.dressStyle());
        sb.append(", and hold ").append(origin.festivalName()).append(" each new season.");
        if (faminesSurvived > 0) sb.append(" They have known hunger ").append(faminesSurvived).append(" times.");
        if (disastersSurvived > 0) sb.append(" They have rebuilt after ").append(disastersSurvived).append(" disasters.");
        if (warsFought > 0) sb.append(" They have marched to war ").append(warsFought).append(" times.");
        if (discoveriesMade > 0) sb.append(" Their scholars have made ").append(discoveriesMade).append(" discoveries.");
        return sb.toString();
    }

    public Compound save() {
        return new Compound()
                .put("origin", origin.name())
                .putStringList("facts", facts)
                .put("wars", warsFought)
                .put("disasters", disastersSurvived)
                .put("famines", faminesSurvived)
                .put("discoveries", discoveriesMade);
    }

    public static Culture load(Compound c) {
        CultureOrigin origin;
        try {
            origin = CultureOrigin.valueOf(c.getString("origin", "PLAINS"));
        } catch (IllegalArgumentException e) {
            origin = CultureOrigin.PLAINS;
        }
        Culture culture = new Culture(origin);
        for (String f : c.getStringList("facts")) {
            culture.addFact(f);
        }
        culture.warsFought = c.getInt("wars", 0);
        culture.disastersSurvived = c.getInt("disasters", 0);
        culture.faminesSurvived = c.getInt("famines", 0);
        culture.discoveriesMade = c.getInt("discoveries", 0);
        return culture;
    }
}
