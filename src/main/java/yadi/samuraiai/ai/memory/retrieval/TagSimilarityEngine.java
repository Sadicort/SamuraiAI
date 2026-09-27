package yadi.samuraiai.ai.memory.retrieval;

import java.util.HashSet;
import java.util.Set;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

/** Structural similarity: same kind, same people, same place, same feeling, and overlapping tags. */
public final class TagSimilarityEngine implements SimilarityEngine {
    @Override public double similarity(MemoryRecord a, MemoryRecord b) {
        double score = 0;
        if (a.kind() == b.kind()) score += 0.30D;
        if (a.actor() != null && b.actor() != null && a.actor().id().equals(b.actor().id())) score += 0.20D;
        else if (a.actor() == null && b.actor() == null) score += 0.10D;
        if (a.target() != null && b.target() != null && a.target().id().equals(b.target().id())) score += 0.10D;
        else if (a.target() == null && b.target() == null) score += 0.05D;
        if (!a.place().zone().isEmpty() && a.place().zone().equals(b.place().zone())) score += 0.15D;
        else if (a.place().known() && b.place().known() && a.place().distance(b.place()) < 24.0D) score += 0.15D;
        if (a.emotion().primary() == b.emotion().primary()) score += 0.10D;
        score += 0.15D * jaccard(a.tags(), b.tags());
        return Math.min(1.0D, score);
    }

    @Override public double similarity(Set<String> tags, MemoryRecord record) { return jaccard(tags, record.tags()); }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0D;
        Set<String> union = new HashSet<>(a); union.addAll(b);
        int both = 0;
        for (String s : a) if (b.contains(s)) both++;
        return union.isEmpty() ? 0.0D : (double) both / union.size();
    }
}
