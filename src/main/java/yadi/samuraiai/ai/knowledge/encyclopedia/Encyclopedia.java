package yadi.samuraiai.ai.knowledge.encyclopedia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/**
 * The NPC's internal encyclopedia: fast lookups of what it believes by name, by category (people, places, objects, creatures,
 * factions, events, traditions) and by prefix. It is a view over the knowledge indexes, so the Brain can consult it every decision
 * without cost. Entries are ranked by how sure the NPC is and how important the topic seems.
 */
public final class Encyclopedia {
    private final KnowledgeRuntime rt;

    public Encyclopedia(KnowledgeRuntime rt) { this.rt = rt; }

    private static final Comparator<KnowledgeRecord> BY_WEIGHT = (a, b) -> Double.compare(b.confidence() * (0.5D + b.importance()), a.confidence() * (0.5D + a.importance()));

    public List<KnowledgeRecord> lookup(String name) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (UUID id : rt.named(name)) { KnowledgeRecord r = rt.get(id); if (r != null && r.alive()) result.add(r); }
        result.sort(BY_WEIGHT);
        return result;
    }

    public List<KnowledgeRecord> entries(KnowledgeType type, int limit) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (UUID id : rt.ofType(type)) { KnowledgeRecord r = rt.get(id); if (r != null && r.alive()) result.add(r); }
        result.sort(BY_WEIGHT);
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    public List<KnowledgeRecord> byCategory(KnowledgeCategory category, int limit) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (KnowledgeRecord r : rt.all()) if (r.alive() && r.category() == category) result.add(r);
        result.sort(BY_WEIGHT);
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    public List<String> namesStartingWith(String prefix, int limit) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String n : rt.names()) if (n.startsWith(p)) { result.add(n); if (result.size() >= limit) break; }
        java.util.Collections.sort(result);
        return result;
    }

    /** What the NPC believes about a named thing, most reliable first, with the belief's state: the answer a dialogue would use. */
    public List<String> describe(String name) {
        List<String> lines = new ArrayList<>();
        for (KnowledgeRecord r : lookup(name)) lines.add(r.summary() + (r.state() == ValidationState.RUMOR ? " (solo un rumor)" : ""));
        return lines;
    }

    public int entryCount() { return rt.size(); }
}
