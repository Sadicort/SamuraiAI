package yadi.samuraiai.npc.lifecycle;

import java.util.*;
import yadi.samuraiai.runtime.ServerScheduler;
import yadi.samuraiai.logging.SamuraiLogger;

public final class NPCLifecycleManager {
    private static final NPCLifecycleManager INSTANCE = new NPCLifecycleManager();
    public static NPCLifecycleManager getInstance() { return INSTANCE; }
    private final Map<UUID, NPCLifecycleState> states = new HashMap<>();
    private final ArrayDeque<NPCLifecycleTransition> history = new ArrayDeque<>();
    private long transitions, rejected;
    public synchronized void transition(UUID id, NPCLifecycleState next) {
        ServerScheduler.getInstance().requireServerThread();
        Objects.requireNonNull(id); Objects.requireNonNull(next);
        NPCLifecycleState old = states.get(id);
        boolean valid = old == null ? next == NPCLifecycleState.UNINITIALIZED || next == NPCLifecycleState.CREATING : switch (old) {
            case UNINITIALIZED -> next == NPCLifecycleState.CREATING || next == NPCLifecycleState.INVALID;
            case CREATING -> oneOf(next, NPCLifecycleState.INITIALIZING, NPCLifecycleState.REMOVING, NPCLifecycleState.ERROR, NPCLifecycleState.REMOVED);
            case INITIALIZING -> oneOf(next, NPCLifecycleState.LOADING_RUNTIME, NPCLifecycleState.ACTIVE, NPCLifecycleState.REMOVING, NPCLifecycleState.ERROR, NPCLifecycleState.REMOVED);
            case LOADING_RUNTIME -> oneOf(next, NPCLifecycleState.ACTIVE, NPCLifecycleState.REMOVING, NPCLifecycleState.ERROR);
            case ACTIVE -> oneOf(next, NPCLifecycleState.IDLE, NPCLifecycleState.PAUSED, NPCLifecycleState.SLEEPING,
                    NPCLifecycleState.HIBERNATING, NPCLifecycleState.UNLOADING, NPCLifecycleState.REMOVING);
            case IDLE -> oneOf(next, NPCLifecycleState.ACTIVE, NPCLifecycleState.PAUSED, NPCLifecycleState.SLEEPING,
                    NPCLifecycleState.HIBERNATING, NPCLifecycleState.UNLOADING, NPCLifecycleState.REMOVING);
            case PAUSED -> oneOf(next, NPCLifecycleState.ACTIVE, NPCLifecycleState.UNLOADING, NPCLifecycleState.REMOVING);
            case SLEEPING, HIBERNATING -> oneOf(next, NPCLifecycleState.ACTIVE, NPCLifecycleState.INITIALIZING,
                    NPCLifecycleState.UNLOADING, NPCLifecycleState.REMOVING);
            case UNLOADING -> oneOf(next, NPCLifecycleState.INACTIVE, NPCLifecycleState.REMOVING, NPCLifecycleState.REMOVED);
            case INACTIVE -> oneOf(next, NPCLifecycleState.INITIALIZING, NPCLifecycleState.REMOVING, NPCLifecycleState.REMOVED);
            case REMOVING -> oneOf(next, NPCLifecycleState.REMOVED, NPCLifecycleState.ERROR);
            case ERROR -> oneOf(next, NPCLifecycleState.INITIALIZING, NPCLifecycleState.REMOVING, NPCLifecycleState.REMOVED);
            case INVALID -> oneOf(next, NPCLifecycleState.REMOVING, NPCLifecycleState.REMOVED);
            case REMOVED -> false;
        };
        if (!valid) { rejected++; throw new IllegalStateException("Invalid lifecycle " + old + " -> " + next); }
        states.put(id, next);
        NPCLifecycleTransition transition = new NPCLifecycleTransition(id, old, next, java.time.Instant.now());
        history.addLast(transition); while (history.size() > 256) history.removeFirst(); transitions++;
        SamuraiLogger.LIFECYCLE.debug("npc={} lifecycle={} previous={}", id, next, old);
        yadi.samuraiai.event.NPCEventBus.getInstance().post(new yadi.samuraiai.event.npc.NPCLifecycleTransitionEvent(transition));
    }
    private static boolean oneOf(NPCLifecycleState value, NPCLifecycleState... allowed) {
        for (NPCLifecycleState candidate : allowed) if (value == candidate) return true;
        return false;
    }
    public synchronized NPCLifecycleState getState(UUID id) { return states.getOrDefault(id, NPCLifecycleState.REMOVED); }
    public synchronized void forget(UUID id) {
        ServerScheduler.getInstance().requireServerThread();
        if (states.containsKey(id) && states.get(id) != NPCLifecycleState.REMOVED)
            throw new IllegalStateException("Only removed NPCs can be forgotten");
        states.remove(id);
    }
    public synchronized List<NPCLifecycleTransition> history() { return List.copyOf(history); }
    public synchronized LifecycleMetrics metrics() {
        Map<NPCLifecycleState, Long> current = new EnumMap<>(NPCLifecycleState.class);
        states.values().forEach(state -> current.merge(state, 1L, Long::sum));
        return new LifecycleMetrics(transitions, rejected, current);
    }
    public synchronized void clear() { ServerScheduler.getInstance().requireServerThread(); states.clear(); history.clear(); transitions = 0; rejected = 0; }
}
