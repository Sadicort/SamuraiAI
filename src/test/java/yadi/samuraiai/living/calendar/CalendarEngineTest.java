package yadi.samuraiai.living.calendar;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.clock.CalendarSpec;
import yadi.samuraiai.living.calendar.clock.DeiliClock;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.calendar.events.AnniversaryEvent;
import yadi.samuraiai.living.calendar.events.DayChangedEvent;
import yadi.samuraiai.living.calendar.events.FestivalEndedEvent;
import yadi.samuraiai.living.calendar.events.FestivalStartedEvent;
import yadi.samuraiai.living.calendar.events.HarvestOutlookEvent;
import yadi.samuraiai.living.calendar.events.HolidayEvent;
import yadi.samuraiai.living.calendar.events.SeasonChangedEvent;
import yadi.samuraiai.living.calendar.events.TimeRewindRejectedEvent;
import yadi.samuraiai.living.calendar.events.WeatherChangedEvent;
import yadi.samuraiai.living.calendar.events.YearChangedEvent;
import yadi.samuraiai.living.calendar.persistence.CalendarStorage;
import yadi.samuraiai.living.calendar.timeline.TimelineCategory;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.MoonPhase;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.persistence.LivingStorage;

class CalendarEngineTest {
    private static final int DAY = CalendarSpec.MINUTES_PER_DAY;

    private final List<NpcEvent> events = new ArrayList<>();

