package yadi.samuraiai.ai;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import yadi.samuraiai.context.AIContext;

class AIRequestQueueTest {
    static AIContext context(String message) { return AIContext.builder().playerMessage(message).build(); }
    @Test void fifoWaitsWithoutDroppingAndTracksStates() {
        List<CompletableFuture<AIResponse>> pending = new ArrayList<>();
        List<String> started = new ArrayList<>();
        try (var queue = new AIRequestQueue(c -> { started.add(c.getPlayerMessage()); var f=new CompletableFuture<AIResponse>(); pending.add(f); return f; },
                () -> new AIRequestQueue.Limits(1,3,10000,10000))) {
            var a=queue.enqueue(UUID.randomUUID(),context("a"));
            var b=queue.enqueue(UUID.randomUUID(),context("b"));
            var c=queue.enqueue(UUID.randomUUID(),context("c"));
            assertEquals(AIRequestState.RUNNING,a.state());
            assertEquals(AIRequestState.WAITING,b.state());
            assertEquals(2,queue.stats().queued());
            assertEquals(0,queue.stats().dropped());
            pending.get(0).complete(AIResponse.ok("one"));
            assertEquals(List.of("a","b"),started);
            pending.get(1).complete(AIResponse.ok("two"));
            pending.get(2).complete(AIResponse.ok("three"));
            assertEquals(List.of("a","b","c"),started);
            assertEquals(3,queue.stats().succeeded());
            assertEquals(AIRequestState.COMPLETED,c.state());
        }
    }
    @Test void boundedAdmissionGivesExplicitFallback() {
        try (var queue = new AIRequestQueue(c -> new CompletableFuture<>(), () -> new AIRequestQueue.Limits(1,1,10000,10000))) {
            queue.submit(UUID.randomUUID(),context("running")); queue.submit(UUID.randomUUID(),context("waiting"));
            var rejected=queue.submit(UUID.randomUUID(),context("full")).join();
            assertEquals("QUEUE_FULL",rejected.error()); assertTrue(rejected.hasSpeakableText());
            assertEquals(1,queue.stats().queued());
        }
    }
    @Test void timeoutCancelsUpstreamAndRestoresSlot() throws Exception {
        var upstream=new CompletableFuture<AIResponse>();
        try (var queue=new AIRequestQueue(c->upstream,()->new AIRequestQueue.Limits(1,2,1000,30))) {
            var request=queue.enqueue(UUID.randomUUID(),context("slow"));
            assertEquals("TIMEOUT",request.future().get(2,TimeUnit.SECONDS).error());
            assertTrue(upstream.isCancelled()); assertEquals(0,queue.stats().inFlight());
            assertEquals(AIRequestState.TIMEOUT,request.state());
        }
    }
    @Test void waitingDeadlineDoesNotStartProvider() throws Exception {
        List<CompletableFuture<AIResponse>> started=new ArrayList<>();
        try(var queue=new AIRequestQueue(c->{ var f=new CompletableFuture<AIResponse>();started.add(f);return f;},
                ()->new AIRequestQueue.Limits(1,2,30,10000))) {
            queue.submit(UUID.randomUUID(),context("first"));
            var second=queue.enqueue(UUID.randomUUID(),context("second"));
            assertEquals("TIMEOUT",second.future().get(2,TimeUnit.SECONDS).error());
            assertEquals(1,started.size()); assertEquals(1,queue.stats().inFlight());
        }
    }
    @Test void cancelNpcRemovesWaitingAndRunningWithoutLaunchingAnotherTurn() {
        UUID npc=UUID.randomUUID();
        List<CompletableFuture<AIResponse>> started=new ArrayList<>();
        try(var queue=new AIRequestQueue(c->{var f=new CompletableFuture<AIResponse>();started.add(f);return f;},
                ()->new AIRequestQueue.Limits(1,8,1000,1000))) {
            var a=queue.enqueue(npc,context("a"));var b=queue.enqueue(npc,context("b"));
            queue.cancelNpc(npc);
            assertTrue(started.get(0).isCancelled()); assertEquals(1,started.size());
            assertEquals(AIRequestState.CANCELLED,a.state());assertEquals(AIRequestState.CANCELLED,b.state());
            assertEquals(0,queue.stats().queued());assertEquals(0,queue.stats().inFlight());
        }
    }
    @Test void synchronousFailureAndNullFutureDoNotLeakSlots() {
        for(AIService provider: List.<AIService>of(c->{throw new IllegalStateException();},c->null)) {
            try(var queue=new AIRequestQueue(provider)) {
                assertFalse(queue.submit(UUID.randomUUID(),context("a")).join().success());
                assertEquals(0,queue.stats().inFlight());
                assertFalse(queue.submit(UUID.randomUUID(),context("b")).join().success());
            }
        }
    }
    @Test void externalFutureCancellationPropagates() {
        var transport=new CompletableFuture<String>();
        var mapped=CancellableFutures.map(transport,(value,error)->value);
        mapped.cancel(true); assertTrue(transport.isCancelled());
    }
    @Test void shutdownCancelsAllAndRejectsNewRequests() {
        try(var queue=new AIRequestQueue(c->new CompletableFuture<>())) {
            var r=queue.enqueue(UUID.randomUUID(),context("a"));queue.pause();
            assertEquals(AIRequestState.CANCELLED,r.state());
            assertEquals("CANCELLED",queue.submit(UUID.randomUUID(),context("b")).join().error());
            queue.resume();assertEquals(AIRequestState.RUNNING,queue.enqueue(UUID.randomUUID(),context("c")).state());
        }
    }
    @Test void concurrentSubmitRespectsCapacity() throws Exception {
        List<CompletableFuture<AIResponse>> transport=new CopyOnWriteArrayList<>();
        try(var queue=new AIRequestQueue(c->{var f=new CompletableFuture<AIResponse>();transport.add(f);return f;},
                ()->new AIRequestQueue.Limits(4,200,10000,10000))) {
            var pool=Executors.newFixedThreadPool(8);
            try {
                List<Future<?>> jobs=new ArrayList<>();
                for(int i=0;i<100;i++) jobs.add(pool.submit(()->queue.submit(UUID.randomUUID(),context("x"))));
                for(var job:jobs) job.get(3,TimeUnit.SECONDS);
                assertEquals(4,queue.stats().inFlight());assertEquals(96,queue.stats().queued());
                assertEquals(4,transport.size());assertEquals(0,queue.stats().dropped());
            } finally { pool.shutdownNow(); }
        }
    }
    @Test void oneThousandRequestFifoStressCompletesWithoutLoss() {
        List<String> started=new ArrayList<>();List<CompletableFuture<AIResponse>> transport=new ArrayList<>();
        try(var queue=new AIRequestQueue(context->{started.add(context.getPlayerMessage());var future=new CompletableFuture<AIResponse>();transport.add(future);return future;},
                ()->new AIRequestQueue.Limits(8,1000,30000,30000))) {
            List<CompletableFuture<AIResponse>> results=new ArrayList<>();
            for(int index=0;index<1000;index++) results.add(queue.submit(new UUID(0,index),context(Integer.toString(index))));
            assertEquals(8,queue.stats().inFlight());assertEquals(992,queue.stats().queued());
            for(int index=0;index<1000;index++) transport.get(index).complete(AIResponse.ok("ok"));
            assertTrue(results.stream().allMatch(CompletableFuture::isDone));
            assertEquals(1000,queue.stats().succeeded());assertEquals(0,queue.stats().dropped());
            for(int index=0;index<1000;index++) assertEquals(Integer.toString(index),started.get(index));
        }
    }
}
