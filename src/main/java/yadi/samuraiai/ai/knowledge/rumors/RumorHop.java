package yadi.samuraiai.ai.knowledge.rumors;

import java.util.UUID;

/** One telling of a rumour: from whom to whom, when, how credible the listener found it, and whether it changed in the telling. */
public record RumorHop(UUID from, UUID to, long at, double credibility, boolean transformed) { }
