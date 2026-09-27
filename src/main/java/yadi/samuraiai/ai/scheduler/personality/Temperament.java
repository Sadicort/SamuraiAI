package yadi.samuraiai.ai.scheduler.personality;

/**
 * What a personality means for the other engines, expressed as plain scale factors (1.0 = neutral) so that neither
 * perception nor navigation has to know anything about traits: a bolder NPC sees and hears the same world but reacts less
 * to alarms, a cautious one keeps further from danger.
 */
public record Temperament(double visionScale, double hearingScale, double curiosity, double suspicionGain, double fearfulness,
                          double attentionSpan, double speedFactor, double dangerAversion, int reactionDelayTicks) {
    public static final Temperament NEUTRAL = new Temperament(1, 1, 1, 1, 1, 1, 1, 1, 0);
}
