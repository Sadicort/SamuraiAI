package yadi.samuraiai.ai.emotion.engine;

import java.util.List;
import yadi.samuraiai.ai.emotion.model.EmotionRecord;
import yadi.samuraiai.ai.emotion.model.TraumaRecord;

/** What a trigger did: the emotion records it created or deepened, and the trauma it started or deepened (if any). */
public record TriggerResult(List<EmotionRecord> records, TraumaRecord trauma, boolean traumaReinforced) {
    public TriggerResult { records = List.copyOf(records); }
    public boolean any() { return !records.isEmpty(); }
}
