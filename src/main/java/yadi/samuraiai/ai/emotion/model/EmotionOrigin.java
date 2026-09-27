package yadi.samuraiai.ai.emotion.model;

import java.util.UUID;

/** Why an emotion exists: the kind of source, the memory it came from (if any), a reference to the thing that caused it and the trace that led here. */
public record EmotionOrigin(TriggerSource source, UUID memoryId, String ref, UUID traceId) {
    public EmotionOrigin {
        source = source == null ? TriggerSource.ADMIN : source;
        ref = ref == null ? "" : ref;
    }
}
