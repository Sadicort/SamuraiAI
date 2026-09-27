package yadi.samuraiai.ai.perception.attention;

import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * What the NPC is currently paying attention to. {@code lost} means the source is no longer being perceived but the NPC
 * has not given up on it yet (it may recover it within the recovery window).
 */
public record AttentionFocus(String key, UUID subject, StimulusType type, StimulusCategory category, String label,
                             double x, double y, double z, double score, long since, long lastRefreshed, long expiresTick, boolean lost,
                             Set<AttentionSource> reasons) {
    public AttentionFocus {
        reasons = Set.copyOf(reasons);
    }
    public long heldTicks(long tick) { return Math.max(0L, tick - since); }
}
