package yadi.samuraiai.ai.scheduler.formation;

/** A position relative to the leader: {@code forward} along its heading and {@code right} across it, in blocks. Slot 0 is the leader's own. */
public record FormationSlot(int index, double right, double forward) { }
