package yadi.samuraiai.ai.cognition;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.Migration;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.memory.storage.MemoryStorage;

class CognitionStorageTest {
    private JsonObject payload(String key, int value) { JsonObject o = new JsonObject(); o.addProperty(key, value); return o; }

    @Test void writesAreSafeChecksummedVersionedAndKeepABackup(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        VersionedStore store = new VersionedStore(dir, false);
        assertTrue(store.write("a/b.json", "test", 3, payload("x", 1)));
        assertTrue(store.write("a/b.json", "test", 3, payload("x", 2)));
        assertFalse(Files.exists(dir.resolve("a/b.json.tmp")), "no temp file is left behind");
        assertTrue(Files.exists(dir.resolve("a/b.json.bak")), "the previous good file is kept");
        String text = Files.readString(dir.resolve("a/b.json"), StandardCharsets.UTF_8);
        assertTrue(text.contains("\"schemaVersion\":3") && text.contains("\"checksum\"") && text.contains("\"domain\":\"test\""), text);
        LoadResult r = store.read("a/b.json", "test", 3, List.of());
        assertEquals(LoadResult.Status.OK, r.status());
        assertEquals(2, r.payload().get("x").getAsInt());
    }

    @Test void aCorruptFileIsDetectedAndTheBackupRestoresIt(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        VersionedStore store = new VersionedStore(dir, false);
        store.write("n.json", "test", 1, payload("x", 1));
        store.write("n.json", "test", 1, payload("x", 2));
        Path main = dir.resolve("n.json");
        Files.writeString(main, Files.readString(main).replace("\"x\":2", "\"x\":999"), StandardCharsets.UTF_8);
        LoadResult r = store.read("n.json", "test", 1, List.of());
        assertEquals(LoadResult.Status.RECOVERED_FROM_BACKUP, r.status(), r.detail());
        assertEquals(1, r.payload().get("x").getAsInt());
        assertEquals(1, store.recoveries());
        assertTrue(Files.list(dir).anyMatch(p -> p.getFileName().toString().contains(".corrupt-")), "the bad file is quarantined, not deleted");
    }

