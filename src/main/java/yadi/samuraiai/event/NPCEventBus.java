package yadi.samuraiai.event;

import yadi.samuraiai.logging.SamuraiLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/**
 * Internal publish/subscribe bus for {@link NpcEvent}s (spawned, damaged,
 * relationship changed, ...). Distinct from Forge's own event bus: this one
 * lets SamuraiAI systems react to what happened to an NPC without polling the
 * world or holding direct references to each other.
 */
public class NPCEventBus {

    private static final NPCEventBus INSTANCE = new NPCEventBus();

    private record Registered(EventPriority priority, long order, Consumer<NpcEvent> listener) { }
    private final Map<Class<? extends NpcEvent>, CopyOnWriteArrayList<Registered>> listeners = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(), published = new AtomicLong(), deliveries = new AtomicLong();
    private final AtomicLong failures = new AtomicLong(), dispatchNanos = new AtomicLong(), maxDispatchNanos = new AtomicLong();

    private NPCEventBus() {
    }

    public static NPCEventBus getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T extends NpcEvent> EventSubscription subscribe(Class<T> eventType, Consumer<T> listener) {
        return subscribe(eventType, EventPriority.NORMAL, listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends NpcEvent> EventSubscription subscribe(Class<T> eventType, EventPriority priority, Consumer<T> listener) {
        Objects.requireNonNull(eventType); Objects.requireNonNull(priority); Objects.requireNonNull(listener);
        Registered registered = new Registered(priority, sequence.getAndIncrement(), (Consumer<NpcEvent>) listener);
        var bucket = listeners.computeIfAbsent(eventType, type -> new CopyOnWriteArrayList<>());
        bucket.add(registered);
        bucket.sort(Comparator.comparing(Registered::priority).thenComparingLong(Registered::order));
        return new EventSubscription() {
            private final AtomicBoolean active = new AtomicBoolean(true);
            public boolean active() { return active.get(); }
            public void close() { if (active.compareAndSet(true, false)) bucket.remove(registered); }
        };
    }

    public <T extends NpcEvent> void unsubscribe(Class<T> eventType, Consumer<T> listener) {

        List<Registered> registered = listeners.get(eventType);

        if (registered != null) {
            registered.removeIf(entry -> entry.listener().equals(listener));
        }
    }

    /**
     * Delivers an event to every listener registered for its class or for any
     * supertype of it.
     *
     * <p>The previous version looked up {@code event.getClass()} only, so
     * subscribing to {@code NpcEvent} to observe everything — the obvious way
     * to write a logger or a debug overlay — silently received nothing.
     *
     * <p>Each listener is isolated: one that throws is logged and skipped
     * rather than cancelling delivery to the listeners after it, since a
     * failing cosmetic subscriber must not stop the spawn pipeline from
     * hearing about a new NPC.
     */
    public void post(NpcEvent event) {
        var scheduler = yadi.samuraiai.runtime.ServerScheduler.getInstance();
        if (!scheduler.isServerThread()) {
            scheduler.execute(() -> post(event));
            return;
        }

        if (event == null) {
            return;
        }

        long started = System.nanoTime(); published.incrementAndGet();
        List<Registered> delivery = listeners.entrySet().stream().filter(entry -> entry.getKey().isInstance(event))
                .flatMap(entry -> entry.getValue().stream())
                .sorted(Comparator.comparing(Registered::priority).thenComparingLong(Registered::order)).toList();
        for (Registered registered : delivery) {
                try {
                    registered.listener().accept(event); deliveries.incrementAndGet();
                } catch (RuntimeException | LinkageError | AssertionError e) {
                    failures.incrementAndGet();
                    SamuraiLogger.EVENTS.error("Un listener de {} fallo: {}", event.getName(), e.toString());
                }
        }
        long elapsed = System.nanoTime() - started; dispatchNanos.addAndGet(elapsed); maxDispatchNanos.accumulateAndGet(elapsed, Math::max);
    }

    public EventBusMetrics metrics() {
        long count = published.get();
        int listenerCount = listeners.values().stream().mapToInt(List::size).sum();
        return new EventBusMetrics(count, deliveries.get(), failures.get(),
                count == 0 ? 0 : dispatchNanos.get() / 1000D / count, maxDispatchNanos.get() / 1000D, listenerCount);
    }

    /** Drops every subscription. Used on server shutdown. */
    public void clear() {
        listeners.clear();
        published.set(0); deliveries.set(0); failures.set(0); dispatchNanos.set(0); maxDispatchNanos.set(0);
    }
}
