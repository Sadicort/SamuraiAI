package yadi.samuraiai.ai.knowledge.model;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** What the knowledge engine is told: a fact, how it was learned, from whom, and how well. Built from a memory, a conversation, a lesson or a rumour by the cognition layer. */
public record KnowledgeEvidence(UUID npc, KnowledgeType type, KnowledgeCategory category, EntityRef subject, Predicate predicate, EntityRef object, Map<String, String> attributes,
                                LearnMethod method, EntityRef source, double confidence, PlaceRef place, Set<String> tags, UUID memoryId, UUID traceId, long at, double attention,
                                double importance, AccessLevel access, UUID rumorId) {
    public KnowledgeEvidence {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        place = place == null ? PlaceRef.unknown() : place;
        category = category == null ? KnowledgeCategory.GENERAL : category;
        access = access == null ? AccessLevel.PUBLIC : access;
        attention = Double.isFinite(attention) ? Math.max(0.0D, Math.min(1.0D, attention)) : 1.0D;
        importance = Double.isFinite(importance) ? Math.max(0.0D, Math.min(1.0D, importance)) : 0.3D;
    }

    /** A fact learned by the NPC's own experience or senses (confidence taken from the method). */
    public static KnowledgeEvidence direct(UUID npc, KnowledgeType type, EntityRef subject, Predicate predicate, EntityRef object, LearnMethod method, long at) {
        return new KnowledgeEvidence(npc, type, KnowledgeCategory.GENERAL, subject, predicate, object, Map.of(), method, null, -1.0D, null, Set.of(), null, null, at, 1.0D, 0.3D, AccessLevel.PUBLIC, null);
    }
}
