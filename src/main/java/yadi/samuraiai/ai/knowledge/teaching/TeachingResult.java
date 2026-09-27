package yadi.samuraiai.ai.knowledge.teaching;

import java.util.List;
import java.util.UUID;

/** How a lesson went: the quality reached and, for each topic, whether the student learned it. */
public record TeachingResult(UUID teacher, UUID student, double quality, List<Topic> topics) {
    public record Topic(UUID recordId, boolean learned, String note) { }
    public TeachingResult { topics = List.copyOf(topics); }
    public int learnedCount() { int n = 0; for (Topic t : topics) if (t.learned()) n++; return n; }
}
