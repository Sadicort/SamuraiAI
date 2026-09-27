package yadi.samuraiai.ai.cognition.trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * A bounded log of {@link TraceStep}s that makes "world event -> perception -> experience -> memory -> emotion -> relationship
 * -> knowledge -> behavior" followable after the fact. Recording is cheap (one small record per hop) and the log is capped.
 */
public final class CognitiveTrace {
    private final int capacity;
    private final Deque<TraceStep> steps = new ArrayDeque<>();

    public CognitiveTrace(int capacity) { this.capacity = Math.max(16, capacity); }

    public synchronized void record(UUID traceId, UUID npcId, TraceStage stage, String detail, long at) {
        if (traceId == null) return;
        if (steps.size() >= capacity) steps.pollFirst();
        steps.addLast(new TraceStep(traceId, npcId, stage, detail == null ? "" : detail, at));
    }

    public synchronized List<TraceStep> steps(UUID traceId) {
        List<TraceStep> result = new ArrayList<>();
        for (TraceStep s : steps) if (s.traceId().equals(traceId)) result.add(s);
        return result;
    }

    /** The newest steps that concern an NPC, newest last. */
    public synchronized List<TraceStep> recent(UUID npcId, int limit) {
        List<TraceStep> result = new ArrayList<>();
        var it = steps.descendingIterator();
        while (it.hasNext() && result.size() < limit) { TraceStep s = it.next(); if (npcId == null || npcId.equals(s.npcId())) result.add(0, s); }
        return result;
    }

    public synchronized int size() { return steps.size(); }
    public synchronized void clear() { steps.clear(); }
}
