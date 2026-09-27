package yadi.samuraiai.ai.perception.sensors;

import yadi.samuraiai.ai.perception.awareness.SuspicionSource;
import yadi.samuraiai.ai.perception.engine.Reports;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/** Turns queued damage reports into critical stimuli: being hurt is the strongest evidence there is. */
public final class DamageSensor implements Sensor {
    @Override public SensorType type() { return SensorType.DAMAGE; }

    @Override public void scan(SensorContext ctx) {
        var state = ctx.state();
        Reports.DamageReport report;
        while ((report = state.pendingDamage.poll()) != null) {
            double intensity = Math.max(0.6D, Math.min(1.0D, report.amount() / 10.0D));
            double x = Double.isNaN(report.x()) ? ctx.perceiver().x() : report.x();
            double y = Double.isNaN(report.y()) ? ctx.perceiver().y() : report.y();
            double z = Double.isNaN(report.z()) ? ctx.perceiver().z() : report.z();
            ctx.out().accept(new Stimulus(StimulusType.DAMAGE, StimulusCategory.DAMAGE_TAKEN, report.source(), report.sourceName(), x, y, z, 0.0D, intensity,
                    StimulusCategory.DAMAGE_TAKEN.basePriority(), ctx.tick(), 200, "damage " + String.format("%.1f", report.amount())));
            ctx.out().suspect(SuspicionSource.THREAT, 40.0D);
            state.memory.remember(MemoryKind.DANGER, "damage:" + (report.source() == null ? "environment" : report.source()), report.source(),
                    report.sourceName(), x, y, z, 0, 0, 1.0D, ctx.tick(), "DAMAGE");
        }
    }
}
