package yadi.samuraiai.ai.knowledge.factions;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;

/** Access control for shared knowledge: not every member of a faction knows everything. A record is visible to a member whose rank is at least the record's access level. */
public final class FactionKnowledge {
    private FactionKnowledge() { }

    public static boolean canKnow(AccessLevel rank, KnowledgeRecord record) { return record.access().visibleTo(rank); }

    public static List<KnowledgeRecord> visible(AccessLevel rank, Iterable<KnowledgeRecord> records) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (KnowledgeRecord r : records) if (canKnow(rank, r)) result.add(r);
        return result;
    }
}
