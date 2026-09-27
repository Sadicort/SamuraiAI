package yadi.samuraiai.living.economy.inventory;

import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.Provenance;

/**
 * A lot of a resource: how much, of what quality and durability, and — always — where it came from ({@link #origin()}: which
 * deposit, farm, workshop, caravan or player produced it) and the settlement it was made in. Lots are split when part of one
 * is taken and merged with the previous lot when they share origin and day, so provenance survives every movement.
 */
public final class ResourceLot {
    private final UUID id;
    private final String resource;
    private double quantity;
    private final double quality, durability;
    private final Provenance origin;
    private final UUID originSettlement;
    private final long producedAt;

    public ResourceLot(UUID id, String resource, double quantity, double quality, double durability, Provenance origin, UUID originSettlement, long producedAt) {
        this.id = id; this.resource = resource; this.quantity = Math.max(0.0D, quantity);
        this.quality = Math.max(0.0D, Math.min(1.0D, quality)); this.durability = Math.max(0.0D, Math.min(1.0D, durability));
        this.origin = origin; this.originSettlement = originSettlement; this.producedAt = producedAt;
    }

    public static ResourceLot fresh(String resource, double quantity, double quality, Provenance origin, UUID originSettlement, long now) {
        return new ResourceLot(UUID.randomUUID(), resource, quantity, quality, 1.0D, origin, originSettlement, now);
    }

    public UUID id() { return id; }
    public String resource() { return resource; }
    public double quantity() { return quantity; }
    public double quality() { return quality; }
    public double durability() { return durability; }
    public Provenance origin() { return origin; }
    public UUID originSettlement() { return originSettlement; }
    public long producedAt() { return producedAt; }

    void add(double amount) { quantity += Math.max(0.0D, amount); }
    void remove(double amount) { quantity = Math.max(0.0D, quantity - Math.max(0.0D, amount)); }

    /** A new lot with part of this one's quantity (same origin and quality). */
    ResourceLot split(double amount) {
        double take = Math.min(quantity, Math.max(0.0D, amount));
        quantity -= take;
        return new ResourceLot(UUID.randomUUID(), resource, take, quality, durability, origin, originSettlement, producedAt);
    }

    boolean mergeable(ResourceLot other, int minutesPerDay) {
        return resource.equals(other.resource) && origin.kind().equals(other.origin.kind()) && origin.id().equals(other.origin.id())
                && java.util.Objects.equals(originSettlement, other.originSettlement) && Math.floorDiv(producedAt, (long) minutesPerDay) == Math.floorDiv(other.producedAt, (long) minutesPerDay)
                && Math.abs(quality - other.quality) < 0.05D;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString()); o.addProperty("r", resource); o.addProperty("q", quantity); o.addProperty("quality", quality); o.addProperty("dur", durability);
        o.add("origin", origin.toJson()); if (originSettlement != null) o.addProperty("from", originSettlement.toString()); o.addProperty("at", producedAt);
        return o;
    }

    public static ResourceLot fromJson(JsonObject o) {
        UUID id = Json.uuid(o, "id");
        if (id == null) return null;
        return new ResourceLot(id, Json.str(o, "r", ""), Json.num(o, "q", 0), Json.num(o, "quality", 0.5), Json.num(o, "dur", 1), Provenance.fromJson(Json.obj(o, "origin")), Json.uuid(o, "from"), Json.lng(o, "at", 0));
    }
}
