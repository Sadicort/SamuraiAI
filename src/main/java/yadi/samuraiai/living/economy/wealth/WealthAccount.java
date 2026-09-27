package yadi.samuraiai.living.economy.wealth;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wealth of one owner — a settlement's treasury, an NPC, a household or family, a merchant, a temple, a faction, a player:
 * coins, the properties it owns (buildings), and income and expenses (all time and over the current 30-day window).
 * Coins never go below zero: a payment without funds fails.
 */
public final class WealthAccount {
    public enum OwnerKind { SETTLEMENT, NPC, HOUSEHOLD, FAMILY, MERCHANT, TEMPLE, FACTION, PLAYER, CARAVAN }

    private final UUID id, owner;
    private final OwnerKind kind;
    private String label;
    private double coins, income, expenses, windowIncome, windowExpenses;
    private long windowStart;
    private final Set<UUID> properties = new LinkedHashSet<>();

    public WealthAccount(UUID id, OwnerKind kind, UUID owner, String label, long now) {
        this.id = id; this.kind = kind; this.owner = owner; this.label = label == null ? "" : label; this.windowStart = now;
    }

    public UUID id() { return id; }
    public OwnerKind kind() { return kind; }
    public UUID owner() { return owner; }
    public String label() { return label; }
    public void label(String v) { label = v; }
    public double coins() { return coins; }
    public double income() { return income; }
    public double expenses() { return expenses; }
    public double windowIncome() { return windowIncome; }
    public double windowExpenses() { return windowExpenses; }
    public Set<UUID> properties() { return properties; }

    void credit(double amount, long now, int windowMinutes) { roll(now, windowMinutes); coins += amount; income += amount; windowIncome += amount; }
    boolean debit(double amount, long now, int windowMinutes) {
        if (amount > coins + 1e-9) return false;
        roll(now, windowMinutes);
        coins = Math.max(0.0D, coins - amount); expenses += amount; windowExpenses += amount;
        return true;
    }
    private void roll(long now, int windowMinutes) { if (now - windowStart >= windowMinutes) { windowStart = now; windowIncome = 0; windowExpenses = 0; } }

    public void restore(double c, double in, double out, double wIn, double wOut, long wStart) { coins = c; income = in; expenses = out; windowIncome = wIn; windowExpenses = wOut; windowStart = wStart; }
    public long windowStart() { return windowStart; }
}
