package yadi.samuraiai.ai.memory.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.Stamp;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;

/**
 * Finds memories through the indexes (never by scanning the history): exact, fuzzy or by similarity. Results are ranked by how
 * well they match, how strong the memory still is, how important and how recent. Retrieval rehearses what it returns: a memory
 * that is recalled fades more slowly.
 */
public final class RetrievalEngine {
    private final SimilarityEngine similarity;

    public RetrievalEngine(SimilarityEngine similarity) { this.similarity = similarity; }

    public List<RetrievalResult> retrieve(MemoryRuntime rt, RetrievalQuery q, long now, MemorySettings s) {
        int limit = q.limit > 0 ? q.limit : s.retrievalLimit();
        Map<UUID, List<String>> matches = new HashMap<>();
        int criteria = 0;
        List<Set<UUID>> sets = new ArrayList<>();
        List<String> names = new ArrayList<>();
        var idx = rt.index();
        if (q.entity != null) { sets.add(idx.entity(q.entity)); names.add("person"); }
        if (q.place != null && q.place.known()) { sets.add(idx.cell(q.place.cell(idx.cellSize()))); names.add("place"); }
        if (q.zone != null && !q.zone.isEmpty()) { sets.add(idx.zone(q.zone)); names.add("zone"); }
        if (q.emotion != null) { sets.add(idx.emotion(q.emotion)); names.add("emotion"); }
        if (q.kind != null) { sets.add(idx.kind(q.kind)); names.add("kind"); }
        if (q.category != null) { sets.add(idx.category(q.category)); names.add("category"); }
        if (q.type != null) { sets.add(idx.type(q.type)); names.add("type"); }
        if (q.minImportance != null) { sets.add(idx.atLeast(q.minImportance)); names.add("importance"); }
        if (q.event != null && !q.event.isEmpty()) { sets.add(idx.event(q.event)); names.add("event"); }
        if (q.fromDay != null && q.toDay != null) {
            Set<UUID> inRange = new HashSet<>();
            long span = q.toDay - q.fromDay;
            if (span >= 0 && span <= 400) for (long d = q.fromDay; d <= q.toDay; d++) inRange.addAll(idx.day(d));
            else for (long d : idx.days()) if (d >= q.fromDay && d <= q.toDay) inRange.addAll(idx.day(d));
            sets.add(inRange); names.add("date");
        }
        for (String tag : q.tags) { sets.add(idx.tag(tag)); names.add("tag:" + tag); }
        criteria = sets.size();

        Set<UUID> pool = new HashSet<>();
        if (criteria == 0) pool.addAll(rt.ids());
        else if (q.mode == RetrievalQuery.Mode.EXACT) {
            int smallest = 0;
            for (int i = 1; i < sets.size(); i++) if (sets.get(i).size() < sets.get(smallest).size()) smallest = i;
            for (UUID id : sets.get(smallest)) {
                boolean all = true;
                for (Set<UUID> set : sets) if (!set.contains(id)) { all = false; break; }
                if (all) pool.add(id);
            }
        } else for (Set<UUID> set : sets) pool.addAll(set);

        List<RetrievalResult> results = new ArrayList<>();
        for (UUID id : pool) {
            MemoryRecord r = rt.get(id);
            if (r == null || r.state() == MemoryState.FORGOTTEN || r.strength() < q.minStrength) continue;
            List<String> matched = new ArrayList<>();
            for (int i = 0; i < sets.size(); i++) if (sets.get(i).contains(id)) matched.add(names.get(i));
            double fraction = criteria == 0 ? 1.0D : (double) matched.size() / criteria;
            if (q.mode == RetrievalQuery.Mode.FUZZY && fraction < s.fuzzyMinMatch()) continue;
            double fit = fraction;
            if (q.mode == RetrievalQuery.Mode.SIMILAR) {
                double sim = q.tags.isEmpty() ? 0.0D : similarity.similarity(q.tags, r);
                if (q.kind != null && r.kind() == q.kind) sim = Math.max(sim, 0.5D) + 0.3D;
                fit = Math.min(1.0D, Math.max(fraction, sim));
                if (fit < s.fuzzyMinMatch() * 0.6D) continue;
            }
            results.add(new RetrievalResult(r, rank(r, fit, now), matched));
        }
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (results.size() > limit) results = new ArrayList<>(results.subList(0, limit));
        rehearse(rt, results, now, s);
        return results;
    }

    static double rank(MemoryRecord r, double fit, long now) {
        double ageDays = Math.max(0, now - r.stamp().gameTime()) / (double) Stamp.TICKS_PER_DAY;
        double recency = 1.0D / (1.0D + ageDays * 0.05D);
        Importance i = r.importance();
        return fit * (0.35D + 0.65D * r.strength()) * (0.5D + 0.5D * i.weight()) * (0.6D + 0.4D * recency);
    }

    static void rehearse(MemoryRuntime rt, List<RetrievalResult> results, long now, MemorySettings s) {
        for (RetrievalResult result : results) {
            MemoryRecord r = result.record();
            rt.cache().served(r.id());
            r.touch(now);
            r.strength(r.strength() + s.rehearsalGain() * (1.0D - r.strength()));
            rt.cache().touched(r.id(), now);
            if (r.strength() > s.fadingThreshold() + 0.1D && r.state() == MemoryState.FADING) r.state(MemoryState.CONSOLIDATED);
        }
        if (!results.isEmpty()) rt.markDirty();
    }

    public SimilarityEngine similarity() { return similarity; }
}
