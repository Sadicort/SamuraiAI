package yadi.samuraiai.living.world.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * The World Event Engine: schedules events and walks each through PREPARATION → START → DEVELOPMENT → END → CONSEQUENCES →
 * CLOSED. Only events whose next transition is due are touched (a priority queue). Every transition is handed to the
 * registered {@link Listener}s — the world itself applies region danger and road blockages; the hub turns the rest into
 * village, economy, quest and family consequences. Two open events of the same type never share a scope.
 */
public final class WorldEventEngine {
    /** Reacts to a phase transition. Listeners may add consequence descriptions to the record. */
    public interface Listener { void onPhase(WorldEventRecord event, WorldEventPhase phase, long now); }

    private final Map<UUID, WorldEventRecord> open = new LinkedHashMap<>();
    private final Deque<WorldEventRecord> archive = new ArrayDeque<>();
    private final PriorityQueue<WorldEventRecord> due = new PriorityQueue<>((a, b) -> Long.compare(a.nextTransition(), b.nextTransition()));
    private final List<Listener> listeners = new ArrayList<>();
    private int maxOpen = 64, archiveSize = 500, consequenceMinutes = 60;
    private long created, closed, refused;
    private boolean dirty;

    public void configure(int maxOpenEvents, int archiveMax, int consequenceDelayMinutes) {
        maxOpen = Math.max(1, maxOpenEvents); archiveSize = Math.max(0, archiveMax); consequenceMinutes = Math.max(1, consequenceDelayMinutes);
    }

    public void listen(Listener listener) { listeners.add(listener); }

    /** Schedules an event. Returns empty when an open event of the same type already covers the scope, or too many are open. */
    public Optional<WorldEventRecord> schedule(WorldEventType type, String title, UUID region, UUID settlement, double severity, long now, long startAt, long durationMinutes,
                                               Provenance cause, Set<String> tags) {
        for (WorldEventRecord e : open.values())
            if (e.type() == type && java.util.Objects.equals(e.region(), region) && java.util.Objects.equals(e.settlement(), settlement)) { refused++; return Optional.empty(); }
        if (open.size() >= maxOpen) { refused++; return Optional.empty(); }
        UUID id = UUID.randomUUID();
        long start = Math.max(now, startAt);
        WorldEventRecord e = new WorldEventRecord(id, type, title, region, settlement, severity, cause, now, start, start + Math.max(1, durationMinutes));
        if (tags != null) e.tags().addAll(tags);
        open.put(id, e);
        due.add(e);
        created++;
        dirty = true;
        fire(e, WorldEventPhase.PREPARATION, now);
        return Optional.of(e);
    }

    /** Advances every event whose transition is due. Returns how many transitions happened. */
    public int tick(long now) {
        int transitions = 0;
        while (!due.isEmpty() && due.peek().nextTransition() <= now) {
            WorldEventRecord e = due.poll();
            if (!open.containsKey(e.id())) continue;
            switch (e.phase()) {
                case PREPARATION -> { move(e, WorldEventPhase.START, Math.max(e.startAt(), Math.min(now, e.startAt()))); move(e, WorldEventPhase.DEVELOPMENT, now); transitions += 2; }
                case START, DEVELOPMENT -> {
                    if (e.resolution() == WorldEventRecord.Resolution.NONE) e.resolve(WorldEventRecord.Resolution.EXPIRED, "", "ran its course");
                    move(e, WorldEventPhase.END, now);
                    e.closeAt(now + consequenceMinutes);
                    move(e, WorldEventPhase.CONSEQUENCES, now);
                    transitions += 2;
                }
                case END -> { e.closeAt(now + consequenceMinutes); move(e, WorldEventPhase.CONSEQUENCES, now); transitions++; }
                case CONSEQUENCES -> { move(e, WorldEventPhase.CLOSED, now); archive(e); transitions++; continue; }
                case CLOSED -> { archive(e); continue; }
            }
            if (open.containsKey(e.id())) due.add(e);
        }
        return transitions;
    }

    private void move(WorldEventRecord e, WorldEventPhase next, long at) {
        e.phase(next, at);
        dirty = true;
        fire(e, next, at);
    }

    private void fire(WorldEventRecord e, WorldEventPhase phase, long now) {
        for (Listener l : listeners) {
            try { l.onPhase(e, phase, now); } catch (RuntimeException error) { e.consequences().add("listener failed: " + error); }
        }
    }

    private void archive(WorldEventRecord e) {
        open.remove(e.id());
        archive.addLast(e);
        while (archive.size() > archiveSize) archive.removeFirst();
        closed++;
        dirty = true;
    }

    /** Ends an event early with an outcome (a player defended the village, a quest cleared the bandits). */
    public boolean resolve(UUID id, boolean success, String by, String outcome, long now) {
        WorldEventRecord e = open.get(id);
        if (e == null || e.phase().ordinal() >= WorldEventPhase.END.ordinal()) return false;
        e.resolve(success ? WorldEventRecord.Resolution.RESOLVED : WorldEventRecord.Resolution.FAILED, by, outcome);
        due.remove(e);
        if (e.phase() == WorldEventPhase.PREPARATION) { e.startNow(now); move(e, WorldEventPhase.START, now); }
        e.endNow(now);
        move(e, WorldEventPhase.END, now);
        e.closeAt(now + consequenceMinutes);
        move(e, WorldEventPhase.CONSEQUENCES, now);
        due.add(e);
        return true;
    }

    /** Cancels an event that has not started (its cause went away). */
    public boolean cancel(UUID id, String why, long now) {
        WorldEventRecord e = open.get(id);
        if (e == null || e.phase() != WorldEventPhase.PREPARATION) return false;
        due.remove(e);
        e.resolve(WorldEventRecord.Resolution.CANCELLED, "", why);
        move(e, WorldEventPhase.CLOSED, now);
        archive(e);
        return true;
    }

    public Optional<WorldEventRecord> get(UUID id) {
        WorldEventRecord e = open.get(id);
        if (e != null) return Optional.of(e);
        for (WorldEventRecord a : archive) if (a.id().equals(id)) return Optional.of(a);
        return Optional.empty();
    }

    public Collection<WorldEventRecord> open() { return List.copyOf(open.values()); }
    public List<WorldEventRecord> running() { return open.values().stream().filter(e -> e.phase().running()).toList(); }
    public List<WorldEventRecord> openIn(UUID region) { return open.values().stream().filter(e -> region.equals(e.region())).toList(); }
    public List<WorldEventRecord> openAt(UUID settlement) { return open.values().stream().filter(e -> settlement.equals(e.settlement())).toList(); }
    public Collection<WorldEventRecord> archive() { return List.copyOf(archive); }
    public long created() { return created; }
    public long closed() { return closed; }
    public long refused() { return refused; }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }

    /** Restores an open event (persistence) without re-firing its phase. */
    public void restoreOpen(WorldEventRecord e) { open.put(e.id(), e); due.add(e); }
    public void restoreArchived(WorldEventRecord e) { archive.addLast(e); while (archive.size() > archiveSize) archive.removeFirst(); }
    public void clear() { open.clear(); archive.clear(); due.clear(); dirty = false; }
}
