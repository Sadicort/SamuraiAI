package yadi.samuraiai.ai.navigation.chunks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.navigation.pathfinding.NavigationPath;

/** Which active sessions walk through which chunk: how a chunk unload or block change finds the paths it hurts. */
public final class ChunkPathIndex {
    private final Map<Long, Set<UUID>> byChunk = new HashMap<>();
    private final Map<UUID, Set<Long>> bySession = new HashMap<>();

    public void register(UUID sessionId, NavigationPath path) {
        unregister(sessionId);
        bySession.put(sessionId, path.chunks());
        for (long chunk : path.chunks()) byChunk.computeIfAbsent(chunk, k -> new HashSet<>()).add(sessionId);
    }

    public void unregister(UUID sessionId) {
        Set<Long> chunks = bySession.remove(sessionId);
        if (chunks == null) return;
        for (long chunk : chunks) {
            Set<UUID> ids = byChunk.get(chunk);
            if (ids != null) { ids.remove(sessionId); if (ids.isEmpty()) byChunk.remove(chunk); }
        }
    }

    public Set<UUID> sessionsIn(long chunkKey) { return Set.copyOf(byChunk.getOrDefault(chunkKey, Set.of())); }
    public int trackedSessions() { return bySession.size(); }
    public void clear() { byChunk.clear(); bySession.clear(); }
}
