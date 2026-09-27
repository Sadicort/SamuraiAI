package yadi.samuraiai.ai.memory.semantic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/** Stable beliefs about people and places, built up by consolidating experiences. Not the same thing as the knowledge engine's facts: these are impressions. */
public final class SemanticMemory {
    private final Map<String, SemanticBelief> beliefs = new HashMap<>();

    private static String key(UUID subject, Aspect aspect) { return subject + "|" + aspect; }

    public void reinforce(EntityRef subject, Aspect aspect, double delta, double rate, long now) {
        if (subject == null || aspect == null) return;
        beliefs.computeIfAbsent(key(subject.id(), aspect), k -> new SemanticBelief(subject, aspect, 0.0D, 0.0D, 0, now)).apply(delta, rate, now);
    }

    public Optional<SemanticBelief> belief(UUID subject, Aspect aspect) { return Optional.ofNullable(beliefs.get(key(subject, aspect))); }
    public double value(UUID subject, Aspect aspect) { return belief(subject, aspect).map(SemanticBelief::value).orElse(0.0D); }

    public List<SemanticBelief> about(UUID subject) {
        List<SemanticBelief> result = new ArrayList<>();
        for (SemanticBelief b : beliefs.values()) if (b.subject().id().equals(subject)) result.add(b);
        return result;
    }

    public Collection<SemanticBelief> all() { return List.copyOf(beliefs.values()); }
    public int size() { return beliefs.size(); }
    public void put(SemanticBelief belief) { beliefs.put(key(belief.subject().id(), belief.aspect()), belief); }
    public void clear() { beliefs.clear(); }
}
