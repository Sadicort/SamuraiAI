package yadi.samuraiai.ai.knowledge.rumors;

import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.knowledge.model.Predicate;

/** What a rumour claims: someone did something (predicate) to someone or something, how big it was (0-1), what reputation label it bears on (if any) and where. */
public record RumorClaim(EntityRef subject, Predicate predicate, EntityRef object, String label, double magnitude, PlaceRef place, String kind) {
    public RumorClaim {
        label = label == null ? "" : label;
        place = place == null ? PlaceRef.unknown() : place;
        kind = kind == null ? "" : kind;
        magnitude = Double.isFinite(magnitude) ? Math.max(0.0D, Math.min(1.0D, magnitude)) : 0.0D;
    }

    public RumorClaim withMagnitude(double value) { return new RumorClaim(subject, predicate, object, label, value, place, kind); }
}
