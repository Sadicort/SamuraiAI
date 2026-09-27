package yadi.samuraiai.living.calendar.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import yadi.samuraiai.event.EventSink;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.agriculture.AgricultureCalendar;
import yadi.samuraiai.living.calendar.astronomy.SunModel;
import yadi.samuraiai.living.calendar.clock.CalendarSpec;
import yadi.samuraiai.living.calendar.clock.DeiliClock;
import yadi.samuraiai.living.calendar.events.AnniversaryEvent;
import yadi.samuraiai.living.calendar.events.DayChangedEvent;
import yadi.samuraiai.living.calendar.events.DayPhaseChangedEvent;
import yadi.samuraiai.living.calendar.events.FestivalEndedEvent;
import yadi.samuraiai.living.calendar.events.FestivalStartedEvent;
import yadi.samuraiai.living.calendar.events.HarvestOutlookEvent;
import yadi.samuraiai.living.calendar.events.HolidayEvent;
import yadi.samuraiai.living.calendar.events.MonthChangedEvent;
import yadi.samuraiai.living.calendar.events.MoonPhaseChangedEvent;
import yadi.samuraiai.living.calendar.events.SeasonChangedEvent;
import yadi.samuraiai.living.calendar.events.TimeRewindRejectedEvent;
import yadi.samuraiai.living.calendar.events.TimelineRecordedEvent;
import yadi.samuraiai.living.calendar.events.WeatherChangedEvent;
import yadi.samuraiai.living.calendar.events.YearChangedEvent;
import yadi.samuraiai.living.calendar.festivals.FestivalCatalog;
import yadi.samuraiai.living.calendar.festivals.FestivalDef;
import yadi.samuraiai.living.calendar.festivals.FestivalEngine;
import yadi.samuraiai.living.calendar.holidays.AnniversaryEngine;
import yadi.samuraiai.living.calendar.holidays.AnniversaryRecord;
import yadi.samuraiai.living.calendar.holidays.HolidayDef;
import yadi.samuraiai.living.calendar.holidays.HolidayEngine;
import yadi.samuraiai.living.calendar.metrics.CalendarMetrics;
import yadi.samuraiai.living.calendar.moon.MoonEngine;
import yadi.samuraiai.living.calendar.seasons.SeasonProfile;
import yadi.samuraiai.living.calendar.seasons.SeasonTable;
import yadi.samuraiai.living.calendar.temperature.TemperatureModel;
import yadi.samuraiai.living.calendar.timeline.TimelineCategory;
import yadi.samuraiai.living.calendar.timeline.TimelineEntry;
import yadi.samuraiai.living.calendar.timeline.WorldTimeline;
import yadi.samuraiai.living.calendar.weather.ClimateProfile;
import yadi.samuraiai.living.calendar.weather.ClimateTable;
import yadi.samuraiai.living.calendar.weather.WeatherEngine;
import yadi.samuraiai.living.calendar.weather.WeatherRuntime;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.DayPhase;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.core.MoonPhase;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.WorldClock;

/**
 * The Living Calendar & Seasons Engine: the official source of time in Deiliora and the {@link WorldClock} every other engine
 * asks. It owns the clock (monotonic, never rewinds), the calendar's shape, seasons, regional weather and temperature, the
 * moon, festivals, holidays, anniversaries, the agricultural calendar and the world timeline.
 *
 * <p>Each server tick it advances the clock from the world's clocks (cheap); only when a day boundary is crossed does it do
 * the day's work (festivals, holidays, anniversaries, moon, harvest outlooks). A jump of many days processes at most
 * {@code maxDaysPerAdvance} of them individually; the older ones are summarised in {@link DayChangedEvent#skippedDays()}.
 * Weather cells are stepped only when their next change is due. Pure Java: no Minecraft class is used here.
 */
public final class CalendarEngine implements WorldClock {
    public static final String WORLD_CELL = "world";

