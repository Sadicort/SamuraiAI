package yadi.samuraiai.living.economy.contracts;

import java.util.UUID;

/**
 * A trade agreement: purchase, sale, delivery, protection or transport of a quantity of a resource at a price by a deadline,
 * between an issuer (a settlement or merchant account) and a counterparty (a merchant, an NPC, a player). A contract may need
 * a minimum of trust to be offered, and may be carried out through a quest (the Quest Engine owns the quest; the contract
 * only knows its id). Delivered quantity accumulates until the contract is fulfilled.
 */
public final class Contract {
    public enum Kind { PURCHASE, SALE, DELIVERY, PROTECTION, TRANSPORT }
    public enum State { OPEN, ACCEPTED, FULFILLED, FAILED, EXPIRED, CANCELLED }

    private final UUID id, issuer, settlement;
    private final Kind kind;
    private final String resource, reason;
    private final double quantity, unitPrice, trustRequired;
    private final long createdAt, deadline;
    private UUID counterparty, quest;
    private State state = State.OPEN;
    private double delivered;
    private long resolvedAt;

    public Contract(UUID id, Kind kind, UUID issuer, UUID settlement, String resource, double quantity, double unitPrice, long createdAt, long deadline, double trustRequired, String reason) {
        this.id = id; this.kind = kind; this.issuer = issuer; this.settlement = settlement; this.resource = resource; this.quantity = Math.max(0, quantity);
        this.unitPrice = Math.max(0, unitPrice); this.createdAt = createdAt; this.deadline = deadline; this.trustRequired = trustRequired; this.reason = reason == null ? "" : reason;
    }

    public UUID id() { return id; }
    public Kind kind() { return kind; }
    public UUID issuer() { return issuer; }
    public UUID settlement() { return settlement; }
    public String resource() { return resource; }
    public double quantity() { return quantity; }
    public double unitPrice() { return unitPrice; }
    public double total() { return quantity * unitPrice; }
    public long createdAt() { return createdAt; }
    public long deadline() { return deadline; }
    public double trustRequired() { return trustRequired; }
    public String reason() { return reason; }
    public UUID counterparty() { return counterparty; }
    public UUID quest() { return quest; }
    public void quest(UUID q) { quest = q; }
    public State state() { return state; }
    public double delivered() { return delivered; }
    public double remaining() { return Math.max(0, quantity - delivered); }
    public long resolvedAt() { return resolvedAt; }
    public boolean open() { return state == State.OPEN || state == State.ACCEPTED; }

    public boolean accept(UUID who) { if (state != State.OPEN) return false; counterparty = who; state = State.ACCEPTED; return true; }
    public double deliver(double amount) { double d = Math.min(remaining(), Math.max(0, amount)); delivered += d; return d; }
    public void resolve(State s, long now) { state = s; resolvedAt = now; }
    public void restore(State s, UUID cp, UUID q, double d, long r) { state = s; counterparty = cp; quest = q; delivered = d; resolvedAt = r; }
}
