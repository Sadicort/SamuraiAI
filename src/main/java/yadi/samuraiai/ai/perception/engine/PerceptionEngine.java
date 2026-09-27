package yadi.samuraiai.ai.perception.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.ai.perception.attention.AttentionFocus;
import yadi.samuraiai.ai.perception.attention.AttentionLevel;
import yadi.samuraiai.ai.perception.attention.AttentionManager;
import yadi.samuraiai.ai.perception.awareness.AwarenessEngine;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.awareness.AwarenessMap;
import yadi.samuraiai.ai.perception.awareness.InterestEngine;
import yadi.samuraiai.ai.perception.awareness.SuspicionMeter;
import yadi.samuraiai.ai.perception.awareness.SuspicionSource;
import yadi.samuraiai.ai.perception.awareness.ThreatEngine;
import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.ai.perception.environment.BlockSensor;
import yadi.samuraiai.ai.perception.environment.EnvironmentSensor;
import yadi.samuraiai.ai.perception.environment.LightSensor;
import yadi.samuraiai.ai.perception.environment.WeatherSensor;
import yadi.samuraiai.ai.perception.events.*;
import yadi.samuraiai.ai.perception.filters.FilterContext;
import yadi.samuraiai.ai.perception.filters.StimulusPipeline;
import yadi.samuraiai.ai.perception.hearing.HeardSound;
import yadi.samuraiai.ai.perception.hearing.SoundLog;
import yadi.samuraiai.ai.perception.memory.MemoryEntry;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.metrics.PerceptionMetrics;
import yadi.samuraiai.ai.perception.sensors.ConversationSensor;
import yadi.samuraiai.ai.perception.sensors.DamageSensor;
import yadi.samuraiai.ai.perception.sensors.EntitySensor;
import yadi.samuraiai.ai.perception.sensors.HearingSensor;
import yadi.samuraiai.ai.perception.sensors.MovementSensor;
import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorRecord;
import yadi.samuraiai.ai.perception.sensors.SensorScheduler;
import yadi.samuraiai.ai.perception.sensors.VisionSensor;
import yadi.samuraiai.ai.perception.sensors.VoiceSensor;
import yadi.samuraiai.ai.perception.smell.SmellSensor;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.touch.TouchSensor;
import yadi.samuraiai.ai.perception.vision.RayBudget;
import yadi.samuraiai.ai.perception.vision.VisibilityState;
import yadi.samuraiai.ai.perception.vision.VisionEngine;
import yadi.samuraiai.ai.perception.vision.VisualTrack;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.logging.SamuraiLogger;

/**
 * One perception pass for one NPC: due sensors observe, the stimulus pipeline filters and prioritises, attention picks a
 * focus, memory remembers and fades, suspicion / interest / threat update, awareness is re-evaluated, and an immutable
 * {@link PerceptionSnapshot} plus the perception events come out. It observes and reports; it never moves the NPC, starts
 * behaviors or touches the Brain. The engine is stateless: all per-NPC state lives in {@link PerceptionState}.
 */
public final class PerceptionEngine {
    /** Output of a pass: the snapshot the brain may read and the events to publish. */
    public record PassResult(PerceptionSnapshot snapshot, List<NpcEvent> events, int sensorsRun, int stimuliAccepted) { }

    private final Supplier<PerceptionSettings> settings;
    private final PerceptionMetrics metrics;
    private final SensorScheduler scheduler = new SensorScheduler();
    private final StimulusPipeline pipeline = new StimulusPipeline();
    private final List<Sensor> sensors;

    public PerceptionEngine(Supplier<PerceptionSettings> settings, PerceptionMetrics metrics) {
        this(settings, metrics, List.of(new EntitySensor(), new VisionSensor(), new TouchSensor(), new MovementSensor(), new HearingSensor(), new DamageSensor(),
                new ConversationSensor(), new VoiceSensor(), new EnvironmentSensor(), new LightSensor(), new WeatherSensor(), new BlockSensor(), new SmellSensor()));
    }

    public PerceptionEngine(Supplier<PerceptionSettings> settings, PerceptionMetrics metrics, List<Sensor> sensors) {
        this.settings = settings; this.metrics = metrics; this.sensors = List.copyOf(sensors);
    }

    public PerceptionMetrics metrics() { return metrics; }
    public List<Sensor> sensors() { return sensors; }

