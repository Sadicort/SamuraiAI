package yadi.samuraiai.ai.scheduler.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import yadi.samuraiai.ai.scheduler.emotion.EmotionInfluence;
import yadi.samuraiai.ai.scheduler.events.BehaviorConflictResolvedEvent;
import yadi.samuraiai.ai.scheduler.events.EmotionPriorityChangedEvent;
import yadi.samuraiai.ai.scheduler.events.PersonalityUpdatedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineCompletedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineInterruptedEvent;
import yadi.samuraiai.ai.scheduler.events.RoutineStartedEvent;
import yadi.samuraiai.ai.scheduler.group.Alarm;
import yadi.samuraiai.ai.scheduler.group.GroupOrder;
import yadi.samuraiai.ai.scheduler.group.GroupRole;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptFrame;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptPolicy;
import yadi.samuraiai.ai.scheduler.metrics.SchedulerMetrics;
import yadi.samuraiai.ai.scheduler.optimize.TickBucket;
import yadi.samuraiai.ai.scheduler.personality.DriftCause;
import yadi.samuraiai.ai.scheduler.personality.PersonalityEngine;
import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.personality.Trait;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.priority.Selection;
import yadi.samuraiai.ai.scheduler.response.EventResponsePlanner;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutinePlanner;
import yadi.samuraiai.ai.scheduler.routine.RoutineProfile;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.stack.BehaviorStack;
import yadi.samuraiai.ai.scheduler.time.CalendarEvent;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.zone.Place;
import yadi.samuraiai.ai.scheduler.zone.PlaceResolver;
import yadi.samuraiai.ai.scheduler.zone.ZoneScheduler;
import yadi.samuraiai.event.EventSink;

/**
 * The NPC tier of the hierarchy: one evaluation of one NPC's schedule. It brings the NPC's condition and mood up to date for
 * the time that has passed, follows the running routine (arrival, progress, completion, timeout), gathers what the NPC could
 * do (its routines, needs, moods, responses to events, its group's order), lets the priority engine pick, and carries out the
 * switch: interrupting through the stack, resuming, or starting afresh. The result is advice; nothing here moves an entity.
 */
public final class NpcScheduler {
    /** Everything from outside the NPC that an evaluation needs; implemented by {@link BehaviorScheduler}. */
    public interface Environment {
        DayPeriod period(long worldTime);
        List<CalendarEvent> activeEvents();
        PlaceResolver places();
        ZoneScheduler zones();
        Optional<GroupOrder> groupOrder(UUID npc);
        Optional<Alarm> alarm(UUID npc, String dimension, double x, double z, long now);
        GroupRole role(UUID npc);
        String groupId(UUID npc);
        void raiseAlarm(UUID npc, EventResponsePlanner.AlarmRequest request, String dimension, long now);
        List<double[]> neighbours(UUID npc, String dimension, double x, double z, double radius);
        EventSink events();
        SchedulerMetrics metrics();
        /** What outside sources (the living world) add to each routine for this NPC now; empty when there are none. */
        default java.util.Map<RoutineType, Double> externalBias(UUID npc) { return java.util.Map.of(); }
    }

    private static final int PATROL_DWELL_TOLERANCE = 1;
    private final Engines e;

    public NpcScheduler(Engines engines) { this.e = engines; }

