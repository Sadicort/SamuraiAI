package yadi.samuraiai.living.core.persistence;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import yadi.samuraiai.ai.cognition.storage.LoadResult;
import yadi.samuraiai.ai.cognition.storage.VersionedStore;

/**
 * The living world's persistence: every engine registers its {@link StoreSection}s and this class loads them at start,
 * writes only the dirty ones (a few per call, so a burst of changes never costs one long tick), and writes everything at
 * shutdown. Files use the shared {@link VersionedStore} envelope under the format {@code samuraiai-living}: safe write
 * (temp + move, previous file kept as backup), checksum, recovery from the backup, step-by-step migrations.
 *
 * <p>A section whose file was written by a newer version, or by an older one with no migration path, is <b>blocked</b>: it
 * keeps running on defaults for the session but is never saved, so the file on disk is left exactly as it was found.
 */
public final class LivingStorage {
    public static final String FORMAT = "samuraiai-living";

    private final VersionedStore store;
    private final Map<String, StoreSection> sections = new LinkedHashMap<>();
    private final Set<String> blocked = new LinkedHashSet<>();
    private final Map<String, LoadResult> lastLoad = new LinkedHashMap<>();
    private final AtomicLong saves = new AtomicLong(), failures = new AtomicLong(), loads = new AtomicLong(), saveNanos = new AtomicLong();
    private int cursor;

    public LivingStorage(Path root, boolean compress) { this.store = new VersionedStore(root, compress, FORMAT); }

    public Path root() { return store.root(); }
    public VersionedStore store() { return store; }

    public void register(StoreSection section) {
        if (sections.containsKey(section.file())) throw new IllegalStateException("Two sections share " + section.file());
        sections.put(section.file(), section);
    }

    public List<StoreSection> sections() { return List.copyOf(sections.values()); }
    public Set<String> blocked() { return Set.copyOf(blocked); }
    public Map<String, LoadResult> lastLoad() { return Map.copyOf(lastLoad); }

    /** Reads every registered section. A missing file is normal (a new world); every other failure is reported, never thrown. */
    public Map<String, LoadResult> loadAll() {
        Map<String, LoadResult> results = new LinkedHashMap<>();
        // a section may register (and load) further sections while it loads (the village index): iterate over a snapshot
        for (StoreSection section : new ArrayList<>(sections.values())) if (!results.containsKey(section.file())) results.put(section.file(), load(section));
        for (StoreSection section : sections.values()) if (!results.containsKey(section.file()) && lastLoad.containsKey(section.file())) results.put(section.file(), lastLoad.get(section.file()));
        return results;
    }

    public LoadResult load(StoreSection section) {
        LoadResult result = store.read(section.file(), section.domain(), section.schemaVersion(), section.migrations());
        loads.incrementAndGet();
        lastLoad.put(section.file(), result);
        if (result.status() == LoadResult.Status.TOO_NEW || result.status() == LoadResult.Status.UNSUPPORTED) blocked.add(section.file());
        if (result.usable()) {
            try { section.read(result.payload()); section.clean(); }
            catch (RuntimeException error) {
                // A payload that parsed but could not be applied: keep the file, run on defaults, do not overwrite it this session.
                blocked.add(section.file());
                LoadResult failed = new LoadResult(LoadResult.Status.CORRUPT, result.storedVersion(), null, "could not apply: " + error);
                lastLoad.put(section.file(), failed);
                return failed;
            }
            // A migrated or recovered payload is rewritten in the current format at the next save.
            if (result.status() != LoadResult.Status.OK) forceDirty(section);
        }
        return result;
    }

    private final Set<String> forced = new LinkedHashSet<>();
    private void forceDirty(StoreSection section) { forced.add(section.file()); }

    /** Writes up to {@code max} dirty sections, round-robin. Returns how many were written. */
    public int saveDirty(int max) {
        if (sections.isEmpty()) return 0;
        List<StoreSection> order = new ArrayList<>(sections.values());
        int written = 0;
        for (int i = 0; i < order.size() && written < max; i++) {
            StoreSection section = order.get((cursor + i) % order.size());
            if (!section.dirty() && !forced.contains(section.file())) continue;
            if (save(section)) written++;
        }
        cursor = (cursor + 1) % order.size();
        return written;
    }

    /** Writes every section that has anything to write (server stopping). */
    public int saveAll() {
        int written = 0, seen = -1;
        // a section may register new sections while it is checked (a new village's file): repeat until nothing new appears
        while (seen != sections.size()) {
            seen = sections.size();
            for (StoreSection section : new ArrayList<>(sections.values())) if ((section.dirty() || forced.contains(section.file())) && save(section)) written++;
        }
        return written;
    }

    /** Writes one section now, dirty or not (commands, tests). */
    public boolean save(StoreSection section) {
        if (blocked.contains(section.file())) return false;
        long started = System.nanoTime();
        JsonObject payload;
        try { payload = section.write(); } catch (RuntimeException error) { failures.incrementAndGet(); return false; }
        boolean ok = store.write(section.file(), section.domain(), section.schemaVersion(), payload);
        saveNanos.addAndGet(System.nanoTime() - started);
        if (ok) { section.clean(); forced.remove(section.file()); saves.incrementAndGet(); } else failures.incrementAndGet();
        return ok;
    }

    public long saves() { return saves.get(); }
    public long failures() { return failures.get(); }
    public long loads() { return loads.get(); }
    public double averageSaveMillis() { long n = saves.get(); return n == 0 ? 0 : saveNanos.get() / 1e6 / n; }
    public long bytesOnDisk() { return store.sizeOf("."); }
}
