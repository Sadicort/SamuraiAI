package yadi.samuraiai.ai.navigation.debug;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.navigation.engine.NavigationEngine;
import yadi.samuraiai.ai.navigation.engine.NavigationSession;
import yadi.samuraiai.ai.navigation.graph.NavNode;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.movement.MovementBody;
import yadi.samuraiai.ai.navigation.prediction.PathProgressPredictor;
import yadi.samuraiai.ai.navigation.terrain.HeightAnalyzer;

/** Explains one navigation session in plain lines: where it is, where it goes, why it does what it does. */
public final class NavigationInspector {
    private NavigationInspector() { }

    public static List<String> inspect(NavigationSession s, NavigationEngine engine, MovementBody body) {
        List<String> lines = new ArrayList<>();
        lines.add("state=" + s.state() + " behavior=" + s.request().behavior() + " goal=" + s.request().goalName());
        lines.add("destination=" + s.destination() + " requested=" + s.request().goal() + " mode=" + s.request().prefs().mode());
        if (s.failure() != yadi.samuraiai.ai.navigation.engine.NavigationFailure.NONE)
            lines.add("failure=" + s.failure() + " (" + s.failureDetail() + ")");
        lines.add("last=" + s.lastEvent());
        var path = s.path();
        if (path != null) {
            NavPos target = path.get(Math.min(s.index(), path.size() - 1)).pos();
            var heights = HeightAnalyzer.analyze(path);
            lines.add("path nodes=" + path.size() + " current=" + s.index() + " target=" + target + " cost=" + String.format("%.1f", path.cost())
                    + " length=" + String.format("%.1f", path.length()) + (path.partial() ? " PARTIAL" : "") + (s.cacheHit() ? " cached" : ""));
            lines.add("height ascent=" + heights.totalAscent() + " descent=" + heights.totalDescent() + " jumps=" + heights.jumps()
                    + " steps=" + heights.steps() + " ladders=" + heights.ladders() + " drops=" + heights.drops());
            if (body != null) {
                NavPos at = body.block();
                NavNode node = engine.graph().node(at);
                lines.add("at=" + at + " chunk=" + at.chunkX() + "," + at.chunkZ() + " terrain=" + node.terrain() + " floor=" + node.floor()
                        + " danger=" + String.format("%.0f", node.staticDanger()));
                lines.add("eta=" + PathProgressPredictor.estimateTicks(path, s.index(), body.x(), body.z(), 0.215D) + " ticks");
            }
        } else {
            lines.add("no path yet" + (s.search() != null ? " (searching, expanded=" + s.search().expanded() + ")" : ""));
        }
        lines.add("recalculations=" + s.recalculations() + " recoveries=" + s.recoveryAttempts() + " blocked=" + s.blockedNodes().size()
                + (s.holding() ? " HOLDING" : "") + (s.blockReason().isEmpty() ? "" : " block=" + s.blockReason()));
        lines.add("metrics " + s.metrics());
        return lines;
    }

    public static List<String> summary(NavigationEngine engine) {
        var m = engine.metrics().snapshot();
        var c = engine.cache().stats();
        return List.of(
                "sessions active=" + m.activeSessions() + " requests=" + m.requests() + " completed=" + m.completed() + " failed=" + m.failed() + " cancelled=" + m.cancelled(),
                "recalculations=" + m.recalculations() + " blocks=" + m.blocks() + " stuck=" + m.stuck() + " recoveries=" + m.recoveries() + " doors=" + m.doorsOpened() + "/" + m.doorsClosed() + " jumps=" + m.jumps(),
                "search nodes=" + m.nodesExpanded() + " time=" + String.format("%.1fms", m.searchMillis()) + " movement=" + String.format("%.1fms", m.movementMillis())
                        + " tick avg=" + String.format("%.0fus", m.averageTickMicros()) + " max=" + String.format("%.0fus", m.maxTickMicros()) + " budgetHits=" + m.budgetExhausted(),
                "path cache size=" + c.size() + "/" + c.capacity() + " hitRate=" + String.format("%.0f%%", c.hitRate() * 100) + " invalidated=" + c.invalidated(),
                "graph nodes=" + engine.graph().cachedNodes() + " hits=" + engine.graph().cacheHits() + " misses=" + engine.graph().cacheMisses(),
                "failures=" + m.failuresByReason());
    }
}
