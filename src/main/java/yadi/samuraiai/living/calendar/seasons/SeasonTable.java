package yadi.samuraiai.living.calendar.seasons;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * The four season profiles. Lines look like
 * {@code WINTER;temp=1;swing=4;weather=SUNNY:26,CLOUDY:30,SNOW:20;crops=0.3;food=1.25;fuel=2.0;trade=0.7;social=0.85;travel=0.7}.
 * A configured line replaces only the season it names; the built-in lines below are the defaults and use the same format.
 */
public final class SeasonTable {
    public static final List<String> DEFAULT_LINES = List.of(
            "SPRING;temp=14;swing=6;weather=SUNNY:34,CLOUDY:24,RAIN:26,STORM:3,FOG:9,STRONG_WIND:4;vegetation=0.85;crops=1.0;food=1.0;fuel=1.0;trade=1.05;social=1.15;travel=1.0;animals=1.2",
            "SUMMER;temp=26;swing=7;weather=SUNNY:46,CLOUDY:18,RAIN:16,STORM:12,FOG:3,STRONG_WIND:5;vegetation=1.0;crops=1.0;food=0.95;fuel=0.5;trade=1.15;social=1.2;travel=1.15;animals=1.0",
            "AUTUMN;temp=15;swing=6;weather=SUNNY:32,CLOUDY:28,RAIN:22,STORM:5,FOG:9,STRONG_WIND:4;vegetation=0.6;crops=1.0;food=1.0;fuel=1.1;trade=1.1;social=1.05;travel=1.0;animals=0.9",
            "WINTER;temp=1;swing=4;weather=SUNNY:26,CLOUDY:30,RAIN:8,STORM:2,FOG:10,SNOW:20,STRONG_WIND:4;vegetation=0.2;crops=0.3;food=1.25;fuel=2.0;trade=0.7;social=0.85;travel=0.7;animals=0.6");

    private final Map<Season, SeasonProfile> profiles = new EnumMap<>(Season.class);
    private final List<String> problems = new ArrayList<>();

    public SeasonTable(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    public static SeasonTable defaults() { return new SeasonTable(List.of()); }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("season line '" + line + "' has no season"); return; }
        Segments s = parsed.get();
        var season = Season.parse(s.id());
        if (season.isEmpty()) { if (report != null) report.add("unknown season " + s.id()); return; }
        SeasonProfile base = profiles.get(season.get());
        Map<WeatherKind, Double> weather = new EnumMap<>(WeatherKind.class);
        if (s.has("weather")) {
            s.weights("weather").forEach((name, value) -> WeatherKind.parse(name).ifPresentOrElse(k -> weather.put(k, value), () -> { if (report != null) report.add(s.id() + ": unknown weather " + name); }));
        } else if (base != null) weather.putAll(base.weather());
        profiles.put(season.get(), new SeasonProfile(season.get(), s.number("temp", base == null ? 12 : base.baseTemperature()), s.number("swing", base == null ? 5 : base.diurnalSwing()), weather,
                s.number("vegetation", base == null ? 1 : base.vegetation()), s.number("crops", base == null ? 1 : base.crops()), s.number("food", base == null ? 1 : base.food()),
                s.number("fuel", base == null ? 1 : base.fuel()), s.number("trade", base == null ? 1 : base.trade()), s.number("social", base == null ? 1 : base.social()),
                s.number("travel", base == null ? 1 : base.travel()), s.number("animals", base == null ? 1 : base.animals())));
        if (report != null) s.problems().forEach(p -> report.add(s.id() + ": " + p));
    }

    public SeasonProfile of(Season season) { return profiles.get(season); }
    public List<String> problems() { return List.copyOf(problems); }
}
