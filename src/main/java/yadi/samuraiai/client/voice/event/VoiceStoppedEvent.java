package yadi.samuraiai.client.voice.event;
import java.util.UUID;
public record VoiceStoppedEvent(UUID sessionId, boolean cancelled) {}
