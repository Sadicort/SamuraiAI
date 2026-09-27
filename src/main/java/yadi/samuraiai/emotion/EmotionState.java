package yadi.samuraiai.emotion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Short-lived emotional state (0-100 per emotion), as opposed to Personality
 * which represents stable long-term tendencies. The DecisionEngine reads this
 * to weigh Goals (high FEAR favours FLEE) and the prompt layer turns it into
 * words the model can act on.
 *
 * <p>Synchronised because emotions are adjusted from the server thread while
 * the dialogue pipeline snapshots them off-thread when building a prompt.
 */
public class EmotionState {

    /**
     * How far an emotion moves back towards its baseline per decay step. Slow
     * enough that a scare still colours the next few exchanges, fast enough
     * that an NPC is not permanently traumatised by one hit.
     */
    private static final int DECAY_STEP = 2;

    private final Map<Emotion, Integer> values = new EnumMap<>(Emotion.class);

    public EmotionState() {
        for (Emotion emotion : Emotion.values()) {
            values.put(emotion, emotion.baseline());
        }
    }

    public synchronized int get(Emotion emotion) {
        return values.getOrDefault(emotion, emotion.baseline());
    }

    synchronized void adjust(Emotion emotion, int amount) {
        values.put(emotion, clamp(get(emotion) + amount));
    }

    synchronized void set(Emotion emotion, int amount) {
        values.put(emotion, clamp(amount));
    }

    /**
     * Nudges every emotion one step towards its baseline. Called from the
     * brain tick, so "how fast do feelings fade" follows the configured tick
     * interval instead of needing its own timer.
     *
     * @return true if anything actually changed, so callers can skip
     *         publishing a no-op update
     */
    synchronized boolean decay() {

        boolean changed = false;

        for (Emotion emotion : Emotion.values()) {

            int current = get(emotion);
            int baseline = emotion.baseline();

            if (current == baseline) {
                continue;
            }

            int next = current > baseline
                    ? Math.max(baseline, current - DECAY_STEP)
                    : Math.min(baseline, current + DECAY_STEP);

            values.put(emotion, next);
            changed = true;
        }

        return changed;
    }

    /** Immutable copy, safe to hand to another thread. */
    public synchronized Map<Emotion, Integer> snapshot() {
        return Map.copyOf(values);
    }

    /**
     * The strongest feeling other than CALM, which sits high by default and
     * would otherwise always win. Empty when the NPC is emotionally flat.
     */
    public synchronized Optional<Emotion> dominant() {
        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != Emotion.CALM)
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /**
     * Emotions strong enough to be worth mentioning in a prompt, strongest
     * first. Weak background noise is filtered out so the model is not handed
     * ten near-zero numbers to reason about.
     */
    public synchronized List<Emotion> notable(int threshold) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .sorted(Map.Entry.<Emotion, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
