package yadi.samuraiai.ai.relationship.model;

import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/** Something the NPC was told about a person's standing, with where it came from: the input the knowledge engine's rumors give the reputation engine. */
public record ReputationHearsay(UUID npc, EntityRef subject, ReputationLabel label, double strength, ReputationScope scope, String scopeId, EntityRef source,
                                UUID rumorId, boolean publicEvent, boolean direct, long at, UUID traceId) {
    public ReputationHearsay {
        strength = Double.isFinite(strength) ? Math.max(0.0D, Math.min(1.0D, strength)) : 0.0D;
        scopeId = scopeId == null ? "" : scopeId;
    }
}
