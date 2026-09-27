package yadi.samuraiai.ai.cognition.engine;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.emotion.engine.TriggerResult;
import yadi.samuraiai.ai.knowledge.engine.LearnResult;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.memory.engine.Observation;
import yadi.samuraiai.ai.relationship.engine.Update;

/** Everything one experience caused across the engines: the memory (if kept), the emotions, the relationship change, what was learned, the rumour and the history it started. */
public record CognitionOutcome(UUID traceId, UUID experienceId, boolean kept, Observation memory, TriggerResult emotion, Update relationship, List<LearnResult> knowledge,
                               RumorRecord rumor, HistoricalEvent history) {
    public CognitionOutcome { knowledge = knowledge == null ? List.of() : List.copyOf(knowledge); }

    public static CognitionOutcome rejected(UUID trace, UUID experience) { return new CognitionOutcome(trace, experience, false, null, null, null, List.of(), null, null); }
}
