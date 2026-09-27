package yadi.samuraiai.ai.relationship.evolution;

import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.Dimension;
import yadi.samuraiai.ai.relationship.model.RelationState;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;

/**
 * Relationships cool with disuse and never vanish: after a period without contact, trust and affinity drift back towards the
 * level a stranger would get, fear and rivalry ease, respect and loyalty fade very slowly, and honor observed does not fade.
 * Decay is computed from the time since the last pass, so it is right however long the NPC was unloaded.
 */
public final class RelationshipEvolutionEngine {
    private static double relax(double value, double target, double elapsed, double halfLife) {
        if (elapsed <= 0) return value;
        return target + (value - target) * Math.pow(0.5D, elapsed / halfLife);
    }

    /** @return whether any value changed */
    public boolean decay(RelationshipRecord r, long now, RelationshipSettings s) {
        long idle = now - r.lastInteraction();
        long elapsed = now - r.lastDecay();
        if (elapsed <= 0) return false;
        r.lastDecay(now);
        boolean changed = false;
        if (idle >= s.coolingAfterTicks()) {
            // The part of the elapsed time that was already past the cooling threshold is what counts.
            double cooling = Math.min(elapsed, idle - s.coolingAfterTicks() + elapsed);
            changed |= move(r, Dimension.TRUST, s.initialTrust(), cooling, s.trustHalfLife());
            changed |= move(r, Dimension.AFFINITY, s.initialAffinity(), cooling, s.affinityHalfLife());
            changed |= move(r, Dimension.RESPECT, 0.0D, cooling, s.respectHalfLife());
            changed |= move(r, Dimension.LOYALTY, 0.0D, cooling, s.loyaltyHalfLife());
        }
        changed |= move(r, Dimension.FEAR, s.initialFear(), elapsed, s.fearHalfLife());
        changed |= move(r, Dimension.RIVALRY, s.initialRivalry(), elapsed, s.rivalryHalfLife());
        RelationState state = r.state();
        if (state != RelationState.BROKEN) {
            if (idle >= s.dormantAfterTicks()) state = RelationState.DORMANT;
            else if (idle >= s.coolingAfterTicks()) state = RelationState.COOLING;
            else state = RelationState.ACTIVE;
            if (state != r.state()) { r.state(state); changed = true; }
        }
        if (changed) r.bump();
        return changed;
    }

    private static boolean move(RelationshipRecord r, Dimension d, double target, double elapsed, double halfLife) {
        double before = r.get(d);
        // Trust and affinity drift towards a stranger's level from either side; the others only ease downwards.
        double goal = (d == Dimension.TRUST || d == Dimension.AFFINITY) ? target : Math.min(before, target);
        double after = relax(before, goal, elapsed, halfLife);
        if (Math.abs(after - before) < 0.005D) return false;
        r.set(d, after);
        return true;
    }
}