    public PassResult process(Perceiver p, PerceptionWorld world, SoundLog sounds, PerceptionState st, long tick, int tierMultiplier, RayBudget rays) {
        long started = System.nanoTime();
        PerceptionSettings s = settings.get();
        long elapsed = st.lastPassTick < 0 ? 1L : Math.max(1L, tick - st.lastPassTick);
        List<NpcEvent> events = new ArrayList<>();
        PassOutput out = new PassOutput();
        SensorContext ctx = new SensorContext(p, world, tick, s, st, sounds, rays, out);
        int raysBefore = rays.consumed(), refusedBefore = rays.refused();

        for (Sensor sensor : sensors) {
            SensorRecord record = st.record(sensor.type());
            int interval = scheduler.interval(sensor.type(), s, tierMultiplier, st.awareness.level());
            if (!scheduler.due(record, tick, interval)) continue;
            ctx.interval(interval);
            long t0 = System.nanoTime();
            scheduler.begin(record);
            try {
                sensor.scan(ctx);
                long took = System.nanoTime() - t0;
                scheduler.end(record, tick, took);
                metrics.sensorScan(sensor.type(), took);
                out.sensorsRun++;
            } catch (RuntimeException error) {
                scheduler.fail(record, tick, error, s);
                metrics.sensorFailed();
                SamuraiLogger.PERCEPTION.warn("Sensor {} failed for npc {}: {}", sensor.type(), p.id(), error.toString());
            }
        }

        // hidden disappearance: a player that was in view close by and is now lost
        for (VisionEngine.Transition t : out.transitions) {
            if (t.lost() && t.kind() == EntityClass.PLAYER) {
                VisualTrack track = st.tracks.get(t.id());
                if (track != null && track.distance < s.visionFar() * 0.6D) out.suspect(SuspicionSource.PLAYER_VANISHED, 15.0D);
            }
        }

        // filter and prioritise
        var filtered = pipeline.process(out.stimuli, new FilterContext(p, tick, s, st.cooldowns, st.memory));
        List<Stimulus> accepted = filtered.accepted();
        metrics.stimuli(filtered.raw(), accepted.size(), filtered.rejected());
        st.stimuliAccepted += accepted.size();

        // suspicion
        for (PassOutput.SuspicionRequest request : out.suspicion) {
            if (st.suspicion.raise(request.source(), request.amount(), p.senses().suspicionGain(), s) == SuspicionMeter.Change.RAISED) {
                events.add(new SuspicionRaisedEvent(p.id(), st.suspicion.value(), request.source().name()));
                metrics.suspicionRaised();
            }
        }
        if (st.suspicion.decay(elapsed, s) == SuspicionMeter.Change.CLEARED) events.add(new SuspicionClearedEvent(p.id()));

        // attention, threat, interest
        AttentionManager.Update attention = st.attention.update(accepted, p, tick, s);
        ThreatEngine.Update threat = st.threat.update(accepted, p, elapsed, tick, s);
        InterestEngine.Update interest = st.interest.update(accepted, p, st.memory, elapsed, tick, s);
        for (var source : threat.newSources()) {
            events.add(new ThreatDetectedEvent(p.id(), source.key(), threat.level(), source.score(), source.category().name(), source.label(), source.x(), source.y(), source.z()));
            metrics.threat();
        }
        if (threat.newSources().isEmpty() && threat.escalated()) st.threat.sources().stream().findFirst().ifPresent(source -> {
            events.add(new ThreatDetectedEvent(p.id(), source.key(), threat.level(), source.score(), source.category().name(), source.label(), source.x(), source.y(), source.z()));
            metrics.threat();
        });
        for (var item : interest.detected()) {
            events.add(new InterestDetectedEvent(p.id(), item.key(), item.label(), item.score(), item.x(), item.y(), item.z()));
            metrics.curiosity();
            st.memory.remember(MemoryKind.INTEREST, "interest:" + item.key(), item.subject(), item.label(), item.x(), item.y(), item.z(), 0, 0, Math.min(1.0D, item.score() / 100.0D), tick, "INTEREST");
        }

        remember(p, st, out, accepted, tick);

        // vision events
        for (VisionEngine.Transition t : out.transitions) {
            if (t.detected()) { events.add(new VisionDetectedEvent(p.id(), t.id(), t.kind().name(), t.name(), t.x(), t.y(), t.z(), t.confidence(), t.to().name())); metrics.objectSeen(); }
            if (t.lost()) { events.add(new VisionLostEvent(p.id(), t.id(), t.kind().name(), t.name(), t.x(), t.y(), t.z())); metrics.visionLost(); }
        }
        for (HeardSound h : out.heard) {
            events.add(new SoundHeardEvent(p.id(), h.sound().category().name(), h.estimatedX(), h.estimatedY(), h.estimatedZ(), h.direction().name(), h.intensity(), h.distance(), h.uncertainty(), h.sound().source()));
            metrics.soundHeard();
        }
        if (!out.heard.isEmpty()) st.recentSounds = List.copyOf(out.heard);
        else if (!st.recentSounds.isEmpty() && tick - st.lastHeardTick > s.soundLogTicks()) st.recentSounds = List.of();

        // awareness
        AwarenessLevel previousAwareness = st.awareness.level();
        AwarenessEngine.Update awareness = st.awareness.update(awarenessInputs(p, st, attention, threat, accepted, tick, s), tick, s);
        if (awareness.changed()) {
            events.add(new AwarenessChangedEvent(p.id(), previousAwareness, awareness.level(), awareness.reason()));
            metrics.awarenessChanged();
        }

        int forgot = st.memory.fade(tick, s);
        if (forgot > 0) metrics.forgotten(forgot);
        metrics.rays(rays.consumed() - raysBefore, rays.refused() - refusedBefore);
        metrics.targets(out.targetsEvaluated);

        st.lastPassTick = tick;
        st.passes++;
        PerceptionSnapshot snapshot = snapshot(p, st, accepted.size(), tick, s);
        st.snapshot = snapshot;
        metrics.pass(System.nanoTime() - started);
        return new PassResult(snapshot, events, out.sensorsRun, accepted.size());
    }

