package yadi.samuraiai.ai.scheduler.emotion;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.personality.Trait;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/**
 * Translates feelings into scheduling pressure. Fear and anxiety pull towards calming or sheltering routines and eventually
 * become an emergency; sadness towards contemplation and company; anger towards effort and away from rest; joy towards
 * company. The mood only changes when another one clearly overtakes it, so a flickering emotion cannot make the schedule flap.
 */
public final class EmotionScheduler {
    /** Remembers the current mood of one NPC so that changes can be detected and damped. */
    public static final class Tracker {
        private Mood mood = Mood.CALM;
        private double strength;
        public Mood mood() { return mood; }
        public double strength() { return strength; }
    }

    /** A change of mood, for the event stream. */
    public record MoodChange(Mood from, Mood to, double strength) { }

    private final SchedulerSettings settings;

    public EmotionScheduler(SchedulerSettings settings) { this.settings = settings; }

    /** Updates the tracker from the current feelings; the change is present when the mood actually switched. */
    public Optional<MoodChange> update(Tracker tracker, EmotionInput e) {
        Mood candidate = classify(e);
        double candidateStrength = strengthOf(candidate, e);
        double currentStrength = strengthOf(tracker.mood, e);
        if (candidate != tracker.mood && candidateStrength >= currentStrength + settings.emotionShiftThreshold()) {
            MoodChange change = new MoodChange(tracker.mood, candidate, candidateStrength);
            tracker.mood = candidate;
            tracker.strength = candidateStrength;
            return Optional.of(change);
        }
        tracker.strength = tracker.mood == candidate ? candidateStrength : currentStrength;
        return Optional.empty();
    }

    /** Pressure from the tracker's mood on each routine, scaled by how strongly it is felt and how emotional the NPC is. */
    public EmotionInfluence influence(Tracker tracker, EmotionInput e, PersonalityTraits traits) {
        double gain = settings.emotionInfluence() * (1.0D - 0.3D * traits.lean(Trait.DISCIPLINE));
        double s = Math.min(1.0D, tracker.strength / 100.0D) * gain;
        Map<RoutineType, Double> bias = new EnumMap<>(RoutineType.class);
        boolean panic = e.fear() >= settings.fearEmergency() * (1.0D + 0.25D * traits.lean(Trait.COURAGE));
        switch (tracker.mood) {
            case PANICKED -> { bias.put(RoutineType.REST, -60 * s); bias.put(RoutineType.SOCIAL, -50 * s); bias.put(RoutineType.WORK, -40 * s); bias.put(RoutineType.SLEEP, -60 * s); bias.put(RoutineType.GUARD, 10 * s); }
            case ANXIOUS -> { bias.put(RoutineType.MEDITATE, 30 * s); bias.put(RoutineType.PRAYER, 25 * s); bias.put(RoutineType.SLEEP, -30 * s); bias.put(RoutineType.GUARD, 15 * s); bias.put(RoutineType.SOCIAL, -15 * s); }
            case GRIEVING -> { bias.put(RoutineType.PRAYER, 30 * s); bias.put(RoutineType.MEDITATE, 25 * s); bias.put(RoutineType.SOCIAL, 20 * traits.lean(Trait.SOCIABILITY) * s); bias.put(RoutineType.WORK, -15 * s); }
            case ANGRY -> { bias.put(RoutineType.TRAINING, 30 * s); bias.put(RoutineType.PATROL, 15 * s); bias.put(RoutineType.REST, -25 * s); bias.put(RoutineType.MEDITATE, -10 * s); bias.put(RoutineType.SOCIAL, -20 * s); }
            case CONTENT -> { bias.put(RoutineType.SOCIAL, 20 * s); bias.put(RoutineType.WORK, 8 * s); }
            case CALM -> { bias.put(RoutineType.MEDITATE, 5 * s); }
        }
        return new EmotionInfluence(tracker.mood, tracker.strength, bias, (e.fear() + e.anxiety()) / 200.0D * 4.0D, panic);
    }

    Mood classify(EmotionInput e) {
        double fear = e.fear(), anger = e.anger(), sadness = e.sadness(), anxiety = e.anxiety(), joy = e.joy();
        double top = Math.max(Math.max(fear, anger), Math.max(Math.max(sadness, anxiety), joy));
        if (top < 25) return e.calm() >= 40 ? Mood.CALM : Mood.CONTENT;
        if (fear == top && fear >= settings.fearEmergency()) return Mood.PANICKED;
        if (anger == top && anger >= settings.angerAggression() / 2.0D) return Mood.ANGRY;
        if (fear == top || anxiety == top) return Mood.ANXIOUS;
        if (sadness == top) return Mood.GRIEVING;
        return Mood.CONTENT;
    }

    private double strengthOf(Mood mood, EmotionInput e) {
        return switch (mood) {
            case PANICKED -> e.fear();
            case ANGRY -> e.anger();
            case ANXIOUS -> Math.max(e.fear(), e.anxiety());
            case GRIEVING -> e.sadness();
            case CONTENT -> e.joy();
            case CALM -> e.calm() / 2.0D;
        };
    }
}
