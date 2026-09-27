package yadi.samuraiai.ai.knowledge.culture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The cultures of the world, parsed from configuration lines {@code id|name|traditions|norms} where traditions are
 * {@code name~KIND~PERIOD~ZONEKIND~strength} separated by {@code ;} and norms are {@code KEY~value} separated by {@code ;}.
 * With no configured lines the built-in cultures apply.
 */
public final class CultureCatalog {
    public static final List<String> BUILT_IN = List.of(
            "village|Cultura de aldea|Saludo cortés~GREETING~ANY~ANY~0.6;Mercado del día~CEREMONY~MORNING~MARKET~0.5|HONOR_SCALE~1.0;OATH_WEIGHT~1.0",
            "temple|Cultura del templo|Meditación al amanecer~RITUAL~DAWN~TEMPLE~0.9;Saludo con reverencia~GREETING~ANY~ANY~0.8|HONOR_SCALE~1.3;OATH_WEIGHT~1.5",
            "market|Cultura de mercado|Regateo matinal~CEREMONY~MORNING~MARKET~0.7|HONOR_SCALE~0.9;OATH_WEIGHT~1.0",
            "guard|Cultura de guardia|Relevo de guardia~RITUAL~DAWN~GUARD_POST~0.8;Saludo militar~GREETING~ANY~ANY~0.7|HONOR_SCALE~1.4;OATH_WEIGHT~1.6");

    private final Map<String, Culture> cultures = new LinkedHashMap<>();

    public CultureCatalog(List<String> lines) {
        for (String line : lines.isEmpty() ? BUILT_IN : lines) parse(line).ifPresent(c -> cultures.put(c.id(), c));
        if (cultures.isEmpty()) for (String line : BUILT_IN) parse(line).ifPresent(c -> cultures.put(c.id(), c));
    }

    public Optional<Culture> get(String id) { return Optional.ofNullable(cultures.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public Culture orDefault(String id) { return get(id).orElseGet(() -> cultures.values().iterator().next()); }
    public List<Culture> all() { return List.copyOf(cultures.values()); }

    static Optional<Culture> parse(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 3) return Optional.empty();
            String id = parts[0].trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) return Optional.empty();
            List<Tradition> traditions = new ArrayList<>();
            for (String t : parts[2].split(";")) {
                String[] f = t.split("~");
                if (f.length < 5) continue;
                traditions.add(new Tradition(id + "." + f[0].trim().toLowerCase(Locale.ROOT).replace(' ', '-'), f[0].trim(), Tradition.Kind.parse(f[1]), f[2], f[3], Double.parseDouble(f[4].trim())));
            }
            Map<String, Double> norms = new LinkedHashMap<>();
            if (parts.length > 3) for (String n : parts[3].split(";")) {
                String[] f = n.split("~");
                if (f.length == 2) norms.put(f[0].trim().toUpperCase(Locale.ROOT), Double.parseDouble(f[1].trim()));
            }
            return Optional.of(new Culture(id, parts[1].trim(), traditions, norms));
        } catch (RuntimeException e) { return Optional.empty(); }
    }
}
