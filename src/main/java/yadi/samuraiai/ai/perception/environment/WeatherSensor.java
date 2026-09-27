package yadi.samuraiai.ai.perception.environment;

import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Rain, storm, snow and the moment the weather changes. Rare by design: weather is slow, so it scans on a long interval
 * and reports only transitions, leaving the scheduler and behaviors to decide whether an NPC takes shelter.
 */
public final class WeatherSensor implements Sensor {
    @Override public SensorType type() { return SensorType.WEATHER; }

    @Override public void scan(SensorContext ctx) {
        var eye = ctx.perceiver();
        var state = ctx.state();
        WeatherState now = ctx.world().environment(eye.x(), eye.y(), eye.z()).weather();
        WeatherState before = state.environment.weather();
        if (!state.environmentKnown || now == before) return;
        double intensity = switch (now) { case STORM -> 0.8D; case RAIN -> 0.4D; case SNOW -> 0.3D; case CLEAR -> 0.15D; };
        state.environment = new EnvironmentSnapshot(state.environment.dimension(), state.environment.biome(), state.environment.y(), state.environment.temperature(),
                now, state.environment.dayTime(), state.environment.lightLevel(), state.environment.inWater(), state.environment.nearLava(), state.environment.skyExposed());
        ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.WEATHER, null, "weather " + before + " -> " + now, eye.x(), eye.y(), eye.z(), intensity, ctx.tick(), 200)
                .withDetail("weather " + now));
    }
}
