package yadi.samuraiai.ai.scheduler.engine;

import java.util.UUID;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInput;
import yadi.samuraiai.ai.scheduler.zone.Place;

/** Everything the scheduler reads about one NPC for one evaluation. Plain data: no entity, no world, no brain. */
public record SchedulerInput(UUID npcId, String typeId, String dimension, double x, double y, double z, long worldTime, long tick,
                             EmotionInput emotion, Perceived perceived, double playerDistance, boolean canFight, Place home) { }
