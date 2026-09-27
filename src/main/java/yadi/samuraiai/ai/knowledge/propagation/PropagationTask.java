package yadi.samuraiai.ai.knowledge.propagation;

import java.util.UUID;

/** One pending telling: who tells whom what (a knowledge record or a rumour), when it arrives and how many hops it has made. */
public record PropagationTask(UUID id, Kind kind, UUID from, UUID to, UUID itemId, long dueAt, int hop, double relevance, String community) {
    public enum Kind { KNOWLEDGE, RUMOR }
}
