package yadi.samuraiai.client.voice.event;
import yadi.samuraiai.client.voice.core.VoiceEngineState;
public record VoiceReadyEvent(VoiceEngineState state, String model) {}
