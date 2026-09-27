package yadi.samuraiai.ai.cognition.trace;

import java.util.UUID;

/** One hop of a cognitive trace: which trace, which NPC, which stage, what happened and when. */
public record TraceStep(UUID traceId, UUID npcId, TraceStage stage, String detail, long at) { }
