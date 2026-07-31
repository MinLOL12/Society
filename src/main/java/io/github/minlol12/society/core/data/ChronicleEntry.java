package io.github.minlol12.society.core.data;

import io.github.minlol12.society.core.io.Compound;
import io.github.minlol12.society.core.types.EventType;

/** One line of recorded history. Text is written once and never changed. */
public final class ChronicleEntry {

    private final int day;
    private final EventType type;
    /** Owning settlement id, or empty string for world-level events. */
    private final String settlementId;
    private final String text;

    public ChronicleEntry(int day, EventType type, String settlementId, String text) {
        this.day = day;
        this.type = type;
        this.settlementId = settlementId == null ? "" : settlementId;
        this.text = text;
    }

    public int day() { return day; }

    public EventType type() { return type; }

    public String settlementId() { return settlementId; }

    public String text() { return text; }

    public Compound save() {
        return new Compound()
                .put("day", day)
                .put("type", type.name())
                .put("settlement", settlementId)
                .put("text", text);
    }

    public static ChronicleEntry load(Compound c) {
        EventType type;
        try {
            type = EventType.valueOf(c.getString("type", "CULTURE"));
        } catch (IllegalArgumentException e) {
            type = EventType.CULTURE;
        }
        return new ChronicleEntry(c.getInt("day", 0), type, c.getString("settlement", ""), c.getString("text", ""));
    }
}