    public SchedulerAdvice evaluate(NpcSchedule st, SchedulerInput in, Environment env, long now, TickBucket bucket) {
        SchedulerSettings cfg = e.settings();
        long elapsed = st.evaluatedOnce ? Math.max(0, now - st.lastEvaluated) : 0;
        st.lastEvaluated = now; st.evaluatedOnce = true; st.bucket = bucket; st.lastInput = in; st.forced = false;
        UUID id = in.npcId();
        DayPeriod period = env.period(in.worldTime() + st.shift);

        // condition and mood, brought up to date for the time that passed since the last evaluation
        RoutineInstance running = st.current;
        RoutineProfile profile = running != null && running.routine() != null ? e.profiles().of(running.routine()) : null;
        e.energy().advance(st.energy, profile, running != null && running.performing(), elapsed, 1.0D + 0.3D * st.traits.lean(Trait.DILIGENCE));
        e.emotions().update(st.mood, in.emotion()).ifPresent(change -> {
            env.metrics().moodChanged();
            env.events().publish(new EmotionPriorityChangedEvent(id, change.from().name(), change.to().name(), change.strength()));
        });
        EmotionInfluence influence = e.emotions().influence(st.mood, in.emotion(), st.traits);
        st.energy.addStress(influence.stressPerSecond() * elapsed / 20.0D);

        Optional<GroupOrder> order = env.groupOrder(id);
        if (st.current != null && !st.current.finished()) follow(st, in, env, order.isPresent(), cfg, now, elapsed, period);

        // what could the NPC be doing?
        String groupId = env.groupId(id);
        Function<RoutineType, Place> places = r -> env.places().resolve(r, in.dimension(), in.x(), in.z(), in.home(), id, groupId, period, st.patrolIndex);
        final String runningKey = st.current == null || st.current.finished() ? null : st.current.key();
        List<Candidate> candidates = new ArrayList<>(e.planner().plan(new RoutinePlanner.Input(period, st.lifestyle, st.traits, st.energy, influence,
                env.activeEvents(), st.cooldowns, now, runningKey, places, env.externalBias(id))));
        respond(st, in, env, influence, groupId, candidates, now);
        order.ifPresent(o -> addOrder(st, o, candidates, runningKey));
        boostAlerts(env, candidates, now);
        st.lastCandidates = List.copyOf(candidates);

        // a response that nothing has asked for lately is over
        if (st.current != null && st.current.intent().isResponse() && !st.current.finished()) {
            String key = st.current.key();
            boolean asked = false;
            for (Candidate c : candidates) if (c.key().equals(key)) asked = true;
            if (asked) st.current.seen(now);
            else if (now - st.current.lastSeenTick() >= cfg.responseHoldTicks()) finish(st, "trigger gone", env, cfg, now);
            else candidates.add(new Candidate(st.current.intent(), st.current.layer(), Source.EVENT, e.priority().threshold(st.current.layer()) + 1.0D,
                    "holding after the trigger ended"));
        }

        // choose and carry out the switch
        String currentKey = st.current == null || st.current.finished() ? null : st.current.key();
        long dwell = st.current == null ? 0 : now - st.current.enteredTick();
        Selection selection = e.priority().select(candidates, currentKey, st.current == null ? null : st.current.layer(), dwell);
        if (selection != null && selection.conflict() != null && selection.conflict().contested() && !selection.winner().key().equals(st.lastConflictWinner)) {
            st.lastConflictWinner = selection.winner().key();
            env.metrics().conflictResolved();
            env.events().publish(new BehaviorConflictResolvedEvent(id, selection.winner().key() + " (" + selection.winner().layer() + ")", selection.winner().source().name(),
                    selection.conflict().losers().stream().map(l -> l.candidate().key() + ": " + l.reason()).limit(6).toList()));
        }
        apply(st, in, env, cfg, selection, places, now);
        return advise(st, in, env, period, bucket, order.isPresent(), cfg, now);
    }

    // ------------------------------------------------------------------ following the running routine

    private void follow(NpcSchedule st, SchedulerInput in, Environment env, boolean ordered, SchedulerSettings cfg, long now, long elapsed, DayPeriod period) {
        RoutineInstance cur = st.current;
        Place place = cur.place();
        boolean atPlace = place == null || place.near(in.dimension(), in.x(), in.z(), cfg.arrivalTolerance());
        if (cur.state() == RoutineInstance.State.TRAVELLING && atPlace) {
            cur.markArrived(now);
            if (cur.routine() == RoutineType.PATROL && !ordered) {
                st.patrolIndex++;
                Place next = env.places().resolve(RoutineType.PATROL, in.dimension(), in.x(), in.z(), in.home(), st.id, env.groupId(st.id), period, st.patrolIndex);
                if (next != null) { cur.retarget(next); cur.markDeparted(); }
            }
        } else if (cur.state() == RoutineInstance.State.ACTIVE && place != null && !place.near(in.dimension(), in.x(), in.z(), cfg.arrivalTolerance() * 2.0D + PATROL_DWELL_TOLERANCE)) {
            cur.markDeparted();
        }
        if (cur.advance(elapsed)) { finish(st, "done", env, cfg, now); return; }
        if (cur.routine() != null && cur.state() == RoutineInstance.State.TRAVELLING) {
            long limit = Math.max(1200, e.profiles().of(cur.routine()).maxTicks());
            if (now - cur.enteredTick() > limit) finish(st, "unreachable", env, cfg, now);
        }
    }

