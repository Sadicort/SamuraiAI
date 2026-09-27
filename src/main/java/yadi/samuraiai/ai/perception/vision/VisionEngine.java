package yadi.samuraiai.ai.perception.vision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionWorld;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusSink;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Turns "there is an entity near me" into "I can see it, partly see it, cannot see it, or lost it". Pipeline per
 * target: field-of-view zone, then several rays from the eyes to points of the body, then a confidence that folds in
 * distance, light, movement and stance. Detection is never by distance alone.
 */
public final class VisionEngine {
    /** Body heights (fraction of the target height) sampled by the rays, in the order they are used. */
    private static final double[] SAMPLE_HEIGHTS = {0.92D, 0.5D, 0.08D, 0.72D, 0.3D, 0.6D, 0.15D, 0.85D, 0.4D};
    private static final double MIN_VISIBLE_FRACTION = 0.6D, BLOCKED_FRACTION = 0.2D, GLIMPSE_FRACTION = 0.03D;

    public record Transition(UUID id, EntityClass kind, String name, VisibilityState from, VisibilityState to, double x, double y, double z,
                             double confidence) {
        public boolean detected() { return to.seen() && !from.seen(); }
        public boolean lost() { return to == VisibilityState.LOST; }
    }

    public record ScanResult(List<Transition> transitions, int targetsEvaluated, int raysUsed, int deferred) { }

    public ScanResult scan(Perceiver eye, PerceptionWorld world, PerceptionSettings s, long tick, Collection<SensedEntity> entities,
                           Map<UUID, VisualTrack> tracks, RayBudget budget, StimulusSink sink, int scanIntervalTicks) {
        List<Transition> transitions = new ArrayList<>();
        Set<UUID> updated = new HashSet<>();
        int evaluated = 0, deferred = 0, raysBefore = budget.consumed();
        for (SensedEntity entity : entities) {
            if (entity.id().equals(eye.entityId())) continue;
            if (entity.invisible() && entity.distanceTo(eye.x(), eye.y(), eye.z()) > s.visionNear()) continue;
            VisionCone.Sample cone = VisionCone.evaluate(eye, entity.x(), entity.centerY(), entity.z(), s);
            if (!cone.inRange()) continue;
            int rays = Math.min(s.raysPerTarget(), SAMPLE_HEIGHTS.length);
            if (!budget.tryConsume(rays)) { deferred++; updated.add(entity.id()); continue; }
            evaluated++;
            double fraction = 0.0D;
            for (int i = 0; i < rays; i++)
                fraction += Raycaster.transmittance(world, eye.x(), eye.eyeY(), eye.z(), entity.x(), entity.y() + entity.height() * SAMPLE_HEIGHTS[i], entity.z());
            fraction /= rays;
            double falloff = cone.distance() <= s.visionNear() ? 1.0D : Math.max(0.0D, 1.0D - Math.pow(cone.distance() / cone.effectiveRange(), 2));
            int light = world.lightLevel((int) Math.floor(entity.x()), (int) Math.floor(entity.centerY()), (int) Math.floor(entity.z()));
            double lightFactor = 0.35D + 0.65D * Math.max(0, Math.min(15, light)) / 15.0D;
            double motion = 1.0D + Math.min(0.5D, entity.speed() * 2.0D);
            double stance = entity.sneaking() ? 0.7D : 1.0D;
            double confidence = Math.max(0.0D, Math.min(1.0D, cone.sensitivity() * fraction * falloff * lightFactor * motion * stance));
            VisualTrack track = tracks.computeIfAbsent(entity.id(), id -> new VisualTrack(id, entity.kind(), entity.name()));
            track.fraction = fraction;
            updated.add(entity.id());
            VisibilityState next;
            if (fraction < BLOCKED_FRACTION) next = VisibilityState.OBSTRUCTED;
            else if (confidence < s.minConfidence()) next = track.state.seen() ? VisibilityState.OBSTRUCTED : track.state;
            else next = confidence >= s.visibleConfidence() && fraction >= MIN_VISIBLE_FRACTION ? VisibilityState.VISIBLE : VisibilityState.PARTIAL;
            if (next.seen()) {
                if (track.firstSeenTick < 0 || !track.state.seen()) track.firstSeenTick = tick;
                track.observedTicks += track.lastUpdateTick < 0 ? 1 : Math.max(1, tick - track.lastUpdateTick);
                track.x = entity.x(); track.y = entity.y(); track.z = entity.z();
                track.vx = entity.vx(); track.vy = entity.vy(); track.vz = entity.vz();
                track.confidence = confidence; track.distance = cone.distance();
                track.lastSeenTick = tick; track.everSeen = true; track.kind = entity.kind(); track.name = entity.name();
                sink.accept(Stimulus.at(StimulusType.VISUAL, entity.kind().category(), entity.id(), entity.name(), entity.x(), entity.y(), entity.z(),
                        confidence, tick, Math.max(10, scanIntervalTicks * 2 + 2)).withDetail(next.name() + " " + cone.zone()));
            }
            track.lastUpdateTick = tick;
            if (next != track.state) {
                transitions.add(new Transition(track.id, track.kind, track.name, track.state, next, track.x, track.y, track.z, confidence));
                track.state = next;
            }
        }
        // Everything not seen this scan: remembered, then lost, then only memory.
        for (Iterator<Map.Entry<UUID, VisualTrack>> it = tracks.entrySet().iterator(); it.hasNext(); ) {
            VisualTrack track = it.next().getValue();
            if (updated.contains(track.id)) continue;
            if (track.state.seen()) {
                transitions.add(new Transition(track.id, track.kind, track.name, track.state, VisibilityState.OBSTRUCTED, track.x, track.y, track.z, track.confidence));
                track.state = VisibilityState.OBSTRUCTED;
            }
            if (track.state == VisibilityState.OBSTRUCTED && track.everSeen && tick - track.lastSeenTick >= s.lostAfterTicks()) {
                transitions.add(new Transition(track.id, track.kind, track.name, VisibilityState.OBSTRUCTED, VisibilityState.LOST, track.x, track.y, track.z, track.confidence));
                track.state = VisibilityState.LOST;
            } else if (track.state == VisibilityState.LOST) track.state = VisibilityState.MEMORY_ONLY;
            if (!track.everSeen && tick - track.lastUpdateTick > s.lostAfterTicks()) it.remove();
            else if (track.everSeen && tick - track.lastSeenTick > s.visualMemoryTicks()) it.remove();
        }
        return new ScanResult(transitions, evaluated, budget.consumed() - raysBefore, deferred);
    }

    /** Was this target seen within the grace period? Used to tell "it just left my sight" from "never saw it". */
    public static boolean recentlySeen(VisualTrack track, long tick, int graceTicks) {
        return track.everSeen && tick - track.lastSeenTick <= graceTicks;
    }
}
