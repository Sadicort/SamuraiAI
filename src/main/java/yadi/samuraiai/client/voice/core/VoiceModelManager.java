package yadi.samuraiai.client.voice.core;

import com.google.gson.*;
import yadi.samuraiai.client.voice.VoiceLanguageManager;
import yadi.samuraiai.client.voice.util.VoiceFileUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class VoiceModelManager {
    public record ModelInfo(String id, VoiceLanguageManager.Language language, String version, long size, String sha256, String url, int priority) {}
    private static final String MANIFEST = "/assets/samuraiai/voice/metadata/models.json";
    private final List<ModelInfo> manifest;
    private final Path directory;
    public VoiceModelManager() { manifest = loadManifest(); directory = null; }
    VoiceModelManager(Path directory, List<ModelInfo> manifest) {
        this.directory = directory.toAbsolutePath().normalize();
        this.manifest = List.copyOf(manifest);
    }
    public List<ModelInfo> models() { return manifest; }
    public Optional<ModelInfo> forLanguage(VoiceLanguageManager.Language language) { return manifest.stream().filter(m -> m.language() == language || m.language() == VoiceLanguageManager.Language.AUTO || language == VoiceLanguageManager.Language.AUTO).sorted(Comparator.comparingInt(ModelInfo::priority).reversed()).findFirst(); }
    public Path path(ModelInfo info) {
        if (info.id() == null || !info.id().matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}"))
            throw new IllegalArgumentException("ID de modelo inválido");
        return (directory == null ? VoiceFileUtils.models() : directory).resolve(info.id() + ".bin");
    }
    public void prepare() throws IOException {
        if (directory == null) VoiceFileUtils.prepare(); else Files.createDirectories(directory);
    }
    public Optional<ModelInfo> installed(VoiceLanguageManager.Language language) { return forLanguage(language).filter(info -> Files.isRegularFile(path(info))); }
    public void delete(ModelInfo info) throws IOException { Files.deleteIfExists(path(info)); }
    private static List<ModelInfo> loadManifest() {
        try (InputStream stream = VoiceModelManager.class.getResourceAsStream(MANIFEST)) {
            if (stream == null) return List.of();
            JsonArray array = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray(); List<ModelInfo> result = new ArrayList<>();
            for (JsonElement element : array) { JsonObject o = element.getAsJsonObject(); result.add(new ModelInfo(o.get("id").getAsString(), VoiceLanguageManager.parse(o.get("language").getAsString()), o.get("version").getAsString(), o.get("size").getAsLong(), o.get("sha256").getAsString(), o.get("url").getAsString(), o.get("priority").getAsInt())); }
            return List.copyOf(result);
        } catch (Exception ignored) { return List.of(); }
    }
}
