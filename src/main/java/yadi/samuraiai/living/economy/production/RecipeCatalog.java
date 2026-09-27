package yadi.samuraiai.living.economy.production;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * Production recipes by profession, from lines such as {@code blacksmith;source=CRAFT;in=iron:0.3,coal:0.3;out=tools:0.25;tools=0.002;building=SMITHY}.
 * Every resource that exists has a producer here, so nothing in the economy appears from nothing.
 */
public final class RecipeCatalog {
    public static final List<String> DEFAULT_LINES = List.of(
            "farmer;source=FARM;out=rice:0.9,wheat:0.5;tools=0.003;building=FARM",
            "fisherman;source=WILDLIFE;out=fish:1.2;tools=0.002",
            "hunter;source=WILDLIFE;out=meat:0.6,leather:0.2;tools=0.003",
            "weaver;source=WILDLIFE;out=cloth:0.3;tools=0.001",
            "woodcutter;source=DEPOSIT;out=wood:2.0,bamboo:0.4;tools=0.004",
            "miner;source=DEPOSIT;out=stone:1.8,iron:0.3,coal:0.5,clay:0.4;tools=0.006;building=MINE",
            "herbalist;source=DEPOSIT;out=herbs:0.7",
            "blacksmith;source=CRAFT;in=iron:0.3,coal:0.3;out=tools:0.25;tools=0.002;building=SMITHY",
            "carpenter;source=CRAFT;in=wood:1.0;out=lumber:0.8;tools=0.003;building=CARPENTRY",
            "cook;source=CRAFT;in=rice:0.6,fish:0.3;out=meal:1.2;building=KITCHEN",
            "monk;source=SERVICE", "guard;source=SERVICE", "samurai;source=SERVICE", "merchant;source=SERVICE", "healer;source=SERVICE");

    private final Map<String, Recipe> recipes = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public RecipeCatalog(List<String> lines) {
        for (String l : DEFAULT_LINES) parse(l, null);
        if (lines != null) for (String l : lines) parse(l, problems);
    }

    public static RecipeCatalog defaults() { return new RecipeCatalog(List.of()); }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("recipe line '" + line + "' has no profession"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        Recipe.Source source;
        try { source = Recipe.Source.valueOf(s.text("source", "SERVICE").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { if (report != null) report.add(id + ": unknown source"); source = Recipe.Source.SERVICE; }
        recipes.put(id, new Recipe(id, source, lower(s.weights("out")), lower(s.weights("in")), s.number("tools", 0), s.text("building", "").toUpperCase(Locale.ROOT)));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    private static Map<String, Double> lower(Map<String, Double> m) { Map<String, Double> out = new LinkedHashMap<>(); m.forEach((k, v) -> out.put(k.toLowerCase(Locale.ROOT), v)); return out; }

    public Optional<Recipe> of(String profession) { return Optional.ofNullable(recipes.get(profession == null ? "" : profession.toLowerCase(Locale.ROOT))); }
    public List<Recipe> all() { return List.copyOf(recipes.values()); }
    public List<String> problems() { return List.copyOf(problems); }

    /** The profession whose recipe produces a resource (first found), for "most needed profession". */
    public Optional<String> producerOf(String resource) {
        for (Recipe r : recipes.values()) if (r.outputs().containsKey(resource)) return Optional.of(r.profession());
        return Optional.empty();
    }
}
