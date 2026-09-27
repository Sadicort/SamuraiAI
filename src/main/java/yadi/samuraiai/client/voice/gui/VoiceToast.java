package yadi.samuraiai.client.voice.gui;

public record VoiceToast(String message, long expiresAtMillis) {
    public boolean visible() { return System.currentTimeMillis() < expiresAtMillis; }
}
