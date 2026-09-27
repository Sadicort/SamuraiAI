package yadi.samuraiai.ai.knowledge.model;

/** How a subject relates to an object or a value. The first ten are the graph relations of the specification; the rest are the attribute-like ones facts need. */
public enum Predicate {
    KNOWS(false), PROTECTS(false), LIVES_AT(true), BELONGS_TO(false), TEACHES(false), LEARNED_FROM(false), VISITED(false), HEARD_ABOUT(false), CREATED(false), DISCOVERED(false),
    LOCATED_AT(true), IS_DANGEROUS(true), HELPED(false), ATTACKED(false), HAS_ROLE(true), CONNECTS(false), IS_HONORABLE(true), TRADES_AT(false), LEADS(true), HAS_PROPERTY(false);

    private final boolean singleValued;
    Predicate(boolean singleValued) { this.singleValued = singleValued; }
    /** Whether a subject can have only one object for this predicate (a new value replaces the old belief). */
    public boolean singleValued() { return singleValued; }
}
