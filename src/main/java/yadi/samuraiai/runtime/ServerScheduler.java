package yadi.samuraiai.runtime;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import yadi.samuraiai.logging.SamuraiLogger;

/** Session-scoped boundary; no Minecraft objects are read by background callbacks. */
public final class ServerScheduler {
    private static final ServerScheduler INSTANCE = new ServerScheduler();
    private volatile Session current;
    private record Session(Executor executor, BooleanSupplier isOwner) {}
    public static ServerScheduler getInstance() { return INSTANCE; }

    public synchronized void bind(Executor executor, BooleanSupplier isOwner) {
        if (current != null) throw new IllegalStateException("Scheduler already bound");
        current = new Session(Objects.requireNonNull(executor), Objects.requireNonNull(isOwner));
    }
    public boolean isRunning() { return current != null; }
    public boolean isServerThread() {
        Session session = current;
        return session != null && session.isOwner().getAsBoolean();
    }
    public void requireServerThread() {
        Session session = current;
        if (session == null || !session.isOwner().getAsBoolean())
            throw new IllegalStateException("Operation requires the active server thread");
    }
    /** Captures the session now; queued work cannot cross a world restart. */
    public Executor executor() {
        Session captured = current;
        return task -> dispatch(captured, task);
    }
    public void execute(Runnable task) { dispatch(current, task); }
    private void dispatch(Session session, Runnable task) {
        if (session == null || current != session) return;
        try {
            session.executor().execute(() -> {
                if (current != session) return;
                requireServerThread();
                try { task.run(); }
                catch (RuntimeException | LinkageError error) {
                    SamuraiLogger.SCHEDULER.error("scheduler task failed thread={}", Thread.currentThread().getName(), error);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException error) {
            SamuraiLogger.SCHEDULER.debug("Server rejected task during shutdown", error);
        }
    }
    public synchronized void close() { current = null; }
}
