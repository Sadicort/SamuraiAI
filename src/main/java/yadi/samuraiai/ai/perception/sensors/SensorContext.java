package yadi.samuraiai.ai.perception.sensors;

import java.util.List;
import yadi.samuraiai.ai.perception.engine.PassOutput;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.engine.PerceptionWorld;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.hearing.SoundLog;
import yadi.samuraiai.ai.perception.vision.RayBudget;

/** Everything a sensor may use during a scan. The only way out is {@link #out()}. */
public final class SensorContext {
    private final Perceiver perceiver;
    private final PerceptionWorld world;
    private final long tick;
    private final PerceptionSettings settings;
    private final PerceptionState state;
    private final SoundLog sounds;
    private final RayBudget rays;
    private final PassOutput out;
    private int interval = 1;

    public SensorContext(Perceiver perceiver, PerceptionWorld world, long tick, PerceptionSettings settings, PerceptionState state,
                         SoundLog sounds, RayBudget rays, PassOutput out) {
        this.perceiver = perceiver; this.world = world; this.tick = tick; this.settings = settings; this.state = state;
        this.sounds = sounds; this.rays = rays; this.out = out;
    }

    public Perceiver perceiver() { return perceiver; }
    public PerceptionWorld world() { return world; }
    public long tick() { return tick; }
    public PerceptionSettings settings() { return settings; }
    public PerceptionState state() { return state; }
    public SoundLog sounds() { return sounds; }
    public RayBudget rays() { return rays; }
    public PassOutput out() { return out; }

    /** Effective ticks between scans of the sensor currently running (base interval stretched by tier, shortened by alertness). */
    public int interval() { return interval; }
    public void interval(int value) { interval = Math.max(1, value); }

    /**
     * Entities around the NPC, fresh within the entity interval. Whichever sensor asks first pays for the world query;
     * the others reuse it, so scan order never matters and one query serves the whole pass.
     */
    public List<SensedEntity> catalog() {
        if (state.catalogTick < 0 || tick - state.catalogTick >= Math.max(1, settings.entityInterval())) refreshCatalog();
        return state.catalog;
    }

    public void refreshCatalog() {
        double radius = Math.min(settings.entityScanRadius(), settings.visionFar() * perceiver.senses().visionRange());
        state.catalog = world.entitiesNear(perceiver.x(), perceiver.eyeY(), perceiver.z(), radius, perceiver.entityId());
        state.catalogTick = tick;
    }
}
