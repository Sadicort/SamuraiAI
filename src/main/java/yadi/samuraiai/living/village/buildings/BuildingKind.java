package yadi.samuraiai.living.village.buildings;

import java.util.Locale;
import java.util.Optional;
import yadi.samuraiai.living.village.districts.DistrictKind;

/**
 * Kinds of building a village has. Each belongs to a district, maps to the Behavior Scheduler's zone kind (so routines find
 * it), has a profession location name (so professions know where they work) and a default capacity (beds for a house,
 * workplaces for a workshop). STABLE is prepared for later.
 */
public enum BuildingKind {
    HOUSE(DistrictKind.RESIDENTIAL, "HOME", "HOUSE", 4),
    PLAZA(DistrictKind.MARKET, "PLAZA", "PLAZA", 40),
    MARKET(DistrictKind.MARKET, "MARKET", "MARKET", 12),
    TEMPLE(DistrictKind.SPIRITUAL, "TEMPLE", "TEMPLE", 20),
    SMITHY(DistrictKind.CRAFTS, "WORK", "SMITHY", 3),
    CARPENTRY(DistrictKind.CRAFTS, "WORK", "CARPENTRY", 3),
    WORKSHOP(DistrictKind.CRAFTS, "WORK", "WORKSHOP", 4),
    WAREHOUSE(DistrictKind.MARKET, "WORK", "WAREHOUSE", 4),
    KITCHEN(DistrictKind.RESIDENTIAL, "DINING", "KITCHEN", 12),
    DOJO(DistrictKind.MILITARY, "TRAINING", "DOJO", 16),
    GUARD_POST(DistrictKind.MILITARY, "GUARD_POST", "GUARD_POST", 6),
    GATE(DistrictKind.MILITARY, "GUARD_POST", "GATE", 4),
    FARM(DistrictKind.AGRICULTURAL, "WORK", "FARM", 6),
    WELL(DistrictKind.RESIDENTIAL, "PLAZA", "WELL", 6),
    DOCK(DistrictKind.AGRICULTURAL, "WORK", "DOCK", 6),
    MINE(DistrictKind.OUTER_FOREST, "WORK", "MINE", 8),
    LUMBER_CAMP(DistrictKind.OUTER_FOREST, "WORK", "OUTSKIRTS", 6),
    STABLE(DistrictKind.AGRICULTURAL, "WORK", "STABLE", 6);

    private final DistrictKind district;
    private final String zoneKind, location;
    private final int capacity;

    BuildingKind(DistrictKind district, String zoneKind, String location, int capacity) {
        this.district = district; this.zoneKind = zoneKind; this.location = location; this.capacity = capacity;
    }

    public DistrictKind district() { return district; }
    /** The scheduler's zone kind for this building (a name of {@code ZoneKind}). */
    public String zoneKind() { return zoneKind; }
    /** The profession location name professions list (SMITHY, FARM, TEMPLE...). */
    public String location() { return location; }
    public int defaultCapacity() { return capacity; }

    public static Optional<BuildingKind> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    /** The building kind a scheduler zone kind most likely is. */
    public static BuildingKind fromZoneKind(String zoneKind) {
        return switch (zoneKind == null ? "" : zoneKind.toUpperCase(Locale.ROOT)) {
            case "HOME" -> HOUSE; case "MARKET" -> MARKET; case "TEMPLE" -> TEMPLE; case "TRAINING" -> DOJO; case "GUARD_POST" -> GUARD_POST;
            case "DINING" -> KITCHEN; case "PLAZA" -> PLAZA; case "PATROL_ROUTE" -> GATE; case "REST_AREA" -> PLAZA; default -> WORKSHOP;
        };
    }
}