    private CalendarEngine engine(CalendarSettings settings) { return new CalendarEngine(() -> settings, events::add, 42L); }
    private CalendarEngine engine() { return engine(CalendarSettings.defaults()); }

    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }

    // ------------------------------------------------------------------ calendar shape

    @Test void defaultCalendarHasTwelveMonthsOfThirtyDaysAndNinetyDaySeasons() {
        CalendarSpec spec = CalendarSpec.defaults();
        assertEquals(12, spec.monthsPerYear());
        assertEquals(360, spec.daysPerYear());
        for (Season s : Season.values()) assertEquals(90, spec.daysInSeason(s));
        CalendarDate d = spec.date(0);
        assertEquals(100, d.year()); assertEquals(1, d.month()); assertEquals(1, d.day()); assertEquals(Season.SPRING, d.season()); assertEquals(1, d.dayOfSeason());
        assertEquals("Mutsuki", d.monthName());
    }

    @Test void datesRoundTripAndHandleTimeBeforeTheEpoch() {
        CalendarSpec spec = CalendarSpec.defaults();
        long m = spec.minuteOf(102, 4, 15, 6, 30);
        CalendarDate d = spec.date(m);
        assertEquals(102, d.year()); assertEquals(4, d.month()); assertEquals(15, d.day()); assertEquals(6, d.hour()); assertEquals(30, d.minuteOfHour());
        assertEquals(Season.SUMMER, d.season()); assertEquals(15, d.dayOfSeason());
        CalendarDate before = spec.date(-1);
        assertEquals(99, before.year()); assertEquals(12, before.month()); assertEquals(30, before.day()); assertEquals(23, before.hour());
        assertEquals(spec.minuteOf(60, 3, 2, 0, 0), spec.date(spec.minuteOf(60, 3, 2, 0, 0)).minute());
    }

    @Test void dayPhasesFollowTheConfiguredBoundaries() {
        CalendarSpec spec = CalendarSpec.defaults();
        assertEquals(yadi.samuraiai.living.core.DayPhase.MIDNIGHT, spec.phaseAt(30));
        assertEquals(yadi.samuraiai.living.core.DayPhase.DAWN, spec.phaseAt(5 * 60 + 10));
        assertEquals(yadi.samuraiai.living.core.DayPhase.NOON, spec.phaseAt(12 * 60));
        assertEquals(yadi.samuraiai.living.core.DayPhase.NIGHT, spec.phaseAt(22 * 60));
    }

    @Test void aConfiguredCalendarChangesMonthsAndSeasons() {
        CalendarSpec spec = new CalendarSpec(List.of("Uno:10:SPRING", "Dos:10:SUMMER", "Tres:5:WINTER", "roto"), List.of("A", "B"), List.of(), 1);
        assertEquals(25, spec.daysPerYear());
        assertEquals(1, spec.problems().size());
        assertEquals(Season.WINTER, spec.date(22L * DAY).season());
        assertEquals("B", spec.date(DAY).weekdayName());
    }

    // ------------------------------------------------------------------ the clock

    @Test void clockFollowsTheSunAndNeverRewinds() {
        DeiliClock clock = new DeiliClock(0);
        clock.configure(24000, 360, true);
        clock.advance(0, 0, true);                   // first tick aligns to 06:00
        assertEquals(360, clock.now());
        clock.advance(1000, 1000, true);             // one Deiliora hour
        assertEquals(420, clock.now());
        clock.advance(1100, 13000, true);            // slept through to 19:00 (dayTime jumped forward)
        assertEquals(19 * 60, clock.now() % DAY);
        long before = clock.now();
        clock.advance(1200, 1000, true);             // "/time set 1000": the sky says 07:00 again
        assertTrue(clock.now() > before, "time must not rewind");
        assertEquals(7 * 60, clock.now() % DAY);     // ... it moves forward to tomorrow's 07:00
        assertEquals(1, clock.rewindAttempts());
    }

    @Test void clockRunsOnGameTimeWhenTheDaylightCycleIsFrozen() {
        DeiliClock clock = new DeiliClock(600);
        clock.configure(24000, 360, true);
        clock.advance(5000, 6000, false);
        clock.advance(7000, 6000, false);           // day time frozen, game time moves 2000 ticks = 120 minutes
        assertEquals(720, clock.now());
        assertEquals(0, clock.addMinutes(-10));
        assertEquals(720, clock.now());
    }

    // ------------------------------------------------------------------ the engine

    @Test void advancingAYearAnnouncesDaysSeasonsFestivalsHolidaysAndTheNewYear() {
        CalendarEngine c = engine();
        c.advanceMinutes(360L * DAY, "test");
        assertEquals(101, c.today().year());
        assertEquals(90, of(DayChangedEvent.class).size());   // compressed: only the last 90 days one by one
        assertTrue(of(DayChangedEvent.class).get(0).skippedDays() > 0);
        events.clear();
        CalendarEngine d = engine(CalendarSettings.builder().set("maxDaysPerAdvance", 400).build());
        d.advanceMinutes(360L * DAY, "test");
        assertEquals(360, of(DayChangedEvent.class).size());
        assertEquals(4, of(SeasonChangedEvent.class).size());
        assertEquals(1, of(YearChangedEvent.class).size());
        List<String> festivals = of(FestivalStartedEvent.class).stream().map(FestivalStartedEvent::festivalId).toList();
        assertTrue(festivals.containsAll(List.of("hanami", "natsu_matsuri", "tsukimi", "aki_matsuri", "fuyu_matsuri")), festivals.toString());
        assertEquals(of(FestivalStartedEvent.class).size(), of(FestivalEndedEvent.class).size());
        assertTrue(of(HolidayEvent.class).stream().anyMatch(h -> h.holidayId().equals("shogatsu")));
        assertTrue(of(HolidayEvent.class).stream().anyMatch(h -> h.holidayId().equals("obon") && h.tags().contains("ancestors")));
    }

    @Test void theMoonFestivalFallsOnAFullMoon() {
        CalendarEngine c = engine();
        var tsukimi = c.festivals().catalog().get("tsukimi").orElseThrow();
        long start = c.festivals().startDay(tsukimi, 100);
        assertEquals(MoonPhase.FULL, c.moon().phase(start));
        assertEquals(MoonPhase.FULL, c.moon().phase(c.spec().date(c.spec().minuteOf(100, 3, 15, 0, 0)).dayIndex()));
    }

    @Test void timeCannotBeMovedBackwards() {
        CalendarEngine c = engine();
        long now = c.now();
        assertEquals(0, c.advanceMinutes(-500, "test"));
        assertEquals(now, c.now());
        assertEquals(1, of(TimeRewindRejectedEvent.class).size());
    }

    @Test void weatherChangesDeterministicallyAndSeasonsChangeTemperature() {
        CalendarEngine a = engine(), b = engine();
        a.trackWeather("region:a", "mountain", 180);
        b.trackWeather("region:a", "mountain", 180);
        a.advanceMinutes(40L * DAY, "test");
        b.advanceMinutes(40L * DAY, "test");
        assertEquals(a.weatherAt("region:a"), b.weatherAt("region:a"));
        assertTrue(of(WeatherChangedEvent.class).size() > 10);
        assertEquals(a.weatherCell("region:a").changes(), b.weatherCell("region:a").changes());
        double mountain = a.temperature("region:a", Double.NaN), valley = a.temperature(CalendarEngine.WORLD_CELL, 64);
        assertTrue(mountain < valley, "altitude and microclimate make mountains colder: " + mountain + " vs " + valley);
        CalendarEngine s = engine();
        s.advanceMinutes(135L * DAY, "test");                  // mid summer, 06:00
        double summer = s.temperature(CalendarEngine.WORLD_CELL, 64);
        s.advanceMinutes(180L * DAY, "test");                  // mid winter
        double winter = s.temperature(CalendarEngine.WORLD_CELL, 64);
        assertTrue(summer > winter + 10, summer + " vs " + winter);
    }

    @Test void forcedWeatherIsPublishedAndLasts() {
        CalendarEngine c = engine();
        c.forceWeather(CalendarEngine.WORLD_CELL, WeatherKind.STORM, 0.9, 120);
        assertEquals(WeatherKind.STORM, c.weatherAt(CalendarEngine.WORLD_CELL));
        assertTrue(of(WeatherChangedEvent.class).stream().anyMatch(e -> e.to() == WeatherKind.STORM));
        c.advanceMinutes(60, "test");
        assertEquals(WeatherKind.STORM, c.weatherAt(CalendarEngine.WORLD_CELL));
    }

    @Test void harvestYieldComesFromTheWeatherOfTheGrowingSeason() {
        CalendarEngine c = engine(CalendarSettings.builder().set("maxDaysPerAdvance", 400).build());
        c.advanceMinutes(200L * DAY, "test");
        List<HarvestOutlookEvent> outlooks = of(HarvestOutlookEvent.class);
        assertTrue(outlooks.stream().anyMatch(o -> o.crop().equals("rice")), outlooks.toString());
        HarvestOutlookEvent rice = outlooks.stream().filter(o -> o.crop().equals("rice")).findFirst().orElseThrow();
        var season = c.agriculture().season(CalendarEngine.WORLD_CELL, "rice").orElseThrow();
        assertTrue(season.days() >= 100, "planting and growth were recorded day by day: " + season.days());
        assertEquals(rice.yieldFactor(), c.agriculture().productionFactor("rice", CalendarEngine.WORLD_CELL, 7), 1e-9);
        assertEquals(0.0D, c.agriculture().productionFactor("rice", CalendarEngine.WORLD_CELL, 10), 1e-9);
        assertEquals(1.0D, c.agriculture().productionFactor("iron", CalendarEngine.WORLD_CELL, 10), 1e-9);
    }

    @Test void anniversariesAreAnnouncedEveryYear() {
        CalendarEngine c = engine(CalendarSettings.builder().set("maxDaysPerAdvance", 800).build());
        c.anniversary("VILLAGE_FOUNDING", "village:x", "Fundación de X", c.now() + 2L * DAY);
        c.advanceMinutes(2L * 360 * DAY + 5L * DAY, "test");
        List<AnniversaryEvent> due = of(AnniversaryEvent.class);
        assertEquals(2, due.size());
        assertEquals(1, due.get(0).years());
        assertEquals(2, due.get(1).years());
    }

    @Test void timelineIsIndexedByScopeAndTrimmedBySignificance() {
        CalendarEngine c = engine(CalendarSettings.builder().set("timelineMax", 100).build());
        for (int i = 0; i < 300; i++) c.record(TimelineCategory.OTHER, "menor " + i, "", Set.of("village:a"), 0.1, Provenance.of("test", "", "", 0));
        c.record(TimelineCategory.WAR, "Gran guerra", "", Set.of("village:a", "region:r"), 0.95, Provenance.of("test", "", "", 0));
        assertTrue(c.timeline().size() <= 100);
        assertEquals(1, c.timeline().of("region:r", 0).size());
        assertTrue(c.timeline().of("village:a", 0).stream().anyMatch(e -> e.title().equals("Gran guerra")));
    }

    @Test void offlineTimeIsAppliedOnlyWhenConfigured() {
        CalendarEngine none = engine();
        assertEquals(0, none.applyOffline(3_600_000L));
        CalendarEngine capped = engine(CalendarSettings.builder().set("offlineMode", 2).set("offlineRealMinutesPerDay", 20).set("offlineMaxDays", 2).build());
        long applied = capped.applyOffline(10L * 60 * 60 * 1000);   // 10 real hours = 30 days, capped to 2
        assertEquals(2L * DAY, applied);
    }

    @Test void everythingSurvivesARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-calendar");
        CalendarEngine a = engine();
        a.trackWeather("region:r", "coastal", 70);
        a.advanceMinutes(95L * DAY + 123, "test");
        a.record(TimelineCategory.FOUNDING, "Fundación", "", Set.of("village:v"), 0.9, Provenance.of("test", "v", "", a.now()));
        a.anniversary("VILLAGE_FOUNDING", "village:v", "Fundación", a.now());
        LivingStorage store = new LivingStorage(dir, false);
        CalendarStorage.sections(a).forEach(store::register);
        assertEquals(5, store.saveAll());

        CalendarEngine b = engine();
        LivingStorage load = new LivingStorage(dir, false);
        CalendarStorage.sections(b).forEach(load::register);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));
        assertEquals(a.now(), b.now());
        assertEquals(a.today().describe(), b.today().describe());
        assertEquals(a.weatherAt("region:r"), b.weatherAt("region:r"));
        assertEquals(a.weatherCell("region:r").nextChange(), b.weatherCell("region:r").nextChange());
        assertEquals(1, b.timeline().of("village:v", 0).size());
        assertEquals(1, b.anniversaries().about("village:v").size());
        // and they keep evolving identically
        a.advanceMinutes(10L * DAY, "test"); b.advanceMinutes(10L * DAY, "test");
        assertEquals(a.weatherAt("region:r"), b.weatherAt("region:r"));
    }
}
