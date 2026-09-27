package yadi.samuraiai.ai.cognition.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.engine.CognitionEngine;
import yadi.samuraiai.ai.cognition.personality.PersonalityLedger;
import yadi.samuraiai.ai.emotion.engine.EmotionRuntime;
import yadi.samuraiai.ai.emotion.storage.EmotionStorage;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.storage.KnowledgeStorage;
import yadi.samuraiai.ai.knowledge.storage.SocietyStorage;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.storage.MemoryStorage;
import yadi.samuraiai.ai.relationship.engine.RelationshipRuntime;
import yadi.samuraiai.ai.relationship.storage.RelationshipStorage;

/**
 * The persistence policy of the cognitive layer: each NPC has its own directory ({@code npc/<uuid>/}) with one versioned,
 * checksummed file per domain (memory, relationships, emotions, knowledge, personality), and the society has one file. Only
 * runtimes that changed are written; a write is safe (temp file, backup); a corrupt file falls back to the backup and, failing that,
 * loads empty rather than stopping the world. The medium is a directory today; each domain storage is the seam for SQLite.
 */
public final class CognitionStorage {
    private final VersionedStore store;
    private final MemoryStorage memory;
    private final RelationshipStorage relationships;
    private final EmotionStorage emotions;
    private final KnowledgeStorage knowledge;
    private final SocietyStorage society;
    private final PersonalityStorage personality;

    public CognitionStorage(Path root, boolean compress) { this(new VersionedStore(root, compress), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()); }

    public CognitionStorage(VersionedStore store, List<Migration> memoryMigrations, List<Migration> relationshipMigrations, List<Migration> emotionMigrations, List<Migration> knowledgeMigrations,
                            List<Migration> societyMigrations, List<Migration> personalityMigrations) {
        this.store = store;
        this.memory = new MemoryStorage(store, memoryMigrations);
        this.relationships = new RelationshipStorage(store, relationshipMigrations);
        this.emotions = new EmotionStorage(store, emotionMigrations);
        this.knowledge = new KnowledgeStorage(store, knowledgeMigrations);
        this.society = new SocietyStorage(store, societyMigrations);
        this.personality = new PersonalityStorage(store, personalityMigrations);
    }

    public VersionedStore store() { return store; }

    /** Loads an NPC's domains into the engines. A domain with nothing usable simply stays empty. */
    public String loadNpc(CognitionEngine engine, UUID npc, long now) {
        long started = System.nanoTime();
        StringBuilder report = new StringBuilder();
        var cs = engine.memory().settings();
        var m = memory.load(npc, cs);
        if (m.runtime() != null) engine.memory().install(m.runtime());
        report.append("memory=").append(m.result().status());
        var r = relationships.load(npc, engine.relationships().settings().maxCauses());
        if (r.runtime() != null) engine.relationships().install(r.runtime(), now);
        report.append(" relationships=").append(r.result().status());
        var e = emotions.load(npc, engine.emotions().settings().maxCauses());
        if (e.runtime() != null) engine.emotions().install(e.runtime());
        report.append(" emotions=").append(e.result().status());
        var k = knowledge.load(npc, cs.cellSize());
        if (k.runtime() != null) engine.knowledge().install(k.runtime());
        report.append(" knowledge=").append(k.result().status());
        PersonalityLedger ledger = new PersonalityLedger(npc);
        var p = personality.load(ledger);
        if (p.usable()) engine.installLedger(ledger);
        report.append(" personality=").append(p.status());
        for (LoadResult result : new LoadResult[] {m.result(), r.result(), e.result(), k.result(), p}) {
            if (result.status() == LoadResult.Status.RECOVERED_FROM_BACKUP) engine.metrics().recoveries.incrementAndGet();
            if (result.status() == LoadResult.Status.CORRUPT || result.status() == LoadResult.Status.TOO_NEW) engine.metrics().loadFailures.incrementAndGet();
        }
        engine.metrics().loads.incrementAndGet();
        engine.metrics().loadNanos.addAndGet(System.nanoTime() - started);
        return report.toString();
    }

    /** Writes the NPC's changed domains (all of them when {@code onlyDirty} is false). @return files written */
    public int saveNpc(CognitionEngine engine, UUID npc, long now, boolean onlyDirty) {
        long started = System.nanoTime();
        int written = 0;
        MemoryRuntime m = engine.memory().peek(npc).orElse(null);
        if (m != null && (!onlyDirty || m.dirty())) written += tally(engine, memory.save(m, now));
        RelationshipRuntime r = engine.relationships().peek(npc).orElse(null);
        if (r != null && (!onlyDirty || r.dirty())) written += tally(engine, relationships.save(r, now));
        EmotionRuntime e = engine.emotions().peek(npc).orElse(null);
        if (e != null && (!onlyDirty || e.dirty())) written += tally(engine, emotions.save(e, now));
        KnowledgeRuntime k = engine.knowledge().peek(npc).orElse(null);
        if (k != null && (!onlyDirty || k.dirty())) written += tally(engine, knowledge.save(k, now));
        PersonalityLedger p = engine.ledgers().get(npc);
        if (p != null && p.seeded() && (!onlyDirty || p.dirty())) written += tally(engine, personality.save(p));
        if (written > 0) { engine.metrics().saves.incrementAndGet(); engine.metrics().saveNanos.addAndGet(System.nanoTime() - started); }
        return written;
    }

    private static int tally(CognitionEngine engine, boolean ok) {
        if (!ok) engine.metrics().saveFailures.incrementAndGet();
        return ok ? 1 : 0;
    }

    public boolean saveSociety(CognitionEngine engine) { return tally(engine, society.save(engine.society())) == 1; }

    public LoadResult loadSociety(CognitionEngine engine) { return society.load(engine.society(), engine.memory().settings().cellSize()); }

    public void deleteNpc(UUID npc) { store.deleteDirectory("npc/" + npc); }

    public long bytesOf(UUID npc) { return store.sizeOf("npc/" + npc); }
    public long totalBytes() { return store.sizeOf("npc") + store.sizeOf("society"); }
}
