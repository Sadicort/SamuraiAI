package yadi.samuraiai.ai.perception.hearing;

import java.util.UUID;

/**
 * Something that made noise at a place and time. Recorded once in the dimension's {@link SoundLog}; each NPC then decides
 * for itself whether it heard it. {@code loudness} is 0..1.
 */
public record SoundEvent(UUID id, SoundCategory category, UUID source, double x, double y, double z, double loudness, long tick,
                         int durationTicks, String label) {
    public SoundEvent {
        loudness = Double.isFinite(loudness) ? Math.max(0.0D, Math.min(1.0D, loudness)) : 0.0D;
        durationTicks = Math.max(1, durationTicks);
        label = label == null ? category.name() : label;
    }
    public static SoundEvent of(SoundCategory category, UUID source, double x, double y, double z, double loudness, long tick) {
        return new SoundEvent(UUID.randomUUID(), category, source, x, y, z, loudness, tick, 40, category.name());
    }
}
