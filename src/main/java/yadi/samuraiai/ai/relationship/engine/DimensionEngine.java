package yadi.samuraiai.ai.relationship.engine;

import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/**
 * The shared mechanics of one axis: a raw effect is scaled by how much room the axis has left in that direction, bent by the
 * personality rules and by the NPC's current mood (a passing mood can tilt the reading by only a bounded amount), and then
 * refined by the axis's own rule ({@link #adjust}). Each axis has its own subclass for its levels and special rules.
 */
public abstract class DimensionEngine {
    private final Dimension dimension;

    protected DimensionEngine(Dimension dimension) { this.dimension = dimension; }

    public Dimension dimension() { return dimension; }

    public final double modulate(double raw, RelationshipRecord r, Modulation m, RelationshipSettings s) {
        if (raw == 0 || !Double.isFinite(raw)) return 0;
        boolean up = raw > 0;
        double value = r.get(dimension);
        double headroom = Math.max(s.headroomFloor(), up ? 1.0D - value / 100.0D : value / 100.0D);
        double f = raw * headroom * s.effectScale();
        double multiplier = 1.0D;
        for (TraitRule rule : m.rules()) {
            if (rule.dimension() != dimension) continue;
            if (rule.sign() != 'B' && (rule.sign() == 'G') != up) continue;
            multiplier *= Math.max(0.1D, 1.0D + rule.factor() * m.personality().lean(rule.trait()));
        }
        boolean good = up == dimension.higherIsBetter();
        double mood = Math.max(-1.0D, Math.min(1.0D, m.mood()));
        multiplier *= 1.0D + s.moodInfluenceCap() * mood * (good ? 1.0D : -1.0D);
        return adjust(f * multiplier, up, r, m, s);
    }

    /** The axis's own rule. The default leaves the value alone. */
    protected double adjust(double delta, boolean up, RelationshipRecord r, Modulation m, RelationshipSettings s) { return delta; }
}
