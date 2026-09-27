package yadi.samuraiai.ai.memory.storage;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.Migration;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;

/** Persists one NPC's memory runtime as {@code npc/<uuid>/memory.json}: versioned, checksummed, written safely, migratable. */
public final class MemoryStorage {
    public static final int SCHEMA = 1;
    public static final String DOMAIN = "memory";

    private final VersionedStore store;
    private final List<Migration> migrations;

    public MemoryStorage(VersionedStore store, List<Migration> migrations) { this.store = store; this.migrations = migrations == null ? List.of() : List.copyOf(migrations); }

    public static String path(UUID npc) { return "npc/" + npc + "/memory.json"; }

    public boolean save(MemoryRuntime rt, long now) {
        boolean ok = store.write(path(rt.npcId()), DOMAIN, SCHEMA, MemoryCodec.toJson(rt));
        if (ok) rt.saved(now);
        return ok;
    }

    /** The loaded runtime (with its storage state set), or null when there is nothing usable; the result says why. */
    public Loaded load(UUID npc, MemorySettings settings) {
        LoadResult result = store.read(path(npc), DOMAIN, SCHEMA, migrations);
        if (!result.usable()) return new Loaded(null, result);
        JsonObject payload = result.payload();
        MemoryRuntime rt = MemoryCodec.fromJson(npc, payload, settings);
        rt.storageState(result.status().name());
        return new Loaded(rt, result);
    }

    public void delete(UUID npc) { store.delete(path(npc)); }

    public record Loaded(MemoryRuntime runtime, LoadResult result) { }
}
