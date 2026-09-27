package yadi.samuraiai.client.voice.util;

import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public final class VoiceFileUtils {
    public static Path root() { return FMLPaths.CONFIGDIR.get().resolve("samuraiai").resolve("voice"); }
    public static Path models() { return root().resolve("models"); }
    public static Path cache() { return root().resolve("cache"); }
    public static Path logs() { return root().resolve("logs"); }
    public static Path temp() { return root().resolve("temp"); }
    public static Path settings() { return root().resolve("settings.json"); }
    public static Path diagnostics() { return root().resolve("diagnostics.json"); }
    public static void prepare() throws IOException { for (Path path : new Path[]{root(), models(), cache(), logs(), temp()}) Files.createDirectories(path); }
    public static void deleteRecursively(Path path) throws IOException { if (!Files.exists(path)) return; try (Stream<Path> files = Files.walk(path)) { files.sorted((a,b) -> b.compareTo(a)).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} }); } }
    private VoiceFileUtils() {}
}
