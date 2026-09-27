package yadi.samuraiai.ai.scheduler.energy;

/** An NPC's condition: energy, fatigue, focus, stress and motivation, each 0-100. Mutated only by {@link EnergyModel}. */
public final class EnergyState {
    private double energy = 90, fatigue = 10, focus = 80, stress = 0, motivation = 60;

    public double energy() { return energy; }
    public double fatigue() { return fatigue; }
    public double focus() { return focus; }
    public double stress() { return stress; }
    public double motivation() { return motivation; }

    void set(double energy, double fatigue, double focus, double stress, double motivation) {
        this.energy = clamp(energy); this.fatigue = clamp(fatigue); this.focus = clamp(focus); this.stress = clamp(stress); this.motivation = clamp(motivation);
    }

    /** Adds stress from outside the model (a scare, a loud noise). */
    public void addStress(double amount) { stress = clamp(stress + amount); }

    /** Restores every value to a rested state. */
    public void reset() { set(90, 10, 80, 0, 60); }

    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.0D, Math.min(100.0D, v)) : 0.0D; }

    @Override public String toString() {
        return String.format("energy=%.0f fatigue=%.0f focus=%.0f stress=%.0f motivation=%.0f", energy, fatigue, focus, stress, motivation);
    }
}