    private final Supplier<CalendarSettings> settingsSupplier;
    private final CalendarMetrics metrics = new CalendarMetrics();
    private final WeatherEngine weather = new WeatherEngine();
    private final AnniversaryEngine anniversaries;
    private final WorldTimeline timeline;
    private final DeiliClock clock;
    private EventSink events;
    private LongSupplier realClock = System::currentTimeMillis;

    private CalendarSettings settings;
    private CalendarSpec spec;
    private SeasonTable seasons;
    private ClimateTable climates;
    private MoonEngine moon;
    private SunModel sun;
    private TemperatureModel temperature;
    private FestivalEngine festivals;
    private HolidayEngine holidays;
    private AgricultureCalendar agriculture;
    private Dice dice;
    private long seed;

    private long lastProcessedDay;
    private DayPhase lastPhase;
    private MoonPhase lastMoon;
    private long reportedRewinds;
    private boolean dirty;
    private long savedAtRealMillis;

    private final WeatherEngine.Conditions conditions = new WeatherEngine.Conditions() {
        @Override public SeasonProfile seasonAt(long minute) { return seasonProfile(minute); }
        @Override public double temperatureAt(WeatherRuntime cell, long minute, WeatherKind kind) { return CalendarEngine.this.temperatureAt(cell, minute, kind); }
    };

    public CalendarEngine(Supplier<CalendarSettings> settings, EventSink events, long seed) {
        this.settingsSupplier = Objects.requireNonNull(settings);
        this.events = Objects.requireNonNull(events);
        this.seed = seed;
        CalendarSettings s = settings.get();
        this.anniversaries = new AnniversaryEngine(s.anniversaryMax());
        this.timeline = new WorldTimeline(s.timelineMax(), s.timelineKeepSignificance());
        rebuild(s);
        long start = (long) (s.startDayOfYear() - 1) * CalendarSpec.MINUTES_PER_DAY + s.startHour() * 60L;
        this.clock = new DeiliClock(start);
        clock.configure(s.ticksPerDay(), s.sunOffsetMinutes(), s.alignToSun());
        this.lastProcessedDay = dayIndex(start);
        this.lastPhase = spec.date(start).phase();
        this.lastMoon = moon.phase(lastProcessedDay);
        festivals.restoreActive(lastProcessedDay);
        weather.track(WORLD_CELL, "temperate", 64, start);
    }

    private void rebuild(CalendarSettings s) {
        this.settings = s;
        this.spec = new CalendarSpec(s.months(), s.weekdays(), s.phases(), s.epochYear());
        this.seasons = new SeasonTable(s.seasons());
        this.climates = new ClimateTable(s.climates());
        this.moon = new MoonEngine(s.moonCycleDays(), s.moonOffsetDays());
        this.sun = new SunModel(spec.daysPerYear(), s.longestDayOfYear(), s.daylightAmplitudeHours());
        this.temperature = new TemperatureModel(s.seasonBlendDays());
        this.festivals = new FestivalEngine(new FestivalCatalog(s.festivals()), spec, moon::phase);
        this.holidays = new HolidayEngine(s.holidays());
        AgricultureCalendar previous = agriculture;
        this.agriculture = new AgricultureCalendar(s.crops(), s.greatHarvestYield(), s.badHarvestYield());
        if (previous != null) { previous.seasons().forEach(agriculture::restoreSeason); previous.fixedYields().forEach((k, v) -> { int bar = k.indexOf('|'); agriculture.restoreYield(k.substring(0, bar), k.substring(bar + 1), v); }); }
        this.dice = new Dice(seed);
        weather.configure(climates, dice, s.weatherMinHours(), s.weatherMaxHours(), s.weatherPersistence(), s.weatherMaxSteps());
        timeline.configure(s.timelineMax(), s.timelineKeepSignificance());
        if (clock != null) clock.configure(s.ticksPerDay(), s.sunOffsetMinutes(), s.alignToSun());
        if (festivals != null && clock != null) festivals.restoreActive(currentDay());
    }

