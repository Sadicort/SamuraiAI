package yadi.samuraiai.ai.perception.sensors;

import yadi.samuraiai.ai.perception.vision.VisionEngine;

/** The main sense: runs the vision engine (field of view, raycasts, occlusion, confidence) over the entity catalog. */
public final class VisionSensor implements Sensor {
    private final VisionEngine engine = new VisionEngine();

    @Override public SensorType type() { return SensorType.VISION; }

    @Override public void scan(SensorContext ctx) {
        VisionEngine.ScanResult result = engine.scan(ctx.perceiver(), ctx.world(), ctx.settings(), ctx.tick(), ctx.catalog(),
                ctx.state().tracks, ctx.rays(), ctx.out(), ctx.interval());
        ctx.out().transitions.addAll(result.transitions());
        ctx.out().targetsEvaluated += result.targetsEvaluated();
    }
}
