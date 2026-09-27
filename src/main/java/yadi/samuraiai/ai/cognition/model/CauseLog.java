package yadi.samuraiai.ai.cognition.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A bounded, newest-last list of {@link Cause}s: the provenance that lets an engine answer "why is this value what it is?". */
public final class CauseLog {
    private final int capacity;
    private final Deque<Cause> causes = new ArrayDeque<>();

    public CauseLog(int capacity) { this.capacity = Math.max(1, capacity); }

    public void add(Cause cause) {
        if (cause == null) return;
        if (causes.size() >= capacity) causes.pollFirst();
        causes.addLast(cause);
    }

    public List<Cause> list() { return new ArrayList<>(causes); }
    public int size() { return causes.size(); }

    /** The most recent causes, newest first. */
    public List<Cause> recent(int limit) {
        List<Cause> all = list();
        List<Cause> result = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && result.size() < limit; i--) result.add(all.get(i));
        return result;
    }

    /** The causes that moved the value most, by absolute delta. */
    public List<Cause> strongest(int limit) {
        List<Cause> all = list();
        all.sort((a, b) -> Double.compare(Math.abs(b.delta()), Math.abs(a.delta())));
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }
}
