package yadi.samuraiai.living.world.regions;

/**
 * A natural resource of a region: timber in a forest, ore in a mountain, clay by a river. It is the <b>origin</b> of every
 * gathered resource in the economy: nothing is gathered that is not taken from a deposit. Stock regenerates lazily
 * ({@code regenPerDay} towards {@code capacity}) when it is read, so a sleeping region costs nothing.
 */
public final class ResourceDeposit {
    private final String resource;
    private double capacity, stock, regenPerDay, extracted;
    private long updatedAt;

    public ResourceDeposit(String resource, double capacity, double stock, double regenPerDay, long updatedAt) {
        this.resource = resource;
        this.capacity = Math.max(0.0D, capacity);
        this.stock = Math.max(0.0D, Math.min(this.capacity, stock));
        this.regenPerDay = Math.max(0.0D, regenPerDay);
        this.updatedAt = updatedAt;
    }

    public String resource() { return resource; }
    public double capacity() { return capacity; }
    public double regenPerDay() { return regenPerDay; }
    public double extracted() { return extracted; }
    public long updatedAt() { return updatedAt; }

    /** Current stock after regeneration up to {@code now}. */
    public double stock(long now, int minutesPerDay) { regenerate(now, minutesPerDay); return stock; }

    private void regenerate(long now, int minutesPerDay) {
        if (now <= updatedAt) return;
        double days = (now - updatedAt) / (double) minutesPerDay;
        stock = Math.min(capacity, stock + regenPerDay * days);
        updatedAt = now;
    }

    /** Takes up to {@code amount}; returns what was really taken (never more than the stock). */
    public double extract(double amount, long now, int minutesPerDay) {
        if (amount <= 0) return 0;
        regenerate(now, minutesPerDay);
        double taken = Math.min(stock, amount);
        stock -= taken;
        extracted += taken;
        return taken;
    }

    /** A disaster or a blessing changes the deposit (a forest fire burns timber). */
    public void damage(double fraction, long now, int minutesPerDay) {
        regenerate(now, minutesPerDay);
        stock = Math.max(0.0D, stock * (1.0D - Math.max(0.0D, Math.min(1.0D, fraction))));
    }

    public double fill(long now, int minutesPerDay) { return capacity <= 0 ? 0 : stock(now, minutesPerDay) / capacity; }

    public void restore(double savedStock, double savedExtracted, long savedAt) { stock = Math.max(0, Math.min(capacity, savedStock)); extracted = savedExtracted; updatedAt = savedAt; }
    public void reconfigure(double newCapacity, double newRegen) { capacity = Math.max(0, newCapacity); regenPerDay = Math.max(0, newRegen); stock = Math.min(stock, capacity); }
}
