package yadi.samuraiai.client.voice.util;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import yadi.samuraiai.foundation.async.*;

/** Bounded voice worker. Cancelling its returned future interrupts the associated task. */
public final class VoiceThreadDispatcher implements AutoCloseable {
    private final String owner;
    private volatile boolean closed;
    public VoiceThreadDispatcher(String name) {
        owner = "voice:" + name + ":" + UUID.randomUUID();
    }
    public <T> CompletableFuture<T> submit(Callable<T> operation) {
        if (closed) return CompletableFuture.failedFuture(new RejectedExecutionException("Voice dispatcher closed"));
        return AsyncEngine.global().submit("voice", owner, AsyncPriority.HIGH, Duration.ofMinutes(15), operation).future();
    }
    @Override public void close() { closed = true; AsyncEngine.global().cancelOwner(owner); }
}