    private void refreshSettings() {
        CalendarSettings s = settingsSupplier.get();
        if (s != settings) rebuild(s);
    }

    public void useEventSink(EventSink sink) { this.events = Objects.requireNonNull(sink); }
    public void useRealClock(LongSupplier millis) { this.realClock = Objects.requireNonNull(millis); }
    public void useSeed(long newSeed) { if (newSeed != seed) { seed = newSeed; dice = new Dice(seed); weather.configure(climates, dice, settings.weatherMinHours(), settings.weatherMaxHours(), settings.weatherPersistence(), settings.weatherMaxSteps()); dirty = true; } }

    private void publish(NpcEvent event) { events.publish(event); }

    // ------------------------------------------------------------------ WorldClock

    @Override public long now() { return clock.now(); }
    @Override public CalendarDate date(long minute) { return spec.date(minute); }
    @Override public int minutesPerDay() { return CalendarSpec.MINUTES_PER_DAY; }

    // ------------------------------------------------------------------ advancing time

    /** One server tick: advance from the world's clocks and do whatever the time that passed requires. */
    public void tick(long gameTime, long dayTime, boolean sunMoves) {
        long started = System.nanoTime();
        refreshSettings();
        long before = clock.now();
        long gained = clock.advance(gameTime, dayTime, sunMoves);
        if (clock.rewindAttempts() > reportedRewinds) {
            reportedRewinds = clock.rewindAttempts();
            metrics.rewindsRefused.incrementAndGet();
            publish(new TimeRewindRejectedEvent(clock.now(), reportedRewinds, "world clock went backwards; calendar kept moving forward"));
        }
        if (gained > 0) passed(before, clock.now());
        metrics.ticks.incrementAndGet();
        metrics.tickNanos.addAndGet(System.nanoTime() - started);
    }

    /** Resumes counting from the world's clocks after a restore, without treating the first tick as a jump. */
    public void resume(long gameTime, long dayTime, boolean sunMoves) { clock.resumeFrom(gameTime, dayTime, sunMoves); }

    /** Moves time forward (commands, offline catch-up, tests). Refuses to go backwards. Returns minutes moved. */
    public long advanceMinutes(long minutes, String cause) {
        refreshSettings();
        if (minutes <= 0) {
            if (minutes < 0) { metrics.rewindsRefused.incrementAndGet(); publish(new TimeRewindRejectedEvent(clock.now(), clock.rewindAttempts() + 1, "refused to move time back: " + cause)); }
            return 0;
        }
        long before = clock.now();
        clock.addMinutes(minutes);
        if ("offline".equals(cause)) metrics.offlineMinutes.addAndGet(minutes); else metrics.manualMinutes.addAndGet(minutes);
        passed(before, clock.now());
        return minutes;
    }

    /** Converts the real time the server was stopped into Deiliora time, per {@code offlineMode}. Returns minutes applied. */
    public long applyOffline(long stoppedRealMillis) {
        CalendarSettings s = settings;
        if (s.offlineMode() == 0 || stoppedRealMillis <= 0) return 0;
        double realMinutes = stoppedRealMillis / 60000.0D;
        long minutes = (long) Math.floor(realMinutes / s.offlineRealMinutesPerDay() * CalendarSpec.MINUTES_PER_DAY);
        if (s.offlineMode() == 2) minutes = Math.min(minutes, (long) s.offlineMaxDays() * CalendarSpec.MINUTES_PER_DAY);
        return advanceMinutes(minutes, "offline");
    }

