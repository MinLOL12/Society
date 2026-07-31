package io.github.minlol12.society.core.types;

/** State of relations between two settlements. */
public enum Treaty {

    NONE("watchful neutrality"),
    TRUCE("an uneasy truce"),
    TRADE_PACT("a trade pact"),
    ALLIANCE("a sworn alliance"),
    WAR("a state of war");

    private final String description;

    Treaty(String description) {
        this.description = description;
    }

    public String description() { return description; }

    public boolean atWar() {
        return this == WAR;
    }
}
