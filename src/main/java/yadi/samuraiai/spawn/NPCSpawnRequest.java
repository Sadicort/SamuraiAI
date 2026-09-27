package yadi.samuraiai.spawn;

import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.world.SpawnLocation;

/**
 * A request to create an NPC. Deliberately data-only so it can come from a
 * command today and from an event, a structure, or a script tomorrow
 * without changing {@link NPCSpawnService}.
 */
public record NPCSpawnRequest(NPCTypeId type, String requestedName, SpawnLocation location) {
}
