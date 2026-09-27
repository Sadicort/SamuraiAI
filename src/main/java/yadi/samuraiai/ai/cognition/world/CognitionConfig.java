package yadi.samuraiai.ai.cognition.world;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.config.RecordConfigBinder;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * Forge-backed configuration of the whole cognitive layer: one file per engine ({@code samuraiai-cognition.toml},
 * {@code -memory}, {@code -relationship}, {@code -emotion}, {@code -knowledge}), each mirroring its settings record one to one.
 * A reload republishes an immutable snapshot; values are clamped by the records themselves.
 */
public final class CognitionConfig {
    public static final ForgeConfigSpec COGNITION_SPEC, MEMORY_SPEC, RELATIONSHIP_SPEC, EMOTION_SPEC, KNOWLEDGE_SPEC;
    private static final RecordConfigBinder<CognitionSettings> COGNITION;
    private static final RecordConfigBinder<MemorySettings> MEMORY;
    private static final RecordConfigBinder<RelationshipSettings> RELATIONSHIP;
    private static final RecordConfigBinder<EmotionSettings> EMOTION;
    private static final RecordConfigBinder<KnowledgeSettings> KNOWLEDGE;
    private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLED;
    private static volatile boolean enabled = true;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Cognitive layer: how experiences become memory, emotion, relationships, knowledge and society. Empty lists use the built-in data.").push("cognition");
        ENABLED = b.comment("Master switch. When false NPCs neither remember nor learn, and nothing is read or written on disk.").define("enabled", true);
        COGNITION = new RecordConfigBinder<>(b, CognitionSettings.class, CognitionSettings.defaults());
        b.pop();
        COGNITION_SPEC = b.build();
        b = new ForgeConfigSpec.Builder();
        b.comment("Living memory engine.").push("memory");
        MEMORY = new RecordConfigBinder<>(b, MemorySettings.class, MemorySettings.defaults());
        b.pop();
        MEMORY_SPEC = b.build();
        b = new ForgeConfigSpec.Builder();
        b.comment("Relationship engine.").push("relationship");
        RELATIONSHIP = new RecordConfigBinder<>(b, RelationshipSettings.class, RelationshipSettings.defaults());
        b.pop();
        RELATIONSHIP_SPEC = b.build();
        b = new ForgeConfigSpec.Builder();
        b.comment("Emotion engine.").push("emotion");
        EMOTION = new RecordConfigBinder<>(b, EmotionSettings.class, EmotionSettings.defaults());
        b.pop();
        EMOTION_SPEC = b.build();
        b = new ForgeConfigSpec.Builder();
        b.comment("Knowledge and society engine.").push("knowledge");
        KNOWLEDGE = new RecordConfigBinder<>(b, KnowledgeSettings.class, KnowledgeSettings.defaults());
        b.pop();
        KNOWLEDGE_SPEC = b.build();
    }

    private CognitionConfig() { }

    public static boolean enabled() { return enabled; }
    /** Switches the layer on or off regardless of the file; physical tests use it. */
    public static void forceEnabled(boolean value) { enabled = value; }

    public static void onLoad(ModConfigEvent.Loading event) { pull(event.getConfig().getSpec()); }
    public static void onReload(ModConfigEvent.Reloading event) { pull(event.getConfig().getSpec()); }

    private static void pull(Object spec) {
        try {
            if (spec == COGNITION_SPEC) {
                CognitionSettings.Builder builder = CognitionSettings.builder();
                COGNITION.pull(builder);
                CognitionSettings.apply(builder.build());
                enabled = ENABLED.get();
            } else if (spec == MEMORY_SPEC) {
                MemorySettings.Builder builder = MemorySettings.builder();
                MEMORY.pull(builder);
                MemorySettings.apply(builder.build());
            } else if (spec == RELATIONSHIP_SPEC) {
                RelationshipSettings.Builder builder = RelationshipSettings.builder();
                RELATIONSHIP.pull(builder);
                RelationshipSettings.apply(builder.build());
            } else if (spec == EMOTION_SPEC) {
                EmotionSettings.Builder builder = EmotionSettings.builder();
                EMOTION.pull(builder);
                EmotionSettings.apply(builder.build());
            } else if (spec == KNOWLEDGE_SPEC) {
                KnowledgeSettings.Builder builder = KnowledgeSettings.builder();
                KNOWLEDGE.pull(builder);
                KnowledgeSettings.apply(builder.build());
            }
        } catch (RuntimeException error) {
            SamuraiLogger.CONFIG.warn("Invalid cognition configuration; keeping the previous values", error);
        }
    }
}
