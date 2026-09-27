package yadi.samuraiai.ai.navigation.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.ai.navigation.engine.NavigationSettings;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Forge-backed navigation configuration ({@code samuraiai-navigation.toml}). Every value is a component of
 * {@link NavigationSettings}; the file is the external configuration the design requires, and reloading it
 * republishes an immutable snapshot atomically.
 */
public final class NavigationConfig {
    public static final ForgeConfigSpec SPEC;
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLED;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COST_OVERRIDES;
    private static volatile boolean enabled = true;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        NavigationSettings d = NavigationSettings.defaults();
        b.comment("Master switch. When false behaviors fall back to the controller's basic movement.").push("navigation");
        ENABLED = b.define("enabled", true);
        b.comment("Search budget").push("search");
        integer(b, "searchNodesPerTick", d.searchNodesPerTick(), 50, 20000);
        integer(b, "maxSearchNodes", d.maxSearchNodes(), 100, 200000);
        integer(b, "maxSearchRadius", d.maxSearchRadius(), 8, 512);
        integer(b, "partialMinGain", d.partialMinGain(), 1, 256);
        integer(b, "maxSessionsPerTick", d.maxSessionsPerTick(), 1, 5000);
        integer(b, "nodeCacheMax", d.nodeCacheMax(), 1000, 1_000_000);
        b.pop().comment("Path cache").push("cache");
        integer(b, "cacheCapacity", d.cacheCapacity(), 0, 10000);
        integer(b, "cacheTtlTicks", d.cacheTtlTicks(), 20, 200000);
        b.pop().comment("Walking and verification").push("movement");
        integer(b, "lookaheadNodes", d.lookaheadNodes(), 1, 64);
        integer(b, "verifyIntervalTicks", d.verifyIntervalTicks(), 1, 200);
        decimal(b, "reachRadius", d.reachRadius(), 0.2D, 3.0D);
        decimal(b, "entityLookaheadBlocks", d.entityLookaheadBlocks(), 1.0D, 16.0D);
        decimal(b, "walkSpeed", d.walkSpeed(), 0.1D, 4.0D);
        decimal(b, "runSpeed", d.runSpeed(), 0.1D, 4.0D);
        decimal(b, "sprintSpeed", d.sprintSpeed(), 0.1D, 4.0D);
        decimal(b, "sneakSpeed", d.sneakSpeed(), 0.1D, 4.0D);
        decimal(b, "turnSlowdown", d.turnSlowdown(), 0.0D, 1.0D);
        decimal(b, "maxTurnDegrees", d.maxTurnDegrees(), 5.0D, 360.0D);
        integer(b, "maxSafeDrop", d.maxSafeDrop(), 1, 20);
        integer(b, "defaultTimeoutTicks", d.defaultTimeoutTicks(), 20, 200000);
        b.pop().comment("Doors").push("doors");
        bool(b, "closeDoorsBehind", d.closeDoorsBehind());
        integer(b, "doorWaitTicks", d.doorWaitTicks(), 0, 200);
        b.pop().comment("Obstacles, stuck detection and recovery").push("recovery");
        integer(b, "tempObstacleWaitTicks", d.tempObstacleWaitTicks(), 0, 1200);
        integer(b, "stuckTicks", d.stuckTicks(), 5, 1200);
        decimal(b, "stuckMinProgress", d.stuckMinProgress(), 0.01D, 5.0D);
        integer(b, "maxRecoveryAttempts", d.maxRecoveryAttempts(), 0, 20);
        integer(b, "maxRecalculations", d.maxRecalculations(), 0, 100);
        bool(b, "allowTeleportRecovery", d.allowTeleportRecovery());
        integer(b, "chunkWaitTicks", d.chunkWaitTicks(), 1, 12000);
        integer(b, "startRetryTicks", d.startRetryTicks(), 0, 400);
        b.pop().comment("Danger and scanning").push("terrain");
        integer(b, "scanRadius", d.scanRadius(), 2, 64);
        decimal(b, "dangerCap", d.dangerCap(), 1.0D, 1000.0D);
        COST_OVERRIDES = b.comment("Cost multipliers as NAME=value (at least 1). NAME is a TerrainType or a floor Material, e.g. MUD=2.0, ROUGH=1.8")
                .defineListAllowEmpty(List.of("costOverrides"), List::of, o -> o instanceof String);
        b.pop().comment("Update frequency by distance to the nearest player").push("frequency");
        decimal(b, "nearPlayerDistance", d.nearPlayerDistance(), 4.0D, 512.0D);
        decimal(b, "farPlayerDistance", d.farPlayerDistance(), 4.0D, 1024.0D);
        integer(b, "farUpdateInterval", d.farUpdateInterval(), 1, 100);
        b.pop().comment("Diagnostics").push("debug");
        bool(b, "debugLogging", d.debugLogging());
        b.pop().pop();
        SPEC = b.build();
    }

    private NavigationConfig() { }

    private static void integer(ForgeConfigSpec.Builder b, String name, int def, int min, int max) { VALUES.put(name, b.defineInRange(name, def, min, max)); }
    private static void decimal(ForgeConfigSpec.Builder b, String name, double def, double min, double max) { VALUES.put(name, b.defineInRange(name, def, min, max)); }
    private static void bool(ForgeConfigSpec.Builder b, String name, boolean def) { VALUES.put(name, b.define(name, def)); }

    public static boolean enabled() { return enabled; }

    public static void onLoad(ModConfigEvent.Loading event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC) return;
        pull();
        NavigationService.getInstance().configReloaded();
    }

    private static void pull() {
        try {
            NavigationSettings.Builder builder = NavigationSettings.builder();
            VALUES.forEach((name, value) -> builder.set(name, value.get()));
            List<String> overrides = new ArrayList<>();
            for (Object entry : COST_OVERRIDES.get()) if (entry instanceof String text) overrides.add(text);
            builder.set("costOverrides", overrides);
            NavigationSettings.apply(builder.build());
            enabled = ENABLED.get();
        } catch (RuntimeException error) {
            SamuraiLogger.CONFIG.warn("Invalid navigation configuration; keeping the previous values", error);
        }
    }
}