    // ------------------------------------------------------------------ candidates from events and the group

    private void respond(NpcSchedule st, SchedulerInput in, Environment env, EmotionInfluence influence, String groupId, List<Candidate> out, long now) {
        UUID id = in.npcId();
        Optional<Alarm> alarm = env.alarm(id, in.dimension(), in.x(), in.z(), now);
        var plan = e.responses().plan(new EventResponsePlanner.Input(in.dimension(), in.x(), in.y(), in.z(), in.perceived(), in.canFight(), env.role(id),
                alarm.orElse(null), in.home(), st.energy, influence, st.traits, groupId != null));
        int delay = e.personality().temperament(st.traits).reactionDelayTicks();
        Set<String> seen = new HashSet<>();
        for (Candidate c : plan.candidates()) {
            seen.add(c.key());
            if (c.layer() != PriorityLayer.EMERGENCY) {
                if (!st.cooldowns.ready(c.key(), now)) continue;
                long first = st.pending.computeIfAbsent(c.key(), k -> now);
                if (now - first < delay) continue;
            }
            out.add(c);
        }
        st.pending.keySet().retainAll(seen);
        if (plan.alarm() != null) env.raiseAlarm(id, plan.alarm(), in.dimension(), now);
    }

    private void addOrder(NpcSchedule st, GroupOrder order, List<Candidate> candidates, String currentKey) {
        double cohesion = 0.5D + 0.25D * (st.traits.lean(Trait.DISCIPLINE) + st.traits.lean(Trait.LOYALTY));
        double best = 0;
        for (Candidate c : candidates) if (c.layer() == PriorityLayer.BASELINE) best = Math.max(best, c.score());
        double score = Math.max(e.settings().baselineThreshold() + 10.0D, best * (0.85D + 0.4D * cohesion) + 5.0D);
        candidates.removeIf(c -> c.layer() == PriorityLayer.BASELINE && c.key().equals(order.intent().key()));
        candidates.add(new Candidate(order.intent(), PriorityLayer.BASELINE, Source.GROUP, score,
                "group " + order.groupId() + " (" + order.role() + ", slot " + order.slot() + ", " + order.formation() + ")"));
    }

