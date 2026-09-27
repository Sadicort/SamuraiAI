package yadi.samuraiai.living.sim;

import yadi.samuraiai.config.RecordSettingsBuilder;

/**
 * The hub's own settings ({@code samuraiai-living.toml}): the master switch, how work is spread over ticks (buckets), save
 * cadence, automatic villages for NPCs that arrive where there is none, how often world conditions are scanned for quests, and
 * what the Forge adapter does with the real game (whether Deiliora's weather drives the vanilla sky is the calendar's
 * {@code driveVanillaWeather}): whether the environment interactions touch real blocks (campfires, crops, beds), whether players near a
 * village hear its news, how often player positions are read for quests, how often scheduler zones become buildings, and
 * which Minecraft items stand for each economy resource ({@code resourceItems}, lines {@code rice=minecraft:wheat|...};
 * empty uses the built-in table).
 */
public record LivingSettings(
        boolean enabled, int buckets, int saveIntervalTicks, int maxSavesPerTick, boolean compressStorage,
        boolean autoVillages, double villageJoinRadius, int autoVillageRadius, boolean planAutoVillages,
        boolean conditionScan, int festivalWarningDays, int ritualWarningDays, double guardRatio, boolean spawnExplorationQuests,
        boolean celebrateGreatHarvests, boolean debugLogging,
        boolean worldInteractions, boolean announceToPlayers, int playerSignalTicks, double announceRadius, int zoneImportTicks, java.util.List<String> resourceItems) {

    public LivingSettings {
        buckets = Math.max(1, Math.min(20, buckets)); saveIntervalTicks = Math.max(20, saveIntervalTicks); maxSavesPerTick = Math.max(1, maxSavesPerTick);
        villageJoinRadius = Math.max(8, villageJoinRadius); autoVillageRadius = Math.max(16, autoVillageRadius);
        festivalWarningDays = Math.max(1, festivalWarningDays); ritualWarningDays = Math.max(0, ritualWarningDays); guardRatio = Math.max(0, Math.min(1, guardRatio));
        playerSignalTicks = Math.max(5, playerSignalTicks); announceRadius = Math.max(0, announceRadius); zoneImportTicks = Math.max(20, zoneImportTicks);
        resourceItems = resourceItems == null ? java.util.List.of() : java.util.List.copyOf(resourceItems);
    }

    public static LivingSettings defaults() { return new LivingSettings(true, 5, 1200, 2, true, true, 96, 64, false, true, 3, 2, 0.1, true, true, false,
            true, true, 40, 96, 600, java.util.List.of()); }

    private static volatile LivingSettings current = defaults();
    public static LivingSettings current() { return current; }
    public static void apply(LivingSettings next) { current = java.util.Objects.requireNonNull(next); }
    public static Builder builder() { return new Builder(defaults()); }

    public static final class Builder extends RecordSettingsBuilder<LivingSettings, Builder> {
        private Builder(LivingSettings base) { super(LivingSettings.class, base); }
        @Override protected Builder self() { return this; }
    }
}
