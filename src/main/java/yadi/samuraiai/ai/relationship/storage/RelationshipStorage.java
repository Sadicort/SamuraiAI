package yadi.samuraiai.ai.relationship.storage;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.Migration;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;
import yadi.samuraiai.ai.relationship.engine.RelationshipRuntime;

/** Persists one NPC's relationships as {@code npc/<uuid>/relationships.json} (versioned, checksummed, safe-written, migratable). */
public final class RelationshipStorage {
    public static final int SCHEMA = 1;
    public static final String DOMAIN = "relationships";

    private final VersionedStore store;
    private final List<Migration> migrations;

    public RelationshipStorage(VersionedStore store, List<Migration> migrations) { this.store = store; this.migrations = migrations == null ? List.of() : List.copyOf(migrations); }

    public static String path(UUID npc) { return "npc/" + npc + "/relationships.json"; }

    public boolean save(RelationshipRuntime rt, long now) {
        boolean ok = store.write(path(rt.npcId()), DOMAIN, SCHEMA, RelationshipCodec.toJson(rt));
        if (ok) rt.saved(now);
        return ok;
    }

    public Loaded load(UUID npc, int causeCapacity) {
        LoadResult result = store.read(path(npc), DOMAIN, SCHEMA, migrations);
        if (!result.usable()) return new Loaded(null, result);
        RelationshipRuntime rt = RelationshipCodec.fromJson(npc, result.payload(), causeCapacity);
        rt.storageState(result.status().name());
        return new Loaded(rt, result);
    }

    public void delete(UUID npc) { store.delete(path(npc)); }

    public record Loaded(RelationshipRuntime runtime, LoadResult result) { }
}