    private void boostAlerts(Environment env, List<Candidate> candidates, long now) {
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            Place p = c.intent().place();
            if (c.layer() != PriorityLayer.BASELINE || p == null || p.zoneId() == null || !env.zones().alerted(p.zoneId(), now)) continue;
            RoutineType r = c.intent().routine();
            if (r == RoutineType.GUARD || r == RoutineType.PATROL) candidates.set(i, new Candidate(c.intent(), c.layer(), Source.ZONE, c.score() + 20.0D, c.reason() + ", zone on alert"));
        }
    }

    // ------------------------------------------------------------------ carrying out a decision

    private void apply(NpcSchedule st, SchedulerInput in, Environment env, SchedulerSettings cfg, Selection sel, Function<RoutineType, Place> places, long now) {
        RoutineInstance cur = st.current != null && !st.current.finished() ? st.current : null;
        if (sel != null && cur != null) {
            if (sel.keepCurrent()) {
                if (sel.winner().intent().place() != null) cur.retarget(sel.winner().intent().place());
                return;
            }
            if (sel.emergency()) env.metrics().emergency();
            interruptOrFinish(st, sel.winner(), env, cfg, now, sel.emergency());
            cur = null;
        } else if (sel == null && cur != null) {
            if (cur.intent().isResponse() || now - cur.enteredTick() >= cfg.minRoutineTicks()) finish(st, "no longer wanted", env, cfg, now);
            else return;
            cur = null;
        }
        // nothing running: prefer to resume what an interruption set aside, unless something outranks it
        boolean stacked = !st.stack.isEmpty();
        if (stacked && now >= st.resumeAt) {
            var top = st.stack.top(now);
            if (top.isPresent() && (sel == null || !sel.winner().layer().above(top.get().instance().layer()))) {
                if (resume(st, env, cfg, places, now)) return;
            } else if (top.isEmpty()) {
                st.stack.expire(now).forEach(f -> lapse(f, env, st));
            }
        }
        if (sel != null && !(stacked && now < st.resumeAt && sel.winner().layer() == PriorityLayer.BASELINE)) start(st, sel.winner(), env, cfg, now);
    }

    private void interruptOrFinish(NpcSchedule st, Candidate winner, Environment env, SchedulerSettings cfg, long now, boolean emergency) {
        RoutineInstance cur = st.current;
        boolean higher = winner.layer().above(cur.layer()) || emergency;
        if (!higher) { finish(st, "switched to " + winner.intent().label(), env, cfg, now); return; }
        InterruptPolicy policy = cur.intent().isResponse() ? InterruptPolicy.CANCEL : e.profiles().of(cur.routine()).policy();
        List<InterruptFrame> dropped = st.stack.push(cur, policy, winner.intent().label(), now);
        st.interruptions++;
        env.metrics().routineInterrupted();
        env.events().publish(new RoutineInterruptedEvent(st.id, cur.intent().label(), winner.intent().label(), policy.name(), st.stack.size()));
        if (policy == InterruptPolicy.CANCEL) {
            finish(st, "interrupted by " + winner.intent().label(), env, cfg, now);
        } else {
            cur.pause(policy == InterruptPolicy.SUSPEND, now);
            env.zones().release(st.id);
            st.current = null;
        }
        for (InterruptFrame f : dropped) if (f.instance() != cur) lapse(f, env, st);
    }

    private boolean resume(NpcSchedule st, Environment env, SchedulerSettings cfg, Function<RoutineType, Place> places, long now) {
        var resumed = st.stack.resume(now);
        if (resumed.isEmpty()) return false;
        resumed.get().lapsed().forEach(f -> lapse(f, env, st));
        RoutineInstance inst = resumed.get().instance();
        if (inst == null) return false;
        inst.resume(now);
        if (inst.routine() != null) { Place fresh = places.apply(inst.routine()); if (fresh != null) inst.retarget(fresh); }
        st.current = inst;
        if (inst.place() != null && inst.place().zoneId() != null) env.zones().occupy(inst.place().zoneId(), st.id);
        env.metrics().routineResumed();
        env.events().publish(new RoutineStartedEvent(st.id, inst.intent().label(), inst.layer().name(), describe(inst.place()), "resumed after " + resumed.get().frame().by() + " (" + resumed.get().frame().policy() + ")"));
        return true;
    }

    private void lapse(InterruptFrame frame, Environment env, NpcSchedule st) {
        frame.instance().cancel();
        env.metrics().routineLapsed();
        env.events().publish(new RoutineCompletedEvent(st.id, frame.instance().intent().label(), frame.instance().doneTicks(), "lapsed (" + frame.policy() + ")"));
    }

    private void start(NpcSchedule st, Candidate c, Environment env, SchedulerSettings cfg, long now) {
        Intent intent = c.intent();
        Place place = intent.place();
        if (intent.routine() != null && place != null && c.source() != Source.GROUP) {
            List<double[]> near = env.neighbours(st.id, place.dimension(), place.x(), place.z(), cfg.crowdSpreadRadius());
            if (!near.isEmpty()) {
                double[] spot = e.social().spread(place.x(), place.z(), near, st.traits);
                place = place.withPoint(spot[0], place.y(), spot[1]);
                intent = Intent.of(intent.routine(), place);
            }
        }
        int planned = intent.isResponse() ? Integer.MAX_VALUE / 2 : e.planner().duration(intent.routine(), st.traits, st.seed + now / 1000L);
        st.current = new RoutineInstance(intent, c.layer(), now, planned, c.reason());
        if (place != null && place.zoneId() != null) env.zones().occupy(place.zoneId(), st.id);
        env.metrics().routineStarted();
        env.events().publish(new RoutineStartedEvent(st.id, intent.label(), c.layer().name(), describe(place), c.reason()));
    }

    /** Ends the running routine (done, gave up, cancelled), records its cooldown and character effect, and prepares to resume what it interrupted. */
    private void finish(NpcSchedule st, String outcome, Environment env, SchedulerSettings cfg, long now) {
        RoutineInstance cur = st.current;
        if (cur == null) return;
        boolean done = outcome.equals("done") || outcome.equals("trigger gone");
        if (done) cur.complete(); else cur.cancel();
        st.current = null;
        env.zones().release(st.id);
        env.metrics().routineCompleted();
        st.routinesDone++;
        env.events().publish(new RoutineCompletedEvent(st.id, cur.intent().label(), cur.doneTicks(), outcome));
        if (cur.routine() != null) {
            int cooldown = e.profiles().of(cur.routine()).cooldownTicks();
            st.cooldowns.start(RoutinePlanner.cooldownKey(cur.routine()), now, done ? cooldown : Math.max(cooldown, cfg.routineCooldownTicks()));
        } else {
            st.cooldowns.start(cur.key(), now, cfg.responseCooldownTicks());
        }
        if (done) drift(st, cur, env);
        if (!st.stack.isEmpty()) st.resumeAt = now + cfg.resumeDelayTicks();
    }

    private void drift(NpcSchedule st, RoutineInstance cur, Environment env) {
        DriftCause cause = null;
        if (cur.routine() != null) {
            cause = switch (cur.routine()) {
                case WORK -> DriftCause.TOILED;
                case MEDITATE, PRAYER -> DriftCause.CONTEMPLATED;
                case SOCIAL -> DriftCause.SOCIALISED;
                default -> null;
            };
        } else if (cur.intent().response() == ResponseKind.FLEE) cause = DriftCause.FRIGHTENED;
        else if (cur.intent().response() == ResponseKind.INVESTIGATE) cause = DriftCause.DISCOVERED;
        else if (cur.intent().response() == ResponseKind.ASSIST) cause = DriftCause.TRIUMPHED;
        if (cause != null) applyDrift(st, cause, 1.0D, env);
    }

    /** Lets an experience shape the NPC's character, publishing what changed. */
    public void applyDrift(NpcSchedule st, DriftCause cause, double magnitude, Environment env) {
        PersonalityEngine.DriftResult result = e.personality().drift(st.traits, cause, magnitude);
        if (result.changes().isEmpty()) return;
        st.traits = result.traits();
        for (var change : result.changes()) {
            env.metrics().personalityChanged();
            env.events().publish(new PersonalityUpdatedEvent(st.id, change.trait().name(), change.before(), change.after(), cause.name()));
        }
    }

    // ------------------------------------------------------------------ the answer

    private SchedulerAdvice advise(NpcSchedule st, SchedulerInput in, Environment env, DayPeriod period, TickBucket bucket, boolean ordered, SchedulerSettings cfg, long now) {
        var temperament = e.personality().temperament(st.traits);
        RoutineInstance cur = st.current != null && !st.current.finished() ? st.current : null;
        if (cur == null) {
            SchedulerAdvice idle = SchedulerAdvice.idle(st.id, now, period, st.mood.mood(), temperament, bucket, st.stack.isEmpty() ? "nothing scheduled" : "waiting to resume");
            st.advice = idle;
            return idle;
        }
        boolean nearby = !env.neighbours(st.id, in.dimension(), in.x(), in.z(), cfg.socialDistance()).isEmpty();
        var background = BehaviorStack.compose(new BehaviorStack.Situation(cur, st.mood.mood(), in.perceived().suspicious(), nearby, st.traits.get(Trait.SOCIABILITY) >= 55.0D, ordered));
        var group = env.groupId(st.id);
        String formation = null;
        if (ordered) formation = env.groupOrder(st.id).map(GroupOrder::formation).orElse(null);
        SchedulerAdvice advice = new SchedulerAdvice(st.id, now, cur.routine(), cur.intent().response(), cur.layer(), cur.state(), cur.place(),
                cur.layer() == PriorityLayer.EMERGENCY, scoreOf(st, cur), period, st.mood.mood(), background, cur.reason(), st.stack.size(), temperament, bucket,
                group, env.role(st.id), formation);
        st.advice = advice;
        return advice;
    }

    private double scoreOf(NpcSchedule st, RoutineInstance cur) {
        for (Candidate c : st.lastCandidates) if (c.key().equals(cur.key())) return c.score();
        return 0;
    }

    private static String describe(Place p) { return p == null ? "-" : p.describe(); }
}
