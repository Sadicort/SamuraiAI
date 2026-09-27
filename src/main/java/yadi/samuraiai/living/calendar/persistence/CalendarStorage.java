package yadi.samuraiai.living.calendar.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.calendar.agriculture.GrowingSeason;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.holidays.AnniversaryRecord;
import yadi.samuraiai.living.calendar.timeline.TimelineEntry;
import yadi.samuraiai.living.calendar.weather.WeatherRuntime;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.persistence.StoreSection;

/**
 * The calendar's persistent sections: the clock (date, seed, when the server was last saved), the weather cells, the world
 * timeline, the anniversaries and the growing seasons. Each is a separate versioned file, written only when it changed.
 */
public final class CalendarStorage {
    public static final int SCHEMA = 1;

    private CalendarStorage() { }

    public static List<StoreSection> sections(CalendarEngine engine) {
        return List.of(new ClockSection(engine), new WeatherSection(engine), new TimelineSection(engine), new AnniversarySection(engine), new AgricultureSection(engine));
    }

    /** The official date. Saved whenever time has moved since the last save. */
    static final class ClockSection implements StoreSection {
        private final CalendarEngine engine;
        private long savedMinute = Long.MIN_VALUE;
        ClockSection(CalendarEngine engine) { this.engine = engine; }
        @Override public String domain() { return "calendar-clock"; }
        @Override public String file() { return "calendar/clock.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return engine.dirty() || engine.now() != savedMinute; }
        @Override public JsonObject write() {
            JsonObject o = new JsonObject();
            long real = engine.realNow();
            engine.markSaved(real);
            o.addProperty("minute", engine.now());
            o.addProperty("fraction", engine.clock().fraction());
            o.addProperty("rewinds", engine.clock().rewindAttempts());
            o.addProperty("jumps", engine.clock().forwardJumps());
            o.addProperty("processedDay", engine.lastProcessedDay());
            o.addProperty("seed", engine.seed());
            o.addProperty("savedAtReal", real);
            o.addProperty("date", engine.today().describe());
            savedMinute = engine.now();
            return o;
        }
        @Override public void read(JsonObject p) {
            long minute = Json.lng(p, "minute", engine.now());
            engine.restoreClock(minute, Json.num(p, "fraction", 0), Json.lng(p, "rewinds", 0), Json.lng(p, "jumps", 0),
                    Json.lng(p, "processedDay", engine.dayIndex(minute)), Json.lng(p, "seed", engine.seed()), Json.lng(p, "savedAtReal", 0));
            savedMinute = minute;
        }
        @Override public void clean() { engine.clean(); savedMinute = engine.now(); }
    }

