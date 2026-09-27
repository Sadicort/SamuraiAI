package yadi.samuraiai.ai.knowledge.model;

/** One step in a piece of knowledge's history: confidence and state before and after, and why. */
public record Revision(long at, double oldConfidence, double newConfidence, ValidationState oldState, ValidationState newState, String reason) {
    public Revision { reason = reason == null ? "" : reason; }
}
