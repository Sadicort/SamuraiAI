package yadi.samuraiai.memory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns one {@link ConversationMemory} per NPC, keyed by the NPC's UUID
 * rather than its display name — two NPCs may share a name, and conflating
 * their histories would have them finishing each other's sentences.
 *
 * <p>Concurrent because dialogue is created on the server thread but
 * appended to from HTTP completion threads. Kept as a process-wide singleton
 * so memory survives an NPC being unloaded and reactivated, which is exactly
 * the case a per-runtime map would lose.
 */
public class MemoryManager implements MemoryAccess {

    private static final MemoryManager INSTANCE = new MemoryManager();

    private final Map<UUID, ConversationMemory> memories = new ConcurrentHashMap<>();

    private MemoryManager() {
    }

    public static MemoryManager getInstance() {
        return INSTANCE;
    }

    /** Returns the NPC's memory, creating an empty one on first contact. */
    public ConversationMemory getMemory(UUID npcId) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        return memories.computeIfAbsent(npcId, id -> new ConversationMemory(null, true));
    }

    /** Lookup that does not create anything, for read-only callers. */
    public Optional<ConversationMemory> peek(UUID npcId) {
        return Optional.ofNullable(memories.get(npcId));
    }

    /**
     * Drops an NPC's history entirely. Called when an NPC is permanently
     * removed — not when it merely unloads, or it would wake up amnesiac.
     */
    public void forget(UUID npcId) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        ConversationMemory removed = memories.remove(npcId);
        if (removed != null) removed.clear();
    }

    public void clearAll() {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        memories.values().forEach(ConversationMemory::clear);
        memories.clear();
    }

    public int trackedNpcs() {
        return memories.size();
    }
}
