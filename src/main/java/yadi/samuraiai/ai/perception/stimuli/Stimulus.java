package yadi.samuraiai.ai.perception.stimuli;

import java.util.UUID;

/**
 * One thing an NPC noticed. Every stimulus has an intensity (how strongly it registered, 0..1), a duration (how long it
 * stays relevant), a priority (how much the NPC cares, 0..100) and a category. It is evidence, never an instruction.
 *
 * @param source     entity that caused it, or null (an unexplained sound, weather)
 * @param uncertainty radius in blocks within which the true position lies (0 = exact)
 * @param key        stable identity used to merge repeats: same source and category are the same stimulus
 */
public record Stimulus(StimulusType type, StimulusCategory category, UUID source, String label, double x, double y, double z,
                       double uncertainty, double intensity, double priority, long createdTick, int durationTicks, String detail) {

    public Stimulus {
        if (type == null || category == null) throw new IllegalArgumentException("type and category required");
        label = label == null ? category.name() : label;
        intensity = clamp(intensity, 0.0D, 1.0D);
        priority = clamp(priority, 0.0D, 100.0D);
        uncertainty = Math.max(0.0D, uncertainty);
        durationTicks = Math.max(1, durationTicks);
        detail = detail == null ? "" : detail;
    }

    public static Stimulus at(StimulusType type, StimulusCategory category, UUID source, String label, double x, double y, double z,
                              double intensity, long tick, int durationTicks) {
        return new Stimulus(type, category, source, label, x, y, z, 0.0D, intensity, category.basePriority() * Math.max(0.2D, intensity), tick, durationTicks, "");
    }

    public String key() { return type + ":" + category + ":" + (source == null ? "none" : source); }
    public boolean expired(long tick) { return tick - createdTick >= durationTicks; }
    public Stimulus withPriority(double value) {
        return new Stimulus(type, category, source, label, x, y, z, uncertainty, intensity, value, createdTick, durationTicks, detail);
    }
    public Stimulus withUncertainty(double value, double nx, double ny, double nz) {
        return new Stimulus(type, category, source, label, nx, ny, nz, value, intensity, priority, createdTick, durationTicks, detail);
    }
    public Stimulus withDetail(String value) {
        return new Stimulus(type, category, source, label, x, y, z, uncertainty, intensity, priority, createdTick, durationTicks, value);
    }
    public double distanceTo(double ox, double oy, double oz) {
        double dx = x - ox, dy = y - oy, dz = z - oz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double clamp(double v, double lo, double hi) { return Double.isFinite(v) ? Math.max(lo, Math.min(hi, v)) : lo; }
}
