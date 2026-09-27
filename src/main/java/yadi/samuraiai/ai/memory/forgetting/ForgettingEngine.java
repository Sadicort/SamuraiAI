package yadi.samuraiai.ai.memory.forgetting;

import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;

/**
 * Decides how quickly each memory fades. The half-life grows with importance, emotional weight, use (each recall), repetition
 * and disciplined character, and is much longer for trauma; protected memories do not fade. Strength is brought up to date
 * lazily from the time since the last pass, so it is correct however rarely (or after however long a shutdown) the pass runs.
 */
public final class ForgettingEngine {
    public double halfLife(MemoryRecord r, PersonalityView personality, MemorySettings s) {
        double scale = switch (r.importance()) {
            case TRIVIAL -> s.scaleTrivial(); case LOW -> s.scaleLow(); case NORMAL -> s.scaleNormal(); case HIGH -> s.scaleHigh();
            case IMPORTANT -> s.scaleImportant(); case CRITICAL -> s.scaleCritical(); case LEGENDARY -> s.scaleLegendary();
        };
        double life = s.halfLifeTicks() * scale;
        life *= 1.0D + s.emotionRetention() * r.emotionalWeight();
        life *= 1.0D + s.useRetention() * Math.min(r.accessCount(), s.useRetentionCap());
        life *= 1.0D + Math.log(Math.max(1, r.repeatCount())) / Math.log(2.0D) * 0.25D;
        if (r.emotion().traumatic()) life *= s.traumaRetention();
        double discipline = (personality == null ? PersonalityView.NEUTRAL : personality).lean(Trait.DISCIPLINE);
        life *= 1.0D + s.personalityRetention() * discipline;
        return life;
    }

    public enum Result { KEPT, FADING, FORGET }

    /** Applies the elapsed decay to one memory and reports what should happen to it. */
    public Result step(MemoryRecord r, long now, PersonalityView personality, MemorySettings s) {
        long elapsed = now - r.lastDecay();
        if (elapsed <= 0) return classify(r, s, false);
        r.lastDecay(now);
        if (r.isProtected()) return classify(r, s, true);
        double keep = ForgettingCurve.retention(elapsed, halfLife(r, personality, s));
        r.strength(r.strength() * keep);
        return classify(r, s, false);
    }

    private static Result classify(MemoryRecord r, MemorySettings s, boolean isProtected) {
        if (isProtected) return Result.KEPT;
        if (r.strength() < s.forgetThreshold() && r.importance().ordinal() < Importance.CRITICAL.ordinal()) return Result.FORGET;
        if (r.strength() < s.fadingThreshold()) { if (r.state() != MemoryState.COMPRESSED) r.state(MemoryState.FADING); return Result.FADING; }
        if (r.state() == MemoryState.FADING) r.state(MemoryState.CONSOLIDATED);
        return Result.KEPT;
    }
}
