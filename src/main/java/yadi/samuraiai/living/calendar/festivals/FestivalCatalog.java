package yadi.samuraiai.living.calendar.festivals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.MoonPhase;

/**
 * The festivals of Deiliora, from lines such as
 * {@code tsukimi;name=Tsukimi (Festival de la Luna);month=8;day=12;days=1;moon=FULL;window=6;bias=PRAYER:25,SOCIAL:30;demand=RICE:1.2;social=1.3;tags=moon}.
 * The five built-in festivals are ordinary lines; configured lines replace a festival with the same id or add new ones.
 */
public final class FestivalCatalog {
    public static final List<String> DEFAULT_LINES = List.of(
            "hanami;name=Hanami;month=2;day=10;days=4;bias=SOCIAL:40,PRAYER:10,WORK:-15,TRAINING:-10;demand=RICE:1.25,CLOTH:1.2,HERBS:1.1;social=1.5;tags=spring,flowers,festival",
            "natsu_matsuri;name=Festival de Verano;month=5;day=20;days=3;bias=SOCIAL:45,MERCHANT:20,WORK:-20,PATROL:5;demand=FISH:1.3,RICE:1.2,CLOTH:1.15;social=1.6;tags=summer,night,festival",
            "tsukimi;name=Festival de la Luna;month=8;day=12;days=1;moon=FULL;window=6;bias=PRAYER:30,MEDITATE:20,SOCIAL:25,WORK:-10;demand=RICE:1.2,HERBS:1.1;social=1.3;tags=autumn,moon,festival",
            "aki_matsuri;name=Festival de la Cosecha;month=9;day=5;days=3;bias=SOCIAL:40,PRAYER:20,MERCHANT:25,WORK:-10;demand=RICE:1.3,WHEAT:1.2,MEAT:1.2;social=1.5;tags=autumn,harvest,festival",
            "fuyu_matsuri;name=Festival de Invierno;month=11;day=20;days=3;bias=SOCIAL:35,PRAYER:25,REST:10,WORK:-15,PATROL:-5;demand=WOOD:1.3,RICE:1.15,CLOTH:1.2;social=1.3;tags=winter,fire,festival");

    private final Map<String, FestivalDef> festivals = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public FestivalCatalog(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    public static FestivalCatalog defaults() { return new FestivalCatalog(List.of()); }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("festival line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        if (s.text("remove", "false").equalsIgnoreCase("true")) { festivals.remove(id); return; }
        MoonPhase.Family moon = null;
        if (s.has("moon")) {
            try { moon = MoonPhase.Family.valueOf(s.text("moon", "").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { if (report != null) report.add(id + ": unknown moon " + s.text("moon", "")); }
        }
        Set<String> cultures = new HashSet<>();
        for (String c : s.list("cultures")) cultures.add(c.toLowerCase(Locale.ROOT));
        festivals.put(id, new FestivalDef(id, s.text("name", id), s.integer("month", 1), s.integer("day", 1), s.integer("days", 1), moon, s.integer("window", 5),
                s.weights("bias"), lower(s.weights("demand")), s.number("social", 1.0D), cultures, s.list("tags")));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    private static Map<String, Double> lower(Map<String, Double> in) {
        Map<String, Double> out = new LinkedHashMap<>();
        in.forEach((k, v) -> out.put(k.toLowerCase(Locale.ROOT), v));
        return out;
    }

    public List<FestivalDef> all() { return List.copyOf(festivals.values()); }
    public java.util.Optional<FestivalDef> get(String id) { return java.util.Optional.ofNullable(festivals.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public List<String> problems() { return List.copyOf(problems); }
}
