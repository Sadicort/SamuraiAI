package yadi.samuraiai.ai.memory.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;

/**
 * "Which memories are relevant now?" without walking the history: the people around, the place, the current feeling and the
 * goal's tags each pull their indexed memories into a small candidate map, which is scored and cut to the top few. Answers are
 * cached for a few ticks and dropped whenever the memories change.
 */
public final class ContextRetrieval {
    public List<RetrievalResult> relevant(MemoryRuntime rt, RetrievalContext ctx, int limit, long now, MemorySettings s) {
        String key = key(rt, ctx, limit, s);
        List<UUID> cached = rt.cache().lookup(key, now);
        if (cached != null) {
            List<RetrievalResult> results = new ArrayList<>();
            for (UUID id : cached) { MemoryRecord r = rt.get(id); if (r != null) results.add(new RetrievalResult(r, RetrievalEngine.rank(r, 1.0D, now), List.of("cached"))); }
            return results;
        }
        var idx = rt.index();
        Map<UUID, Double> score = new HashMap<>();
        Map<UUID, List<String>> why = new HashMap<>();
        for (UUID entity : ctx.nearby()) for (UUID id : idx.entity(entity)) add(score, why, id, 0.5D, "person");
        if (ctx.place().known()) for (UUID id : idx.cell(ctx.place().cell(idx.cellSize()))) add(score, why, id, 0.3D, "place");
        if (!ctx.place().zone().isEmpty()) for (UUID id : idx.zone(ctx.place().zone())) add(score, why, id, 0.25D, "zone");
        if (ctx.emotion() != null) for (UUID id : idx.emotion(ctx.emotion())) add(score, why, id, 0.15D, "emotion");
        for (String tag : ctx.tags()) for (UUID id : idx.tag(tag)) add(score, why, id, 0.2D, "tag:" + tag);
        List<RetrievalResult> results = new ArrayList<>();
        for (var entry : score.entrySet()) {
            MemoryRecord r = rt.get(entry.getKey());
            if (r == null || r.state() == MemoryState.FORGOTTEN) continue;
            results.add(new RetrievalResult(r, RetrievalEngine.rank(r, Math.min(1.0D, entry.getValue()), now), why.get(entry.getKey())));
        }
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (results.size() > limit) results = new ArrayList<>(results.subList(0, limit));
        List<UUID> ids = new ArrayList<>();
        for (RetrievalResult result : results) ids.add(result.record().id());
        rt.cache().store(key, ids, now, s.shortCacheTicks());
        for (RetrievalResult result : results) rt.cache().served(result.record().id());
        return results;
    }

    private static void add(Map<UUID, Double> score, Map<UUID, List<String>> why, UUID id, double weight, String reason) {
        score.merge(id, weight, Double::sum);
        why.computeIfAbsent(id, k -> new ArrayList<>()).add(reason);
    }

    private static String key(MemoryRuntime rt, RetrievalContext ctx, int limit, MemorySettings s) {
        List<String> nearby = new ArrayList<>();
        for (UUID n : ctx.nearby()) nearby.add(n.toString());
        java.util.Collections.sort(nearby);
        return ctx.place().cell(rt.index().cellSize()) + "|" + ctx.place().zone() + "|" + nearby + "|" + ctx.emotion() + "|" + new java.util.TreeSet<>(ctx.tags()) + "|" + limit;
    }
}
