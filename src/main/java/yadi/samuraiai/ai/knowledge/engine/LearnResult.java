package yadi.samuraiai.ai.knowledge.engine;

import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/** What learning did: the record it created or updated and how the belief now stands. */
public record LearnResult(KnowledgeRecord record, boolean created, ValidationState before, ValidationState after, boolean rejected, String note) {
    public static LearnResult rejected(String note) { return new LearnResult(null, false, ValidationState.UNKNOWN, ValidationState.UNKNOWN, true, note); }
    public boolean stateChanged() { return before != after; }
}
