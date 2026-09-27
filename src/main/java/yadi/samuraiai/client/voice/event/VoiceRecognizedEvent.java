package yadi.samuraiai.client.voice.event;
import java.util.UUID;
public record VoiceRecognizedEvent(UUID sessionId, String text, long latencyMillis) {}
