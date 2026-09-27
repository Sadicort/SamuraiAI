package yadi.samuraiai.goal;

/**
 * The kinds of thing an NPC can want.
 *
 * <p>Each carries a base priority so the decision engine has something to
 * differentiate on. Previously every candidate goal was built with a hardcoded
 * 50, which made the utility scoring a no-op: the engine always picked
 * whichever goal the definition happened to list first, so a samurai under
 * attack would keep patrolling.
 */
public enum GoalType {

    IDLE(0),
    REST(15),
    PATROL(35),
    TALK(65),
    INVESTIGATE(45),
    PROTECT(60),
    COMBAT(80),
    FLEE(85),

    // Goals that come from the scheduler's day. Their own priority is low on purpose: they only enter the decision when the
    // scheduler advises them, and the advice then adds a bonus by priority layer.
    WAKE(10),
    WORK(30),
    EAT(25),
    SLEEP(20),
    MEDITATE(25),
    SOCIAL(30),
    TRAINING(30),
    PRAYER(25),
    GUARD(40),
    TRADE(35);

    private final int basePriority;

    GoalType(int basePriority) {
        this.basePriority = basePriority;
    }

    public int basePriority() {
        return basePriority;
    }
}
