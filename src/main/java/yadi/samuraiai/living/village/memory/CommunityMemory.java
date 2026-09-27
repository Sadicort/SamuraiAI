package yadi.samuraiai.living.village.memory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.village.engine.VillagePorts;
import yadi.samuraiai.living.village.runtime.Village;

/**
 * Community memory: what a village lived through — festivals, fires, wars, aid received, betrayals, constructions. Each entry
 * goes to the world timeline under the village's scopes (the persistent Village Memory Timeline is a view of that one
 * chronology) and, when significant, into the linked knowledge community's history, so that its members remember it
 * together and can tell it to others. The village keeps only counters of its own.
 */
public final class CommunityMemory {
    public void remember(Village village, Village.Counter counter, String category, String historyType, String title, String detail, double significance,
                         double memoryThreshold, UUID actor, long minute, VillagePorts.Chronicle chronicle, VillagePorts.Community community, Provenance source) {
        if (counter != null) village.count(counter);
        Set<String> scopes = new LinkedHashSet<>();
        scopes.add(village.scope());
        scopes.add("settlement:" + village.id());
        if (village.region() != null) scopes.add("region:" + village.region());
        if (actor != null) scopes.add("npc:" + actor);
        chronicle.record(minute, category, title, detail, scopes, significance, source == null ? Provenance.of("village", village.id().toString(), title, minute) : source);
        if (significance >= memoryThreshold && historyType != null) community.remember(village.communityKey(), historyType, title, significance, minute, actor);
    }
}
