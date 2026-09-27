package yadi.samuraiai.ai.cognition.testkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.engine.CognitionEngine;
import yadi.samuraiai.ai.cognition.engine.CognitionOutcome;
import yadi.samuraiai.ai.cognition.engine.CognitionSettings;
import yadi.samuraiai.ai.cognition.engine.ExperienceInput;
import yadi.samuraiai.ai.cognition.engine.WorldFacts;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.emotion.engine.EmotionSettings;
import yadi.samuraiai.ai.emotion.model.Activity;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.event.NpcEvent;

/** A cognitive engine wired to a fake world: a clock, names, places and personalities the test controls, and every published event captured. */
public final class CognitionHarness {
    public final List<NpcEvent> events = new ArrayList<>();
    public MemorySettings memorySettings = MemorySettings.defaults();
    public RelationshipSettings relationshipSettings = RelationshipSettings.defaults();
    public EmotionSettings emotionSettings = EmotionSettings.defaults();
    public KnowledgeSettings knowledgeSettings = KnowledgeSettings.defaults();
    public CognitionSettings cognitionSettings = CognitionSettings.defaults();
    public final CognitionEngine engine;
    public long now = 1_000_000L;
    public String weather = "clear";
    public final Map<UUID, EntityRef> refs = new HashMap<>();
    public final Map<UUID, PlaceRef> places = new HashMap<>();
    public final Map<UUID, Set<String>> goals = new HashMap<>();
    public final Map<UUID, double[]> traits = new HashMap<>();

    public CognitionHarness() {
        engine = new CognitionEngine(() -> cognitionSettings, () -> memorySettings, () -> relationshipSettings, () -> emotionSettings, () -> knowledgeSettings, events::add);
        engine.useFacts(new WorldFacts() {
            @Override public String weather() { return weather; }
            @Override public PlaceRef placeOf(UUID npc) { return places.getOrDefault(npc, PlaceRef.unknown()); }
            @Override public Set<String> goalTags(UUID npc) { return goals.getOrDefault(npc, Set.of()); }
            @Override public EntityRef refOf(UUID id) { return refs.getOrDefault(id, new EntityRef(id, EntityKind.UNKNOWN, "")); }
        });
        engine.useTraitSource(id -> traits.get(id) == null ? filled(50.0D) : traits.get(id));
    }

    public static double[] filled(double v) {
        double[] a = new double[yadi.samuraiai.ai.cognition.model.Trait.values().length];
        java.util.Arrays.fill(a, v);
        return a;
    }

    public EntityRef npc(String name) { EntityRef r = EntityRef.npc(UUID.randomUUID(), name); refs.put(r.id(), r); return r; }
    public EntityRef player(String name) { EntityRef r = EntityRef.player(UUID.randomUUID(), name); refs.put(r.id(), r); return r; }
    public PlaceRef at(String zone, double x, double z) { return new PlaceRef("minecraft:overworld", x, 64, z, zone); }
    public void advance(long ticks) { now += ticks; }

    public void setTrait(EntityRef npc, yadi.samuraiai.ai.cognition.model.Trait trait, double value) {
        double[] t = traits.computeIfAbsent(npc.id(), k -> filled(50.0D));
        t[trait.ordinal()] = value;
    }

    public CognitionOutcome live(EntityRef npc, ExperienceKind kind, EntityRef actor, EntityRef target) { return engine.experience(ExperienceInput.of(npc.id(), kind, now).actor(actor).target(target)); }
    public CognitionOutcome live(ExperienceInput input) { return engine.experience(input); }
    public ExperienceInput input(EntityRef npc, ExperienceKind kind) { return ExperienceInput.of(npc.id(), kind, now); }

    /** Runs the NPC's mind for {@code ticks} in steps, as the world adapter would. */
    public void run(EntityRef npc, long ticks, long step, Activity activity) {
        for (long t = 0; t < ticks; t += step) { now += step; engine.tick(npc.id(), now, activity); }
    }

    public <T extends NpcEvent> List<T> events(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (NpcEvent e : events) if (type.isInstance(e)) result.add(type.cast(e));
        return result;
    }
}
