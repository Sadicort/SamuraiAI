package yadi.samuraiai.ai.cognition.model;

/** Read-only view of an NPC's effective personality (base + long-term evolution + temporary emotional modifiers). */
public interface PersonalityView {
    /** The trait, 0-100. */
    double get(Trait trait);

    default double unit(Trait trait) { return get(trait) / 100.0D; }
    /** Relative to the neutral 50, in -1..+1. */
    default double lean(Trait trait) { return (get(trait) - 50.0D) / 50.0D; }

    PersonalityView NEUTRAL = trait -> 50.0D;
}
