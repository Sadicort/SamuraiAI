package yadi.samuraiai.ai.knowledge.propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/**
 * Chooses what one NPC tells another and when it arrives. Only what the teller believes strongly enough is told (rumours only
 * when they cross a threshold), never what the listener may not know (access ranks), never what the listener already believes
 * at least as firmly. Arrival takes longer with distance, with low trust and low relevance, and in larger communities, so
 * information travels rather than teleports. Delivery itself is done by the caller, within a per-tick budget.
 */
public final class PropagationEngine {
    /** Knowledge the teller could pass to the listener, best first (importance and confidence, then novelty to the listener). */
    public List<KnowledgeRecord> select(KnowledgeRuntime teller, KnowledgeRuntime listener, AccessLevel listenerRank, KnowledgeSettings s) {
        List<KnowledgeRecord> candidates = new ArrayList<>();
        for (KnowledgeRecord r : teller.all()) {
            if (!r.alive() || r.state() == ValidationState.FALSE || r.state() == ValidationState.UNKNOWN) continue;
            if (r.state() == ValidationState.RUMOR && r.confidence() < s.rumorTellThreshold()) continue;
            if (!r.access().visibleTo(listenerRank)) continue;
            if (r.importance() < 0.2D && r.state() != ValidationState.VERIFIED) continue;
            KnowledgeRecord known = listener.byKey(r.key());
            if (known != null && (known.supporters().contains(teller.ownerId()) || known.confidence() >= r.confidence() * 0.9D && known.state().ordinal() >= r.state().ordinal())) continue;
            candidates.add(r);
        }
        candidates.sort((a, b) -> Double.compare(score(b, listener), score(a, listener)));
        return candidates.size() > s.gossipMaxItems() ? new ArrayList<>(candidates.subList(0, s.gossipMaxItems())) : candidates;
    }

    private static double score(KnowledgeRecord r, KnowledgeRuntime listener) {
        double novelty = listener.byKey(r.key()) == null ? 0.3D : 0.0D;
        return r.confidence() * (0.4D + r.importance()) + novelty;
    }

    /** Ticks until the telling arrives. */
    public long delay(double distance, double trust01, double relevance, int communitySize, KnowledgeSettings s) {
        double base = s.baseDelayTicks() + Math.max(0.0D, Math.min(distance, 1.0e6D)) * s.delayPerBlock();
        double trustFactor = 1.5D - 0.5D * Math.max(0.0D, Math.min(1.0D, trust01));
        double relevanceFactor = 1.5D - 0.5D * Math.max(0.0D, Math.min(1.0D, relevance));
        double crowd = 1.0D + Math.max(0, communitySize) * s.communitySizeDelay();
        return Math.max(1L, Math.round(base * trustFactor * relevanceFactor * crowd));
    }

    public PropagationTask task(PropagationTask.Kind kind, UUID from, UUID to, UUID item, long now, double distance, double trust01, double relevance, int communitySize, int hop, String community, KnowledgeSettings s) {
        return new PropagationTask(UUID.randomUUID(), kind, from, to, item, now + delay(distance, trust01, relevance, communitySize, s), hop, relevance, community);
    }
}
