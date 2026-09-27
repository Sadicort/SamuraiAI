package yadi.samuraiai.ai.knowledge.teaching;

import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;

/** The quality of a lesson: how much the student respects and trusts the teacher, how much the teacher knows the topic and how attentively the student listens. Below a minimum quality nothing is learned. */
public final class TeachingEngine {
    public double quality(double respect01, double trust01, double teacherConfidence, double attention, KnowledgeSettings s) {
        double weights = s.teachRespect() + s.teachTrust() + s.teachKnowledge() + s.teachAttention();
        if (weights <= 0) return 0.0D;
        double q = s.teachRespect() * clamp(respect01) + s.teachTrust() * clamp(trust01) + s.teachKnowledge() * clamp(teacherConfidence) + s.teachAttention() * clamp(attention);
        return clamp(q / weights);
    }

    public boolean succeeds(double quality, KnowledgeSettings s) { return quality >= s.teachMinQuality(); }

    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.0D, Math.min(1.0D, v)) : 0.0D; }
}
