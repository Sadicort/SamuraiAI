package yadi.samuraiai.ai.scheduler.interrupt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;

/**
 * The interrupt stack: routines set aside by something more important, most recent on top. When the interruption ends the
 * top frame is resumed according to its policy: PAUSE and RESUME continue where they left off (PAUSE only within a time
 * limit), SUSPEND only if the interruption was short, RESTART starts over. CANCEL never enters the stack. The stack has a
 * depth limit; the oldest frame is dropped when it overflows.
 */
public final class InterruptStack {
    /** Outcome of a resume attempt: the routine to continue (if any) and the frames that lapsed on the way. */
    public record Resumed(RoutineInstance instance, InterruptFrame frame, List<InterruptFrame> lapsed) { }

    private final Deque<InterruptFrame> frames = new ArrayDeque<>();
    private final Supplier<SchedulerSettings> settings;

    public InterruptStack(Supplier<SchedulerSettings> settings) { this.settings = settings; }

    public int size() { return frames.size(); }
    public boolean isEmpty() { return frames.isEmpty(); }
    public List<InterruptFrame> frames() { return List.copyOf(frames); }
    public void clear() { frames.clear(); }

    /**
     * Sets a routine aside. Returns the frames dropped to respect the depth limit (the routine itself is in the result only
     * when its policy is CANCEL, in which case it is not stacked).
     */
    public List<InterruptFrame> push(RoutineInstance instance, InterruptPolicy policy, String by, long now) {
        List<InterruptFrame> dropped = new ArrayList<>();
        long expires = switch (policy) {
            case CANCEL -> now;
            case SUSPEND -> now + settings.get().suspendMaxTicks();
            case PAUSE -> now + settings.get().pauseMaxTicks();
            case RESUME, RESTART -> Long.MAX_VALUE;
        };
        InterruptFrame frame = new InterruptFrame(instance, policy, by, now, expires);
        if (policy == InterruptPolicy.CANCEL) { dropped.add(frame); return dropped; }
        frames.push(frame);
        while (frames.size() > settings.get().maxInterruptDepth()) dropped.add(frames.removeLast());
        return dropped;
    }

    /** The routine that would be resumed now, without removing it. */
    public Optional<InterruptFrame> top(long now) {
        for (InterruptFrame f : frames) if (!f.expired(now)) return Optional.of(f);
        return Optional.empty();
    }

    /** Resumes the most recent frame that is still valid; frames that lapsed are discarded and reported. */
    public Optional<Resumed> resume(long now) {
        List<InterruptFrame> lapsed = new ArrayList<>();
        while (!frames.isEmpty()) {
            InterruptFrame f = frames.pop();
            if (f.expired(now)) { lapsed.add(f); continue; }
            if (f.policy() == InterruptPolicy.RESTART) f.instance().restart(now);
            return Optional.of(new Resumed(f.instance(), f, lapsed));
        }
        return lapsed.isEmpty() ? Optional.empty() : Optional.of(new Resumed(null, null, lapsed));
    }

    /** Discards frames whose time has run out. */
    public List<InterruptFrame> expire(long now) {
        List<InterruptFrame> lapsed = new ArrayList<>();
        frames.removeIf(f -> { boolean gone = f.expired(now); if (gone) lapsed.add(f); return gone; });
        return lapsed;
    }
}
