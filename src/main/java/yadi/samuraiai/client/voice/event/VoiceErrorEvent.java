package yadi.samuraiai.client.voice.event;
import java.util.UUID;
public record VoiceErrorEvent(UUID sessionId, String message) {}
