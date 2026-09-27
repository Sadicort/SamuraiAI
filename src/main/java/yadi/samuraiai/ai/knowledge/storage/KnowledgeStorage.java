package yadi.samuraiai.ai.knowledge.storage;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.Migration;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;

/** Persists one NPC's knowledge as {@code npc/<uuid>/knowledge.json} (versioned, checksummed, safe-written, migratable). */
public final class KnowledgeStorage {
    public static final int SCHEMA = 1;
    public static final String DOMAIN = "knowledge";

    private final VersionedStore store;
    private final List<Migration> migrations;

    public KnowledgeStorage(VersionedStore store, List<Migration> migrations) { this.store = store; this.migrations = migrations == null ? List.of() : List.copyOf(migrations); }

    public static String path(UUID npc) { return "npc/" + npc + "/knowledge.json"; }

    public boolean save(KnowledgeRuntime rt, long now) {
        boolean ok = store.write(path(rt.ownerId()), DOMAIN, SCHEMA, KnowledgeCodec.toJson(rt));
        if (ok) rt.saved(now);
        return ok;
    }

    public Loaded load(UUID npc, int cellSize) {
        LoadResult result = store.read(path(npc), DOMAIN, SCHEMA, migrations);
        if (!result.usable()) return new Loaded(null, result);
        KnowledgeRuntime rt = KnowledgeCodec.fromJson(npc, result.payload(), cellSize);
        rt.storageState(result.status().name());
        return new Loaded(rt, result);
    }

    public void delete(UUID npc) { store.delete(path(npc)); }

    public record Loaded(KnowledgeRuntime runtime, LoadResult result) { }
}
