package yadi.samuraiai.ai.cognition.personality;

import java.util.Arrays;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.Trait;

/**
 * One NPC's personality in three layers: the base (what it was born with), the long-term evolution (what experience has slowly
 * made of it, capped and rate-limited) and, computed on the fly by the view, the temporary emotional modifiers (fear does not
 * erase courage, it only masks it for a while). Only the base and the evolution are persisted.
 */
public final class PersonalityLedger {
    private final UUID npcId;
    private final double[] base = new double[Trait.values().length];
    private final double[] evolution = new double[Trait.values().length];
    private final double[] dayBudget = new double[Trait.values().length];
    private long budgetDay = -1;
    private boolean seeded, dirty;
    private int version;

    public PersonalityLedger(UUID npcId) { this.npcId = npcId; Arrays.fill(base, 50.0D); }

    public UUID npcId() { return npcId; }
    public boolean seeded() { return seeded; }
    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public int version() { return version; }

    public void seed(double[] values) {
        for (int i = 0; i < base.length; i++) base[i] = i < values.length ? Math.max(0, Math.min(100, values[i])) : 50.0D;
        seeded = true; dirty = true; version++;
    }

    public double base(Trait t) { return base[t.ordinal()]; }
    public double evolution(Trait t) { return evolution[t.ordinal()]; }
    public double longTerm(Trait t) { return Math.max(0.0D, Math.min(100.0D, base[t.ordinal()] + evolution[t.ordinal()])); }
    public double[] baseValues() { return base.clone(); }
    public double[] evolutionValues() { return evolution.clone(); }
    public void restore(double[] baseValues, double[] evolutionValues) {
        for (int i = 0; i < base.length; i++) { base[i] = i < baseValues.length ? baseValues[i] : 50.0D; evolution[i] = i < evolutionValues.length ? evolutionValues[i] : 0.0D; }
        seeded = true;
    }

    /**
     * Moves a trait slowly. The change is limited by a per-day budget per trait and by a total cap on the distance from the base,
     * so no single event (or run of events) can rewrite a character.
     * @return how far the trait actually moved
     */
    public double evolve(Trait trait, double delta, double dailyLimit, double cap, long day) {
        if (day != budgetDay) { Arrays.fill(dayBudget, 0.0D); budgetDay = day; }
        int i = trait.ordinal();
        double room = dailyLimit - Math.abs(dayBudget[i]);
        if (room <= 0) return 0.0D;
        double step = Math.max(-room, Math.min(room, delta));
        double next = Math.max(-cap, Math.min(cap, evolution[i] + step));
        double moved = next - evolution[i];
        if (Math.abs(moved) < 1e-9) return 0.0D;
        evolution[i] = next;
        dayBudget[i] += Math.abs(moved);
        dirty = true; version++;
        return moved;
    }

    /** The effective personality: base plus evolution plus the temporary modifiers (each in points, given by the caller). */
    public PersonalityView view(java.util.function.Function<Trait, Double> temporary) {
        return trait -> Math.max(0.0D, Math.min(100.0D, longTerm(trait) + (temporary == null ? 0.0D : temporary.apply(trait))));
    }
}
