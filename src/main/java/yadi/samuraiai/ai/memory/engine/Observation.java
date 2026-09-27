package yadi.samuraiai.ai.memory.engine;

import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.pipeline.Evaluation;

/** What the memory pipeline did with an experience: kept it as a new memory, folded it into an existing one, or let it go. */
public record Observation(Result result, MemoryRecord record, Evaluation evaluation) {
    public enum Result { CREATED, REINFORCED, DISCARDED }
    public boolean kept() { return result != Result.DISCARDED; }
}
