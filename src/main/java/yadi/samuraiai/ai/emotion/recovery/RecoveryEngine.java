package yadi.samuraiai.ai.emotion.recovery;

import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.RecoverySource;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;

/** Each trauma recovers on its own progress (0-1): with time, and faster with sleep, meditation, friendship, good events and talking. A resilient NPC recovers faster. Recovery never erases the memory, only its sting. */
public final class RecoveryEngine {
    public static double amount(RecoverySource source, EmotionSettings s) {
        return switch (source) {
            case TIME -> s.recoveryPerDay(); case SLEEP -> s.recoverySleep(); case MEDITATION -> s.recoveryMeditation(); case FRIENDSHIP -> s.recoveryFriendship();
            case POSITIVE_EVENT -> s.recoveryPositive(); case CONVERSATION -> s.recoveryConversation();
        };
    }

    /** @return true when the trauma just finished recovering */
    public boolean advance(TraumaRecord t, double amount, double resilience, long now, EmotionSettings s) {
        if (!t.active() || amount <= 0) return false;
        t.progress(t.progress() + amount * Math.max(0.1D, resilience));
        t.lastProgress(now);
        if (t.progress() >= s.traumaRecoveredAt()) { t.phase(TraumaRecord.Phase.RECOVERED); t.bump(); return true; }
        if (t.progress() > 0.15D && t.phase() == TraumaRecord.Phase.ACTIVE) t.phase(TraumaRecord.Phase.RECOVERING);
        t.bump();
        return false;
    }
}
