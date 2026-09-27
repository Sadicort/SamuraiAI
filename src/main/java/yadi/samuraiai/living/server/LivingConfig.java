package yadi.samuraiai.living.server;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.config.RecordConfigBinder;
import yadi.samuraiai.config.RecordSettingsBuilder;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.economy.engine.EconomySettings;
import yadi.samuraiai.living.family.engine.FamilySettings;
import yadi.samuraiai.living.quest.engine.QuestSettings;
import yadi.samuraiai.living.sim.LivingSettings;
import yadi.samuraiai.living.village.engine.VillageSettings;
import yadi.samuraiai.living.world.engine.WorldSettings;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Forge-backed configuration of the living world: one file per engine ({@code samuraiai-living.toml} for the hub and
 * {@code -calendar}, {@code -world}, {@code -village}, {@code -economy}, {@code -quest}, {@code -family}), each mirroring its
 * settings record one to one. Empty data lists use the built-in data. A reload republishes an immutable snapshot that the
 * engines read at their next tick; values are clamped by the records themselves.
 */
public final class LivingConfig {
    /** One file: its spec and how to pull it into the settings record. */
    private static final class Section<T extends Record, B extends RecordSettingsBuilder<T, B>> {
        final ForgeConfigSpec spec;
        final RecordConfigBinder<T> binder;
        final java.util.function.Supplier<B> builder;
        final java.util.function.Consumer<T> apply;

        Section(String path, String comment, Class<T> type, T defaults, java.util.function.Supplier<B> builder, java.util.function.Consumer<T> apply) {
            ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
            b.comment(comment).push(path);
            this.binder = new RecordConfigBinder<>(b, type, defaults);
            b.pop();
            this.spec = b.build();
            this.builder = builder;
            this.apply = apply;
        }

        void pull() {
            B b = builder.get();
            binder.pull(b);
            apply.accept(b.build());
        }
    }

    private static final Section<LivingSettings, LivingSettings.Builder> LIVING = new Section<>("living",
            "Living world hub: master switch, tick buckets, saving, automatic villages, quest condition scans, vanilla weather.",
            LivingSettings.class, LivingSettings.defaults(), LivingSettings::builder, LivingSettings::apply);
    private static final Section<CalendarSettings, CalendarSettings.Builder> CALENDAR = new Section<>("calendar",
            "Deiliora calendar: official time, seasons, weather, temperature, moon, festivals, holidays, agriculture, timeline.",
            CalendarSettings.class, CalendarSettings.defaults(), CalendarSettings::builder, CalendarSettings::apply);
    private static final Section<WorldSettings, WorldSettings.Builder> WORLD = new Section<>("world",
            "World engine: regions, settlements, roads, world events, streaming LOD, offline simulation, wildlife, professions.",
            WorldSettings.class, WorldSettings.defaults(), WorldSettings::builder, WorldSettings::apply);
    private static final Section<VillageSettings, VillageSettings.Builder> VILLAGE = new Section<>("village",
            "Village engine: villages, districts, buildings, homes, citizens, timetables, market, temple, guards, social life.",
            VillageSettings.class, VillageSettings.defaults(), VillageSettings::builder, VillageSettings::apply);
    private static final Section<EconomySettings, EconomySettings.Builder> ECONOMY = new Section<>("economy",
            "Economy engine: resources, stores, production, consumption, prices, merchants, caravans, contracts, taxes.",
            EconomySettings.class, EconomySettings.defaults(), EconomySettings::builder, EconomySettings::apply);
    private static final Section<QuestSettings, QuestSettings.Builder> QUEST = new Section<>("quest",
            "Dynamic quest engine: conditions, templates, rewards, branches, campaigns.",
            QuestSettings.class, QuestSettings.defaults(), QuestSettings::builder, QuestSettings::apply);
    private static final Section<FamilySettings, FamilySettings.Builder> FAMILY = new Section<>("family",
            "Family engine: people, genealogy, households, lineages, mentorship, inheritance, succession.",
            FamilySettings.class, FamilySettings.defaults(), FamilySettings::builder, FamilySettings::apply);

    public static final ForgeConfigSpec LIVING_SPEC = LIVING.spec, CALENDAR_SPEC = CALENDAR.spec, WORLD_SPEC = WORLD.spec, VILLAGE_SPEC = VILLAGE.spec,
            ECONOMY_SPEC = ECONOMY.spec, QUEST_SPEC = QUEST.spec, FAMILY_SPEC = FAMILY.spec;

    private LivingConfig() { }

    public static void onLoad(ModConfigEvent.Loading event) { pull(event.getConfig().getSpec()); }
    public static void onReload(ModConfigEvent.Reloading event) { pull(event.getConfig().getSpec()); }

    private static void pull(Object spec) {
        try {
            for (Section<?, ?> s : new Section<?, ?>[]{LIVING, CALENDAR, WORLD, VILLAGE, ECONOMY, QUEST, FAMILY})
                if (s.spec == spec) { s.pull(); return; }
        } catch (RuntimeException error) {
            SamuraiLogger.CORE.error("Living world configuration could not be applied; previous values kept", error);
        }
    }
}