    // ------------------------------------------------------------------ memory

    private void remember(Perceiver p, PerceptionState st, PassOutput out, List<Stimulus> accepted, long tick) {
        for (VisualTrack track : st.tracks.values()) {
            if (!track.state.seen() || track.lastSeenTick != tick) continue;
            double importance = Math.min(1.0D, (track.kind.category().basePriority() / 100.0D + 0.2D) * Math.max(0.3D, track.confidence));
            st.memory.remember(MemoryKind.VISUAL, "seen:" + track.id, track.id, track.name, track.x, track.y, track.z, track.vx, track.vz, importance, tick, track.kind.name());
            if (p.knows(track.id))
                st.memory.remember(MemoryKind.SOCIAL, "known:" + track.id, track.id, track.name, track.x, track.y, track.z, track.vx, track.vz, 0.5D + Math.abs(p.relationTo(track.id)) / 200.0D, tick, track.kind.name());
        }
        for (HeardSound h : out.heard) {
            String key = "sound:" + h.sound().category() + ":" + (h.sound().source() != null ? h.sound().source() : (int) Math.floor(h.estimatedX() / 4) + "," + (int) Math.floor(h.estimatedZ() / 4));
            MemoryEntry entry = st.memory.remember(MemoryKind.AUDITORY, key, h.sound().source(), h.sound().label(), h.estimatedX(), h.estimatedY(), h.estimatedZ(), 0, 0,
                    soundImportance(h), tick, "SOUND " + h.sound().category());
            entry.uncertainty = h.uncertainty();
        }
        for (var source : st.threat.sources()) {
            if (source.lastTick() != tick) continue;
            st.memory.remember(MemoryKind.DANGER, "threat:" + source.key(), source.source(), source.label(), source.x(), source.y(), source.z(), 0, 0,
                    Math.min(1.0D, source.score() / 100.0D), tick, source.category() == StimulusCategory.DAMAGE_TAKEN ? "DAMAGE" : "THREAT " + source.category());
        }
    }

    /**
     * How much a heard sound matters, remembered. Mostly what the sound <em>is</em> (an explosion matters even when it is heard
     * faintly from far away), a little how loud it registered.
     */
    private static double soundImportance(HeardSound h) {
        return Math.max(0.15D, Math.min(1.0D, 0.4D * h.intensity() + 0.6D * h.sound().category().stimulusCategory().basePriority() / 100.0D));
    }

    // ------------------------------------------------------------------ awareness inputs and investigation

    private AwarenessEngine.Inputs awarenessInputs(Perceiver p, PerceptionState st, AttentionManager.Update attention, ThreatEngine.Update threat,
                                                   List<Stimulus> accepted, long tick, PerceptionSettings s) {
        AttentionFocus focus = attention.focus();
        VisualTrack focusTrack = focus == null || focus.subject() == null ? null : st.tracks.get(focus.subject());
        boolean focusSeen = focus != null && !focus.lost() && focusTrack != null && focusTrack.state.seen();
        // A threat that is in plain sight counts as seen even if attention is momentarily elsewhere.
        if (!focusSeen) for (var source : st.threat.sources()) {
            VisualTrack t = source.source() == null ? null : st.tracks.get(source.source());
            if (t != null && t.state.seen()) { focusSeen = true; break; }
        }
        boolean lostTarget = false;
        for (VisualTrack track : st.tracks.values()) {
            boolean relevant = track.kind == EntityClass.PLAYER || track.kind == EntityClass.HOSTILE || p.knows(track.id);
            if (relevant && track.everSeen && !track.state.seen() && tick - track.lastSeenTick <= s.visualMemoryTicks() / 2) { lostTarget = true; break; }
        }
        if (!lostTarget && st.memory.strongest(MemoryKind.DANGER).map(e -> e.strength > 0.3D).orElse(false)) lostTarget = true;
        boolean heard = st.lastHeardTick >= 0 && tick - st.lastHeardTick <= 60;
        return new AwarenessEngine.Inputs(attention.level(), focusSeen, attention.focusLost() || (focus != null && focus.lost()), st.suspicion.value(), threat.level(),
                lostTarget, heard, !accepted.isEmpty() || attention.level().atLeast(AttentionLevel.LOW));
    }

