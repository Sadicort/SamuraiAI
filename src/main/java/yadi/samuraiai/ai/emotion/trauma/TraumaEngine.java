package yadi.samuraiai.ai.emotion.trauma;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.EmotionEffect;
import yadi.samuraiai.ai.emotion.model.EmotionTrigger;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;

/**
 * Traumas: a lasting wound made by a defeat, a betrayal, a death witnessed, a fire, an explosion or a lost ally. A trigger
 * becomes one when it is flagged traumatic by the experience catalogue, or when a very strong, important unpleasant emotion
 * crosses the threshold. A similar new trauma (same trigger) deepens the existing one instead of duplicating it. The wound
 * remembers its triggers (places, people, kinds of event) so that meeting them again can echo it. No extreme behavior is
 * implemented; the flashback and avoidance counters only record that it happened.
 */
public final class TraumaEngine {
    /** @return the trauma the trigger created or deepened, with whether it was an existing one. */
    public record Result(TraumaRecord trauma, boolean reinforced) { }

    public Optional<Result> consider(EmotionTrigger t, List<yadi.samuraiai.ai.emotion.model.EmotionRecord> created, EmotionRuntime rt, double resilience, EmotionSettings s) {
        if (t.fromEcho() || t.source() == yadi.samuraiai.ai.emotion.model.TriggerSource.CONTAGION) return Optional.empty();
        EmotionEffect strongest = null;
        for (EmotionEffect e : t.effects()) if (e.kind().unpleasant() && (strongest == null || e.intensity() > strongest.intensity())) strongest = e;
        if (strongest == null) return Optional.empty();
        boolean qualifies = t.traumatic() || (strongest.intensity() * (0.5D + 0.5D * t.weight()) >= s.traumaThreshold() && t.weight() >= s.traumaMinWeight());
        if (!qualifies) return Optional.empty();
        double depth = Math.min(1.0D, strongest.intensity() / 100.0D * (0.6D + 0.4D * t.weight()) / Math.max(0.5D, resilience));
        Set<String> triggers = triggerKeys(t);
        for (TraumaRecord existing : rt.traumas()) {
            if (existing.active() && overlaps(existing.triggers(), triggers) && existing.emotion() == strongest.kind()) {
                existing.intensity(Math.min(1.0D, existing.intensity() + depth * 0.5D));
                existing.progress(Math.max(0.0D, existing.progress() - 0.2D));
                existing.phase(TraumaRecord.Phase.ACTIVE);
                if (t.memoryId() != null) existing.memories().add(t.memoryId());
                existing.triggers().addAll(triggers);
                existing.bump();
                return Optional.of(new Result(existing, true));
            }
        }
        TraumaRecord trauma = new TraumaRecord(UUID.randomUUID(), rt.npcId(), t.experienceKind().isEmpty() ? t.source().name() : t.experienceKind(), strongest.kind(), depth, t.at());
        if (t.memoryId() != null) trauma.memories().add(t.memoryId());
        trauma.triggers().addAll(triggers);
        rt.traumas().add(trauma);
        while (rt.traumas().size() > s.maxTraumas()) {
            TraumaRecord drop = null;
            for (TraumaRecord x : rt.traumas()) if (x.phase() == TraumaRecord.Phase.RECOVERED && (drop == null || x.at() < drop.at())) drop = x;
            if (drop == null) break;
            rt.traumas().remove(drop);
        }
        return Optional.of(new Result(trauma, false));
    }

    public static Set<String> triggerKeys(EmotionTrigger t) {
        Set<String> keys = new HashSet<>();
        if (t.entity() != null) keys.add("entity:" + t.entity().id());
        if (t.place().known()) { keys.add("cell:" + t.place().cell(32)); if (!t.place().zone().isEmpty()) keys.add("zone:" + t.place().zone()); }
        if (!t.experienceKind().isEmpty()) keys.add("kind:" + t.experienceKind());
        return keys;
    }

    private static boolean overlaps(Set<String> a, Set<String> b) {
        for (String k : b) if (a.contains(k) && !k.startsWith("kind:")) return true;
        return false;
    }

    /** The active traumas that the given place or people bring back, with the intensity of the flashback each would cause (0-100). */
    public List<Flashback> matching(EmotionRuntime rt, yadi.samuraiai.ai.cognition.model.PlaceRef place, List<UUID> nearby, EmotionSettings s) {
        List<Flashback> result = new ArrayList<>();
        String cell = place == null || !place.known() ? "" : "cell:" + place.cell(32);
        String zone = place == null || place.zone().isEmpty() ? "" : "zone:" + place.zone();
        for (TraumaRecord t : rt.traumas()) {
            if (!t.active()) continue;
            boolean hit = (!cell.isEmpty() && t.triggers().contains(cell)) || (!zone.isEmpty() && t.triggers().contains(zone));
            if (!hit && nearby != null) for (UUID id : nearby) if (t.triggers().contains("entity:" + id)) { hit = true; break; }
            if (hit) result.add(new Flashback(t, t.remaining() * 100.0D * s.traumaEchoIntensity()));
        }
        return result;
    }

    public record Flashback(TraumaRecord trauma, double intensity) { }

}
