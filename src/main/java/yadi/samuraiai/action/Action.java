package yadi.samuraiai.action;

/**
 * An executable intention (move, attack, talk, ...) produced by a Task. The
 * Brain never performs these itself — {@link ActionExecutor} translates
 * them into real Minecraft operations.
 */
public interface Action {

    ActionType getType();

}
