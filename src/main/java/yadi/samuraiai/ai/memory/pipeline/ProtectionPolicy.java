package yadi.samuraiai.ai.memory.pipeline;

import java.util.Set;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;

/** Which memories are exempt from ordinary forgetting: critical ones, traumatic ones, oaths and anything tagged or typed as protected. */
public final class ProtectionPolicy {
    public boolean protects(MemoryRecord r, MemorySettings s) {
        if (r.importance().ordinal() >= Math.min(s.protectFromImportance(), Importance.LEGENDARY.ordinal())) return true;
        if (r.emotion().traumatic()) return true;
        Set<String> tags = s.effectiveProtectedTags();
        for (String tag : r.tags()) if (tags.contains(tag)) return true;
        return s.effectiveProtectedKinds().contains(r.kind().name());
    }
}
