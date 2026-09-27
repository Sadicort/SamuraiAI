package yadi.samuraiai.action;

import java.util.UUID;
import java.util.Objects;

public record LookAtAction(UUID targetId) implements Action {
    public LookAtAction { Objects.requireNonNull(targetId); }
    @Override public ActionType getType() { return ActionType.LOOK_AT; }
}
