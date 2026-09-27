package yadi.samuraiai.foundation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import yadi.samuraiai.foundation.resource.FoundationResourceManager;
import java.nio.file.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class FoundationResourceManagerTest {
    @TempDir Path directory;
    @Test void preparesAllManagedDirectoriesAndRejectsTraversal() throws Exception {
        var resources = new FoundationResourceManager(directory.resolve("samuraiai")); resources.prepare();
        for (var area : FoundationResourceManager.Area.values()) assertTrue(Files.isDirectory(resources.directory(area)));
        assertThrows(IllegalArgumentException.class, () -> resources.resolve(FoundationResourceManager.Area.ASSETS, "../escape"));
    }
    @Test void verifiedInstallIsAtomicAndPreservesDestinationOnInvalidInput() throws Exception {
        var resources = new FoundationResourceManager(directory.resolve("samuraiai")); resources.prepare();
        Path source = directory.resolve("source.bin"); Files.writeString(source, "valid");
        String hash = resources.sha256(source);
        resources.installVerified(source, FoundationResourceManager.Area.ASSETS, "model.bin", Files.size(source), hash);
        Path installed = resources.resolve(FoundationResourceManager.Area.ASSETS, "model.bin");
        assertEquals("valid", Files.readString(installed));
        Files.writeString(source, "corrupt");
        assertThrows(java.io.IOException.class, () -> resources.installVerified(source,
                FoundationResourceManager.Area.ASSETS, "model.bin", 5, hash));
        assertEquals("valid", Files.readString(installed));
    }
    @Test void cleanupOnlyTouchesManagedTemporaryAreas() throws Exception {
        var resources = new FoundationResourceManager(directory.resolve("samuraiai")); resources.prepare();
        Path temp = resources.resolve(FoundationResourceManager.Area.TEMP, "old.tmp");
        Path asset = resources.resolve(FoundationResourceManager.Area.ASSETS, "keep.bin");
        Files.writeString(temp, "x"); Files.writeString(asset, "x");
        assertEquals(1, resources.cleanTemporary(Duration.ZERO));
        assertFalse(Files.exists(temp)); assertTrue(Files.exists(asset));
    }
}
