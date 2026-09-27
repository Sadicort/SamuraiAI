package yadi.samuraiai.ai.cognition.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Durable JSON files with an envelope: {@code format, domain, schemaVersion, encoding, checksum, savedAt, payload}.
 * Writes are safe (a temp file is written and moved over the target, the previous good file is kept as {@code .bak}); reads
 * verify the checksum, fall back to the backup on corruption, migrate older schema versions step by step and refuse (rather
 * than destroy) a file written by a newer version. Paths are relative to the store root and must be built from UUIDs and
 * fixed names only, never from player-controlled text. The backing medium is a plain directory today; the interface is the
 * seam for a SQLite implementation later.
 */
public final class VersionedStore {
    public static final String FORMAT = "samuraiai-cognition";
    private static final Gson GSON = new Gson();

    private final Path root;
    private final boolean compress;
    private final String format;
    private final AtomicLong written = new AtomicLong(), bytesWritten = new AtomicLong(), reads = new AtomicLong(), recovered = new AtomicLong(), corrupt = new AtomicLong();
    private final AtomicLong writeNanos = new AtomicLong(), readNanos = new AtomicLong();

    public VersionedStore(Path root, boolean compress) { this(root, compress, FORMAT); }

    /** A store whose envelope carries another format name, so files of different layers can never be read as each other's. */
    public VersionedStore(Path root, boolean compress, String format) {
        this.root = root;
        this.compress = compress;
        this.format = format == null || format.isBlank() ? FORMAT : format;
    }

    public String format() { return format; }

    public Path root() { return root; }
    public long filesWritten() { return written.get(); }
    public long bytesWritten() { return bytesWritten.get(); }
    public long filesRead() { return reads.get(); }
    public long recoveries() { return recovered.get(); }
    public long corruptions() { return corrupt.get(); }
    public double averageWriteMillis() { long n = written.get(); return n == 0 ? 0 : writeNanos.get() / 1e6 / n; }
    public double averageReadMillis() { long n = reads.get(); return n == 0 ? 0 : readNanos.get() / 1e6 / n; }

