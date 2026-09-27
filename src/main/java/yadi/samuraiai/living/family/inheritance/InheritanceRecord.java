package yadi.samuraiai.living.family.inheritance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An inheritance: whose estate, who inherits (in order of priority), which assets (homes, land, coins, a workshop, tools,
 * weapons, relics, a business, resources), under what conditions, when it was transferred, how it went and why it happened.
 * Ownership itself moves in the engines that own it (the Village for buildings, the Economy for coins and goods, the
 * heirloom registry for heirlooms); this record is the ownership history.
 */
public final class InheritanceRecord {
    public enum Status { PENDING, EXECUTED, PARTIAL, CONTESTED, VOID }
    public record Asset(Kind kind, UUID id, double amount, String label) { public enum Kind { HOME, LAND, COINS, WORKSHOP, TOOLS, WEAPONS, RELICS, BUSINESS, RESOURCES, HEIRLOOM } }
    public record Transfer(Asset asset, UUID to, long minute, boolean done, String note) { }

    private final UUID id, owner;
    private final List<UUID> heirs;
    private final List<Asset> assets;
    private final String conditions, reason;
    private final long created;
    private long transferDate;
    private Status status = Status.PENDING;
    private final List<Transfer> transfers = new ArrayList<>();

    public InheritanceRecord(UUID id, UUID owner, List<UUID> heirs, List<Asset> assets, String conditions, String reason, long created) {
        this.id = id; this.owner = owner; this.heirs = List.copyOf(heirs); this.assets = List.copyOf(assets); this.conditions = conditions == null ? "" : conditions;
        this.reason = reason == null ? "" : reason; this.created = created;
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public List<UUID> heirs() { return heirs; }
    public List<Asset> assets() { return assets; }
    public String conditions() { return conditions; }
    public String reason() { return reason; }
    public long created() { return created; }
    public long transferDate() { return transferDate; }
    public Status status() { return status; }
    public List<Transfer> transfers() { return transfers; }
    public void executed(Status s, long at) { status = s; transferDate = at; }
    public void restore(Status s, long at, List<Transfer> t) { status = s; transferDate = at; transfers.clear(); transfers.addAll(t); }
}
