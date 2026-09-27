package yadi.samuraiai.ai.perception.vision;

import java.util.UUID;
import yadi.samuraiai.ai.perception.engine.EntityClass;

/**
 * What one NPC believes about one target it has been looking at: last state, where it was and how it moved, how sure
 * it is. Mutable and owned by the NPC's perception state; the outside world sees {@link VisualTarget} copies.
 */
public final class VisualTrack {
    public final UUID id;
    public EntityClass kind;
    public String name;
    public VisibilityState state = VisibilityState.OBSTRUCTED;
    public double x, y, z, vx, vy, vz, confidence, fraction, distance;
    public long firstSeenTick = -1, lastSeenTick = -1, lastUpdateTick = -1, observedTicks;
    public boolean everSeen;

    public VisualTrack(UUID id, EntityClass kind, String name) { this.id = id; this.kind = kind; this.name = name; }

    public VisualTarget view() {
        return new VisualTarget(id, kind, name, x, y, z, vx, vy, vz, distance, state, confidence, firstSeenTick, lastSeenTick, observedTicks);
    }
}
