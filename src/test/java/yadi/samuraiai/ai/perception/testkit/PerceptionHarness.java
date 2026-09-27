package yadi.samuraiai.ai.perception.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import yadi.samuraiai.ai.perception.engine.PerceptionEngine;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.hearing.SoundEvent;
import yadi.samuraiai.ai.perception.hearing.SoundLog;
import yadi.samuraiai.ai.perception.metrics.PerceptionMetrics;
import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.vision.RayBudget;
import yadi.samuraiai.event.NpcEvent;

/** One observer NPC in a grid world, with a full engine, its own state and a captured event log. Ticks are explicit. */
public final class PerceptionHarness {
    public final GridPerceptionWorld world = new GridPerceptionWorld();
    public final SoundLog sounds = new SoundLog();
    public final PerceptionMetrics metrics = new PerceptionMetrics();
    public final List<NpcEvent> events = new ArrayList<>();
    public PerceptionSettings settings;
    public PerceptionEngine engine;
    public final UUID npcId = UUID.randomUUID();
    public PerceptionState state = new PerceptionState(npcId);
    public Perceiver perceiver = Perceiver.simple(npcId, 0.5D, 64.0D, 0.5D, 0.0F);
    public int tierMultiplier = 1;
    public int rayBudget = 100000;
    public long tick = 100;
    public PerceptionSnapshot snapshot;

    public PerceptionHarness() { this(b -> b); }
    public PerceptionHarness(UnaryOperator<PerceptionSettings.Builder> tuning) {
        settings = tuning.apply(PerceptionSettings.builder()).build();
        engine = new PerceptionEngine(() -> settings, metrics);
    }
    public PerceptionHarness(List<Sensor> sensors) {
        settings = PerceptionSettings.defaults();
        engine = new PerceptionEngine(() -> settings, metrics, sensors);
    }

    /** A player standing at the given block, at ground level. */
    public SensedEntity player(double x, double z) { return world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Player", x, 64.0D, z)); }
    public SensedEntity entity(EntityClass kind, String name, double x, double z) { return world.add(SensedEntity.at(UUID.randomUUID(), kind, name, x, 64.0D, z)); }

    public void run(int ticks) { for (int i = 0; i < ticks; i++) step(); }

    public PerceptionEngine.PassResult step() {
        tick++;
        world.tick = tick;
        var result = engine.process(perceiver, world, sounds, state, tick, tierMultiplier, new RayBudget(rayBudget));
        events.addAll(result.events());
        snapshot = result.snapshot();
        return result;
    }

    public void sound(SoundEvent event) { sounds.add(event); }
    public <T extends NpcEvent> List<T> events(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }
    public boolean saw(Class<? extends NpcEvent> type) { return events.stream().anyMatch(type::isInstance); }
    public void clearEvents() { events.clear(); }
    public void face(float yaw) { perceiver = perceiver.at(perceiver.x(), perceiver.y(), perceiver.z(), yaw); }
    public void moveTo(double x, double z) { perceiver = perceiver.at(x, perceiver.y(), z, perceiver.yaw()); }
}
