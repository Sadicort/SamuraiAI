package yadi.samuraiai.ai.relationship.reputation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.relationship.model.ReputationRecord;
import yadi.samuraiai.ai.relationship.model.ReputationScope;

/** What one NPC believes about other people's standing, per scope. */
public final class ReputationBook {
    private final Map<String, ReputationRecord> records = new LinkedHashMap<>();

    public static String key(UUID subject, ReputationScope scope, String scopeId) { return subject + "|" + scope + "|" + (scopeId == null ? "" : scopeId); }

    public ReputationRecord get(UUID subject, ReputationScope scope, String scopeId) { return records.get(key(subject, scope, scopeId)); }
    public ReputationRecord getOrCreate(yadi.samuraiai.ai.cognition.model.EntityRef subject, ReputationScope scope, String scopeId) {
        return records.computeIfAbsent(key(subject.id(), scope, scopeId), k -> new ReputationRecord(subject, scope, scopeId));
    }
    public void put(ReputationRecord record) { records.put(key(record.subject().id(), record.scope(), record.scopeId()), record); }

    public List<ReputationRecord> about(UUID subject) {
        List<ReputationRecord> result = new ArrayList<>();
        for (ReputationRecord r : records.values()) if (r.subject().id().equals(subject)) result.add(r);
        return result;
    }

    public Collection<ReputationRecord> all() { return List.copyOf(records.values()); }
    public int size() { return records.size(); }
    public void remove(String key) { records.remove(key); }
}
