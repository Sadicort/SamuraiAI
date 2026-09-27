package yadi.samuraiai.behavior;

import yadi.samuraiai.goal.GoalType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maps a chosen {@link GoalType} to the Behavior that knows how to pursue it.
 *
 * <p>The registry starts almost empty on purpose. Patrol, combat and flee
 * behaviours need real movement and combat systems that do not exist yet, and
 * a stub that pretends to patrol is worse than an honest fallback. Adding one
 * later is a single {@link #register} call — no change to the Brain.
 */
public class BehaviorRegistry {

    private final Map<GoalType, Behavior> behaviors = new EnumMap<>(GoalType.class);

    private final Behavior fallback;

    public BehaviorRegistry() {
        this(new IdleBehavior());
        register(GoalType.PATROL, new PatrolBehavior());
        register(GoalType.FLEE, new FleeBehavior());
        register(GoalType.COMBAT, new CombatBehavior());
        register(GoalType.INVESTIGATE, new InvestigateBehavior());
        register(GoalType.PROTECT, new ProtectBehavior());
        RoutineBehavior routine = new RoutineBehavior();
        for (GoalType scheduled : new GoalType[]{GoalType.WAKE, GoalType.WORK, GoalType.EAT, GoalType.SLEEP, GoalType.MEDITATE, GoalType.SOCIAL,
                GoalType.TRAINING, GoalType.PRAYER, GoalType.GUARD, GoalType.TRADE, GoalType.REST}) register(scheduled, routine);
    }

    public BehaviorRegistry(Behavior fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public BehaviorRegistry register(GoalType type, Behavior behavior) {
        behaviors.put(type, Objects.requireNonNull(behavior, "behavior"));
        return this;
    }

    /** Never null — an unmapped goal falls back to idling. */
    public Behavior forGoal(GoalType type) {
        return behaviors.getOrDefault(type, fallback);
    }

    public boolean hasDedicatedBehavior(GoalType type) {
        return behaviors.containsKey(type);
    }
}
