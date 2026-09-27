package yadi.samuraiai.ai.perception.hearing;

import java.util.function.ToDoubleFunction;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;

/** Kinds of sound. Each has its own audible radius (configuration) and the stimulus category it becomes. */
public enum SoundCategory {
    FOOTSTEP_WALK(StimulusCategory.FOOTSTEP, PerceptionSettings::footstepWalkRadius),
    FOOTSTEP_SPRINT(StimulusCategory.FOOTSTEP, PerceptionSettings::footstepSprintRadius),
    IMPACT(StimulusCategory.IMPACT, PerceptionSettings::blockBreakRadius),
    BLOCK_BREAK(StimulusCategory.IMPACT, PerceptionSettings::blockBreakRadius),
    BLOCK_PLACE(StimulusCategory.IMPACT, PerceptionSettings::blockPlaceRadius),
    EXPLOSION(StimulusCategory.EXPLOSION, PerceptionSettings::explosionRadius),
    DOOR(StimulusCategory.DOOR, PerceptionSettings::doorRadius),
    PROJECTILE(StimulusCategory.PROJECTILE_SOUND, PerceptionSettings::projectileRadius),
    LIQUID(StimulusCategory.IMPACT, s -> s.footstepWalkRadius()),
    ANIMAL(StimulusCategory.FOOTSTEP, PerceptionSettings::animalRadius),
    NPC_VOICE(StimulusCategory.VOICE, PerceptionSettings::voiceRadius),
    PLAYER_VOICE(StimulusCategory.VOICE, PerceptionSettings::voiceRadius),
    DAMAGE(StimulusCategory.IMPACT, PerceptionSettings::damageRadius),
    LAVA(StimulusCategory.LAVA, s -> s.doorRadius()),
    CUSTOM(StimulusCategory.CUSTOM, s -> s.doorRadius());

    private final StimulusCategory stimulus;
    private final ToDoubleFunction<PerceptionSettings> radius;
    SoundCategory(StimulusCategory stimulus, ToDoubleFunction<PerceptionSettings> radius) { this.stimulus = stimulus; this.radius = radius; }
    public StimulusCategory stimulusCategory() { return stimulus; }
    public double radius(PerceptionSettings settings) { return radius.applyAsDouble(settings); }
}
