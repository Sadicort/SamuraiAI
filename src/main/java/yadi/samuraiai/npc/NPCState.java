package yadi.samuraiai.npc;

/**
 * Operational state of an active NPC. This is deliberately shallow — it
 * says what the NPC is currently doing, not why. The "why" lives in the
 * Goal/DecisionEngine layer so this never grows into a giant state machine.
 */
public enum NPCState {
    IDLE,
    PATROLLING,
    INVESTIGATING,
    TALKING,
    COMBAT,
    FLEEING,
    RESTING,
    DEAD
}
