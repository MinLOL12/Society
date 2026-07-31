package io.github.minlol12.society.core;

/**
 * A message the simulation wants to tell players. LOCAL messages go to
 * players near ({@link #x}, {@link #z}); GLOBAL ones to everyone; NONE
 * messages are only recorded.
 *
 * <p>An announcement may carry the name of the settlement it concerns, so
 * the world adapter can offer a one-click teleport to that place.</p>
 */
public final class Announcement {

    public enum Severity { NONE, LOCAL, GLOBAL }

    public final Severity severity;
    public final double x;
    public final double z;
    public final String text;
    /** Settlement this announcement is about, or empty when it has none. */
    public final String settlementName;

    public Announcement(Severity severity, double x, double z, String text) {
        this(severity, x, z, text, "");
    }

    public Announcement(Severity severity, double x, double z, String text, String settlementName) {
        this.severity = severity;
        this.x = x;
        this.z = z;
        this.text = text;
        this.settlementName = settlementName == null ? "" : settlementName;
    }

    public static Announcement none(String text) {
        return new Announcement(Severity.NONE, 0, 0, text);
    }
}
