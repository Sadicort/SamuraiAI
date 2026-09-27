package yadi.samuraiai.living.economy.markets;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.economy.prices.PricePoint;

/**
 * An economic market: its stalls (the merchants selling there, each with its own finite stock), whether it is open (the
 * Village Engine's market life says so), today's volume and recent sales, and its price table.
 */
public final class MarketRuntime {
    public record Sale(long minute, String resource, double quantity, double unitPrice, UUID seller, UUID buyer) { }

    private final UUID settlement;
    private final Set<UUID> stalls = new LinkedHashSet<>();
    private final Map<String, PricePoint> prices = new LinkedHashMap<>();
    private final Deque<Sale> sales = new ArrayDeque<>();
    private boolean open;
    private int footfall;
    private double volumeToday;
    private long volumeDay = Long.MIN_VALUE;

    public MarketRuntime(UUID settlement) { this.settlement = settlement; }

    public UUID settlement() { return settlement; }
    public Set<UUID> stalls() { return stalls; }
    public Map<String, PricePoint> prices() { return prices; }
    public Deque<Sale> sales() { return sales; }
    public boolean open() { return open; }
    public void open(boolean v) { open = v; }
    public int footfall() { return footfall; }
    public void footfall(int v) { footfall = Math.max(0, v); }
    public double volumeToday(long day) { return day == volumeDay ? volumeToday : 0; }

    public void sold(Sale sale, long day) {
        if (day != volumeDay) { volumeDay = day; volumeToday = 0; }
        volumeToday += sale.quantity() * sale.unitPrice();
        sales.addLast(sale);
        while (sales.size() > 64) sales.removeFirst();
    }
}
