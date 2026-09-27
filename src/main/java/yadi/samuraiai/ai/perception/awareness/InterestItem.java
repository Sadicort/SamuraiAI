package yadi.samuraiai.ai.perception.awareness;

import java.util.UUID;

/** Something the NPC finds interesting (a new entity, an unknown player, an odd event, a place) and how much. */
public record InterestItem(String key, UUID subject, String label, double x, double y, double z, double score, long tick, boolean notified) {
    public InterestItem withScore(double value) { return new InterestItem(key, subject, label, x, y, z, value, tick, notified); }
    public InterestItem withNotified(boolean value) { return new InterestItem(key, subject, label, x, y, z, score, tick, value); }
}
