package yadi.samuraiai.ai.scheduler.routine;

import yadi.samuraiai.ai.scheduler.engine.Intent;
import yadi.samuraiai.ai.scheduler.priority.PriorityLayer;
import yadi.samuraiai.ai.scheduler.zone.Place;

/**
 * One NPC's running routine (or response): where it stands, how far along it is, and how long it is meant to last. A routine
 * travels to its place first and only starts to count (and to spend or restore energy) once the NPC is there. A response is
 * open-ended and lasts as long as its trigger keeps being seen.
 */
public final class RoutineInstance {
    public enum State { TRAVELLING, ACTIVE, PAUSED, SUSPENDED, COMPLETED, CANCELLED }

    private final Intent baseIntent;
    private Place place;
    private final PriorityLayer layer;
    private final long startedTick;
    private final int plannedTicks;
    private final String reason;
    private State state = State.TRAVELLING;
    private long arrivedTick = -1;
    private long lastSeenTick;
    private long enteredTick;
    private int doneTicks;
    private int restarts;

    public RoutineInstance(Intent intent, PriorityLayer layer, long now, int plannedTicks, String reason) {
        this.baseIntent = intent;
        this.place = intent.place();
        this.layer = layer;
        this.startedTick = now;
        this.plannedTicks = Math.max(1, plannedTicks);
        this.reason = reason;
        this.lastSeenTick = now;
        this.enteredTick = now;
    }

    public Intent intent() { return baseIntent; }
    public String key() { return baseIntent.key(); }
    public RoutineType routine() { return baseIntent.routine(); }
    public Place place() { return place; }
    public PriorityLayer layer() { return layer; }
    public State state() { return state; }
    public long startedTick() { return startedTick; }
    public long enteredTick() { return enteredTick; }
    public long arrivedTick() { return arrivedTick; }
    public long lastSeenTick() { return lastSeenTick; }
    public int plannedTicks() { return plannedTicks; }
    public int doneTicks() { return doneTicks; }
    public int restarts() { return restarts; }
    public String reason() { return reason; }
    public boolean finished() { return state == State.COMPLETED || state == State.CANCELLED; }
    /** Whether the routine's own effects apply: it has begun, or it is a patrol, which is performed by walking. */
    public boolean performing() { return state == State.ACTIVE || (baseIntent.routine() == RoutineType.PATROL && state == State.TRAVELLING); }

    public double progress() { return baseIntent.isResponse() ? 0.0D : Math.min(1.0D, doneTicks / (double) plannedTicks); }
    public void retarget(Place next) { if (next != null) this.place = next; }
    public void seen(long now) { lastSeenTick = now; }
    public void markArrived(long now) { if (state == State.TRAVELLING) { state = State.ACTIVE; arrivedTick = now; } }
    public void markDeparted() { if (state == State.ACTIVE) state = State.TRAVELLING; }

    /** Adds performed time; returns true once the routine has run its planned duration. */
    public boolean advance(long ticks) {
        if (!performing() || baseIntent.isResponse()) return false;
        doneTicks += (int) Math.min(ticks, Integer.MAX_VALUE - doneTicks);
        return doneTicks >= plannedTicks;
    }

    public void pause(boolean suspended, long now) { state = suspended ? State.SUSPENDED : State.PAUSED; enteredTick = now; }
    /** Continues after an interruption: travel again if the NPC was moved away, otherwise straight back to work. */
    public void resume(long now) { state = arrivedTick >= 0 ? State.ACTIVE : State.TRAVELLING; enteredTick = now; lastSeenTick = now; }
    public void restart(long now) { doneTicks = 0; arrivedTick = -1; state = State.TRAVELLING; enteredTick = now; lastSeenTick = now; restarts++; }
    public void complete() { state = State.COMPLETED; }
    public void cancel() { state = State.CANCELLED; }
}
