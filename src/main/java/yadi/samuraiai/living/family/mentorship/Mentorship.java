package yadi.samuraiai.living.family.mentorship;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A master and a disciple (the Mentorship Record): when it began and ended, what kind of training (craft, samurai, religious,
 * merchant, scholar), the knowledge and techniques taught so far, progress 0..1, the trust and respect between them when last
 * measured (the Relationship Engine keeps the real values), its state and how much it weighs in the master's legacy. It moves
 * through the apprenticeship pipeline CANDIDATE → MASTER → SKILL → TRAINING → KNOWLEDGE → EXPERIENCE → COMPLETION.
 */
public final class Mentorship {
    public enum Type { CRAFT, SAMURAI, RELIGIOUS, MERCHANT, SCHOLAR, CUSTOM }
    public enum State { PROPOSED, ACTIVE, PAUSED, COMPLETED, FAILED, BROKEN, MASTER_DECEASED, DISCIPLE_DEPARTED }
    public enum Step { CANDIDATE, MASTER, SKILL, TRAINING, KNOWLEDGE, EXPERIENCE, COMPLETION }

    private final UUID id, master, disciple;
    private final Type type;
    private final long start;
    private long end;
    private UUID lineage;
    private final Set<String> knowledgeTaught = new LinkedHashSet<>(), techniquesTaught = new LinkedHashSet<>();
    private double progress, trust = 50, respect = 50, legacyWeight;
    private State state = State.PROPOSED;
    private long lastProgress;

    public Mentorship(UUID id, UUID master, UUID disciple, Type type, long start) {
        this.id = id; this.master = master; this.disciple = disciple; this.type = type; this.start = start; this.lastProgress = start;
    }

    public UUID id() { return id; }
    public UUID master() { return master; }
    public UUID disciple() { return disciple; }
    public Type type() { return type; }
    public long start() { return start; }
    public long end() { return end; }
    public UUID lineage() { return lineage; }
    public void lineage(UUID l) { lineage = l; }
    public Set<String> knowledgeTaught() { return knowledgeTaught; }
    public Set<String> techniquesTaught() { return techniquesTaught; }
    public double progress() { return progress; }
    public double trust() { return trust; }
    public double respect() { return respect; }
    public void bond(double t, double r) { trust = t; respect = r; }
    public double legacyWeight() { return legacyWeight; }
    public State state() { return state; }
    public boolean open() { return state == State.PROPOSED || state == State.ACTIVE || state == State.PAUSED; }
    public long lastProgress() { return lastProgress; }

    public Step step() {
        if (state == State.COMPLETED) return Step.COMPLETION;
        if (state == State.PROPOSED) return Step.CANDIDATE;
        if (progress < 0.1) return Step.MASTER;
        if (progress < 0.3) return Step.SKILL;
        if (progress < 0.6) return Step.TRAINING;
        if (progress < 0.85) return Step.KNOWLEDGE;
        return Step.EXPERIENCE;
    }

    public void activate() { if (state == State.PROPOSED || state == State.PAUSED) state = State.ACTIVE; }
    public void pause() { if (state == State.ACTIVE) state = State.PAUSED; }
    public void end(State s, long at) { state = s; end = at; legacyWeight = progress * (s == State.COMPLETED ? 1.0 : 0.4) * (1 + techniquesTaught.size() * 0.25); }
    public double advance(double amount, long at) { double before = progress; progress = Math.min(1.0, progress + Math.max(0, amount)); lastProgress = at; return progress - before; }
    public void restore(State s, double p, long e, double t, double r, double lw, long last) { state = s; progress = p; end = e; trust = t; respect = r; legacyWeight = lw; lastProgress = last; }
}
