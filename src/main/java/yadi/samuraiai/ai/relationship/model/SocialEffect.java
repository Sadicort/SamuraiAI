package yadi.samuraiai.ai.relationship.model;

/** How an experience moves each axis, in points at full importance (positive raises the axis). The cognition catalogue defines these per kind of experience. */
public record SocialEffect(double trust, double respect, double affinity, double fear, double loyalty, double rivalry, double honor) {
    public static final SocialEffect NONE = new SocialEffect(0, 0, 0, 0, 0, 0, 0);

    public double get(Dimension d) {
        return switch (d) { case TRUST -> trust; case RESPECT -> respect; case AFFINITY -> affinity; case FEAR -> fear; case LOYALTY -> loyalty; case RIVALRY -> rivalry; case HONOR -> honor; };
    }

    public boolean isNone() { return trust == 0 && respect == 0 && affinity == 0 && fear == 0 && loyalty == 0 && rivalry == 0 && honor == 0; }

    public SocialEffect scaled(double factor) { return new SocialEffect(trust * factor, respect * factor, affinity * factor, fear * factor, loyalty * factor, rivalry * factor, honor * factor); }
    public SocialEffect withHonor(double value) { return new SocialEffect(trust, respect, affinity, fear, loyalty, rivalry, value); }
}
