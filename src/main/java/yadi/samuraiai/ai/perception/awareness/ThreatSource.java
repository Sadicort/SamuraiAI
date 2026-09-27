package yadi.samuraiai.ai.perception.awareness;

import java.util.UUID;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;

/** One reason to feel threatened, with where it is (as far as the NPC knows) and how serious it is right now. */
public record ThreatSource(String key, UUID source, StimulusCategory category, String label, double x, double y, double z,
                           double score, long lastTick) {
    public ThreatSource withScore(double value) { return new ThreatSource(key, source, category, label, x, y, z, value, lastTick); }
}
