package yadi.samuraiai.living.economy.trade_routes;

import java.util.List;
import java.util.UUID;

/**
 * A trade route between two settlements over the world's roads: its legs (road edges with their length and danger when it was
 * planned), total distance, safety, danger, bridges, status, the road-network version it was planned against, and how it has
 * been used (caravans sent, arrived, lost, value carried).
 */
public final class TradeRoute {
    public enum Status { OPEN, DANGEROUS, BLOCKED }
    public record Leg(UUID edge, double length, double danger) { }

    private final UUID id, origin, destination;
    private List<Leg> legs;
    private double distance, danger, safety;
    private int bridges;
    private Status status = Status.OPEN;
    private long version, lastUsed;
    private int sent, arrived, lost;
    private double valueCarried;

    public TradeRoute(UUID id, UUID origin, UUID destination) { this.id = id; this.origin = origin; this.destination = destination; this.legs = List.of(); }

    public UUID id() { return id; }
    public UUID origin() { return origin; }
    public UUID destination() { return destination; }
    public List<Leg> legs() { return legs; }
    public double distance() { return distance; }
    public double danger() { return danger; }
    public double safety() { return safety; }
    public int bridges() { return bridges; }
    public Status status() { return status; }
    public void status(Status s) { status = s; }
    public long version() { return version; }
    public long lastUsed() { return lastUsed; }
    public int sent() { return sent; }
    public int arrived() { return arrived; }
    public int lost() { return lost; }
    public double valueCarried() { return valueCarried; }
    public boolean usable() { return status != Status.BLOCKED && !legs.isEmpty(); }
    public boolean uses(UUID edge) { for (Leg l : legs) if (l.edge().equals(edge)) return true; return false; }

    public void plan(List<Leg> newLegs, int bridgeCount, long networkVersion) {
        legs = List.copyOf(newLegs);
        distance = 0; danger = 0;
        double max = 0;
        for (Leg l : legs) { distance += l.length(); danger += l.danger() * l.length(); max = Math.max(max, l.danger()); }
        danger = distance <= 0 ? 0 : danger / distance;
        safety = 1.0D - max;
        bridges = bridgeCount;
        version = networkVersion;
        status = legs.isEmpty() ? Status.BLOCKED : max >= 0.5D ? Status.DANGEROUS : Status.OPEN;
    }

    public void used(long minute) { lastUsed = minute; sent++; }
    public void completed(double value) { arrived++; valueCarried += value; }
    public void failed() { lost++; }
    public void restoreStats(Status s, long v, long last, int se, int ar, int lo, double val) { status = s; version = v; lastUsed = last; sent = se; arrived = ar; lost = lo; valueCarried = val; }
}
