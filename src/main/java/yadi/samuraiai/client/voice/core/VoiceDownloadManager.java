package yadi.samuraiai.client.voice.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Blocking model transaction, called exclusively by the installation worker. Never stores audio. */
public final class VoiceDownloadManager {
    private final HttpClient client;
    private final Duration timeout;
    public VoiceDownloadManager() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL).build(), Duration.ofMinutes(10));
    }
    VoiceDownloadManager(HttpClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }
    public Path download(VoiceModelManager.ModelInfo info, Path target) throws IOException, InterruptedException {
        URI uri = URI.create(info.url());
        // Loopback HTTP permits isolated integration tests, never plaintext remote downloads.
        boolean loopback = "http".equalsIgnoreCase(uri.getScheme()) &&
                ("127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost()));
        if ((!"https".equalsIgnoreCase(uri.getScheme()) && !loopback) ||
                uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null)
            throw new IOException("URL de modelo no permitida");
        if (info.size() <= 0 || info.sha256() == null || !info.sha256().matches("[a-fA-F0-9]{64}"))
            throw new IOException("Manifest sin tamaño o SHA256 válido");
        Files.createDirectories(target.toAbsolutePath().getParent());
        if (Files.isSymbolicLink(target)) throw new IOException("Destino de modelo es un enlace simbólico");
        Path part = Files.createTempFile(target.toAbsolutePath().getParent(), ".voice-model-", ".part");
        CompletableFuture<HttpResponse<Path>> transfer = null;
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
            transfer = client.sendAsync(request, headers -> {
                if (headers.statusCode() != 200)
                    throw new IllegalStateException("Descarga de modelo: HTTP " + headers.statusCode());
                long announced = headers.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (announced >= 0 && announced != info.size())
                    throw new IllegalStateException("Tamaño HTTP distinto del manifest");
                return new LimitedFileSubscriber(part, info.size());
            });
            HttpResponse<Path> response;
            try { response = transfer.get(timeout.toMillis(), TimeUnit.MILLISECONDS); }
            catch (TimeoutException error) { throw new HttpTimeoutException("Timeout descargando modelo Whisper"); }
            catch (ExecutionException error) { throw new IOException("Descarga fallida: " + error.getCause().getMessage(), error.getCause()); }
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
            var integrity = new VoiceIntegrityChecker().check(part, info);
            if (!integrity.valid()) throw new IOException(integrity.detail());
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Instalación cancelada");
            // Fail safely if atomic replacement is unavailable; never delete the previous model.
            Files.move(part, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } finally {
            if (transfer != null && !transfer.isDone()) transfer.cancel(true);
            Files.deleteIfExists(part);
        }
    }
    private static final class LimitedFileSubscriber implements HttpResponse.BodySubscriber<Path> {
        private final HttpResponse.BodySubscriber<Path> delegate;
        private final long limit;
        private long received;
        private Flow.Subscription subscription;
        private boolean failed;
        LimitedFileSubscriber(Path path, long limit) {
            delegate = HttpResponse.BodySubscribers.ofFile(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            this.limit = limit;
        }
        @Override public CompletionStage<Path> getBody() { return delegate.getBody(); }
        @Override public void onSubscribe(Flow.Subscription value) { subscription = value; delegate.onSubscribe(value); }
        @Override public void onNext(List<ByteBuffer> buffers) {
            if (failed) return;
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > limit - received) {
                    failed = true;
                    subscription.cancel();
                    delegate.onError(new IOException("Descarga excede el tamaño del manifest"));
                    return;
                }
                received += buffer.remaining();
            }
            delegate.onNext(buffers);
        }
        @Override public void onError(Throwable error) { if (!failed) { failed = true; delegate.onError(error); } }
        @Override public void onComplete() { if (!failed) delegate.onComplete(); }
    }
}
