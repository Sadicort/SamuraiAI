package yadi.samuraiai.decision;

import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.awareness.ThreatLevel;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.perception.PerceivedEntity;

import java.util.Comparator;
import java.util.List;

/**
 * Scores each candidate goal from its base priority plus modifiers drawn from
 * emotion and perception, instead of a fixed if/else chain.
 *
 * <p>Scoring is intentionally readable rather than clever: each consideration
 * is a named method returning a signed adjustment, so a new one is added
 * without touching the others and the reason an NPC chose a goal can be read
 * straight off the code.
 */
public class UtilityDecisionEngine implements DecisionEngine {

    /** A player this close is worth reacting to rather than ignoring. */
    private static final double SOCIAL_DISTANCE = 8.0D;

    @Override
    public Goal decide(NPCContext context, WorldContext world, List<Goal> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            return new Goal(GoalType.IDLE, 0, "Nada que hacer");
        }

        return candidates.stream()
                .max(Comparator.comparingInt(goal -> score(goal, context, world)))
                .orElseGet(() -> candidates.get(0));
    }

    /** Exposed for tests and for logging why a goal won. */
    public int score(Goal goal, NPCContext context, WorldContext world) {

        int score = goal.getBasePriority();

        score += emotionModifier(goal.getType(), context);
        score += perceptionModifier(goal.getType(), world);
        if (world != null) score += world.snapshot().map(snapshot -> evidenceModifier(goal.getType(), snapshot)).orElse(0);
        if (world != null) score += world.advice().map(advice -> scheduleModifier(goal.getType(), advice)).orElse(0);
        if (world != null) score += world.cognition().map(advice -> cognitionModifier(goal.getType(), advice)).orElse(0);

        return score;
    }

    private static int emotionModifier(GoalType type, NPCContext context) {

        if (context == null || context.getEmotionState() == null) {
            return 0;
        }

        int fear = context.getEmotionState().get(Emotion.FEAR);
        int anger = context.getEmotionState().get(Emotion.ANGER);
        int calm = context.getEmotionState().get(Emotion.CALM);

        return switch (type) {
            // A frightened NPC runs; an angry one is less inclined to.
            case FLEE -> fear - anger / 2;
            // Fear suppresses fighting, anger drives it.
            case COMBAT -> anger - fear;
            case PROTECT -> anger / 2 - fear / 2;
            // Nobody chats while terrified.
            case TALK -> -fear;
            // Routine only appeals to a settled mind.
            case PATROL, REST -> calm / 4 - fear / 2;
            default -> 0;
        };
    }

    /**
     * Turns the perception engine's evidence into score adjustments. Perception never chooses a goal: it reports threat,
     * suspicion, awareness and something worth investigating, and the decision engine weighs them like any other input.
     */
    static int evidenceModifier(GoalType type, PerceptionSnapshot s) {
        int modifier = 0;
        switch (s.threatLevel()) {
            case CRITICAL -> modifier += switch (type) { case FLEE -> 50; case COMBAT -> 40; case PROTECT -> 20; case TALK -> -60; case REST -> -50; case PATROL -> -30; case INVESTIGATE -> -20; default -> 0; };
            case DANGER -> modifier += switch (type) { case FLEE -> 30; case COMBAT -> 25; case PROTECT -> 15; case TALK -> -50; case REST -> -40; case PATROL -> -20; default -> 0; };
            case WARNING -> modifier += switch (type) { case FLEE -> 10; case COMBAT -> 8; case PROTECT -> 8; case TALK -> -15; case REST -> -15; default -> 0; };
            case SAFE -> { }
        }
        boolean curious = s.suspicious() || s.awareness() == AwarenessLevel.SEARCHING || s.awareness() == AwarenessLevel.ALERT || s.investigationTarget().isPresent();
        if (curious && s.threatLevel().compareTo(ThreatLevel.DANGER) < 0) {
            modifier += switch (type) { case INVESTIGATE -> 30 + (s.investigationTarget().isPresent() ? 10 : 0); case PATROL -> -10; case REST -> -20; default -> 0; };
        }
        if (s.awareness().atLeast(AwarenessLevel.TRACKING)) modifier += switch (type) { case PROTECT, COMBAT -> 10; case TALK -> -20; default -> 0; };
        if (type == GoalType.INVESTIGATE && !s.interests().isEmpty() && s.interests().get(0).score() >= 40) modifier += 10;
        return modifier;
    }

    /**
     * The scheduler's advice as a score adjustment: the goal it names gains a bonus by priority layer, and a routine that needs
     * the NPC's undivided attention (sleep, meditation, prayer) makes small talk unattractive. The scheduler never picks a goal;
     * it only leans on the scale, and evidence of danger (above) can still outweigh it.
     */
    static int scheduleModifier(GoalType type, yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice advice) {
        GoalType wanted = yadi.samuraiai.ai.scheduler.world.GoalMapper.goalFor(advice);
        int modifier = 0;
        if (wanted == type) modifier += yadi.samuraiai.ai.scheduler.world.GoalMapper.bonus(advice);
        if (type == GoalType.TALK && (wanted == GoalType.SLEEP || wanted == GoalType.MEDITATE || wanted == GoalType.PRAYER)) modifier -= 60;
        // FLEE and COMBAT carry a high flat priority that made an NPC with no reason to fear or fight prefer them to its whole day.
        // While the scheduler is advising something else they must earn their place from evidence (threat, fear) like any other goal.
        if ((type == GoalType.FLEE || type == GoalType.COMBAT) && wanted != type) modifier -= 50;
        return modifier;
    }

    /**
     * What the NPC's inner life and history add to a goal's score: the people around it that it trusts, fears or has grudges against,
     * a place it knows to be dangerous, a tradition due now, and its mood. Like the scheduler's advice this only leans on the scale:
     * a threat in front of it (above) still outweighs a memory. The total is capped so that history colours a decision without deciding it.
     */
    static int cognitionModifier(GoalType type, yadi.samuraiai.ai.cognition.engine.CognitiveAdvice a) {
        int m = 0;
        if (a.anyHostile()) m += switch (type) { case FLEE -> 14; case COMBAT -> 4; case TALK -> -20; case REST -> -12; case PATROL -> -4; case INVESTIGATE -> -4; default -> 0; };
        if (a.anyFriendly()) m += switch (type) { case TALK -> 14; case PROTECT -> 8; case SOCIAL -> 8; case FLEE -> -6; default -> 0; };
        if (a.dangerNearby() > 0) m += switch (type) { case FLEE -> (int) Math.round(6 * a.dangerNearby()); case INVESTIGATE -> (int) Math.round(-10 * a.dangerNearby()); case REST -> (int) Math.round(-10 * a.dangerNearby()); case PATROL -> (int) Math.round(-4 * a.dangerNearby()); default -> 0; };
        if (!a.ritual().isEmpty()) m += switch (a.ritualZoneKind()) {
            case "TEMPLE" -> type == GoalType.MEDITATE || type == GoalType.PRAYER ? 15 : 0;
            case "GUARD_POST" -> type == GoalType.GUARD ? 12 : 0;
            case "MARKET" -> type == GoalType.TRADE ? 10 : 0;
            case "TRAINING" -> type == GoalType.TRAINING ? 12 : 0;
            default -> 0;
        };
        m += switch (a.mood()) {
            case MELANCHOLIC -> switch (type) { case TALK, SOCIAL -> -8; case REST -> 6; case MEDITATE -> 4; default -> 0; };
            case HAPPY, INSPIRED -> switch (type) { case TALK, SOCIAL -> 6; case TRAINING -> 3; default -> 0; };
            case EXHAUSTED -> type == GoalType.REST || type == GoalType.SLEEP ? 12 : type == GoalType.WORK || type == GoalType.TRAINING ? -6 : 0;
            case ALERT -> type == GoalType.PATROL || type == GoalType.INVESTIGATE || type == GoalType.GUARD ? 4 : 0;
            case ANGRY -> type == GoalType.COMBAT ? 4 : type == GoalType.TALK ? -6 : 0;
            case FEARFUL -> type == GoalType.FLEE ? 6 : type == GoalType.COMBAT || type == GoalType.TALK ? -6 : 0;
            default -> 0;
        };
        return Math.max(-30, Math.min(30, m));
    }

    private static int perceptionModifier(GoalType type, WorldContext world) {

        if (world == null) {
            return 0;
        }

        List<PerceivedEntity> perceived = world.getPerceivedEntities();

        if (perceived.isEmpty()) {
            // With nobody around there is nothing to talk to or guard against.
            return switch (type) {
                case TALK -> -40;
                case REST, PATROL -> 10;
                default -> 0;
            };
        }

        boolean playerClose = perceived.stream()
                .anyMatch(entity -> entity.player() && entity.distance() <= SOCIAL_DISTANCE);

        return switch (type) {
            case TALK -> playerClose ? 30 : 5;
            case PROTECT -> playerClose ? 15 : 0;
            case INVESTIGATE -> 10;
            case REST -> playerClose ? -15 : 0;
            default -> 0;
        };
    }
}
