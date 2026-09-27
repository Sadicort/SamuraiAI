package yadi.samuraiai.living.calendar.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.living.calendar.agriculture.CropCalendar;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarRuntime;
import yadi.samuraiai.living.calendar.festivals.FestivalDef;
import yadi.samuraiai.living.calendar.metrics.CalendarMetrics;
import yadi.samuraiai.living.calendar.timeline.TimelineEntry;
import yadi.samuraiai.living.calendar.weather.WeatherRuntime;
import yadi.samuraiai.living.core.CalendarDate;

/**
 * The Calendar Overlay in text form: date, hour, season, weather, temperature, moon, festivals and active events for a cell,
 * plus the weather history of a cell, the growing seasons, the timeline and the metrics. Commands and tests print these
 * lines; nothing here touches Minecraft.
 */
public final class CalendarInspector {
    private CalendarInspector() { }

    public static String hhmm(int minuteOfDay) { return String.format("%02d:%02d", Math.floorMod(minuteOfDay, 1440) / 60, Math.floorMod(minuteOfDay, 60)); }

    public static List<String> overview(CalendarEngine engine, String cell) {
        CalendarRuntime r = engine.snapshot(cell);
        CalendarDate d = r.date();
        List<String> out = new ArrayList<>();
        out.add(d.describe() + " — " + d.weekdayName() + ", fase " + d.phase());
        out.add(String.format("Estación: %s (día %d de %d)   Luna: %s", CalendarDate.seasonName(d.season()), d.dayOfSeason(), engine.spec().daysInSeason(d.season()), r.moon()));
        out.add(String.format("Clima [%s]: %s (%.0f%%)   Temperatura: %.1f °C   Sol: %s–%s", r.cell(), r.weather(), r.weatherIntensity() * 100, r.temperature(), hhmm(r.sunrise()), hhmm(r.sunset())));
        out.add("Festivales: " + (r.festivals().isEmpty() ? "ninguno" : String.join(", ", r.festivals())) + "   Festivos: " + (r.holidays().isEmpty() ? "ninguno" : String.join(", ", r.holidays())));
        engine.festivals().next(d.dayIndex()).ifPresent(n -> out.add("Próximo festival: " + n.getKey().name() + " en " + n.getValue() + " días"));
        out.add(String.format("Minuto absoluto %d, día %d; reloj: fuente %s, saltos adelante %d, retrocesos rechazados %d", engine.now(), d.dayIndex(), engine.clock().sourceKind(),
                engine.clock().forwardJumps(), engine.clock().rewindAttempts()));
        List<String> problems = engine.problems();
        if (!problems.isEmpty()) out.add("Problemas de configuración: " + String.join("; ", problems));
        return out;
    }

    public static List<String> weather(CalendarEngine engine, String cell) {
        WeatherRuntime w = engine.weatherCell(cell);
        List<String> out = new ArrayList<>();
        out.add(String.format("Celda %s (clima %s, altitud %.0f): %s desde %s, siguiente cambio %s", w.key(), w.climate(), w.altitude(), w.current(),
                engine.date(w.since()).describe(), engine.date(w.nextChange()).describe()));
        StringBuilder hist = new StringBuilder("Historial: ");
        for (WeatherRuntime.Spell s : w.history()) hist.append(s.kind()).append('@').append(engine.date(s.from()).shortDate()).append(' ');
        out.add(hist.toString().trim());
        out.add("Ayer: " + w.yesterdayMinutes() + " (dominante " + w.yesterdayDominant() + ")");
        out.add("Celdas con clima: " + engine.weather().size() + ", avances rápidos: " + engine.weather().fastForwards());
        return out;
    }

    public static List<String> agriculture(CalendarEngine engine, String cell) {
        List<String> out = new ArrayList<>();
        int month = engine.today().month();
        for (CropCalendar c : engine.agriculture().crops()) {
            var gs = engine.agriculture().season(cell, c.resource());
            out.add(String.format("%s: etapa %s, factor de producción %.2f%s", c.resource(), c.stage(month), engine.agriculture().productionFactor(c.resource(), cell, month),
                    gs.map(g -> String.format(" (temporada %d: %d días, buenos %d, secos %d, helada %d, tormenta %d)", g.year(), g.days(), g.goodDays(), g.dryDays(), g.frostDays(), g.stormDays())).orElse("")));
        }
        return out;
    }

    public static List<String> festivals(CalendarEngine engine) {
        List<String> out = new ArrayList<>();
        int year = engine.today().year();
        for (FestivalDef f : engine.festivals().catalog().all()) {
            long start = engine.festivals().startDay(f, year);
            out.add(String.format("%s (%s): %s, %d días%s", f.name(), f.id(), engine.date(start * 1440L).shortDate(), f.days(), f.moon() == null ? "" : ", luna " + f.moon()));
        }
        return out;
    }

    public static List<String> timeline(CalendarEngine engine, String scope, int limit) {
        List<TimelineEntry> entries = scope == null || scope.isBlank() ? engine.timeline().latest(limit) : engine.timeline().of(scope, limit);
        List<String> out = new ArrayList<>();
        for (TimelineEntry e : entries) out.add(String.format("%s [%s] %s%s (%.2f)", engine.date(e.minute()).shortDate(), e.category(), e.title(), e.detail().isEmpty() ? "" : " — " + e.detail(), e.significance()));
        if (out.isEmpty()) out.add("Sin entradas" + (scope == null ? "" : " para " + scope));
        return out;
    }

    public static List<String> metrics(CalendarEngine engine) {
        CalendarMetrics.Snapshot m = engine.metrics().snapshot();
        return List.of(
                String.format("Ticks %d (%.2f µs/tick), días %d (%.2f ms/día, máx %.2f), días comprimidos %d", m.ticks(), m.tickMicros(), m.days(), m.dayMillis(), m.maxDayMillis(), m.skippedDays()),
                String.format("Meses %d, estaciones %d, años %d, cambios de clima %d, festivales %d, festivos %d, aniversarios %d, cosechas %d", m.months(), m.seasons(), m.years(),
                        m.weatherChanges(), m.festivals(), m.holidays(), m.anniversaries(), m.harvests()),
                String.format("Timeline: %d entradas (%d descartadas), aniversarios registrados %d; minutos manuales %d, offline %d, retrocesos rechazados %d",
                        engine.timeline().size(), engine.timeline().dropped(), engine.anniversaries().size(), m.manualMinutes(), m.offlineMinutes(), m.rewindsRefused()));
    }

    public static Map<String, Object> hud(CalendarEngine engine, String cell) {
        CalendarRuntime r = engine.snapshot(cell);
        return Map.of("date", r.date().describe(), "weather", r.weather().name(), "temperature", r.temperature(), "moon", r.moon().name(), "festivals", r.festivals());
    }
}
