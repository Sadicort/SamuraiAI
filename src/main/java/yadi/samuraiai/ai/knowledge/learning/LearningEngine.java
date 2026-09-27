package yadi.samuraiai.ai.knowledge.learning;

import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;

/** How well an NPC takes something in: curious, disciplined and attentive learners hold on to more of what they are shown. The starting confidence is the method's base scaled by this. */
public final class LearningEngine {
    /** Learning speed, around 1.0 for an average attentive learner. */
    public double speed(PersonalityView p, double attention, KnowledgeSettings s) {
        double personality = 1.0D + s.learnCuriosity() * p.lean(Trait.CURIOSITY) + s.learnDiscipline() * p.lean(Trait.DISCIPLINE);
        double attentive = s.attentionFloor() + (1.0D - s.attentionFloor()) * Math.max(0.0D, Math.min(1.0D, attention));
        return Math.max(0.1D, personality * attentive);
    }

    /** The confidence a fresh belief starts with. */
    public double confidence(KnowledgeEvidence e, PersonalityView p, KnowledgeSettings s) {
        double base = e.confidence() >= 0 ? e.confidence() : s.confidenceFor(e.method());
        // What the NPC saw or lived itself is held at the method's full confidence; only what it is told depends on how well it takes things in.
        if (e.method().direct()) return base;
        double speed = speed(p, e.attention(), s);
        return Math.max(0.0D, Math.min(1.0D, base * (0.5D + 0.5D * Math.min(1.5D, speed))));
    }
}
