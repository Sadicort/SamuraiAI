package yadi.samuraiai.npc.persistence;

import yadi.samuraiai.npc.NPCInstance;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placeholder persistence that only survives for the current JVM session.
 * Seam for a future NBT-backed implementation tied to world save/load.
 */
public class InMemoryNPCPersistence implements NPCPersistence {

    private final Map<UUID, NPCInstance> store = new ConcurrentHashMap<>();

    @Override
    public void save(NPCInstance instance) {
        store.put(instance.getIdentity().id(), instance);
    }

    @Override
    public Optional<NPCInstance> load(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
