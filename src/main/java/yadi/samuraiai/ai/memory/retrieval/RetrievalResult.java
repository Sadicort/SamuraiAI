package yadi.samuraiai.ai.memory.retrieval;

import java.util.List;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

/** A retrieved memory, how well it fits the query and which criteria matched (so a decision can say why it remembered this). */
public record RetrievalResult(MemoryRecord record, double score, List<String> matched) {
    public RetrievalResult { matched = List.copyOf(matched); }
}
