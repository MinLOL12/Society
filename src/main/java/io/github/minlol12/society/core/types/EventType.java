package io.github.minlol12.society.core.types;

/**
 * Kinds of historical events. The {@link Level} decides where an event is
 * remembered: MEMORY entries live only in a citizen's private memories,
 * LOCAL entries enter their settlement's chronicle, GLOBAL entries enter
 * the chronicle of the whole world.
 */
public enum EventType {

    FOUNDING(Level.LOCAL),
    TIER_UP(Level.LOCAL),
    TIER_DOWN(Level.LOCAL),
    FIRST_CONTACT(Level.LOCAL),

    BIRTH(Level.MEMORY),
    MARRIAGE(Level.LOCAL),
    DEATH(Level.MEMORY),
    HERO_DEATH(Level.GLOBAL),
    INHERITANCE(Level.MEMORY),
    SKILL_MASTERY(Level.MEMORY),

    MIGRATION(Level.MEMORY),
    TRAVELER(Level.MEMORY),
    FESTIVAL(Level.LOCAL),

    LEADER_CHANGE(Level.LOCAL),
    GOVERNMENT_CHANGE(Level.LOCAL),
    LAW_CHANGE(Level.MEMORY),

    DISCOVERY(Level.LOCAL),
    CULTURE(Level.LOCAL),

    CONSTRUCTION_START(Level.MEMORY),
    CONSTRUCTION(Level.LOCAL),

    TRADE_PACT(Level.LOCAL),
    TRADE_ROUTE(Level.LOCAL),
    ALLIANCE(Level.GLOBAL),
    WAR_START(Level.GLOBAL),
    BATTLE(Level.LOCAL),
    WAR_END(Level.GLOBAL),

    FAMINE(Level.LOCAL),
    FIRE(Level.LOCAL),
    PLAGUE(Level.GLOBAL),
    RAID(Level.LOCAL),
    DEFENCE(Level.LOCAL),
    POLICE(Level.LOCAL),
    MARKET_TRADE(Level.MEMORY),
    HERALD_ANNOUNCEMENT(Level.LOCAL),
    WANDERING_TRADER(Level.MEMORY),
    FIREWORKS(Level.LOCAL);

    public enum Level { MEMORY, LOCAL, GLOBAL }

    private final Level level;

    EventType(Level level) {
        this.level = level;
    }

    public Level level() {
        return level;
    }
}
