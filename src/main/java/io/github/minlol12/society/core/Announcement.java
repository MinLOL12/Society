package io.github.minlol12.society.core;

/**
 * A message the simulation wants to tell players. LOCAL messages go to
 * players near ({@link #x}, {@link #z}); GLOBAL ones to everyone; NONE
 * messages are only recorded.
 */
public final class Announcement {

    public enum Severity { NONE, LOCAL, GLOBAL }

    public final Severity severity;
    public final double x;
    public final double z;
    public final String text;

    public Announcement(Severity severity, double x, double z, String text) {
        this.severity = severity;
        this.x = x;
        this.z = z;
        this.text = text;
    }

    public static Announcement none(String text) {
        return new Announcement(Severity.NONE, 0, 0, text);
    }
}
