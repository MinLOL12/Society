package io.github.minlol12.society.core.types;

/** Political organisation. Form follows population tier, culture and history. */
public enum GovernmentType {

    ELDER_COUNCIL("Council of Elders", "Elder"),
    CHIEFDOM("Chiefdom", "Chief"),
    MAYORALTY("Mayoralty", "Mayor"),
    FREE_COUNCIL("Free Council", "First Speaker"),
    MERCHANT_LEAGUE("Merchant League", "Guildmaster");

    private final String display;
    private final String leaderTitle;

    GovernmentType(String display, String leaderTitle) {
        this.display = display;
        this.leaderTitle = leaderTitle;
    }

    public String display() { return display; }

    /** Title given to the head of this kind of government. */
    public String leaderTitle() { return leaderTitle; }
}
