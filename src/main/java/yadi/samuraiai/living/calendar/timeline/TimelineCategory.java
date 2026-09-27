package yadi.samuraiai.living.calendar.timeline;

import java.util.Locale;
import java.util.Optional;

/** What kind of fact a timeline entry is. BIRTH and DEATH are written by the Family Engine when those systems produce them. */
public enum TimelineCategory {
    FOUNDING, CONSTRUCTION, FESTIVAL, HOLIDAY, WEATHER, HARVEST, ECONOMY, TRADE, WAR, BATTLE, FIRE, DISASTER, HERO, BETRAYAL, QUEST,
    FAMILY, BIRTH, DEATH, SUCCESSION, INHERITANCE, MIGRATION, VISIT, POPULATION, OTHER;

    public static Optional<TimelineCategory> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
