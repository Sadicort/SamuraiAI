package yadi.samuraiai.action;

import java.util.UUID;
import java.util.Objects;

public record AttackAction(UUID targetId) implements Action {
    public AttackAction { Objects.requireNonNull(targetId); }
    @Override public ActionType getType() { return ActionType.ATTACK; }
}
