package yadi.samuraiai.living.calendar.weather;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * The microclimates, parsed from lines such as {@code mountain;temp=-4;lapse=0.04;weather=SNOW:1.8,STRONG_WIND:2,FOG:1.3}.
 * {@code temperate} always exists and is the fallback for an unknown id.
 */
public final class ClimateTable {
    public static final List<String> DEFAULT_LINES = List.of(
            "temperate;temp=0;lapse=0.03",
            "forest;temp=-0.5;lapse=0.03;weather=FOG:1.2,STRONG_WIND:0.6",
            "mountain;temp=-4;lapse=0.04;weather=SNOW:1.8,STRONG_WIND:2.0,FOG:1.3,SUNNY:0.9",
            "coastal;temp=1;lapse=0.03;weather=RAIN:1.2,FOG:1.4,STORM:1.3",
            "river;temp=0;lapse=0.03;weather=FOG:1.5",
            "swamp;temp=2;lapse=0.03;weather=FOG:2.0,RAIN:1.3,SUNNY:0.8",
            "fields;temp=0.5;lapse=0.03;weather=STRONG_WIND:1.3");

    private final Map<String, ClimateProfile> climates = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public ClimateTable(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    public static ClimateTable defaults() { return new ClimateTable(List.of()); }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("climate line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        Map<WeatherKind, Double> scale = new EnumMap<>(WeatherKind.class);
        s.weights("weather").forEach((name, value) -> WeatherKind.parse(name).ifPresentOrElse(k -> scale.put(k, value), () -> { if (report != null) report.add(s.id() + ": unknown weather " + name); }));
        String id = s.id().toLowerCase(Locale.ROOT);
        climates.put(id, new ClimateProfile(id, s.number("temp", 0), scale, s.number("lapse", 0.03), s.integer("sealevel", 63)));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    public ClimateProfile of(String id) {
        ClimateProfile c = id == null ? null : climates.get(id.toLowerCase(Locale.ROOT));
        return c != null ? c : climates.get("temperate");
    }

    public boolean has(String id) { return id != null && climates.containsKey(id.toLowerCase(Locale.ROOT)); }
    public List<String> ids() { return List.copyOf(climates.keySet()); }
    public List<String> problems() { return List.copyOf(problems); }
}
