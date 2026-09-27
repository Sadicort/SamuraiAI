package yadi.samuraiai.ai.emotion.physiology;

import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.Physiology;

/** How the feelings colour the body: fear quickens, sadness and fatigue slow, anger and fear sharpen turning, anxiety makes the gaze restless and reactions slower. Numbers for the behavior layer to use; emotion never moves the entity. */
public final class PhysiologyEngine {
    public Physiology physiology(EmotionRuntime rt, EmotionSettings s) {
        double fear = rt.intensityOf(EmotionKind.FEAR) / 100.0D, sad = Math.max(rt.intensityOf(EmotionKind.SADNESS), rt.intensityOf(EmotionKind.EMOTIONAL_FATIGUE)) / 100.0D;
        double anger = rt.intensityOf(EmotionKind.ANGER) / 100.0D, anxiety = rt.intensityOf(EmotionKind.ANXIETY) / 100.0D, calm = rt.intensityOf(EmotionKind.CALM) / 100.0D;
        double speed = 1.0D + s.fearSpeedBoost() * fear - s.sadnessSpeedDrop() * sad;
        double turn = 1.0D + 0.3D * (anger + fear) - 0.2D * sad;
        double gaze = Math.max(0.3D, 1.0D - 0.5D * anxiety + 0.1D * calm);
        int delay = (int) Math.round(s.reactionDelayMax() * Math.min(1.0D, sad * 0.6D + anxiety * 0.4D));
        double posture = Math.max(-1.0D, Math.min(1.0D, 0.6D * (rt.intensityOf(EmotionKind.PRIDE) + rt.intensityOf(EmotionKind.DETERMINATION)) / 100.0D - 0.6D * (sad + fear * 0.5D)));
        return new Physiology(Math.max(0.4D, Math.min(1.5D, speed)), Math.max(0.5D, Math.min(1.6D, turn)), gaze, delay, posture);
    }
}
