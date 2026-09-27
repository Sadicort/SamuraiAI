package yadi.samuraiai.living.economy.ledger;

import com.google.gson.JsonObject;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.storage.Json;
import yadi.samuraiai.living.core.Provenance;

/**
 * One recorded economic movement: ORIGIN → MOVEMENT → DESTINATION. Goods ({@code resource} and {@code quantity}) or money
 * ({@code resource = "coins"}) moved from one store or account to another for a reason, at a value, with a reference to what
 * caused it (a caravan, a contract, a quest, a world event) and the origin of the goods.
 */
public record MovementRecord(UUID id, long minute, Kind kind, String resource, double quantity, UUID from, UUID to, double value, String reference, Provenance origin, UUID settlement) {
    public enum Kind { PRODUCED, CONSUMED, SOLD, BOUGHT, TRANSPORTED, DELIVERED, TAXED, WAGE, SPOILED, DESTROYED, LOOTED, INHERITED, DONATED, MINTED, REWARD, TRANSFER }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString()); o.addProperty("m", minute); o.addProperty("k", kind.name()); o.addProperty("r", resource); o.addProperty("q", quantity);
        if (from != null) o.addProperty("from", from.toString()); if (to != null) o.addProperty("to", to.toString());
        o.addProperty("v", value); o.addProperty("ref", reference == null ? "" : reference); if (origin != null) o.add("origin", origin.toJson());
        if (settlement != null) o.addProperty("s", settlement.toString());
        return o;
    }

    public static MovementRecord fromJson(JsonObject o) {
        UUID id = Json.uuid(o, "id");
        if (id == null) return null;
        return new MovementRecord(id, Json.lng(o, "m", 0), Json.enumOf(o, "k", Kind.class, Kind.TRANSFER), Json.str(o, "r", ""), Json.num(o, "q", 0), Json.uuid(o, "from"), Json.uuid(o, "to"),
                Json.num(o, "v", 0), Json.str(o, "ref", ""), o.has("origin") ? Provenance.fromJson(Json.obj(o, "origin")) : null, Json.uuid(o, "s"));
    }
}
