package yadi.samuraiai.living.economy.caravans;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Caravan Runtime: merchants, guards and carts carrying cargo (a warehouse of its own) along a trade route from an origin
 * to a destination. It travels in the background (progress in blocks along the route's legs), so a caravan far from any
 * player costs a few arithmetic operations per step; its position can still be shown because progress is exact.
 */
public final class Caravan {
    public enum Type { AGRICULTURAL, METALLURGICAL, RELIGIOUS, COMMERCIAL, MILITARY }
    public enum State { PLANNED, LOADING, TRAVELLING, DELAYED, ARRIVED, RETURNING, COMPLETED, LOST, CANCELLED }

    private final UUID id, route, origin, destination, cargo, merchant;
    private final Type type;
    private final int guards, carts;
    private final double capacity, speed;
    private State state = State.PLANNED;
    private double progress, returnProgress, purchaseCost, saleValue;
    private int legIndex, ambushes;
    private long createdAt, departAt, arrivedAt, delayedSince;
    private String note = "";
    private final List<String> log = new ArrayList<>();

    public Caravan(UUID id, Type type, UUID route, UUID origin, UUID destination, UUID merchant, UUID cargo, int guards, int carts, double capacity, double speed, long createdAt, long departAt) {
        this.id = id; this.type = type; this.route = route; this.origin = origin; this.destination = destination; this.merchant = merchant; this.cargo = cargo;
        this.guards = Math.max(0, guards); this.carts = Math.max(1, carts); this.capacity = capacity; this.speed = Math.max(0.1D, speed);
        this.createdAt = createdAt; this.departAt = departAt;
    }

    public UUID id() { return id; }
    public Type type() { return type; }
    public UUID route() { return route; }
    public UUID origin() { return origin; }
    public UUID destination() { return destination; }
    public UUID merchant() { return merchant; }
    public UUID cargo() { return cargo; }
    public int guards() { return guards; }
    public int carts() { return carts; }
    public double capacity() { return capacity; }
    /** Blocks per Deiliora minute on a good road. */
    public double speed() { return speed; }
    public State state() { return state; }
    public void state(State s) { state = s; }
    public boolean active() { return state != State.COMPLETED && state != State.LOST && state != State.CANCELLED; }
    public double progress() { return progress; }
    public void progress(double v) { progress = v; }
    public double returnProgress() { return returnProgress; }
    public void returnProgress(double v) { returnProgress = v; }
    public int legIndex() { return legIndex; }
    public void legIndex(int v) { legIndex = v; }
    public int ambushes() { return ambushes; }
    public void ambushed() { ambushes++; }
    public double purchaseCost() { return purchaseCost; }
    public void purchaseCost(double v) { purchaseCost = v; }
    public double saleValue() { return saleValue; }
    public void saleValue(double v) { saleValue = v; }
    public long createdAt() { return createdAt; }
    public long departAt() { return departAt; }
    public long arrivedAt() { return arrivedAt; }
    public void arrivedAt(long v) { arrivedAt = v; }
    public long delayedSince() { return delayedSince; }
    public void delayedSince(long v) { delayedSince = v; }
    public String note() { return note; }
    public List<String> log() { return log; }
    public void note(String text) { note = text; log.add(text); while (log.size() > 20) log.remove(0); }

    /** Defence 0..0.85 against an ambush: each guard adds a quarter, carts slow the escape. */
    public double defence() { return Math.min(0.85D, guards * 0.25D / (1.0D + 0.1D * (carts - 1))); }

    public void restore(State s, double prog, double back, int leg, int amb, double cost, double sale, long arrived, long delayed, String n) {
        state = s; progress = prog; returnProgress = back; legIndex = leg; ambushes = amb; purchaseCost = cost; saleValue = sale; arrivedAt = arrived; delayedSince = delayed; note = n == null ? "" : n;
    }
}
