package yadi.samuraiai.ai.perception.engine;

import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;

/** Kinds of block worth noticing. Interest and threat are per kind, so adding a custom block is one enum value. */
public enum BlockInterest {
    DOOR_OPEN(35, StimulusCategory.DOOR), DOOR_CLOSED(10, StimulusCategory.DOOR), CHEST(30, StimulusCategory.OBJECT), TORCH(8, StimulusCategory.OBJECT),
    FIRE(60, StimulusCategory.FIRE), LAVA(55, StimulusCategory.LAVA), WATER(10, StimulusCategory.TERRAIN), BED(15, StimulusCategory.OBJECT),
    BELL(45, StimulusCategory.OBJECT), CUSTOM(25, StimulusCategory.CUSTOM);

    private final int interest;
    private final StimulusCategory category;
    BlockInterest(int interest, StimulusCategory category) { this.interest = interest; this.category = category; }
    public int interest() { return interest; }
    public StimulusCategory category() { return category; }
    public boolean hazard() { return this == FIRE || this == LAVA; }
}
