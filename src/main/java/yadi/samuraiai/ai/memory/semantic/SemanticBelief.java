package yadi.samuraiai.ai.memory.semantic;

import yadi.samuraiai.ai.cognition.model.EntityRef;

/** A stable belief distilled from experience ("Yeremi is trustworthy"): the value runs -1..+1, the confidence grows with support. */
public final class SemanticBelief {
    private final EntityRef subject;
    private final Aspect aspect;
    private double value, confidence;
    private int support;
    private long updated;

    public SemanticBelief(EntityRef subject, Aspect aspect, double value, double confidence, int support, long updated) {
        this.subject = subject; this.aspect = aspect; this.value = value; this.confidence = confidence; this.support = support; this.updated = updated;
    }

    public EntityRef subject() { return subject; }
    public Aspect aspect() { return aspect; }
    public double value() { return value; }
    public double confidence() { return confidence; }
    public int support() { return support; }
    public long updated() { return updated; }

    void apply(double delta, double rate, long now) {
        double step = Math.max(0.05D, rate * (1.0D - confidence * 0.5D));
        value = Math.max(-1.0D, Math.min(1.0D, value + (delta - value * Math.abs(delta) * 0.5D) * step));
        support++;
        confidence = Math.min(1.0D, 1.0D - Math.pow(0.8D, support));
        updated = now;
    }
}
