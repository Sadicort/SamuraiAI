package yadi.samuraiai.ai.emotion.regulation;

import java.util.EnumMap;
import java.util.Map;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.emotion.model.Technique;

/**
 * Regulation: how personality shapes coping. A disciplined NPC regulates efficiently; an impulsive one barely does. When the
 * total unpleasant load passes a threshold the NPC "wants" a technique, chosen by character (meditate, talk, isolate, breathe,
 * sleep); what it is actually doing (sleeping, meditating, talking) is what really regulates.
 */
public final class RegulationEngine {
    public double efficiency(PersonalityView p) { return 0.6D + 0.4D * p.unit(Trait.DISCIPLINE); }

    public RegulationAdvice advise(double unpleasantLoad, double fatigue, PersonalityView p, EmotionSettings s) {
        if (unpleasantLoad < s.regulationThreshold()) return RegulationAdvice.NONE;
        Map<Technique, Double> score = new EnumMap<>(Technique.class);
        score.put(Technique.BREATHE, 0.3D + 0.3D * p.lean(Trait.PATIENCE));
        score.put(Technique.MEDITATE, 0.2D + 0.6D * p.lean(Trait.SPIRITUALITY) + 0.3D * p.lean(Trait.DISCIPLINE));
        score.put(Technique.TALK, 0.2D + 0.6D * p.lean(Trait.SOCIABILITY));
        score.put(Technique.ISOLATE, 0.1D + 0.4D * p.lean(Trait.CAUTION) - 0.4D * p.lean(Trait.SOCIABILITY));
        score.put(Technique.SLEEP, 0.1D + fatigue / 100.0D);
        Technique best = Technique.BREATHE;
        double top = -1e9;
        for (var e : score.entrySet()) if (e.getValue() > top) { top = e.getValue(); best = e.getKey(); }
        return new RegulationAdvice(best, Math.min(1.0D, (unpleasantLoad - s.regulationThreshold()) / Math.max(1.0D, s.regulationThreshold())));
    }

    /** The decay multiplier of what the NPC is doing (or deliberately applying). */
    public double boost(Technique technique, Activity activity, EmotionSettings s) {
        double b = 1.0D;
        Technique effective = technique;
        if (effective == Technique.NONE) effective = switch (activity) { case SLEEPING -> Technique.SLEEP; case MEDITATING -> Technique.MEDITATE; case TALKING -> Technique.TALK; default -> Technique.NONE; };
        switch (effective) {
            case BREATHE -> b = s.breatheBoost(); case MEDITATE -> b = s.meditateBoost(); case SLEEP -> b = s.sleepBoost(); case TALK -> b = s.talkBoost(); case ISOLATE -> b = s.isolateBoost();
            default -> { if (activity == Activity.RESTING) b = s.restBoost(); }
        }
        return b;
    }

    public static Technique implied(Activity activity) {
        return switch (activity) { case SLEEPING -> Technique.SLEEP; case MEDITATING -> Technique.MEDITATE; case TALKING -> Technique.TALK; default -> Technique.NONE; };
    }
}
