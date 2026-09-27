package yadi.samuraiai.ai.relationship.model;

import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/**
 * What the relationship engine is told: this NPC lived (or saw) something with that person, here is what it means for each
 * axis and how much it matters. Built from a memory by the cognition layer; the engine never reads memory itself.
 */
public record SocialEvidence(UUID npc, EntityRef other, String kind, SocialEffect effect, double weight, boolean publicEvent, boolean observedOnly,
                             UUID memoryId, UUID traceId, long at, PlaceRef place, double honorScale, RelationType roleHint, LoyaltyKind loyaltyKind,
                             Set<String> tags, String note) {
    public SocialEvidence {
        kind = kind == null ? "" : kind;
        effect = effect == null ? SocialEffect.NONE : effect;
        weight = Double.isFinite(weight) ? Math.max(0.0D, Math.min(1.0D, weight)) : 0.0D;
        place = place == null ? PlaceRef.unknown() : place;
        honorScale = Double.isFinite(honorScale) ? Math.max(0.0D, Math.min(3.0D, honorScale)) : 1.0D;
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        note = note == null ? "" : note;
    }

    public static SocialEvidence simple(UUID npc, EntityRef other, String kind, SocialEffect effect, double weight, long at) {
        return new SocialEvidence(npc, other, kind, effect, weight, false, false, null, null, at, PlaceRef.unknown(), 1.0D, null, null, Set.of(), "");
    }
}
