package yadi.samuraiai.ai.cognition.model;

/** One reason a value is what it is: what kind of thing caused it, a reference to it, a note and how far it moved the value. */
public record Cause(String kind, String ref, String note, double delta, long at) {
    public Cause {
        kind = kind == null ? "" : kind;
        ref = ref == null ? "" : ref;
        note = note == null ? "" : note;
    }
}
