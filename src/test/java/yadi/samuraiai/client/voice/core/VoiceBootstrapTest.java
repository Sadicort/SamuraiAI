package yadi.samuraiai.client.voice.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import yadi.samuraiai.client.voice.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class VoiceBootstrapTest {
    @TempDir Path directory;
    private VoiceModelManager models() { return new VoiceModelManager(directory, List.of()); }
    private static final class FakeEngine implements SpeechRecognitionService {
        final CountDownLatch closed = new CountDownLatch(1);
        public CompletableFuture<Result> recognize(byte[] wav, VoiceLanguageManager.Language language) {
            return CompletableFuture.completedFuture(Result.ok("hola", 1));
        }
        public String name() { return "test"; }
        public void close() { closed.countDown(); }
    }
    @Test void installationAndLoadingRunOffCallerAndBootstrapIsSingleFlight() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        String caller = Thread.currentThread().getName();
        AtomicReference<String> installThread = new AtomicReference<>(), loadThread = new AtomicReference<>();
        FakeEngine engine = new FakeEngine();
        try (var manager = new VoiceEngineManager(models(), language -> {
            installThread.set(Thread.currentThread().getName()); started.countDown();
            assertTrue(release.await(3, TimeUnit.SECONDS)); return directory.resolve("tiny.bin");
        }, ignored -> { loadThread.set(Thread.currentThread().getName()); return engine; }, Runnable::run)) {
            var first = manager.bootstrap();
            assertTrue(started.await(3, TimeUnit.SECONDS));
            assertSame(first, manager.bootstrap());
            assertFalse(first.isDone());
            release.countDown(); first.get(3, TimeUnit.SECONDS);
            assertNotEquals(caller, installThread.get());
            assertEquals(installThread.get(), loadThread.get());
            assertTrue(manager.usable());
        } finally { release.countDown(); }
        assertTrue(engine.closed.await(1, TimeUnit.SECONDS));
    }
    @Test void closingDuringNativeLoadDiscardsAndClosesLateEngine() throws Exception {
        CountDownLatch loading = new CountDownLatch(1), release = new CountDownLatch(1);
        FakeEngine engine = new FakeEngine();
        var manager = new VoiceEngineManager(models(), ignored -> directory.resolve("tiny.bin"), ignored -> {
            loading.countDown();
            boolean interrupted = false;
            while (release.getCount() != 0) {
                try { release.await(3, TimeUnit.SECONDS); }
                catch (InterruptedException error) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
            return engine;
        }, Runnable::run);
        try {
            var pending = manager.bootstrap();
            assertTrue(loading.await(3, TimeUnit.SECONDS));
            manager.close(); release.countDown();
            assertTrue(pending.isCancelled());
            assertTrue(engine.closed.await(3, TimeUnit.SECONDS));
            assertEquals(VoiceEngineState.DISABLED, manager.state());
            assertFalse(manager.usable());
        } finally { release.countDown(); manager.close(); }
    }
    @Test void detailedInstallationFailureIsVisibleAndRetryCanSucceed() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        try (var manager = new VoiceEngineManager(models(), language -> {
            if (attempts.incrementAndGet() == 1) throw new java.io.IOException("HTTP 503 fixture");
            return directory.resolve("tiny.bin");
        }, ignored -> new FakeEngine(), Runnable::run)) {
            assertThrows(ExecutionException.class, () -> manager.bootstrap().get(3, TimeUnit.SECONDS));
            assertEquals(VoiceEngineState.ERROR, manager.state());
            assertTrue(manager.engine().recognize(new byte[0], VoiceLanguageManager.Language.ES).join().error().contains("503"));
            manager.bootstrap().get(3, TimeUnit.SECONDS);
            assertTrue(manager.usable());
        }
    }
    @Test void cancellingBootstrapInvalidatesLateResultAndAllowsRetry() throws Exception {
        CountDownLatch started = new CountDownLatch(1), cancelled = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        try (var manager = new VoiceEngineManager(models(), language -> {
            if (calls.incrementAndGet() == 1) {
                started.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException error) { cancelled.countDown(); throw error; }
            }
            return directory.resolve("tiny.bin");
        }, ignored -> new FakeEngine(), Runnable::run)) {
            var pending = manager.bootstrap();
            assertTrue(started.await(3, TimeUnit.SECONDS));
            assertTrue(pending.cancel(true));
            assertTrue(cancelled.await(3, TimeUnit.SECONDS));
            assertEquals(VoiceEngineState.UNINITIALIZED, manager.state());
            manager.bootstrap().get(3, TimeUnit.SECONDS);
            assertTrue(manager.usable());
        }
    }
    @Test void reloadClosesPreviousEngineAndLoadsReplacement() throws Exception {
        FakeEngine first=new FakeEngine(),second=new FakeEngine();AtomicInteger loads=new AtomicInteger();
        try(var manager=new VoiceEngineManager(models(),language->directory.resolve("tiny.bin"),
                ignored->loads.getAndIncrement()==0?first:second,Runnable::run)) {
            manager.bootstrap().get(3,TimeUnit.SECONDS);assertSame(first,manager.engine());
            manager.reload().get(3,TimeUnit.SECONDS);
            assertTrue(first.closed.await(1,TimeUnit.SECONDS));assertSame(second,manager.engine());assertTrue(manager.usable());
        }
        assertTrue(second.closed.await(1,TimeUnit.SECONDS));
    }
}
