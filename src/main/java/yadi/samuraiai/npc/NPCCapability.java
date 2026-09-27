package yadi.samuraiai.npc;

/**
 * Something a definition allows its NPCs to do. The Brain checks these
 * before offering a Goal/Behavior, instead of every NPC type implementing
 * every possible action.
 */
public enum NPCCapability {
    CAN_TRADE,
    CAN_FIGHT,
    CAN_PATROL,
    CAN_TALK,
    CAN_MEDITATE,
    CAN_LEAD,
    CAN_HEAL,
    CAN_USE_WEAPON
}
