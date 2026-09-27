package yadi.samuraiai.ai.perception.awareness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.memory.PerceptionMemory;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;

/**
 * Curiosity: scores things worth a look. A new entity, an unknown player, an animal, an item, an odd event or a noise
 * with no known cause each carry an interest, boosted by novelty (not remembered before) and by the NPC's curiosity,
 * and decaying with time. Threats are not "interesting": they are handled by the threat engine.
 */
public final class InterestEngine {
    public record Update(List<InterestItem> detected, Optional<InterestItem> top) { }

    private final Map<String, InterestItem> items = new HashMap<>();

    public List<InterestItem> items() {
        List<InterestItem> list = new ArrayList<>(items.values());
        list.sort(Comparator.comparingDouble(InterestItem::score).reversed());
        return list;
    }
    public Optional<InterestItem> top() { return items().stream().findFirst(); }
    public void reset() { items.clear(); }

    private static double base(Stimulus st, Perceiver p) {
        return switch (st.category()) {
            case PLAYER -> st.source() != null && p.knows(st.source()) ? 5.0D + Math.abs(p.relationTo(st.source())) / 10.0D : 40.0D;
            case NPC -> st.source() != null && p.knows(st.source()) ? 4.0D : 25.0D;
            case PASSIVE -> 20.0D;
            case ITEM, OBJECT -> 15.0D;
            case BLOCK_CHANGE, IMPACT -> 28.0D;
            case DOOR -> 25.0D;
            case FOOTSTEP -> 12.0D;
            case SPEECH, VOICE -> 20.0D;
            case SCENT -> 18.0D;
            default -> 0.0D;
        };
    }

    public Update update(List<Stimulus> stimuli, Perceiver p, PerceptionMemory memory, long elapsedTicks, long tick, PerceptionSettings s) {
        double decay = s.interestDecayPerTick() * Math.max(1L, elapsedTicks);
        for (var entry : new ArrayList<>(items.entrySet())) {
            InterestItem faded = entry.getValue().withScore(entry.getValue().score() - decay);
            if (faded.score() < s.interestThreshold() / 2.0D) faded = faded.withNotified(false);
            if (faded.score() <= 0.5D) items.remove(entry.getKey()); else items.put(entry.getKey(), faded);
        }
        List<InterestItem> detected = new ArrayList<>();
        for (Stimulus st : stimuli) {
            if (st.category().threatWeight() >= 30) continue;
            double base = base(st, p);
            if (base <= 0.0D) continue;
            boolean novel = st.source() != null && !memory.knows(st.source());
            double score = Math.min(100.0D, (base + (novel ? 25.0D : 0.0D)) * p.senses().curiosity() * Math.max(0.3D, st.intensity()));
            String key = st.source() != null ? "subject:" + st.source() : "place:" + st.category() + ":" + (int) Math.floor(st.x() / 4) + ":" + (int) Math.floor(st.z() / 4);
            InterestItem known = items.get(key);
            double next = known == null ? score : Math.max(known.score(), score);
            InterestItem item = new InterestItem(key, st.source(), st.label(), st.x(), st.y(), st.z(), next, tick, known != null && known.notified());
            if (!item.notified() && item.score() >= s.interestThreshold()) { item = item.withNotified(true); detected.add(item); }
            items.put(key, item);
        }
        return new Update(detected, top());
    }
}
