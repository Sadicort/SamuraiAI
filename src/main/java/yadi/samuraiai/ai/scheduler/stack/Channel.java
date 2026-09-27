package yadi.samuraiai.ai.scheduler.stack;

/** The parts of an NPC's behaviour that one activity can occupy; two activities may run together only if their channels differ. */
public enum Channel { MOVE, LOOK, TALK, POSTURE, PACE }
