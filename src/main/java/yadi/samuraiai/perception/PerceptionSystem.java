package yadi.samuraiai.perception;

import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.npc.NPCRuntime;

/**
 * Converts the real world into the limited {@link WorldContext} an NPC is
 * allowed to know about (distance, line of sight, hearing, light, ...).
 * See {@link NoopPerceptionSystem} for the current placeholder — real
 * sensing against the Minecraft world is future work.
 */
public interface PerceptionSystem {

    WorldContext perceive(NPCRuntime runtime);

}
