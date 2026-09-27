package yadi.samuraiai.living.economy.traders;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Merchant Runtime: a trader with a finite stock (a warehouse of their own), money (a wealth account), a home market, the
 * caravan they are travelling with, a reputation, the clients they know and the contracts they hold. {@code npc} is the
 * citizen it embodies, or null for a trader that exists only in the simulation (a merchant house of a market town).
 * Personality decides the margin: greedy &gt; negotiator &gt; honourable &gt; generous.
 */
public final class Merchant {
    public enum Personality {
        GREEDY(1.35D), NEGOTIATOR(1.2D), HONORABLE(1.12D), GENEROUS(1.05D);
        private final double margin;
        Personality(double margin) { this.margin = margin; }
        public double margin() { return margin; }
    }

    private final UUID id, npc, home, account, stock;
    private final String name;
    private final Personality personality;
    private double reputation = 50;
    private UUID caravan;
    private long availableAt;
    private int trades;
    private double profit;
    private final Map<UUID, Integer> clients = new LinkedHashMap<>();
    private final Set<UUID> contracts = new LinkedHashSet<>();

    public Merchant(UUID id, UUID npc, String name, UUID home, UUID account, UUID stock, Personality personality) {
        this.id = id; this.npc = npc; this.name = name; this.home = home; this.account = account; this.stock = stock; this.personality = personality;
    }

    public UUID id() { return id; }
    public UUID npc() { return npc; }
    public boolean embodied() { return npc != null; }
    public String name() { return name; }
    public UUID home() { return home; }
    public UUID account() { return account; }
    public UUID stock() { return stock; }
    public Personality personality() { return personality; }
    public double reputation() { return reputation; }
    public void reputation(double v) { reputation = Math.max(0, Math.min(100, v)); }
    public UUID caravan() { return caravan; }
    public void caravan(UUID v) { caravan = v; }
    public long availableAt() { return availableAt; }
    public void availableAt(long v) { availableAt = v; }
    public boolean available(long now) { return caravan == null && now >= availableAt; }
    public int trades() { return trades; }
    public double profit() { return profit; }
    public void traded(double gain) { trades++; profit += gain; }
    public Map<UUID, Integer> clients() { return clients; }
    public void served(UUID client) { if (client != null) clients.merge(client, 1, Integer::sum); }
    public Set<UUID> contracts() { return contracts; }
    public void restoreStats(double rep, UUID car, long avail, int t, double p) { reputation = rep; caravan = car; availableAt = avail; trades = t; profit = p; }

    /** The price this merchant asks a client: margin by personality, less a discount for trust and repeated custom. */
    public double askingPrice(double marketPrice, double trust01, UUID client) {
        int visits = client == null ? 0 : clients.getOrDefault(client, 0);
        double discount = Math.min(0.2D, 0.1D * Math.max(0, trust01 - 0.5D) * 2 + Math.min(0.05D, visits * 0.005D));
        return marketPrice * personality.margin() * (1.0D - discount);
    }
}
