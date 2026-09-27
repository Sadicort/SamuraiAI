package yadi.samuraiai.ai.knowledge.propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/** Pending tellings ordered by arrival time. Bounded: when full the newest are dropped and counted. Duplicate (from, to, item) tellings are collapsed. */
public final class PropagationQueue {
    private final PriorityQueue<PropagationTask> queue = new PriorityQueue<>((a, b) -> Long.compare(a.dueAt(), b.dueAt()));
    private final Set<String> pending = new HashSet<>();
    private long dropped;

    private static String key(PropagationTask t) { return t.from() + ">" + t.to() + ":" + t.itemId(); }

    public boolean enqueue(PropagationTask task, int max) {
        if (queue.size() >= max) { dropped++; return false; }
        if (!pending.add(key(task))) return false;
        queue.add(task);
        return true;
    }

    /** The tellings due at {@code now}, at most {@code budget} of them. */
    public List<PropagationTask> drain(long now, int budget) {
        List<PropagationTask> due = new ArrayList<>();
        while (due.size() < budget && !queue.isEmpty() && queue.peek().dueAt() <= now) { PropagationTask t = queue.poll(); pending.remove(key(t)); due.add(t); }
        return due;
    }

    public int size() { return queue.size(); }
    public long dropped() { return dropped; }
    public void clear() { queue.clear(); pending.clear(); }
    public void removeInvolving(UUID npc) { queue.removeIf(t -> { boolean hit = t.from().equals(npc) || t.to().equals(npc); if (hit) pending.remove(key(t)); return hit; }); }
    public List<PropagationTask> snapshot() { return new ArrayList<>(queue); }
}
