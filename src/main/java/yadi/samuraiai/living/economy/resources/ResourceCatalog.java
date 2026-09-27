package yadi.samuraiai.living.economy.resources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The resources of Deiliora, from lines such as
 * {@code rice;name=Arroz;category=FOOD;rarity=0.1;weight=1;value=2;spoil=0.003;food=1;seasons=AUTUMN;producers=farmer;consumers=people,cook}.
 * The specification's initial resources plus the three the professions make (tools, lumber, meals).
 */
public final class ResourceCatalog {
    public static final List<String> DEFAULT_LINES = List.of(
            "rice;name=Arroz;category=FOOD;rarity=0.1;weight=1;value=2;spoil=0.002;food=1.0;seasons=AUTUMN;producers=farmer;consumers=people,cook",
            "wheat;name=Trigo;category=FOOD;rarity=0.15;weight=1;value=1.8;spoil=0.002;food=0.9;seasons=SUMMER;producers=farmer;consumers=people",
            "fish;name=Pescado;category=FOOD;rarity=0.2;weight=1;value=2.5;spoil=0.08;food=1.1;producers=fisherman;consumers=people,cook",
            "meat;name=Carne;category=FOOD;rarity=0.3;weight=1;value=3.5;spoil=0.06;food=1.4;producers=hunter;consumers=people",
            "meal;name=Comida preparada;category=FOOD;rarity=0.2;weight=0.8;value=3;spoil=0.25;food=1.2;producers=cook;consumers=people",
            "water;name=Agua;category=WATER;rarity=0;weight=1;value=0.05;spoil=0;producers=well;consumers=people",
            "wood;name=Madera;category=FUEL;rarity=0.05;weight=2;value=1;spoil=0;fuel=1.0;producers=woodcutter;consumers=people,carpenter",
            "coal;name=Carbón;category=FUEL;rarity=0.3;weight=1.5;value=3;spoil=0;fuel=2.5;producers=miner;consumers=blacksmith",
            "iron;name=Hierro;category=MATERIAL;rarity=0.5;weight=3;value=8;spoil=0;producers=miner;consumers=blacksmith",
            "stone;name=Piedra;category=MATERIAL;rarity=0.05;weight=4;value=0.8;spoil=0;producers=miner;consumers=construction",
            "cloth;name=Tela;category=TEXTILE;rarity=0.35;weight=0.5;value=6;spoil=0;producers=weaver;consumers=people",
            "leather;name=Cuero;category=TEXTILE;rarity=0.35;weight=1;value=5;spoil=0.002;producers=hunter;consumers=people,blacksmith",
            "bamboo;name=Bambú;category=MATERIAL;rarity=0.1;weight=1;value=1.2;spoil=0;producers=woodcutter;consumers=carpenter",
            "herbs;name=Hierbas;category=MEDICINE;rarity=0.3;weight=0.2;value=4;spoil=0.03;seasons=SPRING,SUMMER;producers=herbalist;consumers=people,healer,monk",
            "clay;name=Arcilla;category=MATERIAL;rarity=0.1;weight=3;value=0.8;spoil=0;producers=miner;consumers=construction",
            "tools;name=Herramientas;category=TOOL;rarity=0.4;weight=2;value=15;spoil=0;producers=blacksmith;consumers=workers",
            "lumber;name=Tablones;category=MATERIAL;rarity=0.15;weight=2;value=2.5;spoil=0;producers=carpenter;consumers=construction",
            "gold;name=Oro;category=LUXURY;rarity=0.95;weight=0.5;value=120;spoil=0;future=true",
            "silver;name=Plata;category=LUXURY;rarity=0.85;weight=0.5;value=60;spoil=0;future=true");

    private final Map<String, ResourceDef> resources = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public ResourceCatalog(List<String> lines) {
        for (String l : DEFAULT_LINES) parse(l, null);
        if (lines != null) for (String l : lines) parse(l, problems);
    }

    public static ResourceCatalog defaults() { return new ResourceCatalog(List.of()); }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("resource line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        var category = ResourceDef.Category.parse(s.text("category", "MATERIAL"));
        if (category.isEmpty() && report != null) report.add(id + ": unknown category " + s.text("category", ""));
        List<String> seasons = new ArrayList<>();
        for (String x : s.list("seasons")) seasons.add(x.toUpperCase(Locale.ROOT));
        resources.put(id, new ResourceDef(id, s.text("name", id), category.orElse(ResourceDef.Category.MATERIAL), s.number("rarity", 0.2), s.number("weight", 1), s.number("value", 1),
                s.number("spoil", 0), seasons, lower(s.list("producers")), lower(s.list("consumers")), s.number("food", 0), s.number("fuel", 0), s.text("future", "false").equalsIgnoreCase("true")));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    private static List<String> lower(List<String> l) { List<String> out = new ArrayList<>(); for (String x : l) out.add(x.toLowerCase(Locale.ROOT)); return out; }

    public Optional<ResourceDef> get(String id) { return Optional.ofNullable(resources.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public ResourceDef require(String id) { return get(id).orElseThrow(() -> new IllegalArgumentException("unknown resource " + id)); }
    public List<ResourceDef> all() { return List.copyOf(resources.values()); }
    public List<ResourceDef> byCategory(ResourceDef.Category c) { return resources.values().stream().filter(r -> r.category() == c).toList(); }
    public List<String> problems() { return List.copyOf(problems); }
}
