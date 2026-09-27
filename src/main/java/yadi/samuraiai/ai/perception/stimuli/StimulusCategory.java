package yadi.samuraiai.ai.perception.stimuli;

/**
 * What a stimulus is about. Carries the base priority used before relation, emotion and context adjust it, and a
 * default threat weight, so tuning what an NPC cares about is data.
 */
public enum StimulusCategory {
    PLAYER(50, 0), NPC(35, 0), HOSTILE(70, 55), PASSIVE(20, 0), OBJECT(15, 0), ITEM(12, 0), PROJECTILE(60, 45), BLOCK_CHANGE(35, 10),
    FOOTSTEP(30, 0), IMPACT(40, 10), EXPLOSION(90, 80), DOOR(35, 5), PROJECTILE_SOUND(55, 40), DAMAGE_TAKEN(95, 70), FIRE(75, 65), LAVA(70, 60),
    FALL(60, 50), WEATHER(10, 0), LIGHT(10, 0), TERRAIN(5, 0), SPEECH(55, 0), VOICE(55, 0), CONTACT(60, 20), SCENT(30, 0), UNKNOWN(20, 0), CUSTOM(25, 0);

    private final int basePriority;
    private final int threatWeight;
    StimulusCategory(int basePriority, int threatWeight) { this.basePriority = basePriority; this.threatWeight = threatWeight; }
    public int basePriority() { return basePriority; }
    /** How threatening this category is by nature (0 = harmless). */
    public int threatWeight() { return threatWeight; }
}
