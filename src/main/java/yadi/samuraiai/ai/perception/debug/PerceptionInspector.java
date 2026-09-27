package yadi.samuraiai.ai.perception.debug;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.metrics.PerceptionMetrics;
import yadi.samuraiai.ai.perception.sensors.SensorRecord;

/** Explains one NPC's perception in plain lines: what it sees and hears, what worries it, what it remembers and how healthy each sensor is. */
public final class PerceptionInspector {
    private PerceptionInspector() { }

    public static List<String> inspect(PerceptionSnapshot s, PerceptionState st) {
        List<String> lines = new ArrayList<>();
        lines.add("awareness=" + s.awareness() + " attention=" + s.attention() + " threat=" + s.threatLevel() + " (" + (int) s.threatScore() + ") suspicion="
                + (int) s.suspicion() + (s.suspicious() ? " RAISED" : ""));
        if (s.focus() != null) {
            var f = s.focus();
            lines.add("focus=" + f.label() + " [" + f.category() + "/" + f.type() + "] score=" + (int) f.score() + " held=" + f.heldTicks(s.tick()) + "t"
                    + (f.lost() ? " LOST" : "") + " because " + f.reasons());
        } else lines.add("focus=none");
        for (var t : s.targets().stream().limit(6).toList())
            lines.add("  target " + t.name() + " " + t.kind() + " " + t.state() + " conf=" + String.format("%.2f", t.confidence()) + " dist=" + String.format("%.1f", t.distance())
                    + " observed=" + t.observedTicks() + "t");
        for (var h : s.recentSounds().stream().limit(4).toList())
            lines.add("  heard " + h.sound().category() + " from " + h.direction() + " intensity=" + String.format("%.2f", h.intensity()) + " est=" + fmt(h.estimatedX(), h.estimatedY(), h.estimatedZ())
                    + " +/-" + String.format("%.1f", h.uncertainty()));
        for (var th : s.threats().stream().limit(4).toList())
            lines.add("  threat " + th.label() + " [" + th.category() + "] " + (int) th.score() + " at " + fmt(th.x(), th.y(), th.z()));
        if (!s.interests().isEmpty()) lines.add("interest=" + s.interests().get(0).label() + " " + (int) s.interests().get(0).score());
        s.investigationTarget().ifPresent(i -> lines.add("investigate " + fmt(i.x(), i.y(), i.z()) + " +/-" + String.format("%.1f", i.uncertainty()) + " (" + i.reason() + ") urgency=" + (int) i.urgency()));
        lines.add("environment biome=" + s.environment().biome() + " weather=" + s.environment().weather() + " light=" + s.environment().lightLevel()
                + (s.environment().night() ? " night" : "") + (s.environment().cave() ? " cave" : ""));
        StringBuilder memory = new StringBuilder("memory");
        for (MemoryKind kind : MemoryKind.values()) memory.append(' ').append(kind).append('=').append(st.memory.size(kind));
        lines.add(memory.toString() + " map=" + s.map().total());
        StringBuilder sensors = new StringBuilder("sensors");
        for (SensorRecord r : st.sensors.values()) if (r.state != yadi.samuraiai.ai.perception.sensors.SensorState.CREATED)
            sensors.append(' ').append(r.type).append(':').append(r.state).append('/').append(r.scans).append(r.failures > 0 ? "!" + r.failures : "");
        lines.add(sensors.toString());
        return lines;
    }

    public static List<String> summary(PerceptionMetrics.Snapshot m) {
        return List.of(
                "npcs=" + m.trackedNpcs() + " passes=" + m.passes() + " avg=" + String.format("%.0fus", m.averagePassMicros()) + " max=" + String.format("%.0fus", m.maxPassMicros())
                        + " tick avg=" + String.format("%.0fus", m.averageTickMicros()) + " max=" + String.format("%.0fus", m.maxTickMicros()) + " deferred=" + m.npcsDeferred(),
                "raycasts=" + m.raycasts() + " refused=" + m.raysRefused() + " targets=" + m.targetsEvaluated() + " seen=" + m.objectsSeen() + " lost=" + m.visionLosses() + " sounds=" + m.soundsHeard(),
                "stimuli raw=" + m.stimuliRaw() + " accepted=" + m.stimuliAccepted() + " rejected=" + m.stimuliRejected() + " threats=" + m.threats() + " curiosity=" + m.curiosity()
                        + " suspicion=" + m.suspicionRaised() + " awarenessChanges=" + m.awarenessChanges() + " memoryForgotten=" + m.memoryForgotten() + " sensorFailures=" + m.sensorFailures(),
                "scans " + m.sensorScans());
    }

    private static String fmt(double x, double y, double z) { return String.format("%.1f,%.1f,%.1f", x, y, z); }
}
