package yadi.samuraiai.ai.relationship.model;

import java.util.UUID;

/** One entry of a relationship's history: what happened, when, which memory it came from, and the net effect on trust, respect and honor. */
public record SocialEvent(long at, String kind, UUID memoryId, double trust, double respect, double honor, String note) {
    public SocialEvent {
        kind = kind == null ? "" : kind;
        note = note == null ? "" : note;
    }
}