    @Test void whenEverythingIsCorruptTheWorldStillLoadsAndTheNpcStartsEmpty(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        CognitionHarness first = new CognitionHarness();
        EntityRef kenji = first.npc("Kenji"), yeremi = first.player("Y");
        first.engine.useStorage(new CognitionStorage(dir, false));
        first.engine.ensureLoaded(kenji.id(), first.now);
        first.live(kenji, ExperienceKind.HELPED_ME, yeremi, null);
        first.engine.saveAll(first.now);
        Path file = dir.resolve(MemoryStorage.path(kenji.id()));
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);
        Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".bak"));
        CognitionHarness second = new CognitionHarness();
        second.engine.useStorage(new CognitionStorage(dir, false));
        assertDoesNotThrow(() -> second.engine.ensureLoaded(kenji.id(), second.now));
        assertEquals(0, second.engine.memory().runtime(kenji.id()).size(), "memory started empty");
        assertEquals(1, second.engine.relationships().relationships(kenji.id()).size(), "the other domains were unaffected");
        assertTrue(second.engine.metrics().loadFailures.get() >= 1);
        second.live(kenji, ExperienceKind.GIFT_RECEIVED, yeremi, null);
        assertEquals(1, second.engine.memory().runtime(kenji.id()).size(), "and it keeps working");
    }

    @Test void aFileFromANewerVersionIsRefusedAndLeftExactlyAsFound(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        VersionedStore store = new VersionedStore(dir, false);
        store.write("m.json", "memory", 7, payload("future", 1));
        String before = Files.readString(dir.resolve("m.json"));
        LoadResult r = store.read("m.json", "memory", MemoryStorage.SCHEMA, List.of());
        assertEquals(LoadResult.Status.TOO_NEW, r.status());
        assertFalse(r.usable());
        assertEquals(before, Files.readString(dir.resolve("m.json")), "never overwritten or quarantined");
        assertEquals(0, store.corruptions());
    }

    @Test void olderSavesAreMigratedStepByStep(@org.junit.jupiter.api.io.TempDir Path dir) {
        VersionedStore store = new VersionedStore(dir, false);
        store.write("old.json", "test", 1, payload("hp", 10));
        Migration v1to2 = new Migration() {
            @Override public int from() { return 1; }
            @Override public JsonObject apply(JsonObject p) { JsonObject o = new JsonObject(); o.addProperty("health", p.get("hp").getAsInt()); return o; }
        };
        Migration v2to3 = new Migration() {
            @Override public int from() { return 2; }
            @Override public JsonObject apply(JsonObject p) { p.addProperty("max", p.get("health").getAsInt() * 2); return p; }
        };
        LoadResult r = store.read("old.json", "test", 3, List.of(v2to3, v1to2));
        assertEquals(LoadResult.Status.MIGRATED, r.status());
        assertEquals(1, r.storedVersion());
        assertEquals(10, r.payload().get("health").getAsInt());
        assertEquals(20, r.payload().get("max").getAsInt());
        assertFalse(r.payload().has("hp"));
        // A missing step is reported, never silently "fixed" or destroyed.
        LoadResult broken = store.read("old.json", "test", 3, List.of(v2to3));
        assertEquals(LoadResult.Status.UNSUPPORTED, broken.status());
        assertTrue(Files.exists(dir.resolve("old.json")));
    }

    @Test void pathsCannotEscapeTheStoreAndCompressionIsTransparent(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        VersionedStore zipped = new VersionedStore(dir, true);
        assertFalse(zipped.write("../escape.json", "test", 1, payload("x", 1)), "a path outside the store is refused");
        assertFalse(Files.exists(dir.resolveSibling("escape.json")));
        JsonObject big = new JsonObject();
        for (int i = 0; i < 500; i++) big.addProperty("k" + i, "value value value value " + i);
        assertTrue(zipped.write("z.json", "test", 1, big));
        String text = Files.readString(dir.resolve("z.json"));
        assertTrue(text.contains("gzip-base64") && !text.contains("value value"));
        assertEquals(big, zipped.read("z.json", "test", 1, List.of()).payload());
        assertEquals(LoadResult.Status.CORRUPT, zipped.read("z.json", "other-domain", 1, List.of()).status());
    }

    @Test void onlyWhatChangedIsWrittenAndIdleNpcsCostNothing(@org.junit.jupiter.api.io.TempDir Path dir) {
        CognitionHarness h = new CognitionHarness();
        h.cognitionSettings = CognitionSettings.builder().set("saveIntervalTicks", 1200).set("maxSavesPerTick", 100).build();
        EntityRef kenji = h.npc("Kenji"), hanako = h.npc("Hanako"), yeremi = h.player("Y");
        CognitionStorage storage = new CognitionStorage(dir, false);
        h.engine.useStorage(storage);
        h.engine.ensureLoaded(kenji.id(), h.now);
        h.engine.ensureLoaded(hanako.id(), h.now);
        h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null);
        int first = h.engine.saveDirty(h.now, true);
        assertTrue(first >= 4, "files: " + first);
        // The mind settles (memories consolidate, feelings fade away): those are real changes and get written.
        for (int i = 0; i < 400; i++) { h.advance(250); h.engine.tick(kenji.id(), h.now, Activity.NONE); h.engine.tick(hanako.id(), h.now, Activity.NONE); h.engine.saveDirty(h.now, false); }
        h.engine.saveDirty(h.now, true);
        long written = storage.store().filesWritten();
        // Then 60,000 ticks of ordinary life for both NPCs, nothing new happening: nothing to write.
        for (int i = 0; i < 240; i++) { h.advance(250); h.engine.tick(kenji.id(), h.now, Activity.NONE); h.engine.tick(hanako.id(), h.now, Activity.NONE); h.engine.saveDirty(h.now, false); }
        assertEquals(written, storage.store().filesWritten(), "an idle mind is not rewritten");
        // One new experience for Kenji: only Kenji's changed domains are written, and at the next sweep.
        h.live(kenji, ExperienceKind.CONVERSATION, yeremi, null);
        h.advance(h.cognitionSettings.saveIntervalTicks() + 1);
        int again = h.engine.saveDirty(h.now, false);
        assertTrue(again >= 1 && again <= 4, "files: " + again);
        assertFalse(Files.exists(dir.resolve("npc").resolve(hanako.id().toString()).resolve("relationships.json")), "Hanako had nothing relational to save");
    }

    @Test void importantChangesAreWrittenAtOnceBetweenSweeps(@org.junit.jupiter.api.io.TempDir Path dir) {
        CognitionHarness h = new CognitionHarness();
        EntityRef kenji = h.npc("Kenji"), yeremi = h.player("Y");
        h.engine.useStorage(new CognitionStorage(dir, false));
        h.engine.ensureLoaded(kenji.id(), h.now);
        h.engine.saveDirty(h.now, true);
        h.advance(10);
        h.live(kenji, ExperienceKind.BETRAYED, yeremi, null);
        int written = h.engine.saveDirty(h.now, false);
        assertTrue(written >= 2, "a betrayal (critical memory, lost trust, mood) does not wait for the next sweep: " + written);
        assertTrue(Files.exists(dir.resolve(MemoryStorage.path(kenji.id()))));
    }

    @Test void unloadingAnNpcSavesItAndFreesItsRuntimes(@org.junit.jupiter.api.io.TempDir Path dir) {
        CognitionHarness h = new CognitionHarness();
        EntityRef kenji = h.npc("Kenji"), yeremi = h.player("Y");
        h.engine.useStorage(new CognitionStorage(dir, false));
        h.engine.ensureLoaded(kenji.id(), h.now);
        h.live(kenji, ExperienceKind.HELPED_ME, yeremi, null);
        h.engine.unload(kenji.id(), h.now);
        assertTrue(h.engine.memory().peek(kenji.id()).isEmpty());
        assertFalse(h.engine.isLoaded(kenji.id()));
        h.engine.ensureLoaded(kenji.id(), h.now + 1000);
        assertEquals(1, h.engine.memory().runtime(kenji.id()).size(), "it wakes up remembering");
        assertEquals(1, h.engine.relationships().relationships(kenji.id()).size());
    }
}
