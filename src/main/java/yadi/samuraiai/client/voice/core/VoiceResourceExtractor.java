package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.util.VoiceFileUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;

/** Copies small signed resources from the mod jar; models are never silently bundled. */
public final class VoiceResourceExtractor {
    public Path extract(String resource, Path target) throws Exception {
        VoiceFileUtils.prepare();
        if (Files.exists(target)) return target;
        try (InputStream in = VoiceResourceExtractor.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Recurso de voz ausente: " + resource);
            Files.createDirectories(target.getParent());
            Path part = target.resolveSibling(target.getFileName() + ".part");
            Files.copy(in, part, StandardCopyOption.REPLACE_EXISTING);
            try { return Files.move(part, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { return Files.move(part, target, StandardCopyOption.REPLACE_EXISTING); }
        }
    }
}
