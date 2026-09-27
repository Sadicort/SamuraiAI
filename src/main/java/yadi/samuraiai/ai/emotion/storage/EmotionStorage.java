package yadi.samuraiai.ai.emotion.storage;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.Migration;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;

/** Persists one NPC's emotions as {@code npc/<uuid>/emotions.json} (versioned, checksummed, safe-written, migratable). */
public final class EmotionStorage {
    public static final int SCHEMA = 1;
    public static final String DOMAIN = "emotions";

    private final VersionedStore store;
    private final List<Migration> migrations;

    public EmotionStorage(VersionedStore store, List<Migration> migrations) { this.store = store; this.migrations = migrations == null ? List.of() : List.copyOf(migrations); }

    public static String path(UUID npc) { return "npc/" + npc + "/emotions.json"; }

    public boolean save(EmotionRuntime rt, long now) {
        boolean ok = store.write(path(rt.npcId()), DOMAIN, SCHEMA, EmotionCodec.toJson(rt));
        if (ok) rt.saved(now);
        return ok;
    }

    public Loaded load(UUID npc, int causeCapacity) {
        LoadResult result = store.read(path(npc), DOMAIN, SCHEMA, migrations);
        if (!result.usable()) return new Loaded(null, result);
        EmotionRuntime rt = EmotionCodec.fromJson(npc, result.payload(), causeCapacity);
        rt.storageState(result.status().name());
        return new Loaded(rt, result);
    }

    public void delete(UUID npc) { store.delete(path(npc)); }

    public record Loaded(EmotionRuntime runtime, LoadResult result) { }
}
