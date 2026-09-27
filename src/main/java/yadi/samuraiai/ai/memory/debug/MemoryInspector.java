package yadi.samuraiai.ai.memory.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.temporal.RelativeTime;

/** Text views over one NPC's memory for the debugger: timeline, one record in full, a manual search, the index and the links. */
public final class MemoryInspector {
    private MemoryInspector() { }

    public static List<String> timeline(MemoryRuntime rt, int limit, long now) {
        List<MemoryRecord> all = rt.all();
        all.sort((a, b) -> Long.compare(b.stamp().gameTime(), a.stamp().gameTime()));
        List<String> lines = new ArrayList<>();
        for (MemoryRecord r : all) {
            if (lines.size() >= limit) break;
            lines.add(RelativeTime.between(r.stamp().gameTime(), now).phrase() + " (dia " + r.stamp().day() + "): " + r.summary() + " s=" + String.format(Locale.ROOT, "%.2f", r.strength()));
        }
        return lines;
    }

    public static List<String> describe(MemoryRecord r, long now) {
        List<String> lines = new ArrayList<>();
        lines.add(r.id() + " " + r.summary());
        lines.add("  tipo=" + r.type() + " categoria=" + r.category() + " episodio=" + r.episode() + " estado=" + r.state() + (r.isProtected() ? " PROTEGIDO" : ""));
        lines.add("  cuando=" + RelativeTime.between(r.stamp().gameTime(), now).phrase() + " dia=" + r.stamp().day() + " clima=" + r.stamp().weather() + " duracion=" + r.duration());
        lines.add("  emocion=" + r.emotion().primary() + " " + String.format(Locale.ROOT, "%.2f", r.emotion().intensity()) + " valencia=" + String.format(Locale.ROOT, "%.2f", r.emotion().valence())
                + (r.emotion().traumatic() ? " TRAUMATICO" : "") + " secundarias=" + r.emotion().secondary());
        lines.add("  fuerza=" + String.format(Locale.ROOT, "%.2f", r.strength()) + " confianza=" + String.format(Locale.ROOT, "%.2f", r.confidence()) + " accesos=" + r.accessCount() + " repeticiones=" + r.repeatCount() + " version=" + r.version());
        lines.add("  origen=" + r.origin().source() + "/" + r.origin().event() + " traza=" + r.origin().traceId());
        lines.add("  tags=" + r.tags() + " eventos=" + r.events() + " consecuencias=" + r.consequences().size() + " capitulos=" + r.chapters().size());
        lines.add("  vinculos: sociales=" + r.socialLinks().size() + " conocimiento=" + r.knowledgeLinks().size());
        return lines;
    }

    /** Manual search: case-insensitive text matched against the kind, the names, the zone, the emotion, the tags and the context. */
    public static List<MemoryRecord> search(MemoryRuntime rt, String text, int limit) {
        String needle = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        List<MemoryRecord> result = new ArrayList<>();
        for (MemoryRecord r : rt.all()) {
            if (result.size() >= limit) break;
            if (haystack(r).contains(needle)) result.add(r);
        }
        return result;
    }

    private static String haystack(MemoryRecord r) {
        StringBuilder sb = new StringBuilder(r.kind().name().toLowerCase(Locale.ROOT)).append(' ').append(r.category().name().toLowerCase(Locale.ROOT)).append(' ')
                .append(r.emotion().primary().name().toLowerCase(Locale.ROOT)).append(' ').append(r.place().zone().toLowerCase(Locale.ROOT)).append(' ');
        if (r.actor() != null) sb.append(r.actor().name().toLowerCase(Locale.ROOT)).append(' ');
        if (r.target() != null) sb.append(r.target().name().toLowerCase(Locale.ROOT)).append(' ');
        for (String t : r.tags()) sb.append(t.toLowerCase(Locale.ROOT)).append(' ');
        r.context().forEach((k, v) -> sb.append(k.toLowerCase(Locale.ROOT)).append('=').append(v.toLowerCase(Locale.ROOT)).append(' '));
        return sb.toString();
    }

    /** A small graph of the memory: each person or place with how many memories mention it. */
    public static List<String> graph(MemoryRuntime rt, int limit) {
        java.util.Map<UUID, int[]> counts = new java.util.HashMap<>();
        java.util.Map<UUID, String> names = new java.util.HashMap<>();
        for (MemoryRecord r : rt.all()) {
            for (var e : new yadi.samuraiai.ai.cognition.model.EntityRef[] {r.actor(), r.target()}) if (e != null) { counts.computeIfAbsent(e.id(), k -> new int[1])[0]++; names.put(e.id(), e.label()); }
        }
        List<java.util.Map.Entry<UUID, int[]>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        List<String> lines = new ArrayList<>();
        for (var entry : entries) { if (lines.size() >= limit) break; lines.add(names.get(entry.getKey()) + " <- " + entry.getValue()[0] + " recuerdos"); }
        return lines;
    }

    public static List<String> indexReport(MemoryRuntime rt) {
        var idx = rt.index();
        return List.of("entidades=" + idx.entityCount(), "celdas=" + idx.cellCount(), "tags=" + idx.tagCount(), "dias=" + idx.days().size(), "registros indexados=" + idx.size(),
                "cache: corta=" + rt.cache().shortSize() + " caliente=" + rt.cache().hot().size() + " tibia=" + rt.cache().warmSize() + " tasa=" + String.format(Locale.ROOT, "%.0f%%", rt.cache().hitRate() * 100));
    }

}
