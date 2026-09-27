package yadi.samuraiai.ai.perception.memory;

import yadi.samuraiai.ai.perception.engine.PerceptionSettings;

/** Kinds of perceptual memory, each with its own lifetime: danger outlasts a footstep, a place outlasts a glimpse. */
public enum MemoryKind {
    VISUAL, AUDITORY, ENVIRONMENTAL, SOCIAL, DANGER, INTEREST;

    public int lifetimeTicks(PerceptionSettings s) {
        return switch (this) {
            case VISUAL -> s.visualMemoryTicks();
            case AUDITORY -> s.auditoryMemoryTicks();
            case ENVIRONMENTAL -> s.environmentMemoryTicks();
            case SOCIAL -> s.socialMemoryTicks();
            case DANGER -> s.dangerMemoryTicks();
            case INTEREST -> s.interestMemoryTicks();
        };
    }
}
