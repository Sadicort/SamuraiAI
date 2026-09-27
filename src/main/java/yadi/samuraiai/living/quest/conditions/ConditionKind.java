package yadi.samuraiai.living.quest.conditions;

/**
 * The kinds of world condition a quest can come from. Each is reported by the hub from the state of another engine: the
 * economy (scarcity, a workshop without materials, a lost caravan, a cut route), the world (bandits, an attack, a war, a fire,
 * an unexplored region), the villages (no beds, too few guards), the calendar (a festival coming, a full moon for a ritual),
 * the families (someone missing, an heirloom lost, a master looking for a disciple, a dishonour) and memory (an NPC grateful
 * to a player, a grudge).
 */
public enum ConditionKind {
    SCARCITY, WORKSHOP_STARVED, LOST_CARAVAN, ROUTE_BLOCKED, BANDITS, ATTACK, WAR, FIRE, EXPLORATION,
    HOUSING_SHORTAGE, GUARD_SHORTAGE, FESTIVAL_SOON, TEMPLE_RITUAL,
    MISSING_PERSON, HEIRLOOM_LOST, MENTOR_WANTED, FAMILY_DISHONOR, DISPUTE,
    GRATITUDE, GRUDGE, CUSTOM
}