    private Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root.normalize())) throw new IllegalArgumentException("Path escapes the store: " + relative);
        return path;
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String gzip(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream zip = new GZIPOutputStream(out)) { zip.write(text.getBytes(StandardCharsets.UTF_8)); }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static String gunzip(String base64) throws IOException {
        try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64)))) {
            return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Writes the payload safely. Returns false (and leaves the previous file untouched) when the write fails. */
    public boolean write(String relative, String domain, int schemaVersion, JsonObject payload) {
        long started = System.nanoTime();
        try {
            Path target = resolve(relative);
            Files.createDirectories(target.getParent());
            String text = payload.toString();
            JsonObject envelope = new JsonObject();
            envelope.addProperty("format", format);
            envelope.addProperty("domain", domain);
            envelope.addProperty("schemaVersion", schemaVersion);
            envelope.addProperty("savedAt", System.currentTimeMillis());
            envelope.addProperty("checksum", sha256(text));
            if (compress) { envelope.addProperty("encoding", "gzip-base64"); envelope.addProperty("payloadZ", gzip(text)); }
            else { envelope.addProperty("encoding", "plain"); envelope.add("payload", payload); }
            byte[] bytes = GSON.toJson(envelope).getBytes(StandardCharsets.UTF_8);
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(temp, bytes);
            if (Files.exists(target)) Files.copy(target, target.resolveSibling(target.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            written.incrementAndGet(); bytesWritten.addAndGet(bytes.length); writeNanos.addAndGet(System.nanoTime() - started);
            return true;
        } catch (IOException | RuntimeException error) {
            return false;
        }
    }

    /** Reads, verifies and migrates. Never throws: every failure is reported in the result. */
    public LoadResult read(String relative, String domain, int currentVersion, List<Migration> migrations) {
        long started = System.nanoTime();
        try {
            Path target = resolve(relative);
            Path backup = target.resolveSibling(target.getFileName() + ".bak");
            boolean hasMain = Files.isRegularFile(target), hasBackup = Files.isRegularFile(backup);
            if (!hasMain && !hasBackup) return LoadResult.missing();
            LoadResult result = hasMain ? parse(target, domain, currentVersion, migrations) : LoadResult.failed(LoadResult.Status.CORRUPT, "missing main file");
            if (result.status() == LoadResult.Status.TOO_NEW || result.status() == LoadResult.Status.UNSUPPORTED) { reads.incrementAndGet(); readNanos.addAndGet(System.nanoTime() - started); return result; }
            if (!result.usable() && hasBackup) {
                LoadResult fromBackup = parse(backup, domain, currentVersion, migrations);
                if (fromBackup.usable()) {
                    recovered.incrementAndGet();
                    if (hasMain) quarantine(target);
                    result = new LoadResult(LoadResult.Status.RECOVERED_FROM_BACKUP, fromBackup.storedVersion(), fromBackup.payload(), "main file unusable (" + result.detail() + "); restored the backup");
                }
            }
            if (result.status() == LoadResult.Status.CORRUPT) { corrupt.incrementAndGet(); if (hasMain) quarantine(target); }
            reads.incrementAndGet(); readNanos.addAndGet(System.nanoTime() - started);
            return result;
        } catch (RuntimeException error) {
            return LoadResult.failed(LoadResult.Status.CORRUPT, error.toString());
        }
    }

    private static void quarantine(Path file) {
        try { Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException ignored) { /* the corrupt file simply stays and is overwritten by the next good write */ }
    }

    private LoadResult parse(Path file, String domain, int currentVersion, List<Migration> migrations) {
        try {
            JsonObject envelope = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!format.equals(Json.str(envelope, "format", ""))) return LoadResult.failed(LoadResult.Status.CORRUPT, "not a " + format + " file");
            if (!domain.equals(Json.str(envelope, "domain", ""))) return LoadResult.failed(LoadResult.Status.CORRUPT, "wrong domain " + Json.str(envelope, "domain", "?"));
            int version = Json.integer(envelope, "schemaVersion", 0);
            if (version > currentVersion) return LoadResult.failed(LoadResult.Status.TOO_NEW, "written by a newer schema (" + version + " > " + currentVersion + ")");
            String encoding = Json.str(envelope, "encoding", "plain");
            String text = encoding.equals("gzip-base64") ? gunzip(Json.str(envelope, "payloadZ", "")) : envelope.get("payload").toString();
            if (!Json.str(envelope, "checksum", "").equals(sha256(text))) return LoadResult.failed(LoadResult.Status.CORRUPT, "checksum mismatch");
            JsonObject payload = JsonParser.parseString(text).getAsJsonObject();
            int stored = version;
            List<Migration> chain = migrations == null ? List.of() : new ArrayList<>(migrations);
            while (version < currentVersion) {
                final int at = version;
                Migration step = chain.stream().filter(m -> m.from() == at).findFirst().orElse(null);
                if (step == null) return LoadResult.failed(LoadResult.Status.UNSUPPORTED, "no migration from schema " + at);
                payload = step.apply(payload);
                version++;
            }
            return new LoadResult(stored < currentVersion ? LoadResult.Status.MIGRATED : LoadResult.Status.OK, stored, payload, stored < currentVersion ? "migrated " + stored + " -> " + currentVersion : "ok");
        } catch (IOException | RuntimeException error) {
            return LoadResult.failed(LoadResult.Status.CORRUPT, error.toString());
        }
    }

    public boolean exists(String relative) { return Files.isRegularFile(resolve(relative)); }

    public void delete(String relative) {
        try {
            Path target = resolve(relative);
            Files.deleteIfExists(target);
            Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".bak"));
        } catch (IOException ignored) { /* nothing more to do */ }
    }

    /** Removes a whole directory (an NPC that left the world for good). */
    public void deleteDirectory(String relative) {
        try {
            Path dir = resolve(relative);
            if (!Files.isDirectory(dir)) return;
            try (var files = Files.walk(dir)) { for (Path p : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
        } catch (IOException ignored) { /* best effort */ }
    }

    /** Total bytes under a relative directory, for diagnostics. */
    public long sizeOf(String relative) {
        try {
            Path dir = resolve(relative);
            if (!Files.exists(dir)) return 0;
            try (var files = Files.walk(dir)) { return files.filter(Files::isRegularFile).mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } }).sum(); }
        } catch (IOException e) { return 0; }
    }
}