    private void passed(long before, long now) {
        dirty = true;
        long toDay = dayIndex(now);
        long fromDay = lastProcessedDay + 1;
        if (toDay >= fromDay) {
            long count = toDay - fromDay + 1;
            long skipped = 0;
            if (count > settings.maxDaysPerAdvance()) {
                skipped = count - settings.maxDaysPerAdvance();
                fromDay = toDay - settings.maxDaysPerAdvance() + 1;
                metrics.skippedDays.addAndGet(skipped);
            }
            for (long d = fromDay; d <= toDay; d++) processDay(d, d == fromDay ? skipped : 0);
            lastProcessedDay = toDay;
        }
        DayPhase phase = spec.date(now).phase();
        if (lastPhase != null && phase != lastPhase) publish(new DayPhaseChangedEvent(now, lastPhase, phase));
        lastPhase = phase;
        stepWeather(now);
    }

    private void stepWeather(long to) {
        for (WeatherEngine.Change c : weather.stepTo(to, conditions)) {
            metrics.weatherChanges.incrementAndGet();
            publish(new WeatherChangedEvent(c.minute(), c.key(), c.from(), c.to(), c.intensity()));
        }
    }

    private void processDay(long day, long skipped) {
        long started = System.nanoTime();
        long start = spec.startOfDay(day);
        CalendarDate date = spec.date(start), prev = spec.date(start - 1);
        stepWeather(start);
        weather.rollDay(start);
        publish(new DayChangedEvent(start, day, date.year(), date.month(), date.day(), date.weekdayName(), skipped));
        if (date.year() != prev.year()) { metrics.years.incrementAndGet(); publish(new YearChangedEvent(start, date.year())); }
        if (date.month() != prev.month() || date.year() != prev.year()) { metrics.months.incrementAndGet(); publish(new MonthChangedEvent(start, date.year(), date.month(), date.monthName())); }
        if (date.season() != prev.season()) { metrics.seasons.incrementAndGet(); publish(new SeasonChangedEvent(start, prev.season(), date.season(), date.year())); }
        MoonPhase m = moon.phase(day);
        if (lastMoon != null && m != lastMoon) publish(new MoonPhaseChangedEvent(start, lastMoon, m));
        lastMoon = m;
        for (FestivalEngine.Change change : festivals.onDay(day)) {
            FestivalDef f = change.festival();
            if (change.started()) {
                metrics.festivals.incrementAndGet();
                publish(new FestivalStartedEvent(start, f.id(), f.name(), f.days(), change.year()));
                record(start, TimelineCategory.FESTIVAL, f.name() + " del año " + change.year(), "", Set.of("world", "festival:" + f.id()), 0.3D, Provenance.of("festival", f.id(), "calendar", start));
            } else publish(new FestivalEndedEvent(start, f.id(), f.name(), change.year()));
        }
        for (HolidayDef h : holidays.beginningOn(date)) {
            metrics.holidays.incrementAndGet();
            publish(new HolidayEvent(start, h.id(), h.name(), h.kind(), h.tags()));
            if ("NEW_YEAR".equals(h.kind())) record(start, TimelineCategory.HOLIDAY, "Comienza el año " + date.year(), h.name(), Set.of("world"), 0.2D, Provenance.of("holiday", h.id(), "calendar", start));
        }
        for (AnniversaryEngine.Due due : anniversaries.due(date)) {
            metrics.anniversaries.incrementAndGet();
            AnniversaryRecord r = due.record();
            publish(new AnniversaryEvent(start, r.id(), r.kind(), r.subject(), r.title(), due.years()));
        }
        for (WeatherRuntime cell : weather.cells()) {
            WeatherKind dominant = cell.yesterdayDominant();
            long wet = 0;
            for (var e : cell.yesterdayMinutes().entrySet()) if (e.getKey().wet()) wet += e.getValue();
            double coldest = temperatureAt(cell, start - CalendarSpec.MINUTES_PER_DAY + 4 * 60, dominant);
            for (AgricultureCalendar.Outlook o : agriculture.onDay(cell.key(), date.year(), date.month(), dominant, wet, coldest)) {
                metrics.harvests.incrementAndGet();
                publish(new HarvestOutlookEvent(start, o.cell(), o.crop(), o.year(), o.yieldFactor(), o.verdict().name()));
                if (o.verdict() != AgricultureCalendar.Verdict.NORMAL)
                    record(start, TimelineCategory.HARVEST, (o.verdict() == AgricultureCalendar.Verdict.GREAT ? "Gran cosecha de " : "Mala cosecha de ") + o.crop(),
                            String.format("rendimiento x%.2f", o.yieldFactor()), Set.of(o.cell(), "crop:" + o.crop()), 0.5D, Provenance.of("agriculture", o.cell() + "/" + o.crop(), "weather of the growing season", start));
            }
        }
        metrics.day(System.nanoTime() - started);
    }

