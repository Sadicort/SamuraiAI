package yadi.samuraiai.ai.scheduler.testkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInput;
import yadi.samuraiai.ai.scheduler.engine.BehaviorScheduler;
import yadi.samuraiai.ai.scheduler.engine.InputSource;
import yadi.samuraiai.ai.scheduler.engine.Light;
import yadi.samuraiai.ai.scheduler.engine.Perceived;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.ai.scheduler.engine.SchedulerInput;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.zone.Place;
import yadi.samuraiai.event.NpcEvent;

/**
 * A small simulated world for the scheduler: NPCs with a position, senses and feelings the test can set, and a stand-in for
 * the Brain and Navigation that walks each NPC towards the place its advice names at a fixed speed. That closes the loop
 * (advice, movement, arrival, next advice) without Minecraft.
 */
public final class SchedulerHarness implements InputSource {
    public static final String DIM = "test:world";

    /** One simulated NPC. */
    public static final class Sim {
        public final UUID id;
        public final String type;
        public double x, y = 64, z;
        public Perceived perceived = Perceived.CALM;
        public EmotionInput emotion = EmotionInput.CALM;
        public boolean canFight;
        public Place home;
        public double playerDistance = 10;
        public double speed = 0.25D;
        public double walked;
        Sim(UUID id, String type, double x, double z) { this.id = id; this.type = type; this.x = x; this.z = z; this.home = new Place(DIM, x, 64, z, 3, null); }
    }

    public final List<NpcEvent> events = new ArrayList<>();
    public final BehaviorScheduler scheduler;
    public final Map<UUID, Sim> npcs = new LinkedHashMap<>();
    public long tick = 1000;
    public long worldTime;
    private int counter;

    public SchedulerHarness() { this(SchedulerSettings.defaults()); }

    public SchedulerHarness(SchedulerSettings settings) {
        this.scheduler = new BehaviorScheduler(() -> settings, events::add);
    }

    public static SchedulerHarness with(UnaryOperator<SchedulerSettings.Builder> change) {
        return new SchedulerHarness(change.apply(SchedulerSettings.builder()).build());
    }

    public Sim add(String type, double x, double z) {
        Sim s = new Sim(new UUID(0L, 1000L + ++counter), type, x, z);
        npcs.put(s.id, s);
        return s;
    }

    public void remove(Sim s) { npcs.remove(s.id); }

    /** Runs the world for {@code ticks} ticks, walking every NPC towards its advised place after each scheduler tick. */
    public void run(int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick++; worldTime++;
            scheduler.tick(tick, worldTime, this);
            for (Sim s : npcs.values()) walk(s);
        }
    }

    public void runUntil(java.util.function.BooleanSupplier condition, int maxTicks) {
        for (int i = 0; i < maxTicks && !condition.getAsBoolean(); i++) run(1);
    }

    private void walk(Sim s) {
        SchedulerAdvice advice = scheduler.advice(s.id).orElse(null);
        if (advice == null || advice.place() == null || !advice.active()) return;
        double dx = advice.place().x() - s.x, dz = advice.place().z() - s.z, d = Math.hypot(dx, dz);
        if (d <= Math.max(0.5D, advice.place().radius() * 0.5D)) return;
        double step = Math.min(s.speed, d);
        s.x += dx / d * step; s.z += dz / d * step; s.walked += step;
    }

    public SchedulerAdvice advice(Sim s) { return scheduler.advice(s.id).orElse(null); }

    public <T extends NpcEvent> List<T> events(Class<T> type) {
        List<T> out = new ArrayList<>();
        for (NpcEvent e : events) if (type.isInstance(e)) out.add(type.cast(e));
        return out;
    }

    public void setWorldTime(long time) { this.worldTime = time; }
    public void canFight(Sim s) { s.canFight = true; }

    @Override public Collection<UUID> npcs() { return List.copyOf(npcs.keySet()); }

    @Override public Optional<Light> light(UUID id) {
        Sim s = npcs.get(id);
        return s == null ? Optional.empty() : Optional.of(new Light(s.type, DIM, s.x, s.y, s.z, s.playerDistance));
    }

    @Override public Optional<SchedulerInput> input(UUID id, long tick, long worldTime) {
        Sim s = npcs.get(id);
        return s == null ? Optional.empty() : Optional.of(new SchedulerInput(id, s.type, DIM, s.x, s.y, s.z, worldTime, tick, s.emotion, s.perceived, s.playerDistance, s.canFight, s.home));
    }
}
