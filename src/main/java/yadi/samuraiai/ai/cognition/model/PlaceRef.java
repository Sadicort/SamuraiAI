package yadi.samuraiai.ai.cognition.model;

/** Where something happened: dimension, position and (when the scheduler knows one) the zone. Immutable. */
public record PlaceRef(String dimension, double x, double y, double z, String zone) {
    private static final PlaceRef UNKNOWN = new PlaceRef("", 0, 0, 0, "");

    public PlaceRef {
        dimension = dimension == null ? "" : dimension;
        zone = zone == null ? "" : zone;
    }

    public static PlaceRef unknown() { return UNKNOWN; }
    public static PlaceRef at(String dimension, double x, double y, double z) { return new PlaceRef(dimension, x, y, z, ""); }
    public boolean known() { return !dimension.isEmpty(); }
    public PlaceRef inZone(String zoneId) { return new PlaceRef(dimension, x, y, z, zoneId); }

    /** Distance in blocks, or infinity when the places are in different dimensions or one is unknown. */
    public double distance(PlaceRef other) {
        if (!known() || !other.known() || !dimension.equals(other.dimension)) return Double.POSITIVE_INFINITY;
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** The grid cell this place falls in, used as an index key. */
    public String cell(int size) {
        if (!known()) return "";
        int s = Math.max(1, size);
        return dimension + "|" + Math.floorDiv((int) Math.floor(x), s) + "|" + Math.floorDiv((int) Math.floor(z), s);
    }
}
