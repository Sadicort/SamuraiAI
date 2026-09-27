package yadi.samuraiai.ai.relationship.memorylink;

import java.util.UUID;
import yadi.samuraiai.ai.relationship.engine.RelationshipSettings;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.model.SocialEvidence;

/** Ties a relationship to the memories and world events it came from, without copying them: the relationship keeps ids, the memory keeps the relationship's target. */
public final class MemoryLinkEngine {
    /** @return whether a new link was recorded */
    public boolean link(RelationshipRecord r, SocialEvidence e, RelationshipSettings s) {
        boolean added = false;
        if (e.memoryId() != null) added = r.memories().add(e.memoryId());
        if (e.traceId() != null) r.events().add(e.traceId());
        trim(r.memories(), s.maxMemoryLinks());
        trim(r.events(), s.maxMemoryLinks());
        return added;
    }

    private static void trim(java.util.Set<UUID> set, int max) {
        var it = set.iterator();
        while (set.size() > max && it.hasNext()) { it.next(); it.remove(); }
    }
}
