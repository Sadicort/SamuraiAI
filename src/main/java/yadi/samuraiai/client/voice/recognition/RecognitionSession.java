package yadi.samuraiai.client.voice.recognition;

import yadi.samuraiai.client.voice.VoiceLanguageManager;
import java.util.UUID;

public record RecognitionSession(UUID id, VoiceLanguageManager.Language language, long startedAtMillis, long durationMillis, String text) {
    public RecognitionSession(VoiceLanguageManager.Language language) { this(UUID.randomUUID(), language, System.currentTimeMillis(), 0L, ""); }
}
