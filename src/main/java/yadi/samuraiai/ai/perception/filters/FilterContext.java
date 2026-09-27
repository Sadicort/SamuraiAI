package yadi.samuraiai.ai.perception.filters;

import java.util.Map;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.Perceiver;
import yadi.samuraiai.ai.perception.memory.PerceptionMemory;

/** What a filter may look at: who is perceiving, when, with which settings, and the per-NPC cooldown table. */
public record FilterContext(Perceiver perceiver, long tick, PerceptionSettings settings, Map<String, Long> cooldowns, PerceptionMemory memory) { }
