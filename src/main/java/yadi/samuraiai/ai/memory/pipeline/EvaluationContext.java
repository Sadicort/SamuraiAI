package yadi.samuraiai.ai.memory.pipeline;

import java.util.Set;
import yadi.samuraiai.ai.cognition.model.PersonalityView;

/** What the memory engine needs to know about the NPC's situation to judge an experience: its goals, how much the actor matters to it, its personality. */
public record EvaluationContext(Set<String> goalTags, double relationshipWeight, PersonalityView personality, double currentEmotion) {
    public EvaluationContext {
        goalTags = goalTags == null ? Set.of() : Set.copyOf(goalTags);
        personality = personality == null ? PersonalityView.NEUTRAL : personality;
        relationshipWeight = Math.max(0.0D, Math.min(1.0D, relationshipWeight));
        currentEmotion = Math.max(0.0D, Math.min(1.0D, currentEmotion));
    }

    public static EvaluationContext none() { return new EvaluationContext(Set.of(), 0.0D, PersonalityView.NEUTRAL, 0.0D); }
}
