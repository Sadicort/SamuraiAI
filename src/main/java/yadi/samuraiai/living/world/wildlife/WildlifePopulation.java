package yadi.samuraiai.living.world.wildlife;

/** How many animals of one species live in one region, the most it can hold, when it was last simulated and how many were taken by people. */
public final class WildlifePopulation {
    private final String species;
    private double count, capacity, taken, preyedOn;
    private long updatedAt;

    public WildlifePopulation(String species, double count, double capacity, long updatedAt) {
        this.species = species; this.count = Math.max(0, count); this.capacity = Math.max(0, capacity); this.updatedAt = updatedAt;
    }

    public String species() { return species; }
    public double count() { return count; }
    public double capacity() { return capacity; }
    public double taken() { return taken; }
    public double preyedOn() { return preyedOn; }
    public long updatedAt() { return updatedAt; }

    void set(double value, long at) { count = Math.max(0.0D, value); updatedAt = at; }
    void capacity(double value) { capacity = Math.max(0.0D, value); }
    void preyed(double amount) { preyedOn += amount; }
    double take(double amount) { double t = Math.max(0.0D, Math.min(count, amount)); count -= t; taken += t; return t; }

    public void restore(double savedCount, double savedCapacity, double savedTaken, double savedPreyed, long at) {
        count = Math.max(0, savedCount); capacity = Math.max(0, savedCapacity); taken = savedTaken; preyedOn = savedPreyed; updatedAt = at;
    }
}
