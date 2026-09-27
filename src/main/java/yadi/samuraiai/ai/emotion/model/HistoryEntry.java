package yadi.samuraiai.ai.emotion.model;

import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;

/** One line of an NPC's emotional history: what it felt, how strongly, for how long, why and how it ended. */
public record HistoryEntry(EmotionKind kind, double peak, long start, long end, String source, UUID memoryId, String outcome) {
    public HistoryEntry {
        source = source == null ? "" : source;
        outcome = outcome == null ? "" : outcome;
    }
}
