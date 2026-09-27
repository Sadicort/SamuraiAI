package yadi.samuraiai.foundation.async;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AsyncTaskHandle<T> {
    private final UUID id;
    private final CompletableFuture<T> future;
    private final Runnable cancel;
    AsyncTaskHandle(UUID id, CompletableFuture<T> future, Runnable cancel) {
        this.id = id; this.future = future; this.cancel = cancel;
    }
    public UUID id() { return id; }
    public CompletableFuture<T> future() { return future; }
    public boolean cancel() { if (future.isDone()) return false; cancel.run(); return future.isCancelled(); }
}
