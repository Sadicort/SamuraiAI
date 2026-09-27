package yadi.samuraiai.client.voice.gui;

public final class VoiceWaveRenderer {
    public float amplitude(float level, long tick) { return Math.max(0f, Math.min(1f, level)) * (0.75f + 0.25f * (float)Math.sin(tick * .35)); }
}
