package yadi.samuraiai.ai.memory.retrieval;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** The NPC's situation right now, for "which memories are relevant?": where it is, who is around, what it feels and what it is trying to do. */
public record RetrievalContext(PlaceRef place, List<UUID> nearby, EmotionKind emotion, Set<String> tags) {
    public RetrievalContext {
        place = place == null ? PlaceRef.unknown() : place;
        nearby = nearby == null ? List.of() : List.copyOf(nearby);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
