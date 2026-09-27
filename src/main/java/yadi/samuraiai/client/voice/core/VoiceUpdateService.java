package yadi.samuraiai.client.voice.core;

import java.util.concurrent.CompletableFuture;

/** Update hook. Atomic installation and rollback are owned by VoiceInstallationService. */
public final class VoiceUpdateService {
    public CompletableFuture<Boolean> checkForUpdates() { return CompletableFuture.completedFuture(false); }
}