    static final class WeatherSection implements StoreSection {
        private final CalendarEngine engine;
        private long savedMinute = Long.MIN_VALUE;
        private int savedCells = -1;
        WeatherSection(CalendarEngine engine) { this.engine = engine; }
        @Override public String domain() { return "calendar-weather"; }
        @Override public String file() { return "calendar/weather.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return engine.now() != savedMinute || engine.weather().size() != savedCells; }
        @Override public JsonObject write() {
            JsonArray cells = new JsonArray();
            for (WeatherRuntime c : engine.weather().cells()) {
                JsonObject o = new JsonObject();
                o.addProperty("key", c.key()); o.addProperty("climate", c.climate()); o.addProperty("altitude", c.altitude());
                o.addProperty("current", c.current().name()); o.addProperty("intensity", c.intensity()); o.addProperty("since", c.since());
                o.addProperty("next", c.nextChange()); o.addProperty("changes", c.changes()); o.addProperty("accounted", c.accountedUntil());
                o.add("today", tally(c.todayMinutes())); o.add("yesterday", tally(c.yesterdayMinutes()));
                JsonArray spells = new JsonArray();
                for (WeatherRuntime.Spell s : c.history()) { JsonObject x = new JsonObject(); x.addProperty("kind", s.kind().name()); x.addProperty("from", s.from()); x.addProperty("intensity", s.intensity()); spells.add(x); }
                o.add("history", spells);
                cells.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("cells", cells);
            savedMinute = engine.now(); savedCells = engine.weather().size();
            return p;
        }
        @Override public void read(JsonObject p) {
            for (JsonElement e : Json.arr(p, "cells")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String key = Json.str(o, "key", null);
                if (key == null) continue;
                WeatherRuntime c = new WeatherRuntime(key, Json.str(o, "climate", "temperate"), Json.num(o, "altitude", 64), Json.lng(o, "since", 0));
                List<WeatherRuntime.Spell> spells = new ArrayList<>();
                for (JsonElement s : Json.arr(o, "history")) if (s.isJsonObject()) {
                    JsonObject x = s.getAsJsonObject();
                    spells.add(new WeatherRuntime.Spell(Json.enumOf(x, "kind", WeatherKind.class, WeatherKind.SUNNY), Json.lng(x, "from", 0), Json.num(x, "intensity", 0.5)));
                }
                c.restore(Json.enumOf(o, "current", WeatherKind.class, WeatherKind.SUNNY), Json.num(o, "intensity", 0.5), Json.lng(o, "since", 0), Json.lng(o, "next", 0), Json.lng(o, "changes", 0),
                        Json.lng(o, "accounted", 0), untally(Json.obj(o, "today")), untally(Json.obj(o, "yesterday")), spells);
                engine.weather().restore(c);
            }
            savedMinute = engine.now(); savedCells = engine.weather().size();
        }
        @Override public void clean() { savedMinute = engine.now(); savedCells = engine.weather().size(); }

        private static JsonObject tally(Map<WeatherKind, Long> m) { JsonObject o = new JsonObject(); m.forEach((k, v) -> o.addProperty(k.name(), v)); return o; }
        private static Map<WeatherKind, Long> untally(JsonObject o) {
            Map<WeatherKind, Long> m = new EnumMap<>(WeatherKind.class);
            for (String k : o.keySet()) WeatherKind.parse(k).ifPresent(kind -> m.put(kind, Json.lng(o, k, 0)));
            return m;
        }
    }

    static final class TimelineSection implements StoreSection {
        private final CalendarEngine engine;
        TimelineSection(CalendarEngine engine) { this.engine = engine; }
        @Override public String domain() { return "calendar-timeline"; }
        @Override public String file() { return "calendar/timeline.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return engine.timeline().dirty(); }
        @Override public JsonObject write() {
            JsonObject p = new JsonObject();
            p.add("entries", Json.array(engine.timeline().all(), TimelineEntry::toJson));
            return p;
        }
        @Override public void read(JsonObject p) {
            engine.timeline().clear();
            for (TimelineEntry e : Json.list(Json.arr(p, "entries"), TimelineEntry::fromJson)) engine.timeline().record(e);
        }
        @Override public void clean() { engine.timeline().clean(); }
    }

    static final class AnniversarySection implements StoreSection {
        private final CalendarEngine engine;
        AnniversarySection(CalendarEngine engine) { this.engine = engine; }
        @Override public String domain() { return "calendar-anniversaries"; }
        @Override public String file() { return "calendar/anniversaries.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return engine.anniversaries().dirty(); }
        @Override public JsonObject write() {
            JsonArray a = new JsonArray();
            for (AnniversaryRecord r : engine.anniversaries().all()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", r.id().toString()); o.addProperty("kind", r.kind()); o.addProperty("subject", r.subject()); o.addProperty("title", r.title());
                o.addProperty("origin", r.originMinute()); o.addProperty("month", r.month()); o.addProperty("day", r.day()); o.addProperty("year", r.originYear());
                a.add(o);
            }
            JsonObject p = new JsonObject();
            p.add("records", a);
            return p;
        }
        @Override public void read(JsonObject p) {
            engine.anniversaries().clear();
            for (AnniversaryRecord r : Json.list(Json.arr(p, "records"), o -> {
                var id = Json.uuid(o, "id");
                return id == null ? null : new AnniversaryRecord(id, Json.str(o, "kind", "CUSTOM"), Json.str(o, "subject", ""), Json.str(o, "title", ""), Json.lng(o, "origin", 0),
                        Json.integer(o, "month", 1), Json.integer(o, "day", 1), Json.integer(o, "year", 0));
            })) engine.anniversaries().register(r);
        }
        @Override public void clean() { engine.anniversaries().clean(); }
    }

    static final class AgricultureSection implements StoreSection {
        private final CalendarEngine engine;
        AgricultureSection(CalendarEngine engine) { this.engine = engine; }
        @Override public String domain() { return "calendar-agriculture"; }
        @Override public String file() { return "calendar/agriculture.json"; }
        @Override public int schemaVersion() { return SCHEMA; }
        @Override public boolean dirty() { return engine.agriculture().dirty(); }
        @Override public JsonObject write() {
            JsonArray seasons = new JsonArray();
            for (GrowingSeason g : engine.agriculture().seasons()) {
                JsonObject o = new JsonObject();
                o.addProperty("cell", g.cell()); o.addProperty("crop", g.crop()); o.addProperty("year", g.year()); o.addProperty("good", g.goodDays()); o.addProperty("dry", g.dryDays());
                o.addProperty("frost", g.frostDays()); o.addProperty("storm", g.stormDays()); o.addProperty("days", g.days()); o.addProperty("announced", g.announced());
                seasons.add(o);
            }
            JsonObject yields = new JsonObject();
            engine.agriculture().fixedYields().forEach(yields::addProperty);
            JsonObject p = new JsonObject();
            p.add("seasons", seasons); p.add("yields", yields);
            return p;
        }
        @Override public void read(JsonObject p) {
            for (GrowingSeason g : Json.list(Json.arr(p, "seasons"), o -> {
                GrowingSeason gs = new GrowingSeason(Json.str(o, "cell", "world"), Json.str(o, "crop", ""), Json.integer(o, "year", 0));
                gs.restore(Json.integer(o, "year", 0), Json.integer(o, "good", 0), Json.integer(o, "dry", 0), Json.integer(o, "frost", 0), Json.integer(o, "storm", 0), Json.integer(o, "days", 0), Json.bool(o, "announced", false));
                return gs;
            })) engine.agriculture().restoreSeason(g);
            JsonObject y = Json.obj(p, "yields");
            for (String k : y.keySet()) { int bar = k.indexOf('|'); if (bar > 0) engine.agriculture().restoreYield(k.substring(0, bar), k.substring(bar + 1), Json.num(y, k, 1.0)); }
        }
        @Override public void clean() { engine.agriculture().clean(); }
    }
}
