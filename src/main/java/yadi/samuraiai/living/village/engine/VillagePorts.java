package yadi.samuraiai.living.village.engine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * What the Village Engine needs from the other engines, as interfaces the hub implements: the profession catalogue (World),
 * the calendar, the knowledge community each village is linked to, the chronicle, the economy's view of a village and the
 * family's view of a person. The Village Engine imports none of them.
 */
public final class VillagePorts {
    private VillagePorts() { }

    public record ProfessionInfo(String id, String name, String workRoutine, Map<String, Double> bias, Set<String> locations, boolean future) { }
    public record FestivalInfo(String id, String name, Map<String, Double> bias, double social) { }

    public interface Professions {
        Optional<ProfessionInfo> get(String id);
        Optional<String> forNpcType(String npcType);
        List<String> ids();
    }

    public interface Calendar {
        CalendarDate today();
        int sunriseShift();
        double seasonSocial();
        WeatherKind weather(String regionScope);
        double temperature(String regionScope);
        List<FestivalInfo> festivals(String culture);
        boolean holyDay();
    }

    /** The knowledge community a village is linked to: collective knowledge, culture and standing stay in the Knowledge Engine. */
    public interface Community {
        String ensure(String key, String name, String culture, String dimension, double x, double y, double z, double radius);
        void join(UUID npc, String key, boolean leader);
        void leave(UUID npc, String key);
        void remember(String key, String historyType, String title, double significance, long minute, UUID actor);
    }

    public interface Chronicle {
        void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source);
    }

    public interface Economy {
        /** Days of food the village has in store at its current consumption. */
        double foodCoverDays(UUID village);
        double prosperity(UUID village);
        /** The profession whose output the village lacks most, if any. */
        Optional<String> mostNeededProfession(UUID village);
    }

    public interface Family {
        /** Life stage name of a person (CHILD_FUTURE, YOUNG_ADULT, ADULT, ELDER...); ADULT when unknown. */
        String lifeStage(UUID npc);
    }

    public static final Professions NO_PROFESSIONS = new Professions() {
        @Override public Optional<ProfessionInfo> get(String id) { return Optional.empty(); }
        @Override public Optional<String> forNpcType(String npcType) { return Optional.empty(); }
        @Override public List<String> ids() { return List.of(); }
    };

    public static Calendar fixedCalendar(CalendarDate date) {
        return new Calendar() {
            @Override public CalendarDate today() { return date; }
            @Override public int sunriseShift() { return 0; }
            @Override public double seasonSocial() { return 1.0D; }
            @Override public WeatherKind weather(String regionScope) { return WeatherKind.SUNNY; }
            @Override public double temperature(String regionScope) { return 18.0D; }
            @Override public List<FestivalInfo> festivals(String culture) { return List.of(); }
            @Override public boolean holyDay() { return false; }
        };
    }

    public static final Community NO_COMMUNITY = new Community() {
        @Override public String ensure(String key, String name, String culture, String dimension, double x, double y, double z, double radius) { return key; }
        @Override public void join(UUID npc, String key, boolean leader) { }
        @Override public void leave(UUID npc, String key) { }
        @Override public void remember(String key, String historyType, String title, double significance, long minute, UUID actor) { }
    };

    public static final Chronicle SILENT = new Chronicle() {
        @Override public void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source) { }
    };

    public static final Economy NO_ECONOMY = new Economy() {
        @Override public double foodCoverDays(UUID village) { return 10.0D; }
        @Override public double prosperity(UUID village) { return 0.5D; }
        @Override public Optional<String> mostNeededProfession(UUID village) { return Optional.empty(); }
    };

    public static final Family NO_FAMILY = npc -> "ADULT";
}
