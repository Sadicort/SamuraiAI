package yadi.samuraiai.ai.perception.engine;

import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;

/** What kind of thing a sensed entity is. Maps to the stimulus category it produces. */
public enum EntityClass {
    PLAYER(StimulusCategory.PLAYER), NPC(StimulusCategory.NPC), HOSTILE(StimulusCategory.HOSTILE), ANIMAL(StimulusCategory.PASSIVE),
    VILLAGER(StimulusCategory.PASSIVE), OBJECT(StimulusCategory.ITEM), PROJECTILE(StimulusCategory.PROJECTILE), VEHICLE(StimulusCategory.OBJECT),
    UNKNOWN(StimulusCategory.UNKNOWN);

    private final StimulusCategory category;
    EntityClass(StimulusCategory category) { this.category = category; }
    public StimulusCategory category() { return category; }
    public boolean living() { return this == PLAYER || this == NPC || this == HOSTILE || this == ANIMAL || this == VILLAGER; }
}
