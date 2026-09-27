package yadi.samuraiai.living.quest.chains;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quest chains: a quest that ends well may lead to a follow-up (the smith you helped forges you something), which inherits
 * its variables and its giver. This registry remembers each chain from its first quest, so a chain can be told as one story.
 */
public final class QuestChain {
    private final Map<UUID, UUID> rootOf = new LinkedHashMap<>();
    private final Map<UUID, List<UUID>> chains = new LinkedHashMap<>();

    public void link(UUID parent, UUID child) {
        UUID root = rootOf.getOrDefault(parent, parent);
        rootOf.put(child, root);
        chains.computeIfAbsent(root, k -> new ArrayList<>(List.of(root))).add(child);
    }

    public List<UUID> chainOf(UUID quest) { UUID root = rootOf.getOrDefault(quest, quest); return List.copyOf(chains.getOrDefault(root, List.of(quest))); }
    public Map<UUID, List<UUID>> all() { return Map.copyOf(chains); }
    public void clear() { rootOf.clear(); chains.clear(); }
}
