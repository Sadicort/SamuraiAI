package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.VoiceLanguageManager;
import java.nio.file.Path;

/** Directory access, hashing and downloading run exclusively on the bootstrap worker. */
public final class VoiceInstallationService {
    private final VoiceModelManager models;
    private final VoiceIntegrityChecker integrity = new VoiceIntegrityChecker();
    private final VoiceDownloadManager downloads;
    public VoiceInstallationService(VoiceModelManager models) { this(models, new VoiceDownloadManager()); }
    VoiceInstallationService(VoiceModelManager models, VoiceDownloadManager downloads) {
        this.models = models;
        this.downloads = downloads;
    }
    public Path ensure(VoiceLanguageManager.Language language) throws Exception {
        models.prepare();
        var info = models.forLanguage(language)
                .orElseThrow(() -> new IllegalStateException("No hay modelo en el manifest"));
        Path target = models.path(info);
        if (integrity.check(target, info).valid()) return target;
        return downloads.download(info, target);
    }
}
