package yadi.samuraiai.ai.perception.hearing;

import java.util.ArrayList;
import java.util.List;

/**
 * Recent sounds of one dimension, shared by every listener so a noise is recorded once, not once per NPC. Bounded by age
 * and by count. Server-thread only.
 */
public final class SoundLog {
    private static final int MAX_ENTRIES = 2048;
    private final List<SoundEvent> events = new ArrayList<>();
    private long total;

    public void add(SoundEvent event) {
        events.add(event);
        total++;
        if (events.size() > MAX_ENTRIES) events.subList(0, events.size() - MAX_ENTRIES).clear();
    }

    /** Sounds emitted after {@code tick} (exclusive), oldest first. */
    public List<SoundEvent> since(long tick) {
        List<SoundEvent> result = new ArrayList<>();
        for (SoundEvent event : events) if (event.tick() > tick) result.add(event);
        return result;
    }

    public int prune(long now, int ttlTicks) {
        int before = events.size();
        events.removeIf(event -> now - event.tick() > ttlTicks);
        return before - events.size();
    }

    /** Tick of the newest sound, or -1 when the log is empty: lets a listener skip a pass when nothing new was heard. */
    public long latestTick() { return events.isEmpty() ? -1L : events.get(events.size() - 1).tick(); }
    public int size() { return events.size(); }
    public long totalEmitted() { return total; }
    public void clear() { events.clear(); }
}
