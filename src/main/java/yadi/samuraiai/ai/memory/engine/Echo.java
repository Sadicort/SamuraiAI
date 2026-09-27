package yadi.samuraiai.ai.memory.engine;

import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;

/** A memory reawakening a feeling because its place or person is here again. */
public record Echo(UUID memoryId, EmotionKind emotion, double intensity, boolean traumatic) { }