    /**
     * Where there is a reason to go and look. Sound evidence lasts as long as the NPC remembers it (a far explosion is still
     * worth checking a few seconds later), lost targets and threats need the NPC to be alert, and something curious counts when
     * it is interesting enough. Reaching the spot with nothing there resolves it: the evidence is dropped and the NPC stops
     * being sent to the same empty place.
     */
    private InvestigationTarget investigation(Perceiver p, PerceptionState st, long tick, PerceptionSettings s) {
        AwarenessLevel awareness = st.awareness.level();
        InvestigationTarget best = null;
        double bestUrgency = 0.0D;
        AttentionFocus focus = st.attention.focus();
        if (awareness.atLeast(AwarenessLevel.ALERT)) {
            if (focus != null && focus.category().basePriority() >= 25 && focus.category() != StimulusCategory.WEATHER && focus.category() != StimulusCategory.LIGHT) {
                VisualTrack t = focus.subject() == null ? null : st.tracks.get(focus.subject());
                boolean unseen = t == null || !t.state.seen();
                double away = Math.sqrt((focus.x() - p.x()) * (focus.x() - p.x()) + (focus.z() - p.z()) * (focus.z() - p.z()));
                if (unseen && away <= 2.5D && focus.lost()) st.attention.reset();
                else if (unseen) { best = new InvestigationTarget(focus.x(), focus.y(), focus.z(), focus.lost() ? 3.0D : 1.5D, "last seen/heard: " + focus.label(), focus.score(), tick); bestUrgency = focus.score(); }
            }
            for (var threat : st.threat.sources()) {
                VisualTrack t = threat.source() == null ? null : st.tracks.get(threat.source());
                if ((t == null || !t.state.seen()) && threat.score() + 20.0D > bestUrgency) {
                    bestUrgency = threat.score() + 20.0D;
                    best = new InvestigationTarget(threat.x(), threat.y(), threat.z(), 2.0D, "threat: " + threat.label(), bestUrgency, tick);
                }
            }
        }
        for (MemoryEntry sound : new ArrayList<>(st.memory.entries(MemoryKind.AUDITORY))) {
            if (sound.detail.contains("FOOTSTEP") || sound.detail.contains("ANIMAL")) continue;
            if (sound.distanceTo(p.x(), p.y(), p.z()) <= sound.uncertainty + 2.5D) { st.memory.forget(MemoryKind.AUDITORY, sound.key); continue; }
            double urgency = sound.importance * 40.0D + sound.strength * 40.0D;
            if (sound.strength >= 0.12D && urgency > bestUrgency) { bestUrgency = urgency; best = new InvestigationTarget(sound.x, sound.y, sound.z, sound.uncertainty, "heard: " + sound.label, urgency, tick); }
        }
        var interest = st.interest.top();
        if (interest.isPresent() && interest.get().score() >= s.interestThreshold() && interest.get().score() > bestUrgency)
            best = new InvestigationTarget(interest.get().x(), interest.get().y(), interest.get().z(), 1.0D, "curious about " + interest.get().label(), interest.get().score(), tick);
        return best;
    }

    private PerceptionSnapshot snapshot(Perceiver p, PerceptionState st, int accepted, long tick, PerceptionSettings s) {
        var targets = new ArrayList<yadi.samuraiai.ai.perception.vision.VisualTarget>();
        for (VisualTrack track : st.tracks.values()) if (track.everSeen || track.state != VisibilityState.OBSTRUCTED) targets.add(track.view());
        targets.sort(java.util.Comparator.comparingDouble(yadi.samuraiai.ai.perception.vision.VisualTarget::distance));
        return new PerceptionSnapshot(p.id(), tick, st.awareness.level(), st.attention.level(), st.attention.focus(), targets, st.recentSounds, st.threat.level(), st.threat.score(),
                st.threat.sources(), st.suspicion.value(), st.suspicion.raised(), st.interest.items(), st.environment, AwarenessMap.from(st.memory, tick),
                investigation(p, st, tick, s), accepted);
    }
}
