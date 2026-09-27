package yadi.samuraiai.ai.memory.evolution;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.Stamp;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Consequence;
import yadi.samuraiai.ai.memory.model.EmotionalSignature;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

/**
 * Memories change with time and with what comes after. Their emotional charge relaxes (fear and anger fade, pride and gratitude
 * linger, trauma keeps a residue), their confidence drifts down unless reinforced, and a later experience with the same person
 * that contradicts an earlier one reinterprets it: a friendship remembered after a betrayal is no longer remembered warmly.
 */
public final class MemoryEvolutionEngine {
    private static double scale(EmotionKind kind) {
        return switch (kind) {
            case PRIDE, GRATITUDE, HOPE, RESPECT, COMPASSION, INSPIRATION -> 3.0D;
            case FEAR, ANGER, ANXIETY -> 0.7D;
            default -> 1.0D;
        };
    }

    /** Relaxes emotional intensity and confidence over {@code elapsed} ticks. */
    public void relax(MemoryRecord r, long elapsed, MemorySettings s) {
        if (elapsed <= 0) return;
        EmotionalSignature e = r.emotion();
        if (e.intensity() > 0) {
            double floor = e.traumatic() ? s.traumaResidual() * r.emotionalWeight() : 0.0D;
            if (e.intensity() > floor) {
                double halfLife = s.evolutionHalfLifeTicks() * scale(e.primary());
                double next = floor + (e.intensity() - floor) * Math.pow(0.5D, elapsed / halfLife);
                r.emotion(e.withIntensity(next));
            }
        }
        if (!r.isProtected() && r.confidence() > 0.2D)
            r.confidence(Math.max(0.2D, r.confidence() - s.confidenceDriftPerDay() * elapsed / Stamp.TICKS_PER_DAY));
    }

    /** An experience that reinforces a memory also steadies its confidence. */
    public void confirm(MemoryRecord r) { r.confidence(r.confidence() + 0.05D); }

    /**
     * A new, important memory about a person contradicts older ones about them: the old ones' valence shifts towards the new.
     * @return the memories that were reinterpreted
     */
    public List<MemoryRecord> reinterpret(MemoryRuntime rt, MemoryRecord fresh, MemorySettings s) {
        List<MemoryRecord> changed = new java.util.ArrayList<>();
        if (fresh.actor() == null || s.reinterpretLookback() == 0 || !fresh.importance().atLeast(Importance.HIGH)) return changed;
        double newValence = fresh.emotion().valence();
        if (Math.abs(newValence) < 0.3D) return changed;
        int checked = 0;
        // Iterate a copy: reindexing an old memory changes the very index set being read.
        for (UUID id : new java.util.ArrayList<>(rt.index().entity(fresh.actor().id()))) {
            if (checked++ >= s.reinterpretLookback() * 4) break;
            MemoryRecord old = rt.get(id);
            if (old == null || old == fresh || old.stamp().gameTime() >= fresh.stamp().gameTime() || !old.importance().atLeast(Importance.LOW)) continue;
            if (old.emotion().valence() * newValence >= 0 || old.tags().contains("reinterpreted:" + fresh.id())) continue;
            double shifted = old.emotion().valence() + (newValence - old.emotion().valence()) * s.reinterpretShift();
            rt.reindex(old, () -> {
                old.emotion(old.emotion().withValence(shifted));
                old.tags().add("reinterpreted:" + fresh.id());
                old.consequences().add(new Consequence("reinterpreted", fresh.id().toString(), s.reinterpretShift()));
                if (old.consequences().size() > s.maxConsequences()) old.consequences().remove(0);
                old.bumpVersion();
            });
            changed.add(old);
            if (changed.size() >= s.reinterpretLookback()) break;
        }
        return changed;
    }
}
