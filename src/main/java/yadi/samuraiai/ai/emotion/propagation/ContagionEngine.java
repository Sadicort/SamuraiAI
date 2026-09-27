package yadi.samuraiai.ai.emotion.propagation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.EmotionPhase;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.TriggerSource;

/**
 * Some emotions spread: panic, calm, joy (celebration) and grief (mourning). Only neighbours within reach catch them, more
 * readily when they trust the source and are sociable, and less the farther away they are. An emotion that was itself caught
 * never spreads again, so a rumour of panic does not echo around a village forever.
 */
public final class ContagionEngine {
    public record Transfer(UUID target, EmotionKind kind, double intensity) { }

    private static boolean contagious(EmotionKind kind) { return kind == EmotionKind.FEAR || kind == EmotionKind.CALM || kind == EmotionKind.JOY || kind == EmotionKind.SADNESS; }

    public List<Transfer> spread(EmotionRuntime source, List<EmotionNeighbor> neighbors, EmotionSettings s) {
        Map<EmotionKind, Double> strongest = new EnumMap<>(EmotionKind.class);
        for (EmotionRecord r : source.activeRecords()) {
            if (r.phase() != EmotionPhase.ACTIVE || !contagious(r.kind()) || r.origin().source() == TriggerSource.CONTAGION || r.origin().source() == TriggerSource.ECHO) continue;
            if (r.intensity() >= s.contagionMinIntensity()) strongest.merge(r.kind(), r.intensity(), Math::max);
        }
        List<Transfer> result = new ArrayList<>();
        if (strongest.isEmpty()) return result;
        List<EmotionNeighbor> near = new ArrayList<>(neighbors);
        near.sort((a, b) -> Double.compare(a.distance(), b.distance()));
        for (EmotionNeighbor n : near) {
            if (result.size() >= s.contagionMaxTargets() * strongest.size()) break;
            if (n.distance() > s.contagionRadius()) continue;
            double closeness = 1.0D - n.distance() / s.contagionRadius();
            double receptivity = (0.5D + 0.5D * Math.max(0.0D, Math.min(100.0D, n.trustInSource())) / 100.0D) * (0.6D + 0.4D * (n.sociabilityLean() + 1.0D) / 2.0D);
            for (var e : strongest.entrySet()) {
                double intensity = e.getValue() * s.contagionFactor() * closeness * receptivity;
                if (intensity >= s.minIntensityKeep()) result.add(new Transfer(n.id(), e.getKey(), intensity));
            }
        }
        return result;
    }
}
