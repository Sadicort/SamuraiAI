package yadi.samuraiai.ai.perception.smell;

import java.util.Map;
import yadi.samuraiai.ai.perception.engine.BlockInterest;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;
import yadi.samuraiai.ai.perception.vision.VisionCone;
import yadi.samuraiai.ai.perception.vision.VisionZone;

/**
 * Smell needs no line of sight: smoke from a fire the block sensor already knows about carries through walls, and a living
 * thing standing close behind the NPC (where it cannot see) is noticed by scent. Cheap by construction: it reuses the block
 * memory and the entity catalog instead of scanning anything itself.
 */
public final class SmellSensor implements Sensor {
    private static final double SCENT_RADIUS = 4.0D;

    @Override public SensorType type() { return SensorType.SMELL; }

    @Override public void scan(SensorContext ctx) {
        var eye = ctx.perceiver();
        for (Map.Entry<Long, BlockInterest> block : ctx.state().blockMemory.entrySet()) {
            if (block.getValue() != BlockInterest.FIRE) continue;
            double bx = PerceptionState.unpackX(block.getKey()) + 0.5D, by = PerceptionState.unpackY(block.getKey()) + 0.5D, bz = PerceptionState.unpackZ(block.getKey()) + 0.5D;
            double distance = Math.sqrt((bx - eye.x()) * (bx - eye.x()) + (by - eye.y()) * (by - eye.y()) + (bz - eye.z()) * (bz - eye.z()));
            double smokeRadius = ctx.settings().blockScanRadius();
            if (distance > smokeRadius) continue;
            ctx.out().accept(new Stimulus(StimulusType.SMELL, StimulusCategory.SCENT, null, "smoke", bx, by, bz, distance * 0.5D, 1.0D - distance / smokeRadius,
                    StimulusCategory.FIRE.basePriority() * 0.6D, ctx.tick(), 120, "smoke"));
        }
        for (SensedEntity e : ctx.catalog()) {
            if (e.id().equals(eye.entityId()) || !e.kind().living()) continue;
            double distance = e.distanceTo(eye.x(), eye.y(), eye.z());
            if (distance > SCENT_RADIUS) continue;
            if (VisionCone.evaluate(eye, e.x(), e.centerY(), e.z(), ctx.settings()).zone() != VisionZone.REAR) continue;
            ctx.out().accept(new Stimulus(StimulusType.SMELL, StimulusCategory.SCENT, e.id(), e.name(), e.x(), e.y(), e.z(), distance * 0.6D, 0.3D + 0.4D * (1.0D - distance / SCENT_RADIUS),
                    StimulusCategory.SCENT.basePriority(), ctx.tick(), 60, "scent of " + e.kind()));
        }
    }
}
