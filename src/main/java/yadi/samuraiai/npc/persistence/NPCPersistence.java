package yadi.samuraiai.npc.persistence;

import yadi.samuraiai.npc.NPCInstance;

import java.util.Optional;
import java.util.UUID;

/**
 * Boundary for saving/loading the durable parts of an NPC (identity,
 * relationships, reputation) independently of runtime-only state (Brain,
 * active tasks, current goal). A real implementation will back this with
 * Minecraft's SavedData/NBT once world save/load integration is wired in;
 * see {@link InMemoryNPCPersistence} for the current placeholder.
 *
 * <p><strong>Security note for a future file-backed implementation:</strong>
 * any on-disk storage (loose NBT files, region-style buckets, etc.) must
 * derive file/entry names only from {@code id} (a {@link UUID}), never from
 * the NPC's display name or any other player-influenced string. Names come
 * from {@code /samuraiai spawn <type> <name>} and are attacker-controlled;
 * using them to build a file path is a path-traversal risk.
 */
public interface NPCPersistence {

    void save(NPCInstance instance);

    Optional<NPCInstance> load(UUID id);

}
