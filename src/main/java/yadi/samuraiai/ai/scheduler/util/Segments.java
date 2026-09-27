package yadi.samuraiai.ai.scheduler.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parser for the one-line data format used by the scheduler's configurable catalogues (lifestyles, calendar, routine
 * overrides): {@code id;key=value;key=value}. Malformed pieces are reported, never thrown, so a typo in the configuration
 * costs one entry instead of the whole catalogue.
 */
public final class Segments {
    private final String id;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    private Segments(String id) { this.id = id; }

    public static Optional<Segments> parse(String line) {
        if (line == null) return Optional.empty();
        String[] parts = line.trim().split(";");
        if (parts.length == 0 || parts[0].isBlank() || parts[0].contains("=")) return Optional.empty();
        Segments result = new Segments(parts[0].trim());
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            int eq = part.indexOf('=');
            if (eq <= 0) { result.problems.add("'" + part + "' is not key=value"); continue; }
            result.values.put(part.substring(0, eq).trim().toLowerCase(Locale.ROOT), part.substring(eq + 1).trim());
        }
        return Optional.of(result);
    }

    public String id() { return id; }
    public boolean has(String key) { return values.containsKey(key.toLowerCase(Locale.ROOT)); }
    public String text(String key, String fallback) { return values.getOrDefault(key.toLowerCase(Locale.ROOT), fallback); }
    public Map<String, String> all() { return Map.copyOf(values); }
    public List<String> problems() { return List.copyOf(problems); }

    public double number(String key, double fallback) {
        String raw = values.get(key.toLowerCase(Locale.ROOT));
        if (raw == null) return fallback;
        try { return Double.parseDouble(raw); } catch (NumberFormatException e) { problems.add(key + "='" + raw + "' is not a number"); return fallback; }
    }

    public int integer(String key, int fallback) { return (int) Math.round(number(key, fallback)); }

    /** {@code a,b,c} as trimmed, non-empty strings. */
    public List<String> list(String key) {
        String raw = values.get(key.toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String item : raw.split(",")) if (!item.isBlank()) out.add(item.trim());
        return out;
    }

    /** {@code A:1.5,B:2} as name to number; entries that do not parse are reported and skipped. */
    public Map<String, Double> weights(String key) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (String item : list(key)) {
            int colon = item.lastIndexOf(':');
            if (colon <= 0) { problems.add(key + " entry '" + item + "' is not name:number"); continue; }
            try { out.put(item.substring(0, colon).trim().toUpperCase(Locale.ROOT), Double.parseDouble(item.substring(colon + 1).trim())); }
            catch (NumberFormatException e) { problems.add(key + " entry '" + item + "' has no number"); }
        }
        return out;
    }
}
