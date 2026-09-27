package yadi.samuraiai.ai.perception.environment;

import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Sunlight, artificial light, darkness, shadow and night. Samples the light around the NPC (the darkest nearby point
 * counts, since shadows are what matter) and reports the moments the surroundings turn dark or night falls. Later
 * behaviors can make patrols and fear depend on it.
 */
public final class LightSensor implements Sensor {
    private static final int[][] OFFSETS = {{0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3}};

    @Override public SensorType type() { return SensorType.LIGHT; }

    @Override public void scan(SensorContext ctx) {
        var eye = ctx.perceiver();
        var state = ctx.state();
        int x = (int) Math.floor(eye.x()), y = (int) Math.floor(eye.eyeY()), z = (int) Math.floor(eye.z());
        int here = ctx.world().lightLevel(x, y, z), darkest = here;
        for (int[] o : OFFSETS) darkest = Math.min(darkest, ctx.world().lightLevel(x + o[0], y, z + o[1]));
        boolean wasDark = state.lightLevel < 4;
        int previous = state.lightLevel;
        state.lightLevel = here;
        boolean nowDark = here < 4;
        if (nowDark && !wasDark && state.environmentKnown)
            ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.LIGHT, null, "it got dark", eye.x(), eye.y(), eye.z(), 0.6D, ctx.tick(), 100)
                    .withDetail("darkness " + here + " (shadow " + darkest + ")"));
        else if (!nowDark && wasDark && previous < 4 && state.environmentKnown)
            ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.LIGHT, null, "light returned", eye.x(), eye.y(), eye.z(), 0.2D, ctx.tick(), 60)
                    .withDetail("light " + here));
    }
}
