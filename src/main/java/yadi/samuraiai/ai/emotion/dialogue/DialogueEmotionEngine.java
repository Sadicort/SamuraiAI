package yadi.samuraiai.ai.emotion.dialogue;

import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.model.Blend;
import yadi.samuraiai.ai.emotion.model.DialogueTone;

/** How the current feelings would colour speech: vocabulary formality, length, pace, silences, questions and warmth, plus a voice tone label a future voice engine can use. */
public final class DialogueEmotionEngine {
    private static double c(double v) { return Math.max(0.0D, Math.min(1.0D, v)); }

    public DialogueTone tone(EmotionRuntime rt, Blend blend) {
        if (blend == null || blend.intensity() < 15.0D) return DialogueTone.NEUTRAL;
        double joy = rt.intensityOf(EmotionKind.JOY) / 100.0D, anger = rt.intensityOf(EmotionKind.ANGER) / 100.0D, sad = rt.intensityOf(EmotionKind.SADNESS) / 100.0D;
        double fear = rt.intensityOf(EmotionKind.FEAR) / 100.0D, shame = rt.intensityOf(EmotionKind.SHAME) / 100.0D, curiosity = rt.intensityOf(EmotionKind.CURIOSITY) / 100.0D;
        double respect = rt.intensityOf(EmotionKind.RESPECT) / 100.0D, distrust = rt.intensityOf(EmotionKind.DISTRUST) / 100.0D;
        double formality = c(0.5D + 0.3D * anger + 0.3D * respect + 0.2D * distrust - 0.3D * joy);
        double verbosity = c(0.5D + 0.4D * joy + 0.3D * curiosity - 0.4D * sad - 0.3D * anger - 0.2D * shame);
        double pace = c(0.3D + 0.5D * blend.arousal() + 0.2D * fear);
        double silence = c(0.1D + 0.5D * sad + 0.4D * shame + 0.2D * fear);
        double questions = c(0.3D + 0.5D * curiosity + 0.2D * fear - 0.2D * anger);
        double warmth = c(0.5D + 0.5D * blend.valence());
        String voice = anger > 0.4D ? "cold" : fear > 0.4D ? "trembling" : sad > 0.4D ? "flat" : joy > 0.4D ? "warm" : curiosity > 0.4D ? "lively" : "neutral";
        return new DialogueTone(formality, verbosity, pace, silence, questions, warmth, voice);
    }
}
