package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.*;
import yadi.samuraiai.client.voice.util.VoiceFileUtils;
import java.nio.file.*;
import java.util.*;

public final class VoiceDiagnostics {
    public record Report(VoiceEngineState state, boolean microphone, String device, boolean model, String detail, long freeBytes) {}
    public static Report run(VoiceEngineManager manager) {
        var mic = VoicePermissionChecker.check(VoiceConfig.get().microphone());
        boolean model = manager.model() != null && Files.isRegularFile(manager.model());
        String detail = mic.detail();
        if (!model) detail = detail + "; modelo no verificado";
        long free; try { free = VoiceFileUtils.root().toFile().getFreeSpace(); } catch (Exception e) { free = -1; }
        return new Report(manager.state(), mic.available(), mic.device(), model, detail, free);
    }
    private VoiceDiagnostics() {}
}
