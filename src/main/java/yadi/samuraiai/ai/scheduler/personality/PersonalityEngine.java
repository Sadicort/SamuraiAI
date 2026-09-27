package yadi.samuraiai.ai.scheduler.personality;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/**
 * Turns trait values into decisions' inclinations: how much an NPC likes each routine, how it tends to answer a surprise,
 * how it carries itself (the {@link Temperament} handed to perception and navigation), and how experience reshapes it. It
 * holds no state; the traits live with the NPC.
 */
public final class PersonalityEngine {
    /** Change to one trait produced by an experience. */
    public record TraitChange(Trait trait, double before, double after) { }

    private static final Map<RoutineType, Map<Trait, Double>> AFFINITY = new EnumMap<>(RoutineType.class);
    private static final Map<ResponseKind, Map<Trait, Double>> RESPONSE = new EnumMap<>(ResponseKind.class);

    static {
        AFFINITY.put(RoutineType.WAKE, Map.of(Trait.DISCIPLINE, 0.3D));
        AFFINITY.put(RoutineType.PATROL, Map.of(Trait.DISCIPLINE, 0.4D, Trait.COURAGE, 0.3D, Trait.DILIGENCE, 0.2D, Trait.CAUTION, 0.2D));
        AFFINITY.put(RoutineType.WORK, Map.of(Trait.DILIGENCE, 0.6D, Trait.DISCIPLINE, 0.3D, Trait.PATIENCE, 0.2D));
        AFFINITY.put(RoutineType.REST, Map.of(Trait.DILIGENCE, -0.3D, Trait.PATIENCE, 0.2D));
        AFFINITY.put(RoutineType.EAT, Map.of(Trait.SOCIABILITY, 0.1D));
        AFFINITY.put(RoutineType.MEDITATE, Map.of(Trait.SPIRITUALITY, 0.5D, Trait.PATIENCE, 0.4D, Trait.AGGRESSION, -0.3D));
        AFFINITY.put(RoutineType.SLEEP, Map.of(Trait.DILIGENCE, -0.2D, Trait.CAUTION, 0.1D));
        AFFINITY.put(RoutineType.SOCIAL, Map.of(Trait.SOCIABILITY, 0.7D, Trait.CURIOSITY, 0.2D, Trait.CAUTION, -0.1D));
        AFFINITY.put(RoutineType.TRAINING, Map.of(Trait.AGGRESSION, 0.3D, Trait.DISCIPLINE, 0.4D, Trait.COURAGE, 0.3D, Trait.DILIGENCE, 0.3D));
        AFFINITY.put(RoutineType.PRAYER, Map.of(Trait.SPIRITUALITY, 0.7D, Trait.LOYALTY, 0.1D));
        AFFINITY.put(RoutineType.GUARD, Map.of(Trait.LOYALTY, 0.4D, Trait.DISCIPLINE, 0.4D, Trait.CAUTION, 0.3D));
        AFFINITY.put(RoutineType.MERCHANT, Map.of(Trait.SOCIABILITY, 0.4D, Trait.DILIGENCE, 0.3D, Trait.PATIENCE, 0.2D));

        RESPONSE.put(ResponseKind.INVESTIGATE, Map.of(Trait.CURIOSITY, 0.6D, Trait.COURAGE, 0.4D, Trait.CAUTION, -0.4D));
        RESPONSE.put(ResponseKind.FLEE, Map.of(Trait.CAUTION, 0.6D, Trait.COURAGE, -0.7D));
        RESPONSE.put(ResponseKind.RAISE_ALARM, Map.of(Trait.LOYALTY, 0.5D, Trait.CAUTION, 0.3D, Trait.SOCIABILITY, 0.2D));
        RESPONSE.put(ResponseKind.ASSIST, Map.of(Trait.LOYALTY, 0.5D, Trait.COURAGE, 0.4D));
        RESPONSE.put(ResponseKind.WATCH, Map.of(Trait.CAUTION, 0.4D, Trait.PATIENCE, 0.4D));
        RESPONSE.put(ResponseKind.IGNORE, Map.of(Trait.DISCIPLINE, 0.2D, Trait.PATIENCE, 0.2D, Trait.CURIOSITY, -0.4D));
    }

    private final SchedulerSettings settings;

    public PersonalityEngine(SchedulerSettings settings) { this.settings = settings; }

    /** Multiplier (about 0.25..2.5) for how much this personality likes a routine; 1.0 for a neutral one. */
    public double affinity(PersonalityTraits traits, RoutineType routine) {
        double v = 1.0D + settings.affinityStrength() * sum(traits, AFFINITY.get(routine));
        return Math.max(0.25D, Math.min(2.5D, v));
    }

    /** Multiplier for how readily this personality gives each kind of answer to a surprise. */
    public double responseBias(PersonalityTraits traits, ResponseKind kind) {
        double v = 1.0D + settings.affinityStrength() * sum(traits, RESPONSE.get(kind));
        return Math.max(0.3D, Math.min(2.0D, v));
    }

    private static double sum(PersonalityTraits traits, Map<Trait, Double> weights) {
        double total = 0;
        if (weights != null) for (var entry : weights.entrySet()) total += entry.getValue() * traits.lean(entry.getKey());
        return total;
    }

    /** The scale factors this personality gives perception and navigation. */
    public Temperament temperament(PersonalityTraits t) {
        double courage = t.lean(Trait.COURAGE), caution = t.lean(Trait.CAUTION), curiosity = t.lean(Trait.CURIOSITY);
        double patience = t.lean(Trait.PATIENCE), discipline = t.lean(Trait.DISCIPLINE), aggression = t.lean(Trait.AGGRESSION);
        return new Temperament(
                1.0D + 0.20D * caution + 0.10D * curiosity,
                1.0D + 0.25D * caution + 0.10D * curiosity,
                1.0D + 0.60D * curiosity,
                1.0D + 0.40D * caution - 0.25D * courage,
                1.0D - 0.45D * courage + 0.25D * caution,
                1.0D + 0.35D * patience + 0.25D * discipline - 0.25D * curiosity,
                1.0D + 0.12D * aggression - 0.06D * caution,
                1.0D + 0.60D * caution - 0.40D * courage,
                (int) Math.round(Math.max(0.0D, 10.0D - 8.0D * discipline - 6.0D * courage + 4.0D * caution)));
    }

    /** How much room this personality wants around itself, in blocks. */
    public double personalSpace(PersonalityTraits t) {
        return settings.personalSpace() * Math.max(0.4D, 1.0D + 0.35D * t.lean(Trait.CAUTION) - 0.35D * t.lean(Trait.SOCIABILITY));
    }

    /** A personality after an experience, with what changed. */
    public record DriftResult(PersonalityTraits traits, List<TraitChange> changes) { }

    /** Applies an experience to a personality: each trait it touches moves by {@code magnitude} times the configured drift rate. */
    public DriftResult drift(PersonalityTraits traits, DriftCause cause, double magnitude) {
        List<TraitChange> changes = new ArrayList<>();
        PersonalityTraits next = traits;
        for (var effect : cause.effect().entrySet()) {
            double before = next.get(effect.getKey());
            double after = Math.max(0.0D, Math.min(100.0D, before + effect.getValue() * magnitude * settings.personalityDrift()));
            if (Math.abs(after - before) < 1e-9) continue;
            next = next.with(effect.getKey(), after);
            changes.add(new TraitChange(effect.getKey(), before, after));
        }
        return new DriftResult(next, changes);
    }
}
