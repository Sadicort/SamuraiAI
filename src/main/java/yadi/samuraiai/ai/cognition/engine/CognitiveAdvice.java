package yadi.samuraiai.ai.cognition.engine;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.emotion.model.Expression;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.emotion.model.Technique;
import yadi.samuraiai.ai.relationship.model.FriendshipStage;
import yadi.samuraiai.ai.relationship.model.HonorCategory;

/**
 * What the Brain may read of an NPC's inner life: mood and dominant feeling, the people around it that it has history with,
 * whether it knows the place to be dangerous, the tradition due now, and the relevant memories. It is advice: the Brain decides,
 * the scheduler schedules, navigation moves. Immutable.
 */
public record CognitiveAdvice(UUID npcId, long tick, MoodKind mood, EmotionKind dominant, double dominantIntensity, String blend, double fear, double anger, double sadness, double joy, double calm,
                              Technique regulation, Expression expression, double speedScale, List<Familiar> familiars, double dangerNearby, String ritual, String ritualZoneKind,
                              List<String> memories) {
    /** Someone nearby the NPC has a history with, summarised. */
    public record Familiar(UUID id, String name, double trust, double fear, double respect, double rivalry, HonorCategory honor, FriendshipStage stage, boolean hostile, boolean friendly) { }

    public CognitiveAdvice {
        familiars = List.copyOf(familiars);
        memories = List.copyOf(memories);
        ritual = ritual == null ? "" : ritual;
        ritualZoneKind = ritualZoneKind == null ? "" : ritualZoneKind;
    }

    public static CognitiveAdvice calm(UUID npcId, long tick) {
        return new CognitiveAdvice(npcId, tick, MoodKind.NEUTRAL, EmotionKind.CALM, 0, "CALM", 0, 0, 0, 0, 0, Technique.NONE, Expression.NEUTRAL, 1.0D, List.of(), 0, "", "", List.of());
    }

    public boolean anyHostile() { for (Familiar f : familiars) if (f.hostile()) return true; return false; }
    public boolean anyFriendly() { for (Familiar f : familiars) if (f.friendly()) return true; return false; }
}
