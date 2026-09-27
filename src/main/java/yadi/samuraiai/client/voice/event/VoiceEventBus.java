package yadi.samuraiai.client.voice.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Client-only event bus with listener isolation. */
public final class VoiceEventBus {
    private static final List<Subscription<?>> SUBSCRIPTIONS = new CopyOnWriteArrayList<>();
    public static <T> AutoCloseable subscribe(Class<T> type, Consumer<T> listener) {
        Subscription<T> subscription = new Subscription<>(type, listener); SUBSCRIPTIONS.add(subscription); return () -> SUBSCRIPTIONS.remove(subscription);
    }
    public static void post(Object event) {
        for (Subscription<?> raw : SUBSCRIPTIONS) try { raw.dispatch(event); } catch (Throwable ignored) { }
    }
    private record Subscription<T>(Class<T> type, Consumer<T> listener) {
        void dispatch(Object event) { if (type.isInstance(event)) listener.accept(type.cast(event)); }
    }
    private VoiceEventBus() {}
}
