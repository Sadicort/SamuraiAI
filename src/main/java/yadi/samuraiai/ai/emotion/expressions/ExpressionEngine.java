package yadi.samuraiai.ai.emotion.expressions;

import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.Blend;
import yadi.samuraiai.ai.emotion.model.Expression;

/** Picks the body-language cue that matches the current blend. Advice for a future animation system (GeckoLib or otherwise). */
public final class ExpressionEngine {
    public Expression expression(Blend blend, EmotionSettings s) {
        if (blend == null || blend.intensity() < s.expressionThreshold()) return Expression.NEUTRAL;
        return switch (blend.dominant()) {
            case FEAR, ANXIETY -> Expression.STEP_BACK;
            case SADNESS, SHAME, GUILT, LONELINESS, EMOTIONAL_FATIGUE -> Expression.LOOK_DOWN;
            case PRIDE, HOPE, INSPIRATION, DETERMINATION -> Expression.LOOK_UP;
            case ANGER, DISTRUST -> Expression.CROSS_ARMS;
            case CURIOSITY, SURPRISE -> Expression.TILT_HEAD;
            case JOY, COMPASSION, GRATITUDE, RESPECT -> Expression.STEP_FORWARD;
            default -> Expression.NEUTRAL;
        };
    }

    public static boolean approaching(Expression e) { return e == Expression.STEP_FORWARD; }
    public static boolean withdrawing(Expression e) { return e == Expression.STEP_BACK; }
}
