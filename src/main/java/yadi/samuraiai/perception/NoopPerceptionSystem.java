package yadi.samuraiai.perception;

import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.npc.NPCRuntime;

/**
 * Placeholder used until real Minecraft sensing (line of sight, hearing,
 * distance checks) is implemented. Always reports an empty WorldContext.
 */
public class NoopPerceptionSystem implements PerceptionSystem {

    @Override
    public WorldContext perceive(NPCRuntime runtime) {
        return WorldContext.empty();
    }
}