    // ------------------------------------------------------------------ seasons, weather, temperature

    public SeasonProfile seasonProfile(long minute) { return seasons.of(spec.date(minute).season()); }
    public SeasonProfile seasonProfile() { return seasonProfile(now()); }
    public Season season() { return today().season(); }

    /** Starts tracking weather over a cell (a region scope such as {@code region:<id>}). */
    public WeatherRuntime trackWeather(String cell, String climate, double altitude) { dirty = true; return weather.track(cell, climate, altitude, now()); }
    public WeatherRuntime weatherCell(String cell) { return weather.cell(cell).orElseGet(() -> weather.cell(WORLD_CELL).orElseThrow()); }
    public WeatherKind weatherAt(String cell) { return weatherCell(cell).current(); }

    /** Imposes weather on a cell for some minutes (a storm event, a command). */
    public void forceWeather(String cell, WeatherKind kind, double intensity, long minutes) {
        WeatherEngine.Change c = weather.force(cell, kind, intensity, now(), minutes);
        dirty = true;
        if (c.from() != c.to()) { metrics.weatherChanges.incrementAndGet(); publish(new WeatherChangedEvent(c.minute(), c.key(), c.from(), c.to(), c.intensity())); }
    }

    double temperatureAt(WeatherRuntime cell, long minute, WeatherKind kind) {
        CalendarDate d = spec.date(minute);
        SeasonProfile s = seasons.of(d.season()), next = seasons.of(d.season().next());
        ClimateProfile climate = climates.of(cell.climate());
        return temperature.temperature(s, next, d.dayOfSeason(), Math.max(1, spec.daysInSeason(d.season())), d.hour() + d.minuteOfHour() / 60.0D, climate, cell.altitude(), kind);
    }

    /** Temperature now over a cell at an altitude (NaN altitude = the cell's own). */
    public double temperature(String cell, double altitude) {
        WeatherRuntime c = weatherCell(cell);
        if (Double.isNaN(altitude)) return temperatureAt(c, now(), c.current());
        CalendarDate d = today();
        SeasonProfile s = seasons.of(d.season()), next = seasons.of(d.season().next());
        return temperature.temperature(s, next, d.dayOfSeason(), Math.max(1, spec.daysInSeason(d.season())), d.hour() + d.minuteOfHour() / 60.0D, climates.of(c.climate()), altitude, c.current());
    }

    public MoonPhase moonPhase() { return moon.phase(currentDay()); }
    public List<FestivalDef> activeFestivals() { return festivals.activeToday(); }
    public List<FestivalDef> festivalsOn(long day) { return festivals.activeOn(day); }
    public List<HolidayDef> activeHolidays() { return holidays.activeOn(currentDay(), spec); }

    /** The whole calendar at this instant for one cell. */
    public CalendarRuntime snapshot(String cell) {
        CalendarDate d = today();
        WeatherRuntime w = weatherCell(cell);
        List<String> f = new ArrayList<>(), h = new ArrayList<>();
        for (FestivalDef x : festivals.activeToday()) f.add(x.name());
        for (HolidayDef x : activeHolidays()) h.add(x.name());
        return new CalendarRuntime(d, w.key(), w.current(), w.intensity(), temperatureAt(w, now(), w.current()), moonPhase(), f, h, sun.sunrise(d.dayOfYear()), sun.sunset(d.dayOfYear()));
    }

