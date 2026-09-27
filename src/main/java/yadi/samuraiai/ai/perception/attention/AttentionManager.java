package yadi.samuraiai.ai.perception.attention;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Chooses what the NPC attends to. Every stimulus is scored (its priority, adjusted by movement, sound, damage, who it
 * is, how the NPC feels about them, its emotions and what it is doing); the best one becomes the focus. Switching needs
 * a clearly better candidate (hysteresis, so attention does not flicker); a focus that stops being perceived is kept for
 * a recovery window, so an NPC can pick a target back up.
 */
public final class AttentionManager {
    public record Update(AttentionLevel level, AttentionFocus focus, boolean focusChanged, boolean focusLost, boolean focusRecovered) { }

    private AttentionFocus focus;
    private AttentionLevel level = AttentionLevel.NONE;

    public AttentionFocus focus() { return focus; }
    public AttentionLevel level() { return level; }
    public void reset() { focus = null; level = AttentionLevel.NONE; }

    public Update update(List<Stimulus> stimuli, Perceiver p, long tick, PerceptionSettings s) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Set<AttentionSource>> reasons = new HashMap<>();
        Map<String, Stimulus> byKey = new HashMap<>();
        Stimulus best = null;
        double bestScore = -1.0D;
        for (Stimulus stimulus : stimuli) {
            Set<AttentionSource> why = EnumSet.noneOf(AttentionSource.class);
            double score = score(stimulus, p, why);
            String key = stimulus.key();
            if (score > scores.getOrDefault(key, -1.0D)) { scores.put(key, score); reasons.put(key, why); byKey.put(key, stimulus); }
            if (score > bestScore) { bestScore = score; best = stimulus; }
        }
        boolean changed = false, lost = false, recovered = false;
        int recoverWindow = (int) (s.attentionRecoverTicks() * p.senses().attentionSpan());
        if (focus != null) {
            Stimulus current = byKey.get(focus.key());
            if (current != null) {
                recovered = focus.lost();
                focus = new AttentionFocus(focus.key(), focus.subject(), focus.type(), focus.category(), focus.label(), current.x(), current.y(), current.z(),
                        scores.get(focus.key()), focus.since(), tick, tick + current.durationTicks(), false, reasons.get(focus.key()));
            } else {
                // A stimulus stays relevant for its own duration: a brief blink of sight, or a hit taken a moment ago, is not "lost".
                if (!focus.lost() && tick > focus.expiresTick()) {
                    lost = true;
                    focus = new AttentionFocus(focus.key(), focus.subject(), focus.type(), focus.category(), focus.label(), focus.x(), focus.y(), focus.z(),
                            focus.score(), focus.since(), focus.lastRefreshed(), focus.expiresTick(), true, focus.reasons());
                }
                if (tick > focus.expiresTick() + recoverWindow) { focus = null; changed = true; }
            }
        }
        if (best != null && bestScore >= s.attentionMinScore()) {
            String key = best.key();
            boolean adopt = focus == null
                    || (!key.equals(focus.key()) && (bestScore > focus.score() * s.attentionSwitchRatio() || focus.lost()));
            if (adopt) {
                focus = new AttentionFocus(key, best.source(), best.type(), best.category(), best.label(), best.x(), best.y(), best.z(), bestScore, tick, tick, tick + best.durationTicks(), false, reasons.get(key));
                changed = true;
                recovered = false;
            }
        }
        level = level(focus, tick);
        return new Update(level, focus, changed, lost, recovered);
    }

    /** Scores one stimulus and records why. Public so the debug tools can explain an attention decision. */
    public static double score(Stimulus stimulus, Perceiver p, Set<AttentionSource> why) {
        double bonus = 0.0D;
        if (stimulus.detail().contains("movement")) { bonus += 0.15D; why.add(AttentionSource.MOVEMENT); }
        if (stimulus.type() == StimulusType.AUDIO) { bonus += 0.10D * stimulus.intensity(); why.add(AttentionSource.SOUND); }
        if (stimulus.type() == StimulusType.DAMAGE || stimulus.category() == StimulusCategory.DAMAGE_TAKEN) { bonus += 0.5D; why.add(AttentionSource.DAMAGE); }
        if (stimulus.category() == StimulusCategory.PLAYER) { bonus += 0.15D; why.add(AttentionSource.PLAYER); }
        if (stimulus.source() != null && p.knows(stimulus.source())) {
            bonus += Math.abs(p.relationTo(stimulus.source())) / 250.0D;
            why.add(AttentionSource.KNOWN_NAME); why.add(AttentionSource.RELATION);
        } else if (stimulus.source() != null && stimulus.type() == StimulusType.VISUAL && stimulus.category() == StimulusCategory.PLAYER) why.add(AttentionSource.NOVELTY);
        if (stimulus.category().threatWeight() > 0) {
            bonus += p.fear() / 200.0D * p.senses().fearfulness();
            if (p.fear() > 10) why.add(AttentionSource.EMOTION);
            if (stimulus.category().threatWeight() >= 50) why.add(AttentionSource.THREAT);
        }
        if (p.anger() > 20 && (stimulus.category() == StimulusCategory.PLAYER || stimulus.category() == StimulusCategory.HOSTILE)) { bonus += p.anger() / 300.0D; why.add(AttentionSource.EMOTION); }
        switch (p.goal()) {
            case "PATROL", "GUARD", "PROTECT", "INVESTIGATE" -> { bonus += 0.10D; why.add(AttentionSource.GOAL); }
            case "REST" -> { bonus -= 0.10D; why.add(AttentionSource.GOAL); }
            default -> { }
        }
        return Math.max(0.0D, stimulus.priority() * (1.0D + bonus));
    }

    private static AttentionLevel level(AttentionFocus focus, long tick) {
        if (focus == null) return AttentionLevel.NONE;
        double score = focus.score();
        int threat = focus.category().threatWeight();
        AttentionLevel result;
        if (focus.category() == StimulusCategory.DAMAGE_TAKEN || (threat >= 70 && score >= 60)) result = AttentionLevel.CRITICAL;
        else if (score >= 75 || (focus.heldTicks(tick) >= 60 && score >= 50)) result = AttentionLevel.FOCUSED;
        else if (score >= 55) result = AttentionLevel.HIGH;
        else if (score >= 30) result = AttentionLevel.MEDIUM;
        else result = AttentionLevel.LOW;
        if (focus.lost() && result.compareTo(AttentionLevel.MEDIUM) > 0) result = AttentionLevel.MEDIUM;
        return result;
    }
}
