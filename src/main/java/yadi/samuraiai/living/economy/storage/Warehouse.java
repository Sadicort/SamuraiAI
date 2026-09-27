package yadi.samuraiai.living.economy.storage;

import java.util.UUID;
import yadi.samuraiai.living.economy.inventory.Inventory;

/**
 * The Warehouse Runtime: a store of goods — a village storehouse, a merchant's stock, a caravan's cargo, a household's pantry.
 * It has an inventory, a capacity in weight, an owner, how well it is protected (0..1, against theft and looting), where it
 * is (a building or a settlement) and a running count of what went in and out (the movements themselves are in the ledger).
 */
public final class Warehouse {
    public enum OwnerKind { SETTLEMENT, MERCHANT, CARAVAN, HOUSEHOLD, FAMILY, TEMPLE, PLAYER }

    private final UUID id;
    private final OwnerKind ownerKind;
    private final UUID owner;
    private final UUID settlement;
    private String location;
    private double capacity, security;
    private final Inventory inventory;
    private double inTotal, outTotal;
    private boolean dirty = true;

    public Warehouse(UUID id, OwnerKind ownerKind, UUID owner, UUID settlement, String location, double capacity, double security, int maxLots, int minutesPerDay) {
        this.id = id; this.ownerKind = ownerKind; this.owner = owner; this.settlement = settlement; this.location = location == null ? "" : location;
        this.capacity = Math.max(0.0D, capacity); this.security = Math.max(0.0D, Math.min(1.0D, security)); this.inventory = new Inventory(maxLots, minutesPerDay);
    }

    public UUID id() { return id; }
    public OwnerKind ownerKind() { return ownerKind; }
    public UUID owner() { return owner; }
    public UUID settlement() { return settlement; }
    public String location() { return location; }
    public void location(String v) { location = v == null ? "" : v; dirty = true; }
    public double capacity() { return capacity; }
    public void capacity(double v) { capacity = Math.max(0.0D, v); dirty = true; }
    public double security() { return security; }
    public void security(double v) { security = Math.max(0.0D, Math.min(1.0D, v)); dirty = true; }
    public Inventory inventory() { return inventory; }
    public double inTotal() { return inTotal; }
    public double outTotal() { return outTotal; }
    public void counted(double in, double out) { inTotal += Math.max(0, in); outTotal += Math.max(0, out); dirty = true; }
    public void restoreTotals(double in, double out) { inTotal = in; outTotal = out; }
    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clean() { dirty = false; }
}
