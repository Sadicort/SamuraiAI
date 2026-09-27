package yadi.samuraiai.living.world.professions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The professions of Deiliora, from lines such as
 * {@code blacksmith;name=Herrero;tasks=forjar,reparar;tools=tools;locations=SMITHY;work=WORK;bias=WORK:25,SOCIAL:-5;types=blacksmith;xp=1.0}.
 * The spec's initial professions plus the gatherers the economy needs so that no resource appears without a producer.
 */
public final class ProfessionCatalog {
    public static final List<String> DEFAULT_LINES = List.of(
            "farmer;name=Campesino;tasks=sembrar,regar,cosechar;tools=tools;locations=FARM,HOUSE;work=WORK;bias=WORK:20,WAKE:10;types=farmer,villager;xp=1.0",
            "fisherman;name=Pescador;tasks=pescar,remendar redes;tools=tools;locations=DOCK,MARKET;work=WORK;bias=WORK:20,WAKE:15;types=fisherman;xp=1.0",
            "blacksmith;name=Herrero;tasks=forjar,reparar,templar;tools=tools;locations=SMITHY;work=WORK;bias=WORK:25;types=blacksmith;xp=1.0",
            "carpenter;name=Carpintero;tasks=serrar,construir,reparar;tools=tools;locations=CARPENTRY,WAREHOUSE;work=WORK;bias=WORK:20;types=carpenter;xp=1.0",
            "merchant;name=Mercader;tasks=vender,comprar,viajar;tools=;locations=MARKET,WAREHOUSE;work=MERCHANT;bias=MERCHANT:25,SOCIAL:5;types=merchant,trader;xp=1.0",
            "monk;name=Monje;tasks=rezar,meditar,enseñar;tools=;locations=TEMPLE;work=PRAYER;bias=PRAYER:15,MEDITATE:15;types=monk,priest;xp=0.8",
            "guard;name=Guardia;tasks=patrullar,vigilar,entrenar;tools=tools;locations=GATE,GUARD_POST,DOJO;work=GUARD;bias=GUARD:15,PATROL:15;types=guard;xp=0.8",
            "samurai;name=Samurái;tasks=entrenar,proteger,servir;tools=tools;locations=DOJO,GUARD_POST;work=TRAINING;bias=TRAINING:20,PATROL:10;types=samurai,ronin;xp=0.8",
            "cook;name=Cocinero;tasks=cocinar,servir;tools=;locations=KITCHEN,MARKET;work=WORK;bias=WORK:20,EAT:5;types=cook;xp=1.0",
            "woodcutter;name=Leñador;tasks=talar,acarrear;tools=tools;locations=OUTSKIRTS,WAREHOUSE;work=WORK;bias=WORK:20;types=woodcutter;xp=1.0",
            "miner;name=Minero;tasks=picar,acarrear;tools=tools;locations=MINE,WAREHOUSE;work=WORK;bias=WORK:20;types=miner;xp=1.0",
            "hunter;name=Cazador;tasks=rastrear,cazar,curtir;tools=tools;locations=OUTSKIRTS;work=WORK;bias=WORK:15,PATROL:5;types=hunter;xp=1.0",
            "herbalist;name=Herbolario;tasks=recolectar,secar hierbas;tools=;locations=OUTSKIRTS,TEMPLE;work=WORK;bias=WORK:15;types=herbalist;xp=1.0",
            "weaver;name=Tejedor;tasks=hilar,tejer;tools=;locations=HOUSE,WORKSHOP;work=WORK;bias=WORK:20;types=weaver;xp=1.0",
            "healer;name=Sanador;tasks=curar,preparar remedios;tools=;locations=TEMPLE,HOUSE;work=WORK;bias=WORK:10,PRAYER:5;types=healer;xp=1.0;future=true");

    private final Map<String, ProfessionDef> professions = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public ProfessionCatalog(List<String> lines) {
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    public static ProfessionCatalog defaults() { return new ProfessionCatalog(List.of()); }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("profession line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        if (s.text("remove", "false").equalsIgnoreCase("true")) { professions.remove(id); return; }
        Set<String> locations = new HashSet<>();
        for (String l : s.list("locations")) locations.add(l.toUpperCase(Locale.ROOT));
        Set<String> types = new HashSet<>();
        for (String t : s.list("types")) types.add(t.toLowerCase(Locale.ROOT));
        List<String> tools = new ArrayList<>();
        for (String t : s.list("tools")) tools.add(t.toLowerCase(Locale.ROOT));
        professions.put(id, new ProfessionDef(id, s.text("name", id), s.list("tasks"), tools, locations, s.text("work", "WORK").toUpperCase(Locale.ROOT), s.weights("bias"),
                types, s.number("xp", 1.0), s.text("future", "false").equalsIgnoreCase("true"), s.text("lifestyle", "")));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    public List<ProfessionDef> all() { return List.copyOf(professions.values()); }
    public Optional<ProfessionDef> get(String id) { return Optional.ofNullable(professions.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public List<String> problems() { return List.copyOf(problems); }

    /** The profession an NPC type takes up by default (a guard NPC is a guard), or empty. */
    public Optional<ProfessionDef> forNpcType(String npcType) {
        String t = npcType == null ? "" : npcType.toLowerCase(Locale.ROOT);
        for (ProfessionDef p : professions.values()) if (p.npcTypes().contains(t) && !t.equals("villager")) return Optional.of(p);
        return Optional.empty();
    }
}
