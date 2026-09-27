package yadi.samuraiai.ai.perception.touch;

import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Contact sense: anything within touch distance registers whatever the NPC is facing, even behind its back and even in
 * the dark. Something brushing past is noticed by feel, not by sight.
 */
public final class TouchSensor implements Sensor {
    @Override public SensorType type() { return SensorType.TOUCH; }

    @Override public void scan(SensorContext ctx) {
        var eye = ctx.perceiver();
        double radius = ctx.settings().touchRadius();
        for (SensedEntity e : ctx.catalog()) {
            if (e.id().equals(eye.entityId()) || !e.kind().living()) continue;
            double distance = Math.hypot(e.x() - eye.x(), e.z() - eye.z());
            double reach = radius + e.width() / 2.0D;
            if (distance > reach || Math.abs(e.y() - eye.y()) > 1.6D) continue;
            double intensity = Math.max(0.2D, 1.0D - distance / reach);
            ctx.out().accept(new Stimulus(StimulusType.TOUCH, StimulusCategory.CONTACT, e.id(), e.name(), e.x(), e.y(), e.z(), 0.0D, intensity,
                    StimulusCategory.CONTACT.basePriority() * intensity + (e.kind() == yadi.samuraiai.ai.perception.engine.EntityClass.HOSTILE ? 15.0D : 0.0D),
                    ctx.tick(), 10, "contact"));
        }
    }
}
