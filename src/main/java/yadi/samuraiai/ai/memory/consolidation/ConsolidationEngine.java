package yadi.samuraiai.ai.memory.consolidation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;
import yadi.samuraiai.ai.memory.model.MemoryType;
import yadi.samuraiai.ai.memory.retrieval.SimilarityEngine;

/**
 * Settles temporary memories: each one that has aged past the consolidation delay (or, while the NPC sleeps, any) is either
 * merged into a similar consolidated memory or promoted to a consolidated one, and its impressions are folded into semantic
 * memory. A pass is limited to a batch, so sleeping does not cost a burst of work.
 */
public final class ConsolidationEngine {
    public record Report(int promoted, int merged, List<UUID> survivors, List<UUID> absorbed) { }

    private final SimilarityEngine similarity;
    private final MemoryMerger merger;

    public ConsolidationEngine(SimilarityEngine similarity, MemoryMerger merger) { this.similarity = similarity; this.merger = merger; }

    public Report run(MemoryRuntime rt, long now, int batch, boolean sleeping, MemorySettings s) {
        int promoted = 0, merged = 0;
        List<UUID> survivors = new ArrayList<>(), absorbed = new ArrayList<>();
        List<UUID> queue = new ArrayList<>(rt.temporaryIds());
        int processed = 0;
        for (UUID id : queue) {
            if (processed >= batch) break;
            MemoryRecord r = rt.get(id);
            if (r == null) { rt.dropTemporary(id); continue; }
            long age = now - r.stamp().gameTime();
            if (!sleeping && age < s.consolidationDelayTicks()) continue;
            processed++;
            Optional<MemoryRecord> target = findMergeTarget(rt, r, s);
            if (target.isPresent()) {
                MemoryRecord survivor = target.get();
                rt.reindex(survivor, () -> merger.merge(survivor, r, s));
                rt.remove(r.id());
                survivors.add(survivor.id()); absorbed.add(r.id());
                merged++;
                applySemantic(rt, survivor, s, r);
            } else {
                rt.reindex(r, () -> {
                    r.state(MemoryState.CONSOLIDATED);
                    r.importance(merger.promoted(r));
                    r.strength(Math.min(1.0D, r.strength() + s.consolidationBoost() + (sleeping ? s.sleepReinforce() : 0.0D)));
                });
                rt.dropTemporary(r.id());
                promoted++;
                applySemantic(rt, r, s, null);
            }
        }
        if (promoted + merged > 0) rt.markDirty();
        return new Report(promoted, merged, survivors, absorbed);
    }

    /** Sleep strengthens what mattered: important or emotional memories gain a little strength (bounded to the batch). */
    public int reinforceForSleep(MemoryRuntime rt, int batch, MemorySettings s) {
        int changed = 0;
        for (MemoryRecord r : rt.all()) {
            if (changed >= batch) break;
            if (r.state() == MemoryState.FORGOTTEN || r.strength() >= 0.98D) continue;
            if (r.importance().atLeast(Importance.HIGH) || r.emotionalWeight() >= 0.6D) { r.strength(r.strength() + s.sleepReinforce() * (1.0D - r.strength())); changed++; }
        }
        if (changed > 0) rt.markDirty();
        return changed;
    }

    private void applySemantic(MemoryRuntime rt, MemoryRecord r, MemorySettings s, MemoryRecord absorbed) {
        MemoryRecord source = absorbed != null ? absorbed : r;
        if (source.semanticApplied()) return;
        source.semanticApplied(true);
        for (var impression : source.impressions()) {
            var subject = subjectFor(source, impression.aspect());
            if (subject != null) rt.semantic().reinforce(subject, impression.aspect(), impression.delta(), 0.35D, source.stamp().gameTime());
        }
    }

    private static yadi.samuraiai.ai.cognition.model.EntityRef subjectFor(MemoryRecord r, yadi.samuraiai.ai.memory.semantic.Aspect aspect) {
        switch (aspect) {
            case SAFE, SACRED -> {
                if (r.place().known()) return new yadi.samuraiai.ai.cognition.model.EntityRef(yadi.samuraiai.ai.cognition.model.EntityRef.nameId("place", r.place().zone().isEmpty() ? r.place().dimension() + r.place().cell(32) : r.place().zone()),
                        yadi.samuraiai.ai.cognition.model.EntityKind.PLACE, r.place().zone());
                return r.actor();
            }
            default -> { return r.actor() != null ? r.actor() : r.target(); }
        }
    }

    private Optional<MemoryRecord> findMergeTarget(MemoryRuntime rt, MemoryRecord r, MemorySettings s) {
        if (r.isProtected() || r.importance().atLeast(Importance.IMPORTANT) || r.type() == MemoryType.SEMANTIC) return Optional.empty();
        Set<UUID> candidates = new LinkedHashSet<>();
        if (r.actor() != null) candidates.addAll(rt.index().entity(r.actor().id()));
        else candidates.addAll(rt.index().kind(r.kind()));
        MemoryRecord best = null;
        double bestScore = s.mergeSimilarity();
        int checked = 0;
        for (UUID id : candidates) {
            if (checked++ >= 300) break;
            MemoryRecord c = rt.get(id);
            if (c == null || c == r || c.kind() != r.kind() || c.state() == MemoryState.TEMPORARY || c.state() == MemoryState.FORGOTTEN) continue;
            if (c.importance().atLeast(Importance.IMPORTANT)) continue;
            if (Math.abs(c.stamp().gameTime() - r.stamp().gameTime()) > s.mergeSpanTicks()) continue;
            double sim = similarity.similarity(c, r);
            if (sim >= bestScore) { best = c; bestScore = sim; }
        }
        return Optional.ofNullable(best);
    }
}
