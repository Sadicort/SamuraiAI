package yadi.samuraiai.foundation;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.foundation.async.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncEngineTest {
    @Test void priorityQueueRunsHigherPriorityFirstAfterCurrentWork() throws Exception {
        try (var engine = new AsyncEngine(1, 4, "async-test")) {
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
            var blocker = engine.submit("test", "one", AsyncPriority.NORMAL, Duration.ofSeconds(2), () -> {
                started.countDown(); release.await(); return "blocker";
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            List<String> order = new CopyOnWriteArrayList<>();
            var low = engine.submit("test", "two", AsyncPriority.LOW, Duration.ofSeconds(2), () -> { order.add("low"); return 1; });
            var high = engine.submit("test", "three", AsyncPriority.HIGH, Duration.ofSeconds(2), () -> { order.add("high"); return 2; });
            release.countDown();
            CompletableFuture.allOf(blocker.future(), low.future(), high.future()).get(2, TimeUnit.SECONDS);
            assertEquals(List.of("high", "low"), order);
            assertEquals(3, engine.metrics().completed());
        }
    }
    @Test void saturationIsBoundedAndReported() throws Exception {
        try (var engine = new AsyncEngine(1, 1, "async-capacity")) {
            CountDownLatch release = new CountDownLatch(1);
            var first = engine.submit("test", "a", AsyncPriority.NORMAL, Duration.ofSeconds(2), () -> { release.await(); return 1; });
            var second = engine.submit("test", "b", AsyncPriority.NORMAL, Duration.ofSeconds(2), () -> 2);
            var rejected = engine.submit("test", "c", AsyncPriority.NORMAL, Duration.ofSeconds(2), () -> 3);
            assertThrows(ExecutionException.class, () -> rejected.future().get(1, TimeUnit.SECONDS));
            assertEquals(1, engine.metrics().rejected());
            release.countDown(); CompletableFuture.allOf(first.future(), second.future()).get(2, TimeUnit.SECONDS);
        }
    }
    @Test void timeoutInterruptsWorkAndReleasesOwnership() throws Exception {
        try (var engine = new AsyncEngine(1, 2, "async-timeout")) {
            CountDownLatch interrupted = new CountDownLatch(1);
            var task = engine.submit("voice", "session", AsyncPriority.NORMAL, Duration.ofMillis(80), () -> {
                try { Thread.sleep(10_000); } catch (InterruptedException error) { interrupted.countDown(); throw error; }
                return 1;
            });
            assertThrows(ExecutionException.class, () -> task.future().get(2, TimeUnit.SECONDS));
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            assertEquals(1, engine.metrics().timedOut());
            assertTrue(engine.activeTasks().isEmpty());
        }
    }
    @Test void cancellingOwnerCancelsQueuedAndRunningTasks() throws Exception {
        try (var engine = new AsyncEngine(1, 3, "async-owner")) {
            CountDownLatch started = new CountDownLatch(1);
            var first = engine.submit("dialogue", "npc:1", AsyncPriority.NORMAL, Duration.ofSeconds(5), () -> {
                started.countDown(); Thread.sleep(10_000); return 1;
            });
            var second = engine.submit("dialogue", "npc:1", AsyncPriority.NORMAL, Duration.ofSeconds(5), () -> 2);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals(2, engine.cancelOwner("npc:1"));
            assertTrue(first.future().isCancelled()); assertTrue(second.future().isCancelled());
            assertEquals(2, engine.metrics().cancelled());
        }
    }
}
