package yadi.samuraiai.ai;

import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.AIContext;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.time.Duration;
import yadi.samuraiai.foundation.async.*;

/** Bounded FIFO admission. Providers must propagate cancellation to their transport. */
public final class AIRequestQueue implements AutoCloseable {
    private static final class Holder { static final AIRequestQueue INSTANCE = new AIRequestQueue(new OllamaAIService()); }
    public static AIRequestQueue getInstance() { return Holder.INSTANCE; }
    private final AIService service;
    private final Supplier<Limits> limits;
    private final Deque<Request> waiting = new ArrayDeque<>();
    private final Map<UUID, Request> live = new LinkedHashMap<>();
    private boolean closed, terminated, pumping;
    private int running;
    private long submitted, succeeded, failed, dropped, cancelled, timeouts, totalNanos, maxNanos, providerNanos;

    public record Limits(int concurrency, int size, long waitMillis, long timeoutMillis) {
        public Limits {
            if (concurrency < 1 || size < 1 || waitMillis < 1 || timeoutMillis < 1)
                throw new IllegalArgumentException("Positive queue limits required");
        }
    }
    public AIRequestQueue(AIService service) {
        this(service, () -> new Limits(SamuraiSettings.maxConcurrentRequests(), SamuraiSettings.queueSize(),
                SamuraiSettings.queueWaitMillis(), SamuraiSettings.aiTimeoutSeconds() * 1000L));
    }
    public AIRequestQueue(AIService service, Supplier<Limits> limits) {
        this.service = Objects.requireNonNull(service);
        this.limits = Objects.requireNonNull(limits);
    }
    public final class Request {
        private final UUID id = UUID.randomUUID(), npcId;
        private final AIContext context;
        private final CompletableFuture<AIResponse> result = new CompletableFuture<>();
        private volatile AIRequestState state = AIRequestState.PENDING;
        private CompletableFuture<AIResponse> upstream;
        private AsyncDeadline deadline;
        private final long created = System.nanoTime();
        private long started;
        private Request(UUID npcId, AIContext context) {
            this.npcId = Objects.requireNonNull(npcId);
            this.context = Objects.requireNonNull(context);
            result.whenComplete((value, error) -> { if (result.isCancelled()) cancel(); });
        }
        public UUID id() { return id; }
        public UUID npcId() { return npcId; }
        public AIRequestState state() { return state; }
        public CompletableFuture<AIResponse> future() { return result; }
        public void cancel() { finish(this, AIResponse.failure(AIError.CANCELLED), AIRequestState.CANCELLED, true); }
    }
    public CompletableFuture<AIResponse> submit(UUID npcId, AIContext context) { return enqueue(npcId, context).future(); }
    public synchronized Request enqueue(UUID npcId, AIContext context) {
        Request request = new Request(npcId, context);
        submitted++;
        if (closed) {
            cancelled++; request.state = AIRequestState.CANCELLED;
            request.result.complete(AIResponse.failure(AIError.CANCELLED)); return request;
        }
        Limits policy = limits.get();
        if (waiting.size() >= policy.size()) {
            dropped++; request.state = AIRequestState.FAILED;
            request.result.complete(AIResponse.failure(AIError.QUEUE_FULL)); return request;
        }
        request.state = AIRequestState.WAITING;
        live.put(request.id, request); waiting.addLast(request);
        request.deadline = AsyncEngine.global().deadline("ai-queue", Duration.ofMillis(policy.waitMillis()),
                () -> finish(request, AIResponse.failure(AIError.TIMEOUT), AIRequestState.TIMEOUT, true));
        pump();
        return request;
    }
    private void pump() {
        if (pumping || closed) return;
        pumping = true;
        try {
            while (!closed && running < limits.get().concurrency() && !waiting.isEmpty()) {
                Request r = waiting.removeFirst();
                if (r.state != AIRequestState.WAITING) continue;
                r.deadline.cancel();
                r.state = AIRequestState.RUNNING; r.started = System.nanoTime(); running++;
                r.deadline = AsyncEngine.global().deadline("ai-queue", Duration.ofMillis(limits.get().timeoutMillis()),
                        () -> finish(r, AIResponse.failure(AIError.TIMEOUT), AIRequestState.TIMEOUT, true));
                try {
                    r.upstream = Objects.requireNonNull(service.generate(r.context), "Provider returned null");
                    r.upstream.whenComplete((response, error) -> {
                        AIResponse value = error == null && response != null ? response :
                                AIResponse.failure(error == null ? AIError.INTERNAL : AIError.classify(error));
                        finish(r, value, value.success() ? AIRequestState.COMPLETED : AIRequestState.FAILED, false);
                    });
                } catch (RuntimeException error) {
                    finish(r, AIResponse.failure(AIError.classify(error)), AIRequestState.FAILED, true);
                }
            }
        } finally { pumping = false; }
    }
    private synchronized void finish(Request r, AIResponse response, AIRequestState state, boolean abort) {
        if (!live.containsKey(r.id)) return;
        live.remove(r.id); waiting.remove(r);
        if (r.deadline != null) r.deadline.cancel();
        if (r.state == AIRequestState.RUNNING) { running--; providerNanos += System.nanoTime() - r.started; }
        r.state = state;
        long elapsed = System.nanoTime() - r.created;
        totalNanos += elapsed; maxNanos = Math.max(maxNanos, elapsed);
        switch (state) {
            case COMPLETED -> succeeded++;
            case CANCELLED -> cancelled++;
            case TIMEOUT -> timeouts++;
            default -> failed++;
        }
        if (abort && r.upstream != null) r.upstream.cancel(true);
        r.result.complete(response);
        pump();
    }
    public synchronized void cancelNpc(UUID npcId) {
        boolean wasPumping = pumping; pumping = true;
        try { new ArrayList<>(live.values()).stream().filter(r -> r.npcId.equals(npcId)).forEach(Request::cancel); }
        finally { pumping = wasPumping; }
        pump();
    }
    public synchronized void cancelAll() {
        boolean wasPumping = pumping; pumping = true;
        try { new ArrayList<>(live.values()).forEach(Request::cancel); }
        finally { pumping = wasPumping; }
    }
    public synchronized void pause() { closed = true; cancelAll(); }
    public synchronized void resume() {
        if (terminated) throw new IllegalStateException("Queue closed");
        closed = false; pump();
    }
    public synchronized void reconfigure() { pump(); }
    @Override public synchronized void close() { pause(); terminated = true; }
    public synchronized Stats stats() {
        long ended = succeeded + failed + cancelled + timeouts;
        return new Stats(submitted, succeeded, failed, dropped, running, limits.get().concurrency(),
                waiting.size(), cancelled, timeouts, ended == 0 ? 0 : totalNanos / ended / 1_000_000D,
                maxNanos / 1_000_000D, ended == 0 ? 0 : providerNanos / ended / 1_000_000D);
    }
    public record Stats(long submitted, long succeeded, long failed, long dropped, int inFlight, int capacity,
                        int queued, long cancelled, long timeouts, double averageMillis, double maximumMillis,
                        double providerAverageMillis) {}
}
