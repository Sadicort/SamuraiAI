package yadi.samuraiai.ai.memory.retrieval;

import java.util.Set;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

/** How alike two memories are (0-1). The seam for a future semantic or embedding-based comparison; today it compares structure. */
public interface SimilarityEngine {
    double similarity(MemoryRecord a, MemoryRecord b);
    double similarity(Set<String> tags, MemoryRecord record);
}
