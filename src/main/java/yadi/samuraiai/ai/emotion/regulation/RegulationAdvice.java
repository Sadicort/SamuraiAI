package yadi.samuraiai.ai.emotion.regulation;

import yadi.samuraiai.ai.emotion.model.Technique;

/** What the NPC would like to do to calm down and how badly (0-1). The scheduler may weigh it; the emotion engine does not act on it. */
public record RegulationAdvice(Technique technique, double urgency) {
    public static final RegulationAdvice NONE = new RegulationAdvice(Technique.NONE, 0.0D);
}
