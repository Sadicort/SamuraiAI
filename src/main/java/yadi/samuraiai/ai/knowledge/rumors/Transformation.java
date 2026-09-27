package yadi.samuraiai.ai.knowledge.rumors;

/** A change a rumour underwent as it passed along: at which hop, what happened and how its magnitude moved. */
public record Transformation(long at, int hop, String note, double before, double after) {
    public Transformation { note = note == null ? "" : note; }
}
