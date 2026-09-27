package yadi.samuraiai.brain;

import yadi.samuraiai.action.ActionExecutor;
import yadi.samuraiai.action.DefaultActionExecutor;
import yadi.samuraiai.behavior.Behavior;
import yadi.samuraiai.behavior.BehaviorRegistry;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.decision.DecisionEngine;
import yadi.samuraiai.decision.UtilityDecisionEngine;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCCapability;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCState;
import yadi.samuraiai.task.Task;
import yadi.samuraiai.task.TaskStatus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Reference Brain: pulls the NPC default goals from its definition, asks the
 * DecisionEngine which one to pursue right now, plans it into tasks via a
 * Behavior, and ticks those tasks.
 *
 * <p>One instance per NPC. {@code activeTasks} is per-individual state, and
 * sharing a Brain between NPCs would have them executing each other plans.
 */
public class DefaultBrain implements Brain {

    /**
     * Goals an NPC may only pursue if its definition allows it. A merchant
     * with no CAN_FIGHT never even considers COMBAT, so the decision engine
     * cannot pick a goal the NPC has no way to carry out.
     */
    private static final Map<GoalType, NPCCapability> REQUIRED_CAPABILITIES =
            new EnumMap<>(Map.of(
                    GoalType.COMBAT, NPCCapability.CAN_FIGHT,
                    GoalType.PROTECT, NPCCapability.CAN_FIGHT,
                    GoalType.PATROL, NPCCapability.CAN_PATROL,
                    GoalType.TALK, NPCCapability.CAN_TALK));

    private final DecisionEngine decisionEngine;

    private final BehaviorRegistry behaviors;

    private final ActionExecutor actionExecutor;

    private final List<Task> activeTasks = new ArrayList<>();

    /**
     * Work pushed in from outside the goal loop. Concurrent because it is
     * filled by event handlers and drained by the tick loop.
     */
    private final Queue<Task> pendingTasks = new ConcurrentLinkedQueue<>();

    /**
     * Candidate goals are built once from the definition instead of being
     * reallocated every tick. For 50 NPCs at 20 tps that was thousands of
     * short-lived objects per second describing a list that never changes.
     */
    private List<Goal> candidateGoals;

    public DefaultBrain() {
        this(new UtilityDecisionEngine(), new BehaviorRegistry(), new DefaultActionExecutor());
    }

    public DefaultBrain(DecisionEngine decisionEngine,
                        BehaviorRegistry behaviors,
                        ActionExecutor actionExecutor) {
        this.decisionEngine = decisionEngine;
        this.behaviors = behaviors;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void submit(Task task) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();

        if (task != null) {
            pendingTasks.add(task);
        }
    }

    @Override
    public boolean hasPendingWork() {
        return !pendingTasks.isEmpty();
    }

    @Override
    public void tick(NPCContext context, WorldContext world, NPCRuntime runtime, NPCController controller) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        if (!runtime.isActive()) return;

        Goal chosen = decisionEngine.decide(context, world, candidates(runtime, world));
        Goal previous = runtime.getCurrentGoal();

        if (!chosen.equals(previous)) {
            activeTasks.forEach(Task::cancel);
            activeTasks.clear();
            runtime.setCurrentGoal(chosen);
            runtime.setState(stateFor(chosen.getType()));

            SamuraiLogger.BRAIN.debug("{} cambia de objetivo: {} -> {}",
                    runtime.getName(), previous, chosen);
        }

        boolean interrupted = drainPendingInto(activeTasks);
        // Only plan routine work when nothing urgent was just injected;
        // otherwise a player who spoke would be queued behind a fresh patrol.
        if (activeTasks.isEmpty() && !interrupted) {
            Behavior behavior = behaviors.forGoal(chosen.getType());
            activeTasks.addAll(behavior.plan(context, world, chosen));
        }

        runTasks(context, world, runtime, controller);

        if (SamuraiSettings.emotionDecay()) {
            yadi.samuraiai.emotion.EmotionService.getInstance().decay(runtime);
        }
    }

    /**
     * Iterating with an explicit iterator, rather than collecting finished
     * tasks and calling removeAll, avoids removing a different task that
     * merely happens to be equal to a finished one.
     */
    private void runTasks(NPCContext context, WorldContext world,
                          NPCRuntime runtime, NPCController controller) {

        Iterator<Task> iterator = activeTasks.iterator();

        while (runtime.isActive() && iterator.hasNext()) {

            Task task = iterator.next();
            TaskStatus status;

            try {
                status = task.tick(context, world, runtime, controller, actionExecutor);
            } catch (RuntimeException e) {
                // One broken task must not stop the NPC from thinking again
                // next tick, nor take the whole tick loop down with it.
                SamuraiLogger.BRAIN.error("Tarea {} fallo en {}: {}",
                        task.getClass().getSimpleName(), runtime.getName(), e.toString());
                iterator.remove();
                continue;
            }

            if (status != TaskStatus.RUNNING) {
                iterator.remove();
            } else break;
        }
    }

    /**
     * Injected tasks go to the front so a player who just spoke is answered
     * before the NPC resumes its routine.
     *
     * @return true if anything was injected this tick
     */
    private boolean drainPendingInto(List<Task> target) {

        Task task;
        int index = 0;

        while ((task = pendingTasks.poll()) != null) {
            target.add(index++, task);
        }

        return index > 0;
    }

    /**
     * The goals the NPC may pursue: those its definition allows, plus the one the scheduler advises when the definition does not
     * list it (a merchant's day includes eating and sleeping whether or not its definition says so).
     */
    private List<Goal> candidates(NPCRuntime runtime, WorldContext world) {
        List<Goal> base = candidates(runtime);
        GoalType advised = world == null ? null : world.advice().map(yadi.samuraiai.ai.scheduler.world.GoalMapper::goalFor).orElse(null);
        if (advised == null) return base;
        for (Goal goal : base) if (goal.getType() == advised) return base;
        List<Goal> extended = new ArrayList<>(base);
        extended.add(new Goal(advised));
        return extended;
    }

    private List<Goal> candidates(NPCRuntime runtime) {

        if (candidateGoals == null) {

            List<Goal> built = new ArrayList<>();

            for (GoalType type : runtime.getDefinition().getDefaultGoals()) {

                NPCCapability required = REQUIRED_CAPABILITIES.get(type);

                if (required == null || runtime.hasCapability(required)) {
                    built.add(new Goal(type));
                }
            }

            if (built.isEmpty()) {
                built.add(new Goal(GoalType.IDLE));
            }

            candidateGoals = List.copyOf(built);
        }

        return candidateGoals;
    }

    private static NPCState stateFor(GoalType type) {
        return switch (type) {
            case PATROL -> NPCState.PATROLLING;
            case COMBAT -> NPCState.COMBAT;
            case FLEE -> NPCState.FLEEING;
            case TALK -> NPCState.TALKING;
            case REST, SLEEP, MEDITATE -> NPCState.RESTING;
            case INVESTIGATE -> NPCState.INVESTIGATING;
            default -> NPCState.IDLE;
        };
    }

    @Override public void cancelAll() {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        for (Task task : activeTasks) {
            try { task.cancel(); } catch (RuntimeException error) { SamuraiLogger.BRAIN.warn("Task cancellation failed", error); }
        }
        activeTasks.clear();
        Task pending;
        while ((pending = pendingTasks.poll()) != null) {
            try { pending.cancel(); } catch (RuntimeException error) { SamuraiLogger.BRAIN.warn("Task cancellation failed", error); }
        }
    }
}
