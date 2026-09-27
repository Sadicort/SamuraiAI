package yadi.samuraiai.ai.memory.model;

import yadi.samuraiai.ai.memory.semantic.Aspect;

/** What an experience says about its subject (this person is dangerous, this place is safe) and how strongly: input to semantic memory. */
public record Impression(Aspect aspect, double delta) {
    public Impression { delta = Double.isFinite(delta) ? Math.max(-1.0D, Math.min(1.0D, delta)) : 0.0D; }
}
