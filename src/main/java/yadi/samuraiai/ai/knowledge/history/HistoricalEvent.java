package yadi.samuraiai.ai.knowledge.history;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** Something a community remembers as part of its history: what kind of event, when, where, who was in it and how much it mattered. */
public record HistoricalEvent(UUID id, HistoryType type, long at, PlaceRef place, List<EntityRef> participants, Set<String> tags, double significance, String kind, UUID traceId,
                              String community, UUID witness) {
    public HistoricalEvent {
        participants = participants == null ? List.of() : List.copyOf(participants);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        place = place == null ? PlaceRef.unknown() : place;
        kind = kind == null ? "" : kind;
        community = community == null ? "" : community;
        significance = Double.isFinite(significance) ? Math.max(0.0D, Math.min(1.0D, significance)) : 0.0D;
    }
}
