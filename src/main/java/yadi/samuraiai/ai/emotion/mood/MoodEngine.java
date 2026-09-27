package yadi.samuraiai.ai.emotion.mood;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.MoodKind;

/**
 * Mood is the dominant, persistent emotional state, not one emotion. Each mood has a score that follows the active emotions
 * through an inertial average (so it moves gradually), and the mood only switches when another mood has led by a margin and
 * the current one has lasted a minimum time: a passing shock does not flip it, a long grief does.
 */
public final class MoodEngine {
    private static final Map<MoodKind, Map<EmotionKind, Double>> CONTRIBUTIONS = new EnumMap<>(MoodKind.class);

    static {
        put(MoodKind.PEACEFUL, EmotionKind.CALM, 1.0D, EmotionKind.COMPASSION, 0.3D);
        put(MoodKind.FOCUSED, EmotionKind.DETERMINATION, 1.0D, EmotionKind.CURIOSITY, 0.4D, EmotionKind.CALM, 0.3D);
        put(MoodKind.HAPPY, EmotionKind.JOY, 1.0D, EmotionKind.PRIDE, 0.5D, EmotionKind.GRATITUDE, 0.6D);
        put(MoodKind.MELANCHOLIC, EmotionKind.SADNESS, 1.0D, EmotionKind.LONELINESS, 0.9D, EmotionKind.GUILT, 0.5D, EmotionKind.SHAME, 0.3D);
        put(MoodKind.ANGRY, EmotionKind.ANGER, 1.0D, EmotionKind.DISTRUST, 0.3D);
        put(MoodKind.FEARFUL, EmotionKind.FEAR, 1.0D, EmotionKind.ANXIETY, 0.6D);
        put(MoodKind.HOPEFUL, EmotionKind.HOPE, 1.0D, EmotionKind.DETERMINATION, 0.3D);
        put(MoodKind.INSPIRED, EmotionKind.INSPIRATION, 1.0D, EmotionKind.JOY, 0.3D, EmotionKind.CURIOSITY, 0.3D);
        put(MoodKind.EXHAUSTED, EmotionKind.EMOTIONAL_FATIGUE, 1.0D);
        put(MoodKind.ALERT, EmotionKind.ANXIETY, 0.5D, EmotionKind.FEAR, 0.4D, EmotionKind.SURPRISE, 0.6D, EmotionKind.DISTRUST, 0.4D);
    }

    private static void put(MoodKind mood, Object... pairs) {
        Map<EmotionKind, Double> map = new EnumMap<>(EmotionKind.class);
        for (int i = 0; i < pairs.length; i += 2) map.put((EmotionKind) pairs[i], (Double) pairs[i + 1]);
        CONTRIBUTIONS.put(mood, map);
    }

    /** The mood each emotion would push towards on its own (0-1 per mood), before smoothing. */
    public double[] targets(EmotionRuntime rt, EmotionSettings s) {
        double[] target = new double[MoodKind.values().length];
        for (MoodKind mood : MoodKind.values()) {
            if (mood == MoodKind.NEUTRAL) { target[mood.ordinal()] = s.moodBaseline(); continue; }
            double sum = 0;
            for (var c : CONTRIBUTIONS.get(mood).entrySet()) sum = Math.max(sum, rt.intensityOf(c.getKey()) / 100.0D * c.getValue());
            target[mood.ordinal()] = sum;
        }
        return target;
    }

    /** Advances the mood scores by {@code elapsed} ticks and switches mood if warranted. @return the previous mood when it changed */
    public Optional<MoodKind> update(EmotionRuntime rt, long now, long elapsed, EmotionSettings s) {
        double[] target = targets(rt, s);
        double[] scores = rt.moodScores();
        double steps = Math.max(1.0D, (double) elapsed / s.moodUpdateTicks());
        double alpha = 1.0D - Math.pow(s.moodInertia(), steps);
        for (int i = 0; i < scores.length; i++) scores[i] += (target[i] - scores[i]) * alpha;
        MoodKind current = rt.mood();
        MoodKind best = current;
        double bestScore = scores[current.ordinal()];
        for (MoodKind m : MoodKind.values()) if (scores[m.ordinal()] > bestScore) { best = m; bestScore = scores[m.ordinal()]; }
        // Falling back to neutral needs no margin: the mood has simply faded below the level of "nothing in particular".
        boolean clearLead = bestScore >= scores[current.ordinal()] + s.moodSwitchMargin() || best == MoodKind.NEUTRAL;
        if (best != current && clearLead && now - rt.moodSince() >= s.moodMinTicks()) {
            rt.mood(best, now);
            return Optional.of(current);
        }
        return Optional.empty();
    }

    public static double valence(MoodKind mood) { return mood.valence(); }

    public static Map<EmotionKind, Double> contributions(MoodKind mood) { return CONTRIBUTIONS.getOrDefault(mood, Map.of()); }

}
