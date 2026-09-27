package yadi.samuraiai.ai.emotion.propagation;

import java.util.UUID;

/** Another NPC within reach of an emotion: how far, how much it trusts the source (0-100) and how sociable it is (-1..+1). Supplied by the world adapter. */
public record EmotionNeighbor(UUID id, double distance, double trustInSource, double sociabilityLean) { }
