package yadi.samuraiai.ai.scheduler.social;

import java.util.List;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.personality.PersonalityEngine;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;

/**
 * Social space: how close is too close for a given personality, what kind of distance a gap is, and where to stand so a
 * crowd does not pile onto a single spot. Pure geometry plus personality; it never moves anyone.
 */
public final class SocialDistance {
    private final SchedulerSettings settings;
    private final PersonalityEngine personality;

    public SocialDistance(SchedulerSettings settings, PersonalityEngine personality) { this.settings = settings; this.personality = personality; }

    public Proxemics classify(double distance, PersonalityTraits traits) {
        double space = personality.personalSpace(traits);
        if (distance < space * 0.5D) return Proxemics.INTIMATE;
        if (distance < space) return Proxemics.PERSONAL;
        if (distance < settings.socialDistance()) return Proxemics.SOCIAL;
        if (distance < settings.publicDistance()) return Proxemics.PUBLIC;
        return Proxemics.DISTANT;
    }

    /** Whether someone this close makes the NPC uncomfortable. */
    public boolean crowded(double distance, PersonalityTraits traits) { return distance < personality.personalSpace(traits); }

    /**
     * Moves a wanted standing point away from others already near it until the NPC's personal space is respected, staying
     * within the crowd spread radius of the original point.
     *
     * @param others x,z pairs of the NPCs already occupying the area
     * @return the adjusted x,z
     */
    public double[] spread(double x, double z, List<double[]> others, PersonalityTraits traits) {
        double space = personality.personalSpace(traits);
        double px = x, pz = z;
        for (int pass = 0; pass < 4; pass++) {
            double pushX = 0, pushZ = 0;
            for (double[] o : others) {
                double dx = px - o[0], dz = pz - o[1];
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d >= space) continue;
                if (d < 1e-6) { dx = 1; dz = 0; d = 1; }
                double push = space - d;
                pushX += dx / d * push; pushZ += dz / d * push;
            }
            if (pushX == 0 && pushZ == 0) break;
            px += pushX; pz += pushZ;
            double offX = px - x, offZ = pz - z, off = Math.sqrt(offX * offX + offZ * offZ);
            if (off > settings.crowdSpreadRadius()) { px = x + offX / off * settings.crowdSpreadRadius(); pz = z + offZ / off * settings.crowdSpreadRadius(); break; }
        }
        return new double[]{px, pz};
    }
}
