package yadi.samuraiai.perception;

import java.util.UUID;
import yadi.samuraiai.world.SpawnLocation;

/**
 * What the NPC *perceived* about a nearby entity — not ground truth about the
 * world, which is exactly the distinction the perception layer exists to
 * enforce (see {@link PerceptionSystem}).
 *
 * @param distance blocks away at the moment of perception
 * @param player   whether this is a human player, which the decision layer
 *                 weighs differently from a mob
 */
public record PerceivedEntity(UUID id, String name, double distance, boolean player, SpawnLocation location) {
    public PerceivedEntity(UUID id, String name, double distance, boolean player) {
        this(id, name, distance, player, null);
    }

    /** True when close enough for conversation rather than mere awareness. */
    public boolean isWithin(double blocks) {
        return distance <= blocks;
    }
}
