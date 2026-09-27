package yadi.samuraiai.ai.scheduler.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import yadi.samuraiai.ai.scheduler.cooldown.CooldownEngine;
import yadi.samuraiai.ai.scheduler.emotion.EmotionScheduler;
import yadi.samuraiai.ai.scheduler.energy.EnergyState;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptStack;
import yadi.samuraiai.ai.scheduler.lifestyle.Lifestyle;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;

/** Everything the scheduler remembers about one NPC between evaluations: character, condition, running routine, interrupt stack, cooldowns. */
public final class NpcSchedule {
    final UUID id;
    final String typeId;
    final Lifestyle lifestyle;
    final int shift;
    final long seed;
    PersonalityTraits traits;
    final EnergyState energy = new EnergyState();
    final CooldownEngine cooldowns = new CooldownEngine();
    final InterruptStack stack;
    final EmotionScheduler.Tracker mood = new EmotionScheduler.Tracker();
    RoutineInstance current;
    long resumeAt;
    long lastEvaluated;
    boolean evaluatedOnce;
    long bucketTick = Long.MIN_VALUE / 2;
    TickBucket bucket = TickBucket.FAR;
    int patrolIndex;
    SchedulerAdvice advice;
    SchedulerInput lastInput;
    Light light;
    boolean forced;
    final Map<String, Long> pending = new HashMap<>();
    List<Candidate> lastCandidates = List.of();
    String lastConflictWinner = "";
    long routinesDone, interruptions;

    NpcSchedule(UUID id, String typeId, Lifestyle lifestyle, PersonalityTraits traits, int shift, Supplier<SchedulerSettings> settings, long lastEvaluated) {
        this.id = id; this.typeId = typeId; this.lifestyle = lifestyle; this.traits = traits; this.shift = shift;
        this.seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
        this.stack = new InterruptStack(settings);
        this.lastEvaluated = lastEvaluated;
    }

    public UUID id() { return id; }
    public String typeId() { return typeId; }
    public Lifestyle lifestyle() { return lifestyle; }
    public int shift() { return shift; }
    public PersonalityTraits traits() { return traits; }
    public EnergyState energy() { return energy; }
    public CooldownEngine cooldowns() { return cooldowns; }
    public InterruptStack stack() { return stack; }
    public EmotionScheduler.Tracker mood() { return mood; }
    public RoutineInstance current() { return current; }
    public TickBucket bucket() { return bucket; }
    public SchedulerAdvice advice() { return advice; }
    public SchedulerInput lastInput() { return lastInput; }
    public Light light() { return light; }
    public List<Candidate> lastCandidates() { return lastCandidates; }
    public long lastEvaluated() { return lastEvaluated; }
    public long routinesDone() { return routinesDone; }
    public long interruptions() { return interruptions; }
    public void replaceTraits(PersonalityTraits next) { this.traits = next; }
    /** Makes the next tick evaluate this NPC regardless of its interval (something just happened to it). */
    public void force() { this.forced = true; }
}
