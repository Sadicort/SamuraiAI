package yadi.samuraiai.ai.relationship.model;

import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/** A promise: who made it, to whom, what, when, until when and how it ended. Public promises carry reputational weight. */
public final class PromiseRecord {
    public enum Kind { ESCORT, PROTECT, DELIVER, MEET, TEACH, DUEL, OATH, GENERIC }

    private final UUID id;
    private final EntityRef promiser, promisee;
    private final Kind kind;
    private final String subject;
    private final long madeAt, dueAt;
    private final boolean publicPromise;
    private final double weight;
    private PromiseStatus status = PromiseStatus.ACTIVE;
    private long resolvedAt;

    public PromiseRecord(UUID id, EntityRef promiser, EntityRef promisee, Kind kind, String subject, long madeAt, long dueAt, boolean publicPromise, double weight) {
        this.id = id; this.promiser = promiser; this.promisee = promisee; this.kind = kind; this.subject = subject == null ? "" : subject;
        this.madeAt = madeAt; this.dueAt = dueAt; this.publicPromise = publicPromise; this.weight = Math.max(0.0D, Math.min(1.0D, weight));
    }

    public UUID id() { return id; }
    public EntityRef promiser() { return promiser; }
    public EntityRef promisee() { return promisee; }
    public Kind kind() { return kind; }
    public String subject() { return subject; }
    public long madeAt() { return madeAt; }
    public long dueAt() { return dueAt; }
    public boolean publicPromise() { return publicPromise; }
    public double weight() { return weight; }
    public PromiseStatus status() { return status; }
    public long resolvedAt() { return resolvedAt; }
    public void resolve(PromiseStatus next, long at) { status = next; resolvedAt = at; }
    public boolean oath() { return kind == Kind.OATH; }
}
