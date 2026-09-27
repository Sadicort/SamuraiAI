package yadi.samuraiai.action;

import yadi.samuraiai.world.SpawnLocation;
import java.util.Objects;

public record MoveToAction(SpawnLocation target, double speed) implements Action {
    public MoveToAction { Objects.requireNonNull(target); if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("speed"); }
    @Override public ActionType getType() { return ActionType.MOVE_TO; }
}