    // ------------------------------------------------------------------ history

    /** Records a fact in the world timeline and announces it. */
    public TimelineEntry record(long minute, TimelineCategory category, String title, String detail, Set<String> scopes, double significance, Provenance source) {
        TimelineEntry e = new TimelineEntry(UUID.randomUUID(), minute, category, title, detail, scopes, significance, source);
        if (timeline.record(e).isPresent()) {
            metrics.timelineEntries.incrementAndGet();
            publish(new TimelineRecordedEvent(minute, e.id(), category.name(), title, e.significance(), e.scopes()));
        }
        return e;
    }

    public TimelineEntry record(TimelineCategory category, String title, String detail, Set<String> scopes, double significance, Provenance source) {
        return record(now(), category, title, detail, scopes, significance, source);
    }

    /** Registers a yearly anniversary of an instant. */
    public AnniversaryRecord anniversary(String kind, String subject, String title, long minute) {
        AnniversaryRecord r = AnniversaryEngine.create(kind, subject, title, spec.date(minute));
        anniversaries.register(r);
        return r;
    }

    // ------------------------------------------------------------------ accessors

    public CalendarSettings settings() { return settings; }
    public CalendarSpec spec() { return spec; }
    public SeasonTable seasons() { return seasons; }
    public ClimateTable climates() { return climates; }
    public MoonEngine moon() { return moon; }
    public SunModel sun() { return sun; }
    public FestivalEngine festivals() { return festivals; }
    public HolidayEngine holidays() { return holidays; }
    public AnniversaryEngine anniversaries() { return anniversaries; }
    public AgricultureCalendar agriculture() { return agriculture; }
    public WorldTimeline timeline() { return timeline; }
    public WeatherEngine weather() { return weather; }
    public DeiliClock clock() { return clock; }
    public CalendarMetrics metrics() { return metrics; }
    public long seed() { return seed; }
    public long lastProcessedDay() { return lastProcessedDay; }
    public long savedAtRealMillis() { return savedAtRealMillis; }
    public long realNow() { return realClock.getAsLong(); }

    /** Problems found in the configured data lines (shown by the debug commands). */
    public List<String> problems() {
        List<String> all = new ArrayList<>();
        all.addAll(spec.problems()); all.addAll(seasons.problems()); all.addAll(climates.problems()); all.addAll(festivals.catalog().problems());
        all.addAll(holidays.problems()); all.addAll(agriculture.problems());
        return all;
    }

    // ------------------------------------------------------------------ persistence hooks (used by CalendarStorage)

    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void markSaved(long realMillis) { savedAtRealMillis = realMillis; }

    public void restoreClock(long minute, double fraction, long rewinds, long jumps, long processedDay, long savedSeed, long savedReal) {
        clock.restore(minute, fraction, rewinds, jumps);
        reportedRewinds = rewinds;
        lastProcessedDay = processedDay;
        savedAtRealMillis = savedReal;
        useSeed(savedSeed);
        lastPhase = spec.date(minute).phase();
        lastMoon = moon.phase(dayIndex(minute));
        festivals.restoreActive(dayIndex(minute));
        dirty = false;
    }

    /** Forgets every dynamic state (a new session starts from a load). */
    public void reset() {
        weather.clear();
        anniversaries.clear();
        timeline.clear();
        agriculture.clear();
        metrics.reset();
        CalendarSettings s = settingsSupplier.get();
        rebuild(s);
        long start = (long) (s.startDayOfYear() - 1) * CalendarSpec.MINUTES_PER_DAY + s.startHour() * 60L;
        clock.restore(start, 0, 0, 0);
        reportedRewinds = 0;
        lastProcessedDay = dayIndex(start);
        lastPhase = spec.date(start).phase();
        lastMoon = moon.phase(lastProcessedDay);
        festivals.restoreActive(lastProcessedDay);
        weather.track(WORLD_CELL, "temperate", 64, start);
        dirty = false;
    }
}
