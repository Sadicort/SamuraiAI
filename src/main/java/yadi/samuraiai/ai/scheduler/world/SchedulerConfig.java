package yadi.samuraiai.ai.scheduler.world;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.config.RecordConfigBinder;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Forge-backed scheduler configuration ({@code samuraiai-scheduler.toml}). The file mirrors {@link SchedulerSettings} one to one
 * (timeline boundaries, thresholds, energy rates, interruption limits, group and zone parameters, optimisation buckets and
 * budgets) plus the three catalogues (lifestyles, calendar, routine overrides), and a reload republishes an immutable snapshot.
 */
public final class SchedulerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final RecordConfigBinder<SchedulerSettings> BINDER;
    private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLED;
    private static volatile boolean enabled = true;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Behavior scheduler. Values are clamped to safe ranges on load. Empty lifestyle/calendar/routineOverrides lists use the built-in catalogue.").push("scheduler");
        ENABLED = builder.comment("Master switch. When false NPCs are driven only by their brain's own goals.").define("enabled", true);
        BINDER = new RecordConfigBinder<>(builder, SchedulerSettings.class, SchedulerSettings.defaults());
        builder.pop();
        SPEC = builder.build();
    }

    private SchedulerConfig() { }

    public static boolean enabled() { return enabled; }

    /** Switches the scheduler on or off regardless of the file; physical tests use it to exercise one engine at a time. */
    public static void forceEnabled(boolean value) { enabled = value; }

    public static void onLoad(ModConfigEvent.Loading event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    public static void onReload(ModConfigEvent.Reloading event) { if (event.getConfig().getSpec() == SPEC) pull(); }

    private static void pull() {
        try {
            SchedulerSettings.Builder builder = SchedulerSettings.builder();
            BINDER.pull(builder);
            SchedulerSettings.apply(builder.build());
            enabled = ENABLED.get();
        } catch (RuntimeException error) {
            SamuraiLogger.CONFIG.warn("Invalid scheduler configuration; keeping the previous values", error);
        }
    }
}
