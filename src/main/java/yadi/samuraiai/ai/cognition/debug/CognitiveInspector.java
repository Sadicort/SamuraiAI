package yadi.samuraiai.ai.cognition.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.engine.CognitionEngine;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.cognition.trace.TraceStep;
import yadi.samuraiai.ai.emotion.debug.EmotionInspector;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.knowledge.debug.KnowledgeInspector;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.memory.debug.MemoryInspector;
import yadi.samuraiai.ai.memory.diagnostics.MemoryDiagnostics;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.relationship.debug.SocialInspector;
import yadi.samuraiai.ai.relationship.engine.RelationshipRuntime;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/** The unified cognitive inspector: everything the specification lists for one NPC (memory, knowledge, relationships, emotions, society, storage, cost) as text lines. */
public final class CognitiveInspector {
    private CognitiveInspector() { }

    /** @param extras what only the world adapter knows (name, current goal, behavior, routine), shown first */
    public static List<String> inspect(CognitionEngine engine, UUID npc, long now, Map<String, String> extras) {
        List<String> lines = new ArrayList<>();
        extras.forEach((k, v) -> lines.add(k + ": " + v));
        lines.add("UUID: " + npc);
        lines.add("-- Personalidad --");
        var ledger = engine.ledgers().get(npc);
        if (ledger != null) {
            StringBuilder sb = new StringBuilder();
            for (Trait t : Trait.values()) sb.append(t.name().toLowerCase(Locale.ROOT)).append('=').append(Math.round(ledger.base(t))).append(Math.abs(ledger.evolution(t)) >= 0.05D ? String.format(Locale.ROOT, "(%+.1f)", ledger.evolution(t)) : "").append(' ');
            lines.add(sb.toString().trim());
        }
        MemoryRuntime mem = engine.memory().peek(npc).orElse(null);
        lines.add("-- Memoria --");
        if (mem == null) lines.add("sin memoria cargada");
        else {
            var d = MemoryDiagnostics.snapshot(mem);
            lines.add("recuerdos=" + d.count() + " temporales=" + d.temporary() + " protegidos=" + d.protectedCount() + " comprimidos=" + d.compressed() + " fuerza media=" + String.format(Locale.ROOT, "%.2f", d.averageStrength())
                    + " nodos espaciales=" + d.spatialNodes() + " habilidades=" + d.skills() + " creencias semánticas=" + d.beliefs());
            List<MemoryRecord> all = mem.all();
            all.sort((a, b) -> Long.compare(b.stamp().gameTime(), a.stamp().gameTime()));
            lines.add("recientes:");
            for (int i = 0; i < Math.min(3, all.size()); i++) lines.add("  " + all.get(i).summary());
            lines.add("críticos:");
            int shown = 0;
            for (MemoryRecord r : all) if (r.importance().atLeast(Importance.CRITICAL) && shown++ < 3) lines.add("  " + r.summary());
            lines.add("calientes: " + mem.cache().hot().size() + " tibios: " + mem.cache().warmSize() + " tasa de caché " + String.format(Locale.ROOT, "%.0f%%", mem.cache().hitRate() * 100) + " almacenamiento: " + mem.storageState());
        }
        lines.add("-- Conocimiento --");
        KnowledgeRuntime kr = engine.knowledge().peek(npc).orElse(null);
        if (kr == null) lines.add("sin conocimiento cargado");
        else {
            lines.addAll(KnowledgeInspector.summary(kr));
            lines.add("lugares conocidos: " + kr.ofType(KnowledgeType.PLACE).size() + " personas: " + kr.ofType(KnowledgeType.PERSON).size());
        }
        lines.add("-- Relaciones --");
        RelationshipRuntime rr = engine.relationships().peek(npc).orElse(null);
        if (rr == null || rr.size() == 0) lines.add("sin relaciones");
        else {
            for (RelationshipRecord r : rr.all()) lines.add(SocialInspector.line(r, engine.relationships().settings()));
            lines.addAll(SocialInspector.promises(rr.promises().values()));
            lines.addAll(SocialInspector.reputation(rr.reputation().all()));
        }
        lines.add("-- Emociones --");
        EmotionRuntime er = engine.emotions().peek(npc).orElse(null);
        if (er == null) lines.add("sin emociones cargadas");
        else {
            lines.addAll(EmotionInspector.summary(er, now));
            lines.addAll(EmotionInspector.chart(er));
            lines.addAll(EmotionInspector.traumas(er));
        }
        lines.add("-- Sociedad --");
        var communities = engine.society().communitiesOf(npc);
        lines.add("comunidades=" + communities + " rumores oídos=" + (kr == null ? 0 : kr.rumors().size()));
        for (String id : communities) engine.society().community(id).ifPresent(c -> lines.add("  " + c.id() + " cultura=" + c.cultureId() + " rango=" + c.rankOf(npc)));
        lines.add("-- Rastro reciente --");
        for (TraceStep s : engine.trace().recent(npc, 6)) lines.add("  " + s.stage() + ": " + s.detail());
        var m = engine.metrics().snapshot();
        lines.add("coste: experiencia " + String.format(Locale.ROOT, "%.1f", m.experienceMicros()) + " µs, tick de mente " + String.format(Locale.ROOT, "%.1f", m.tickMicros()) + " µs");
        return lines;
    }
}
