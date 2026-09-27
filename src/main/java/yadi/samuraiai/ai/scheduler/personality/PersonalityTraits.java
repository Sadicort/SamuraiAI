package yadi.samuraiai.ai.scheduler.personality;

import java.util.Arrays;
import java.util.Map;

/** One NPC's ten trait values (0-100). Immutable: changing a trait yields a new instance. */
public final class PersonalityTraits {
    private final double[] values;

    private PersonalityTraits(double[] values) { this.values = values; }

    public static PersonalityTraits neutral() {
        double[] v = new double[Trait.values().length];
        Arrays.fill(v, 50.0D);
        return new PersonalityTraits(v);
    }

    /** Neutral except for the traits given. */
    public static PersonalityTraits of(Map<Trait, Double> baseline) {
        double[] v = neutral().values;
        baseline.forEach((trait, value) -> v[trait.ordinal()] = clamp(value));
        return new PersonalityTraits(v);
    }

    /** A deterministic individual: every trait moved by up to {@code amount} points either way, derived from the seed. */
    public PersonalityTraits individual(long seed, double amount) {
        double[] v = values.clone();
        long state = seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        for (int i = 0; i < v.length; i++) {
            state ^= state << 13; state ^= state >>> 7; state ^= state << 17;
            double unit = ((state >>> 11) & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL * 2.0D - 1.0D;
            v[i] = clamp(v[i] + unit * amount);
        }
        return new PersonalityTraits(v);
    }

    public double get(Trait trait) { return values[trait.ordinal()]; }
    /** The trait as a 0..1 fraction. */
    public double unit(Trait trait) { return values[trait.ordinal()] / 100.0D; }
    /** The trait relative to the neutral 50, -1..+1. */
    public double lean(Trait trait) { return (values[trait.ordinal()] - 50.0D) / 50.0D; }

    public PersonalityTraits with(Trait trait, double value) {
        double[] v = values.clone();
        v[trait.ordinal()] = clamp(value);
        return new PersonalityTraits(v);
    }

    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.0D, Math.min(100.0D, v)) : 50.0D; }

    @Override public boolean equals(Object o) { return o instanceof PersonalityTraits p && Arrays.equals(values, p.values); }
    @Override public int hashCode() { return Arrays.hashCode(values); }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Trait t : Trait.values()) sb.append(sb.length() == 0 ? "" : " ").append(t.name().toLowerCase()).append('=').append(Math.round(values[t.ordinal()]));
        return sb.toString();
    }
}
