package yadi.samuraiai.ai.perception.environment;

import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Where the NPC is: biome, height, temperature, water, lava, dimension. Keeps the environment snapshot current and reports
 * what changed (a new biome, entering water, lava close by, a change of dimension); lava nearby is also a hazard stimulus.
 */
public final class EnvironmentSensor implements Sensor {
    @Override public SensorType type() { return SensorType.ENVIRONMENT; }

    @Override public void scan(SensorContext ctx) {
        var eye = ctx.perceiver();
        var state = ctx.state();
        EnvironmentSnapshot raw = ctx.world().environment(eye.x(), eye.y(), eye.z());
        EnvironmentSnapshot before = state.environment;
        boolean known = state.environmentKnown;
        // The weather sensor owns the weather part and the light sensor the light part, so each can detect its own change.
        WeatherState weather = known ? before.weather() : raw.weather();
        EnvironmentSnapshot now = new EnvironmentSnapshot(raw.dimension(), raw.biome(), raw.y(), raw.temperature(), weather, raw.dayTime(), state.lightLevel,
                raw.inWater(), raw.nearLava(), raw.skyExposed());
        state.environment = now;
        state.environmentKnown = true;
        state.memory.remember(MemoryKind.ENVIRONMENTAL, "biome", null, now.biome(), eye.x(), eye.y(), eye.z(), 0, 0, 0.3D, ctx.tick(), "TERRAIN");
        if (!known) return;
        String change = null;
        if (!now.dimension().equals(before.dimension())) change = "dimension " + now.dimension();
        else if (!now.biome().equals(before.biome())) change = "biome " + now.biome();
        else if (now.inWater() && !before.inWater()) change = "entered water";
        else if (now.cave() && !before.cave()) change = "entered a cave";
        if (change != null)
            ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.TERRAIN, null, change, eye.x(), eye.y(), eye.z(), 0.4D, ctx.tick(), 100).withDetail(change));
        if (now.nearLava() && !before.nearLava())
            ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.LAVA, null, "lava nearby", eye.x(), eye.y(), eye.z(), 0.5D, ctx.tick(), 100).withDetail("lava nearby"));
    }
}
