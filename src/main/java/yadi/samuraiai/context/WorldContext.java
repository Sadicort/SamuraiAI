package yadi.samuraiai.context;

import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.cognition.engine.CognitiveAdvice;
import yadi.samuraiai.ai.scheduler.engine.SchedulerAdvice;
import yadi.samuraiai.perception.PerceivedEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * What the NPC currently perceives about the world around it — deliberately
 * not "the real state of the world".
 *
 * <p>Produced by {@link yadi.samuraiai.perception.PerceptionSystem} and
 * consumed by the DecisionEngine and Behaviors, so an NPC can only ever act
 * on what it actually noticed.
 */
public class WorldContext {

    private static final WorldContext EMPTY = new WorldContext(List.of(), 0L);

    private final List<PerceivedEntity> perceivedEntities;

    private final long worldTime;

    /** What this NPC's senses concluded, when the perception engine produced one; empty for the legacy nearest-player perception. */
    private final PerceptionSnapshot snapshot;

    private final SchedulerAdvice advice;

    /** What the cognitive layer says about the NPC's inner life and the people and places around it; null when that layer is off. */
    private final CognitiveAdvice cognition;

    public WorldContext(List<PerceivedEntity> perceivedEntities, long worldTime) {
        this(perceivedEntities, worldTime, null);
    }

    public WorldContext(List<PerceivedEntity> perceivedEntities, long worldTime, PerceptionSnapshot snapshot) {
        this(perceivedEntities, worldTime, snapshot, null);
    }

    public WorldContext(List<PerceivedEntity> perceivedEntities, long worldTime, PerceptionSnapshot snapshot, SchedulerAdvice advice) {
        this(perceivedEntities, worldTime, snapshot, advice, null);
    }

    public WorldContext(List<PerceivedEntity> perceivedEntities, long worldTime, PerceptionSnapshot snapshot, SchedulerAdvice advice, CognitiveAdvice cognition) {
        this.perceivedEntities = perceivedEntities == null ? List.of() : List.copyOf(perceivedEntities);
        this.worldTime = worldTime;
        this.snapshot = snapshot;
        this.advice = advice;
        this.cognition = cognition;
    }

    /** What the behavior scheduler advises for this NPC right now (its routine or response and where); empty when the scheduler is off. */
    public Optional<SchedulerAdvice> advice() {
        return Optional.ofNullable(advice);
    }

    /** The same perception with the scheduler's advice attached. */
    public WorldContext withAdvice(SchedulerAdvice value) {
        return new WorldContext(perceivedEntities, worldTime, snapshot, value, cognition);
    }

    /** The cognitive layer's advice (mood, familiar people nearby, known dangers, tradition due); empty when that layer is off. */
    public Optional<CognitiveAdvice> cognition() {
        return Optional.ofNullable(cognition);
    }

    /** The same perception with the cognitive advice attached. */
    public WorldContext withCognition(CognitiveAdvice value) {
        return new WorldContext(perceivedEntities, worldTime, snapshot, advice, value);
    }

    public Optional<PerceptionSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    /** Shared instance: an empty context is immutable and allocated constantly. */
    public static WorldContext empty() {
        return EMPTY;
    }

    public List<PerceivedEntity> getPerceivedEntities() {
        return perceivedEntities;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public List<PerceivedEntity> getPlayers() {
        return perceivedEntities.stream().filter(PerceivedEntity::player).toList();
    }

    public Optional<PerceivedEntity> nearest() {
        return perceivedEntities.stream().min(Comparator.comparingDouble(PerceivedEntity::distance));
    }

    public Optional<PerceivedEntity> nearestPlayer() {
        return perceivedEntities.stream()
                .filter(PerceivedEntity::player)
                .min(Comparator.comparingDouble(PerceivedEntity::distance));
    }

    public boolean isEmpty() {
        return perceivedEntities.isEmpty();
    }
}
