package yadi.samuraiai.ai.memory.pipeline;

import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Importance;

/** Turns an evaluation score (0-1) into an importance level, using the configured cut points. */
public final class ImportanceCalculator {
    public Importance level(double score, MemorySettings s) {
        if (score >= s.cutLegendary()) return Importance.LEGENDARY;
        if (score >= s.cutCritical()) return Importance.CRITICAL;
        if (score >= s.cutImportant()) return Importance.IMPORTANT;
        if (score >= s.cutHigh()) return Importance.HIGH;
        if (score >= s.cutNormal()) return Importance.NORMAL;
        if (score >= s.cutLow()) return Importance.LOW;
        return Importance.TRIVIAL;
    }
}
