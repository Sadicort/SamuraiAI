package yadi.samuraiai.living.world.regions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.Dice;

/**
 * What each type of region holds and how it is named. Deposit lines look like
 * {@code MOUNTAINS;stone=5000:50;iron=800:4;coal=1200:6;danger=0.15} (capacity:regeneration per day). Names are built from a
 * stem and a type suffix, deterministically from the region key, so the same region always gets the same name.
 */
public final class RegionCatalog {
    public record DepositSpec(String resource, double capacity, double regenPerDay) { }

    public static final List<String> DEFAULT_LINES = List.of(
            "MOUNTAINS;stone=6000:60;iron=900:4;coal=1400:7;herbs=150:6;water=2000:200;danger=0.15",
            "FOREST;wood=4000:45;herbs=500:15;bamboo=900:30;water=1500:150;danger=0.08",
            "VILLAGE;water=3000:400;wood=400:6;stone=300:3;clay=200:4;danger=0.02",
            "TEMPLE;water=2000:250;herbs=250:8;wood=300:5;danger=0.01",
            "FIELDS;clay=700:6;water=2000:250;herbs=200:8;wood=300:4;danger=0.03",
            "RIVER;water=10000:2000;clay=900:9;bamboo=400:15;danger=0.04",
            "SWAMP;herbs=700:22;clay=1200:10;bamboo=300:10;water=4000:500;danger=0.12",
            "COAST;water=2000:200;clay=300:3;stone=500:4;danger=0.05",
            "RUINS;stone=1500:0;iron=150:0;danger=0.25");

    public static final List<String> STEMS = List.of("Kuro", "Shira", "Aka", "Ao", "Kita", "Minami", "Higashi", "Nishi", "Taka", "Naga", "Oka", "Mori", "Kawa", "Yama",
            "Sakura", "Matsu", "Kaze", "Tsuki", "Hoshi", "Kiri", "Iwa", "Suzu", "Fuji", "Kage", "Asa", "Yoru", "Hana", "Take", "Umi", "Kumo");

    private final Map<RegionType, List<DepositSpec>> deposits = new EnumMap<>(RegionType.class);
    private final Map<RegionType, Double> danger = new EnumMap<>(RegionType.class);
    private final List<String> problems = new ArrayList<>();

    public RegionCatalog(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("region line '" + line + "' has no type"); return; }
        Segments s = parsed.get();
        var type = RegionType.parse(s.id());
        if (type.isEmpty()) { if (report != null) report.add("unknown region type " + s.id()); return; }
        List<DepositSpec> list = new ArrayList<>();
        for (var e : s.all().entrySet()) {
            if (e.getKey().equals("danger")) continue;
            String[] p = e.getValue().split(":");
            try { list.add(new DepositSpec(e.getKey().toLowerCase(Locale.ROOT), Double.parseDouble(p[0]), p.length > 1 ? Double.parseDouble(p[1]) : 0)); }
            catch (NumberFormatException ex) { if (report != null) report.add(s.id() + ": bad deposit " + e.getKey() + "=" + e.getValue()); }
        }
        deposits.put(type.get(), list);
        danger.put(type.get(), s.number("danger", 0.05));
    }

    public List<DepositSpec> depositsOf(RegionType type) { return deposits.getOrDefault(type, List.of()); }
    public double dangerOf(RegionType type) { return danger.getOrDefault(type, 0.05D); }
    public List<String> problems() { return List.copyOf(problems); }

    /** Gives a new region its deposits (full) and base danger. */
    public void endow(Region region, long now) {
        for (DepositSpec d : depositsOf(region.type()))
            region.deposits().computeIfAbsent(d.resource(), k -> new ResourceDeposit(d.resource(), d.capacity(), d.capacity(), d.regenPerDay(), now));
        region.baseDanger(dangerOf(region.type()));
    }

    public static String name(String key, RegionType type, Dice dice) {
        String stem = STEMS.get(dice.below("region-name:" + key, 0, STEMS.size()));
        String suffix = switch (type) {
            case MOUNTAINS -> "yama (montes)"; case FOREST -> "mori (bosque)"; case VILLAGE -> "no sato (tierras habitadas)"; case TEMPLE -> "dera (tierras sagradas)";
            case FIELDS -> "hara (llanura)"; case RIVER -> "gawa (ribera)"; case SWAMP -> "numa (ciénaga)"; case COAST -> "hama (costa)"; case RUINS -> "ato (ruinas)";
        };
        return stem + suffix;
    }
}
