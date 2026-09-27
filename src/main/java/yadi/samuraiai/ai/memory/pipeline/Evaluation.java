package yadi.samuraiai.ai.memory.pipeline;

import java.util.Map;
import yadi.samuraiai.ai.memory.model.Importance;

/** The verdict on an experience: the score, its importance level, whether to keep it and how each factor contributed (for auditing). */
public record Evaluation(double score, Importance importance, boolean keep, Map<String, Double> factors) {
    public Evaluation { factors = Map.copyOf(factors); }
}
