package yadi.samuraiai.foundation.async;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Bounded shared worker engine with ownership, deadlines and observable state. */
public final class AsyncEngine implements AutoCloseable {
    private static final class Holder {
        static final AsyncEngine INSTANCE = new AsyncEngine(Math.max(2, Math.min(8,
                Runtime.getRuntime().availableProcessors() / 2)), 256, "SamuraiAI-Async");
    }
    public static AsyncEngine global() { return Holder.INSTANCE; }

    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor deadlines;
    private final Semaphore admission;
    private final ConcurrentMap<UUID, Work<?>> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(), submitted = new AtomicLong(), completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong(), cancelled = new AtomicLong(), timedOut = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong(), totalNanos = new AtomicLong(), maxNanos = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    public AsyncEngine(int threads, int capacity, String threadPrefix) {
        if (threads < 1 || capacity < 1) throw new IllegalArgumentException("Positive threads/capacity required");
        Objects.requireNonNull(threadPrefix);
        admission = new Semaphore(threads + capacity);
        AtomicInteger workerId = new AtomicInteger();
        workers = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(), runnable -> daemon(threadPrefix + "-" + workerId.incrementAndGet(), runnable),
                new ThreadPoolExecutor.AbortPolicy());
        workers.allowCoreThreadTimeOut(true);
        deadlines = new ScheduledThreadPoolExecutor(1, runnable -> daemon(threadPrefix + "-Deadline", runnable));
        deadlines.setRemoveOnCancelPolicy(true);
        deadlines.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    private static Thread daemon(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name); thread.setDaemon(true); return thread;
    }

    public <T> AsyncTaskHandle<T> submit(String module, String owner, AsyncPriority priority,
                                          Duration timeout, Callable<T> operation) {
        validate(module, "module"); validate(owner, "owner"); Objects.requireNonNull(priority);
        Objects.requireNonNull(timeout); Objects.requireNonNull(operation);
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("Positive timeout required");
        if (closed.get()) return rejectedHandle(new RejectedExecutionException("Async engine closed"));
        if (!admission.tryAcquire()) return rejectedHandle(new RejectedExecutionException("Async engine saturated"));
        Work<T> work = new Work<>(UUID.randomUUID(), module, owner, priority, timeout, operation, sequence.getAndIncrement());
        tasks.put(work.id, work); submitted.incrementAndGet();
        try {
            work.deadline = deadlines.schedule(() -> work.finish(null,
                    new TimeoutException("Async task deadline exceeded"), AsyncTaskState.TIMED_OUT, true),
                    timeout.toNanos(), TimeUnit.NANOSECONDS);
            workers.execute(work);
        } catch (RuntimeException error) {
            work.finish(null, error, AsyncTaskState.FAILED, true);
        }
        return new AsyncTaskHandle<>(work.id, work.result, () -> work.finish(null,
                new CancellationException("Cancelled"), AsyncTaskState.CANCELLED, true));
    }

    /** Lightweight deadline callback; it must not call Minecraft APIs. */
    public AsyncDeadline deadline(String owner, Duration delay, Runnable callback) {
        validate(owner, "owner"); Objects.requireNonNull(delay); Objects.requireNonNull(callback);
        if (delay.isNegative() || delay.isZero()) throw new IllegalArgumentException("Positive delay required");
        if (closed.get()) throw new RejectedExecutionException("Async engine closed");
        ScheduledFuture<?> future = deadlines.schedule(callback, delay.toNanos(), TimeUnit.NANOSECONDS);
        return new AsyncDeadline() {
            public boolean cancel() { return future.cancel(false); }
            public boolean done() { return future.isDone(); }
        };
    }

    private <T> AsyncTaskHandle<T> rejectedHandle(RuntimeException error) {
        rejected.incrementAndGet();
        CompletableFuture<T> future = CompletableFuture.failedFuture(error);
        return new AsyncTaskHandle<>(UUID.randomUUID(), future, () -> { });
    }

    public int cancelOwner(String owner) {
        validate(owner, "owner");
        List<Work<?>> owned = new ArrayList<>(tasks.values().stream().filter(task -> task.owner.equals(owner)).toList());
        // Freeze queued work before interrupting a running task can free a worker.
        owned.sort(Comparator.comparingInt(task -> task.state.get() == AsyncTaskState.QUEUED ? 0 : 1));
        owned.forEach(task -> task.finish(null, new CancellationException("Owner cancelled"), AsyncTaskState.CANCELLED, true));
        return owned.size();
    }

    public Optional<AsyncTaskSnapshot> find(UUID id) {
        Work<?> work = tasks.get(id); return work == null ? Optional.empty() : Optional.of(work.snapshot());
    }
    public List<AsyncTaskSnapshot> activeTasks() {
        return tasks.values().stream().map(Work::snapshot)
                .sorted(Comparator.comparing(AsyncTaskSnapshot::createdAt).thenComparing(AsyncTaskSnapshot::id)).toList();
    }
    public AsyncMetrics metrics() {
        long ended = completed.get() + failed.get() + cancelled.get() + timedOut.get();
        return new AsyncMetrics(submitted.get(), completed.get(), failed.get(), cancelled.get(), timedOut.get(),
                rejected.get(), workers.getActiveCount(), workers.getQueue().size(),
                ended == 0 ? 0 : totalNanos.get() / 1_000_000D / ended, maxNanos.get() / 1_000_000D);
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        new ArrayList<>(tasks.values()).forEach(task -> task.finish(null,
                new CancellationException("Engine closed"), AsyncTaskState.CANCELLED, true));
        workers.shutdownNow(); deadlines.shutdownNow();
    }

    private static void validate(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Blank " + name);
    }

    private final class Work<T> implements Runnable, Comparable<Work<?>> {
        final UUID id; final String module, owner; final AsyncPriority priority; final Duration timeout;
        final Callable<T> operation; final long order, createdNanos = System.nanoTime();
        final Instant createdAt = Instant.now(); final CompletableFuture<T> result = new CompletableFuture<>();
        final AtomicReference<AsyncTaskState> state = new AtomicReference<>(AsyncTaskState.QUEUED);
        volatile Thread runnerThread; volatile ScheduledFuture<?> deadline; volatile String failure = "";
        Work(UUID id, String module, String owner, AsyncPriority priority, Duration timeout, Callable<T> operation, long order) {
            this.id=id; this.module=module; this.owner=owner; this.priority=priority; this.timeout=timeout;
            this.operation=operation; this.order=order;
            result.whenComplete((value, error) -> {
                if (result.isCancelled()) finish(null, new CancellationException("Future cancelled"), AsyncTaskState.CANCELLED, true);
            });
        }
        @Override public int compareTo(Work<?> other) {
            int priorityOrder = Integer.compare(priority.ordinal(), other.priority.ordinal());
            return priorityOrder != 0 ? priorityOrder : Long.compare(order, other.order);
        }
        @Override public void run() {
            runnerThread = Thread.currentThread();
            if (!state.compareAndSet(AsyncTaskState.QUEUED, AsyncTaskState.RUNNING)) return;
            try { finish(operation.call(), null, AsyncTaskState.COMPLETED, false); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); finish(null, error, AsyncTaskState.CANCELLED, false); }
            catch (Throwable error) {
                finish(null, error, AsyncTaskState.FAILED, false);
                if (error instanceof VirtualMachineError fatal) throw fatal;
            } finally { runnerThread = null; }
        }
        void finish(Object value, Throwable error, AsyncTaskState terminal, boolean interrupt) {
            AsyncTaskState previous;
            do {
                previous = state.get();
                if (previous == AsyncTaskState.COMPLETED || previous == AsyncTaskState.FAILED ||
                        previous == AsyncTaskState.CANCELLED || previous == AsyncTaskState.TIMED_OUT) return;
            } while (!state.compareAndSet(previous, terminal));
            if (deadline != null) deadline.cancel(false);
            if (interrupt && previous == AsyncTaskState.QUEUED) workers.remove(this);
            Thread executing = runnerThread;
            if (interrupt && executing != null && executing != Thread.currentThread()) executing.interrupt();
            failure = error == null ? "" : error.getClass().getSimpleName() + ": " + Objects.toString(error.getMessage(), "");
            long elapsed = System.nanoTime() - createdNanos;
            totalNanos.addAndGet(elapsed); maxNanos.accumulateAndGet(elapsed, Math::max);
            switch (terminal) {
                case COMPLETED -> completed.incrementAndGet();
                case FAILED -> failed.incrementAndGet();
                case CANCELLED -> cancelled.incrementAndGet();
                case TIMED_OUT -> timedOut.incrementAndGet();
                default -> { }
            }
            tasks.remove(id, this); admission.release();
            if (terminal == AsyncTaskState.CANCELLED) result.cancel(true);
            else if (error != null) result.completeExceptionally(error);
            else {
                @SuppressWarnings("unchecked") T typed = (T) value;
                result.complete(typed);
            }
        }
        AsyncTaskSnapshot snapshot() {
            return new AsyncTaskSnapshot(id, module, owner, priority, state.get(), createdAt, timeout.toMillis(),
                    (System.nanoTime() - createdNanos) / 1_000_000, failure);
        }
    }
}
