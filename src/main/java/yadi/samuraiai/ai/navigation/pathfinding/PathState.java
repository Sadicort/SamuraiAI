package yadi.samuraiai.ai.navigation.pathfinding;

/** Lifecycle of one navigation session. Transitions are validated so a bug cannot resurrect a finished path. */
public enum PathState {
    REQUESTED, BUILDING, READY, RUNNING, BLOCKED, RECALCULATING, COMPLETED, FAILED, CANCELLED;

    public boolean terminal() { return this == COMPLETED || this == FAILED || this == CANCELLED; }
    public boolean active() { return !terminal(); }

    public boolean canMoveTo(PathState next) {
        if (terminal() || next == this) return false;
        if (next == FAILED || next == CANCELLED) return true;
        return switch (this) {
            case REQUESTED -> next == BUILDING || next == READY;
            case BUILDING -> next == READY;
            case READY -> next == RUNNING;
            case RUNNING -> next == BLOCKED || next == RECALCULATING || next == COMPLETED;
            case BLOCKED -> next == RUNNING || next == RECALCULATING;
            case RECALCULATING -> next == RUNNING || next == BLOCKED;
            default -> false;
        };
    }
}
