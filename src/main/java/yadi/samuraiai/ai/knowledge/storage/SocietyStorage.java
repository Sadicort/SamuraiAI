package yadi.samuraiai.ai.knowledge.storage;

import java.util.List;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.Migration;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;
import yadi.samuraiai.ai.knowledge.society.SocietyEngine;

/** Persists the whole society as {@code society/society.json}: communities, their collective knowledge and history, rumours and legends. */
public final class SocietyStorage {
    public static final int SCHEMA = 1;
    public static final String DOMAIN = "society";
    public static final String PATH = "society/society.json";

    private final VersionedStore store;
    private final List<Migration> migrations;

    public SocietyStorage(VersionedStore store, List<Migration> migrations) { this.store = store; this.migrations = migrations == null ? List.of() : List.copyOf(migrations); }

    public boolean save(SocietyEngine society) { return store.write(PATH, DOMAIN, SCHEMA, SocietyCodec.toJson(society)); }

    /** Loads into the engine. The result says what happened; the engine is left empty when nothing usable was found. */
    public LoadResult load(SocietyEngine society, int cellSize) {
        LoadResult result = store.read(PATH, DOMAIN, SCHEMA, migrations);
        if (result.usable()) SocietyCodec.load(society, result.payload(), cellSize);
        return result;
    }
}
