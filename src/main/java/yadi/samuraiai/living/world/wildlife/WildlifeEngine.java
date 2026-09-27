package yadi.samuraiai.living.world.wildlife;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.regions.RegionType;

/**
 * Animal populations per region, simulated as aggregates (no entity is spawned or needed): logistic growth towards the
 * region's capacity, scaled by the season's animal activity; predators eat their prey; pests grow with a village's food
 * surplus; people take animals by hunting, fishing and husbandry (the Economy asks through the hub). The logistic step is
 * solved in closed form, so a region asleep for a year costs the same as one simulated yesterday.
 *
 * <p>Species are data: {@code deer;name=Ciervos;habitat=FOREST:120,FIELDS:40;growth=0.02;yield=meat:3,leather:1}.
 */
public final class WildlifeEngine {
    public static final List<String> DEFAULT_LINES = List.of(
            "deer;name=Ciervos;habitat=FOREST:120,FIELDS:50,MOUNTAINS:40;growth=0.02;yield=meat:3,leather:1",
            "wolves;name=Lobos;habitat=FOREST:18,MOUNTAINS:24;growth=0.01;prey=deer;predation=0.15;yield=leather:1;danger=0.6",
            "birds;name=Aves;habitat=FOREST:300,FIELDS:200,RIVER:150,SWAMP:250,COAST:200;growth=0.04",
            "fish;name=Peces;habitat=RIVER:900,COAST:1500,SWAMP:300;growth=0.03;yield=fish:1",
            "livestock;name=Ganado;habitat=VILLAGE:60,FIELDS:80;growth=0.012;yield=meat:2,leather:1,cloth:0.5;domestic=true",
            "rats;name=Ratas;habitat=VILLAGE:200,TEMPLE:40;growth=0.05;pest=true");

    private final Map<String, SpeciesDef> species = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public WildlifeEngine(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("species line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        if (s.text("remove", "false").equalsIgnoreCase("true")) { species.remove(id); return; }
        Map<RegionType, Double> habitat = new EnumMap<>(RegionType.class);
        s.weights("habitat").forEach((name, v) -> RegionType.parse(name).ifPresentOrElse(t -> habitat.put(t, v), () -> { if (report != null) report.add(id + ": unknown region type " + name); }));
        Map<String, Double> yields = new LinkedHashMap<>();
        s.weights("yield").forEach((k, v) -> yields.put(k.toLowerCase(Locale.ROOT), v));
        species.put(id, new SpeciesDef(id, s.text("name", id), habitat, s.number("growth", 0.02), s.text("prey", "").toLowerCase(Locale.ROOT), s.number("predation", 0),
                yields, s.number("danger", 0), s.text("pest", "false").equalsIgnoreCase("true"), s.text("domestic", "false").equalsIgnoreCase("true")));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    public List<SpeciesDef> species() { return List.copyOf(species.values()); }
    public Optional<SpeciesDef> get(String id) { return Optional.ofNullable(species.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public List<String> problems() { return List.copyOf(problems); }

    /** Seeds a region's populations at half their capacity (a new region). */
    public void populate(Region region, long now) {
        for (SpeciesDef def : species.values()) {
            double cap = def.capacity(region.type());
            if (cap <= 0 || region.wildlife().containsKey(def.id())) continue;
            region.wildlife().put(def.id(), new WildlifePopulation(def.id(), cap * 0.5D, cap, now));
        }
        region.markDirty();
    }

    /**
     * Simulates a region's animals up to {@code now}. {@code seasonActivity} is the season's animal multiplier, {@code foodSurplus}
     * how much more food the region's villages hold than they need (1 = balanced), which feeds the pests.
     * Returns the danger the animals add to the region.
     */
    public double simulate(Region region, long now, int minutesPerDay, double seasonActivity, double foodSurplus) {
        for (WildlifePopulation pop : region.wildlife().values()) {
            SpeciesDef def = species.get(pop.species());
            if (def == null) continue;
            double capacity = def.capacity(region.type()) * (def.pest() ? Math.max(0.2D, Math.min(3.0D, foodSurplus)) : 1.0D);
            pop.capacity(capacity);
            double days = (now - pop.updatedAt()) / (double) minutesPerDay;
            if (days <= 0) continue;
            double r = def.growthPerDay() * Math.max(0.1D, seasonActivity);
            pop.set(logistic(pop.count(), capacity, r, days), now);
        }
        // predation after growth: each predator eats a share of its prey over the elapsed time (bounded by the prey there is)
        for (WildlifePopulation predator : region.wildlife().values()) {
            SpeciesDef def = species.get(predator.species());
            if (def == null || def.prey().isEmpty()) continue;
            WildlifePopulation prey = region.wildlife().get(def.prey());
            if (prey == null) continue;
            double eaten = Math.min(prey.count() * 0.5D, predator.count() * def.predationPerDay() * Math.min(30.0D, Math.max(0.0D, (now - region.lastSimulated()) / (double) minutesPerDay)));
            if (eaten > 0) { prey.set(prey.count() - eaten, now); prey.preyed(eaten); }
        }
        region.markDirty();
        return danger(region);
    }

    /** Logistic growth solved exactly over {@code days}. */
    static double logistic(double n, double k, double r, double days) {
        if (k <= 0) return 0.0D;
        if (n <= 0) return 0.0D;
        double e = Math.exp(-r * days);
        return k / (1.0D + ((k - n) / n) * e);
    }

    public double danger(Region region) {
        double danger = 0;
        for (WildlifePopulation pop : region.wildlife().values()) {
            SpeciesDef def = species.get(pop.species());
            if (def != null) danger += def.dangerPer100() * pop.count() / 100.0D;
        }
        return Math.min(1.0D, danger);
    }

    /** People take up to {@code animals} of a species (never more than a sustainable share). Returns animals really taken. */
    public double take(Region region, String speciesId, double animals, double maxShare) {
        WildlifePopulation pop = region.wildlife().get(speciesId);
        if (pop == null || animals <= 0) return 0.0D;
        double limit = pop.count() * Math.max(0.0D, Math.min(1.0D, maxShare));
        double taken = pop.take(Math.min(animals, limit));
        if (taken > 0) region.markDirty();
        return taken;
    }

    /** The species of a region that yield a resource, best first. */
    public List<String> sourcesOf(Region region, String resource) {
        List<String> out = new ArrayList<>();
        for (WildlifePopulation pop : region.wildlife().values()) {
            SpeciesDef def = species.get(pop.species());
            if (def != null && def.yields().containsKey(resource) && pop.count() > 0) out.add(def.id());
        }
        out.sort((a, b) -> Double.compare(region.wildlife().get(b).count(), region.wildlife().get(a).count()));
        return out;
    }
}
