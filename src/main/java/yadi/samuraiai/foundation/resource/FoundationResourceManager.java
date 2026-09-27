package yadi.samuraiai.foundation.resource;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;

/** Owns Foundation directories and atomic, hash-verified resource installation. */
public final class FoundationResourceManager {
    public enum Area { CONFIG, VOICE, CACHE, LOGS, DIAGNOSTICS, REPORTS, TEMP, DOWNLOADS, ASSETS }
    private final Path root;
    private final EnumMap<Area, Path> directories = new EnumMap<>(Area.class);

    public FoundationResourceManager(Path root) {
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
        for (Area area : Area.values()) directories.put(area,
                area == Area.CONFIG ? this.root : this.root.resolve(area.name().toLowerCase(Locale.ROOT)));
    }

    public synchronized void prepare() throws IOException {
        for (Path directory : directories.values()) Files.createDirectories(directory);
    }
    public Path directory(Area area) { return directories.get(Objects.requireNonNull(area)); }
    public Path resolve(Area area, String relative) {
        if (relative == null || relative.isBlank()) throw new IllegalArgumentException("Blank resource path");
        Path base = directory(area), result = base.resolve(relative).normalize();
        if (!result.startsWith(base)) throw new IllegalArgumentException("Resource escapes managed directory");
        return result;
    }

    public String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public boolean valid(Path file, long expectedSize, String expectedSha256) throws IOException {
        return Files.isRegularFile(file) && expectedSize >= 0 && Files.size(file) == expectedSize &&
                expectedSha256 != null && expectedSha256.matches("(?i)[0-9a-f]{64}") &&
                sha256(file).equalsIgnoreCase(expectedSha256);
    }

    public void installVerified(Path source, Area area, String relative, long expectedSize,
                                String expectedSha256) throws IOException {
        Objects.requireNonNull(source);
        if (!valid(source, expectedSize, expectedSha256)) throw new IOException("Resource integrity validation failed");
        Path destination = resolve(area, relative);
        Files.createDirectories(destination.getParent());
        Path staging = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".installing");
        try {
            Files.copy(source, staging, StandardCopyOption.REPLACE_EXISTING);
            if (!valid(staging, expectedSize, expectedSha256)) throw new IOException("Staged resource integrity validation failed");
            Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(staging); }
    }

    public int cleanTemporary(Duration olderThan) throws IOException {
        Objects.requireNonNull(olderThan);
        if (olderThan.isNegative()) throw new IllegalArgumentException("Negative retention");
        long cutoff = System.currentTimeMillis() - olderThan.toMillis();
        int removed = 0;
        for (Area area : List.of(Area.TEMP, Area.DOWNLOADS)) {
            Path directory = directory(area);
            if (!Files.isDirectory(directory)) continue;
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    if (Files.getLastModifiedTime(path).toMillis() <= cutoff && Files.deleteIfExists(path)) removed++;
                }
            }
        }
        return removed;
    }
}
