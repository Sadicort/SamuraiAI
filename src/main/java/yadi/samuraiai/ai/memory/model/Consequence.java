package yadi.samuraiai.ai.memory.model;

/** Something that followed from an experience (a wound, a lost ally, gained standing) and how much. */
public record Consequence(String kind, String target, double magnitude) {
    public Consequence {
        kind = kind == null ? "" : kind;
        target = target == null ? "" : target;
    }
}
