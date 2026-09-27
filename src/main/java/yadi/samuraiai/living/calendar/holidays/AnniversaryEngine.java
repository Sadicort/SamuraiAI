package yadi.samuraiai.living.calendar.holidays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.living.core.CalendarDate;

/**
 * The anniversary registry. Records are indexed by month and day, so finding today's anniversaries costs one map lookup
 * whatever the number of records. An anniversary is announced from its first full year onwards.
 */
public final class AnniversaryEngine {
    public record Due(AnniversaryRecord record, int years) { }

    private final Map<UUID, AnniversaryRecord> records = new LinkedHashMap<>();
    private final Map<Integer, List<UUID>> byDay = new HashMap<>();
    private final int max;
    private boolean dirty;

    public AnniversaryEngine(int max) { this.max = Math.max(16, max); }

    private static int dayKey(int month, int day) { return month * 1000 + day; }

    /** Registers (or replaces, same id) an anniversary for the date of {@code origin}. Returns false when the registry is full. */
    public boolean register(AnniversaryRecord record) {
        if (!records.containsKey(record.id()) && records.size() >= max) return false;
        AnniversaryRecord old = records.put(record.id(), record);
        if (old != null) byDay.getOrDefault(dayKey(old.month(), old.day()), new ArrayList<>()).remove(old.id());
        byDay.computeIfAbsent(dayKey(record.month(), record.day()), k -> new ArrayList<>()).add(record.id());
        dirty = true;
        return true;
    }

    public static AnniversaryRecord create(String kind, String subject, String title, CalendarDate origin) {
        UUID id = UUID.nameUUIDFromBytes(("anniversary:" + kind + ":" + subject + ":" + origin.minute()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new AnniversaryRecord(id, kind, subject, title, origin.minute(), origin.month(), origin.day(), origin.year());
    }

    public boolean remove(UUID id) {
        AnniversaryRecord old = records.remove(id);
        if (old == null) return false;
        byDay.getOrDefault(dayKey(old.month(), old.day()), new ArrayList<>()).remove(id);
        dirty = true;
        return true;
    }

    public List<Due> due(CalendarDate today) {
        List<Due> out = new ArrayList<>();
        for (UUID id : byDay.getOrDefault(dayKey(today.month(), today.day()), List.of())) {
            AnniversaryRecord r = records.get(id);
            if (r != null && today.year() > r.originYear()) out.add(new Due(r, today.year() - r.originYear()));
        }
        return out;
    }

    public Optional<AnniversaryRecord> get(UUID id) { return Optional.ofNullable(records.get(id)); }
    public List<AnniversaryRecord> all() { return List.copyOf(records.values()); }
    public List<AnniversaryRecord> about(String subject) { return records.values().stream().filter(r -> r.subject().equals(subject)).toList(); }
    public int size() { return records.size(); }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { records.clear(); byDay.clear(); dirty = false; }
}
