package yadi.samuraiai.ai.scheduler.cooldown;

import java.util.HashMap;
import java.util.Map;

/** Per-NPC cooldowns keyed by name: a routine, once done, is not wanted again straight away. */
public final class CooldownEngine {
    private final Map<String, Long> until = new HashMap<>();

    public void start(String key, long now, long ticks) { if (ticks > 0) until.put(key, now + ticks); }
    public boolean ready(String key, long now) { Long t = until.get(key); return t == null || now >= t; }
    public long remaining(String key, long now) { Long t = until.get(key); return t == null ? 0 : Math.max(0, t - now); }
    public void clear(String key) { until.remove(key); }
    public void clearAll() { until.clear(); }
    public int size() { return until.size(); }

    /** Forgets cooldowns that have run out. */
    public int prune(long now) {
        int before = until.size();
        until.values().removeIf(t -> now >= t);
        return before - until.size();
    }

    public Map<String, Long> snapshot(long now) {
        Map<String, Long> copy = new HashMap<>();
        until.forEach((k, t) -> { if (t > now) copy.put(k, t - now); });
        return copy;
    }
}
