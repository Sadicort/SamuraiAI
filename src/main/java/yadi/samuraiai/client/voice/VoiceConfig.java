package yadi.samuraiai.client.voice;

import java.util.concurrent.atomic.AtomicReference;

/** Client-only immutable snapshot. Audio is never stored in this object. */
public final class VoiceConfig {
    public record Values(boolean enabled, VoiceLanguageManager.Language language, String microphone,
                         float sensitivity, float gain, int maxRecordingSeconds, boolean insertAutomatically,
                         boolean sendAutomatically, boolean holdToTalk, boolean showOverlay,
                         boolean subtitles, boolean debug) {
        public Values {
            language = language == null ? VoiceLanguageManager.Language.AUTO : language;
            microphone = microphone == null ? "" : microphone;
            sensitivity = clamp(sensitivity, 0.01f, 1f); gain = clamp(gain, 0.1f, 4f);
            maxRecordingSeconds = Math.max(1, Math.min(120, maxRecordingSeconds));
        }
        private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    }
    private static final Values DEFAULTS = new Values(true, VoiceLanguageManager.Language.ES, "", .12f, 1f, 30, true, false, false, true, true, false);
    private static final AtomicReference<Values> CURRENT = new AtomicReference<>(DEFAULTS);
    public static Values get() { return CURRENT.get(); }
    public static Values defaults() { return DEFAULTS; }
    public static void apply(Values values) { CURRENT.set(values == null ? DEFAULTS : values); }
    private VoiceConfig() {}
}
