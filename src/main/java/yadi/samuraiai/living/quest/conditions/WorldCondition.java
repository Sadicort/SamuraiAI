package yadi.samuraiai.living.quest.conditions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * A condition of the world that may deserve a quest: its kind, a stable key (the same problem reported twice has the same
 * key, so it never makes two quests), where (settlement, region), what it is about ({@code subject}: a resource, a person, a
 * road), how serious (0..1), why ({@link #cause()}) and the variables templates may use ({@code resource}, {@code quantity},
 * {@code eventId}, {@code x}, {@code z}...).
 */
public record WorldCondition(ConditionKind kind, String key, UUID settlement, UUID region, String subject, double severity, Provenance cause, Map<String, String> variables) {
    public WorldCondition {
        key = key == null ? kind.name() : key;
        subject = subject == null ? "" : subject;
        severity = Double.isFinite(severity) ? Math.max(0.0D, Math.min(1.0D, severity)) : 0.5D;
        variables = variables == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(variables));
    }

    public String var(String name, String fallback) { return variables.getOrDefault(name, fallback); }
}
