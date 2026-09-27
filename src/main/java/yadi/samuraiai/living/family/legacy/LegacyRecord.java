package yadi.samuraiai.living.family.legacy;

import java.util.List;
import java.util.UUID;

/**
 * What a person leaves behind, as causes — never just a number. Each cause names its kind (family, trade, knowledge taught,
 * disciples, reputation, honour, historic events, properties, heirlooms, the community), how much it weighs and what exactly
 * it was. The total is only a summary of the causes.
 */
public record LegacyRecord(UUID person, long computedAt, List<Cause> causes) {
    public enum Kind { FAMILY_IMPACT, PROFESSION_IMPACT, TAUGHT_KNOWLEDGE, DISCIPLES, REPUTATION, HONOR, HISTORIC_EVENT, PROPERTY, HEIRLOOM, COMMUNITY_IMPACT }
    public record Cause(Kind kind, double weight, String text) { }

    public LegacyRecord { causes = List.copyOf(causes); }

    public double total() { double t = 0; for (Cause c : causes) t += c.weight(); return t; }
}
