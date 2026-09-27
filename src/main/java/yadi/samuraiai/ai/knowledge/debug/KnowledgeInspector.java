package yadi.samuraiai.ai.knowledge.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.graph.KnowledgeGraph;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.knowledge.society.Community;

/** Text views over knowledge and society: the beliefs of an NPC, its graph, rumours with their propagation chain, communities, culture and history. */
public final class KnowledgeInspector {
    private KnowledgeInspector() { }

    public static List<String> summary(KnowledgeRuntime rt) {
        return List.of("conocimientos=" + rt.size() + " verificados=" + rt.countIn(ValidationState.VERIFIED) + " probables=" + rt.countIn(ValidationState.LIKELY) + " rumores=" + rt.countIn(ValidationState.RUMOR)
                + " falsos=" + rt.countIn(ValidationState.FALSE), "sujetos=" + rt.subjects() + " rumores oídos=" + rt.rumors().size());
    }

    public static List<String> records(KnowledgeRuntime rt, int limit) {
        List<KnowledgeRecord> all = rt.all();
        all.sort((a, b) -> Double.compare(b.confidence() * (0.5D + b.importance()), a.confidence() * (0.5D + a.importance())));
        List<String> lines = new ArrayList<>();
        for (KnowledgeRecord r : all) { if (lines.size() >= limit) break; lines.add(r.type() + " " + r.summary() + (r.source() == null ? "" : " <- " + r.source().label()) + " [" + r.origin() + "]"); }
        return lines;
    }

    public static List<String> graph(KnowledgeRuntime rt, UUID node, int limit) {
        KnowledgeGraph graph = new KnowledgeGraph(rt, limit);
        List<String> lines = new ArrayList<>();
        for (var e : graph.outgoing(node, null)) lines.add(e.predicate() + " -> " + e.record().object().label() + " (" + Math.round(e.record().confidence() * 100) + "%)");
        for (var e : graph.incoming(node, null)) lines.add(e.record().subject().label() + " " + e.predicate() + " -> este nodo");
        return lines;
    }

    public static List<String> rumor(RumorRecord r) {
        List<String> lines = new ArrayList<>();
        lines.add(r.id().toString().substring(0, 8) + " " + r.state() + " " + r.claim().subject().label() + " " + r.claim().predicate() + (r.claim().object() == null ? "" : " " + r.claim().object().label())
                + String.format(Locale.ROOT, " magnitud=%.2f (inicial %.2f) fuerza=%.2f", r.claim().magnitude(), r.initialMagnitude(), r.strength()));
        lines.add("  origen=" + r.origin().label() + " memoria=" + r.originMemory() + " portadores=" + r.holders().size() + " saltos=" + r.hops().size());
        for (var h : r.hops()) lines.add("  salto " + h.from().toString().substring(0, 8) + " -> " + h.to().toString().substring(0, 8) + String.format(Locale.ROOT, " credibilidad=%.2f", h.credibility()) + (h.transformed() ? " (transformado)" : ""));
        for (var t : r.transformations()) lines.add("  transformación salto " + t.hop() + ": " + t.note() + String.format(Locale.ROOT, " %.2f -> %.2f", t.before(), t.after()));
        return lines;
    }

    public static List<String> community(Community c) {
        List<String> lines = new ArrayList<>();
        lines.add(c.id() + " (" + c.kind() + ", cultura " + c.cultureId() + ") miembros=" + c.members().size() + " conocimiento colectivo=" + c.collective().size() + " historia=" + c.history().size() + " tradiciones=" + c.traditions().size());
        c.traditions().forEach((k, t) -> lines.add("  tradición " + k + String.format(Locale.ROOT, " fuerza=%.2f observada=%d", t.strength(), t.observed())));
        return lines;
    }

    public static List<String> history(List<HistoricalEvent> events, int limit) {
        List<String> lines = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && lines.size() < limit; i--) { HistoricalEvent e = events.get(i); lines.add("t=" + e.at() + " " + e.type() + String.format(Locale.ROOT, " importancia=%.2f", e.significance()) + " " + e.kind() + " " + e.community()); }
        return lines;
    }
}
