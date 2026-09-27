package yadi.samuraiai.ai.perception.sensors;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.perception.awareness.SuspicionSource;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;
import yadi.samuraiai.ai.perception.vision.VisibilityState;
import yadi.samuraiai.ai.perception.vision.VisualTrack;

/**
 * Watches how things move: speed, direction, acceleration, distance and whether they head toward the NPC. Strange
 * movement (sudden acceleration, sprinting close, erratic turns) draws attention; movement glimpsed behind cover raises
 * suspicion, which is the "something is moving over there" reaction.
 */
public final class MovementSensor implements Sensor {
    /** Per entity: last vx, last vz, last distance, last tick. */
    private static final int VX = 0, VZ = 1, DIST = 2, TICK = 3;

    @Override public SensorType type() { return SensorType.MOVEMENT; }

    @Override public void scan(SensorContext ctx) {
        var state = ctx.state();
        var eye = ctx.perceiver();
        Set<UUID> present = new HashSet<>();
        for (SensedEntity e : ctx.catalog()) {
            if (e.id().equals(eye.entityId())) continue;
            present.add(e.id());
            double distance = e.distanceTo(eye.x(), eye.y(), eye.z());
            double[] last = state.movement.get(e.id());
            state.movement.put(e.id(), new double[]{e.vx(), e.vz(), distance, ctx.tick()});
            if (last == null || !e.kind().living()) continue;
            double speed = e.speed();
            double acceleration = Math.hypot(e.vx() - last[VX], e.vz() - last[VZ]);
            boolean approaching = distance < last[DIST] - 0.05D && speed > 0.1D;
            boolean erratic = acceleration > 0.14D && speed > 0.05D;
            VisualTrack track = state.tracks.get(e.id());
            boolean seen = track != null && track.state.seen();
            if (seen && ((approaching && distance < 14.0D && speed > 0.18D) || erratic || (e.sprinting() && distance < 10.0D))) {
                double intensity = Math.min(1.0D, 0.4D + speed * 2.0D + acceleration);
                ctx.out().accept(new Stimulus(StimulusType.VISUAL, e.kind().category(), e.id(),
                        e.name(), e.x(), e.y(), e.z(), 0.0D, intensity, e.kind().category().basePriority() * (0.6D + 0.4D * intensity) + 8.0D, ctx.tick(), 20,
                        "movement" + (approaching ? " approach" : "") + (erratic ? " erratic" : "")));
                if (erratic && distance < 12.0D) ctx.out().suspect(SuspicionSource.HIDDEN_MOVEMENT, 3.0D * Math.min(1.0D, acceleration * 4.0D));
            }
            // Movement glimpsed through cover: the NPC cannot see it properly but something is moving.
            if (track != null && !seen && track.state == VisibilityState.OBSTRUCTED && track.fraction >= 0.03D && speed > 0.08D && distance < ctx.settings().visionFar() * 0.6D) {
                ctx.out().suspect(SuspicionSource.HIDDEN_MOVEMENT, 4.0D * Math.min(1.0D, speed * 4.0D) * (0.5D + track.fraction * 2.0D));
            }
        }
        state.movement.keySet().removeIf(id -> !present.contains(id));
    }
}
