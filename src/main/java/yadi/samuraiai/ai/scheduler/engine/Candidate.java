package yadi.samuraiai.ai.scheduler.engine;

import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;

/** One thing an NPC could be doing right now, how strongly, in which priority layer, and why. */
public record Candidate(Intent intent, PriorityLayer layer, Source source, double score, String reason) {
    public String key() { return intent.key(); }
}
