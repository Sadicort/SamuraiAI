package yadi.samuraiai.client.voice.core;

import yadi.samuraiai.client.voice.SpeechRecognitionService;

public final class VoiceEngineLoader {
    public SpeechRecognitionService load(VoiceEngineManager manager) { return new EmbeddedWhisperEngine(manager); }
}
