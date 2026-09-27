package yadi.samuraiai.ai.perception.world;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.config.RecordConfigBinder;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Forge-backed perception configuration ({@code samuraiai-perception.toml}). The file mirrors {@link PerceptionSettings}
 * one to one (field of view, distances, radii per sound, memory lifetimes, suspicion decay, attention thresholds, sensor
 * intervals, budgets), and a reload republishes an immutable snapshot atomically.
 */
public final class PerceptionConfig {
    public static final ForgeConfigSpec SPEC;
    private static final RecordConfigBinder<PerceptionSettings> BINDER;
    private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLED;
    private static volatile boolean enabled = true;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Perception engine 2.0. Values are clamped to safe ranges on load.").push("perception");
        ENABLED = builder.comment("Master switch. When false NPCs fall back to the basic nearest-player perception.").define("enabled", true);
        BINDER = new RecordConfigBinder<>(builder, PerceptionSettings.class, PerceptionSettings.defaults());
        builder.pop();
        SPEC = builder.build();
    }

    private PerceptionConfig() { }

    public static boolean enabled() { return enabled; }

    public static void onLoad(ModConfigEvent.Loading event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    public static void onReload(ModConfigEvent.Reloading event) { if (event.getConfig().getSpec() == SPEC) pull(); }

    private static void pull() {
        try {
            PerceptionSettings.Builder builder = PerceptionSettings.builder();
            BINDER.pull(builder);
            PerceptionSettings.apply(builder.build());
            enabled = ENABLED.get();
        } catch (RuntimeException error) {
            SamuraiLogger.CONFIG.warn("Invalid perception configuration; keeping the previous values", error);
        }
    }
}
