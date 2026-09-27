package yadi.samuraiai.client.voice;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.logging.SamuraiLogger;

public final class VoiceForgeConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue enabled, insert, send, hold, overlay, subtitles, debug;
    private static final ForgeConfigSpec.EnumValue<VoiceLanguageManager.Language> language;
    private static final ForgeConfigSpec.ConfigValue<String> microphone;
    private static final ForgeConfigSpec.DoubleValue sensitivity, gain;
    private static final ForgeConfigSpec.IntValue maxSeconds;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        enabled = b.define("voice.enabled", true); language = b.defineEnum("voice.language", VoiceLanguageManager.Language.ES);
        microphone = b.define("voice.microphone", "");
        sensitivity = b.defineInRange("voice.sensitivity", .12D, .01D, 1D); gain = b.defineInRange("voice.gain", 1D, .1D, 4D);
        maxSeconds = b.defineInRange("voice.maxRecordingSeconds", 30, 1, 120); insert = b.define("voice.insertAutomatically", true);
        send = b.define("voice.sendAutomatically", false); hold = b.define("voice.holdToTalk", false); overlay = b.define("voice.showOverlay", true);
        subtitles = b.define("voice.showSubtitles", true); debug = b.define("voice.debug", false); SPEC = b.build();
    }
    public static void onLoad(ModConfigEvent event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    public static void onReload(ModConfigEvent event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    private static void pull() {
        VoiceConfig.apply(new VoiceConfig.Values(enabled.get(), language.get(), microphone.get(), sensitivity.get().floatValue(), gain.get().floatValue(), maxSeconds.get(), insert.get(), send.get(), hold.get(), overlay.get(), subtitles.get(), debug.get()));
        SamuraiLogger.CONFIG.info("Voice configuration published");
    }
    public static void save(VoiceConfig.Values values) {
        if (values == null || !SPEC.isLoaded()) return;
        enabled.set(values.enabled()); language.set(values.language()); microphone.set(values.microphone());
        sensitivity.set((double)values.sensitivity()); gain.set((double)values.gain()); maxSeconds.set(values.maxRecordingSeconds());
        insert.set(values.insertAutomatically()); send.set(values.sendAutomatically()); hold.set(values.holdToTalk());
        overlay.set(values.showOverlay()); subtitles.set(values.subtitles()); debug.set(values.debug());
        SPEC.save(); pull();
    }
    public static void restoreDefaults() { save(VoiceConfig.defaults()); }
    private VoiceForgeConfig() {}
}
