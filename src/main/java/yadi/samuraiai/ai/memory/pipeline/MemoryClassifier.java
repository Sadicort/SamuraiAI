package yadi.samuraiai.ai.memory.pipeline;

import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Experience;
import yadi.samuraiai.ai.memory.model.MemoryType;

/** Chooses how a kept experience is held: place-defining ones as spatial, practised ones as procedural, deeply felt ones as emotional, the rest as episodes. */
public final class MemoryClassifier {
    public MemoryType classify(Experience e, MemorySettings s) {
        switch (e.kind()) {
            case DISCOVERED_PLACE, VISITED_PLACE -> { return MemoryType.SPATIAL; }
            case PATROLLED, TRAVELED, MEDITATED, TRAINED_TOGETHER -> { return MemoryType.PROCEDURAL; }
            case CELEBRATED, ATTENDED_RITUAL -> { return MemoryType.TEMPORAL; }
            default -> { }
        }
        if (e.emotion().intensity() >= 0.7D || e.emotion().traumatic()) return MemoryType.EMOTIONAL;
        return MemoryType.EPISODIC;
    }
}
