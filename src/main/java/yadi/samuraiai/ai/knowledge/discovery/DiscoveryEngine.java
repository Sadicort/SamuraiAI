package yadi.samuraiai.ai.knowledge.discovery;

import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;

/** Classifies a discovery (a cave, a temple, a relic, a stranger, a shortcut) by how much it is likely to matter, so that the NPC files it as important or as passing curiosity. */
public final class DiscoveryEngine {
    public double importance(KnowledgeType type, KnowledgeCategory category, boolean novel, double danger, KnowledgeSettings s) {
        double weight = switch (category) {
            case TEMPLE, CASTLE, RELIC -> 0.9D; case MARKET, CAMP, BOOK -> 0.65D; case CAVE, MOUNTAIN, WEAPON -> 0.55D; case BRIDGE, RIVER -> 0.5D;
            case FOREST, HOUSE, TOOL, FOOD -> 0.35D; default -> type == KnowledgeType.PERSON || type == KnowledgeType.ROUTE ? 0.5D : 0.3D;
        };
        double score = weight * (1.0D - s.discoveryNovelty()) + (novel ? s.discoveryNovelty() : 0.0D) + 0.25D * Math.max(0.0D, Math.min(1.0D, danger));
        return Math.max(0.0D, Math.min(1.0D, score));
    }

    public boolean worthRecording(double importance, KnowledgeSettings s) { return importance >= s.discoveryMinImportance(); }
}
