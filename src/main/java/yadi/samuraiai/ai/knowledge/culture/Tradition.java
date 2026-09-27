package yadi.samuraiai.ai.knowledge.culture;

import java.util.Locale;

/** A cultural practice: what it is, when (a day period, or ANY) and where (a zone kind, or ANY) it is observed, and how strongly the culture holds to it (0-1). */
public record Tradition(String id, String name, Kind kind, String period, String zoneKind, double strength) {
    public enum Kind { CEREMONY, GREETING, SCHEDULE, RESTRICTION, RITUAL;
        public static Kind parse(String s) { try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (RuntimeException e) { return CEREMONY; } } }

    public Tradition {
        id = id == null ? "" : id;
        name = name == null ? id : name;
        period = period == null || period.isBlank() ? "ANY" : period.trim().toUpperCase(Locale.ROOT);
        zoneKind = zoneKind == null || zoneKind.isBlank() ? "ANY" : zoneKind.trim().toUpperCase(Locale.ROOT);
        strength = Double.isFinite(strength) ? Math.max(0.0D, Math.min(1.0D, strength)) : 0.5D;
    }

    public boolean matches(String periodName, String zoneKindName) {
        return (period.equals("ANY") || period.equalsIgnoreCase(periodName)) && (zoneKind.equals("ANY") || zoneKind.equalsIgnoreCase(zoneKindName));
    }
}
