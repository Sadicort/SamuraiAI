package yadi.samuraiai.client.voice.core;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import yadi.samuraiai.client.voice.VoiceLanguageManager.Language;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class VoiceInstallationTest {
    @TempDir Path directory;
    private HttpServer server;
    private final byte[] expected = "verified model fixture".getBytes(StandardCharsets.UTF_8);
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    private String url() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/model"; }
    private VoiceModelManager.ModelInfo info(String url) throws Exception {
        return new VoiceModelManager.ModelInfo("tiny", Language.AUTO, "test", expected.length,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(expected)), url, 1);
    }
    private void serve(int status, byte[] body, boolean chunked) {
        server.createContext("/model", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(status, chunked ? 0 : body.length);
                exchange.getResponseBody().write(body);
            }
        });
    }
    private void noPartFiles() throws Exception {
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.toString().endsWith(".part")));
        }
    }
    @Test void validDownloadReplacesOnlyAfterVerification() throws Exception {
        serve(200, expected, false);
        Path target = Files.writeString(directory.resolve("tiny.bin"), "previous");
        assertEquals(target, new VoiceDownloadManager().download(info(url()), target));
        assertArrayEquals(expected, Files.readAllBytes(target));
        noPartFiles();
    }
    @Test void wrongHashPreservesPreviousModel() throws Exception {
        byte[] bad = expected.clone(); bad[0] ^= 1;
        serve(200, bad, false);
        Path target = Files.writeString(directory.resolve("tiny.bin"), "previous");
        assertThrows(java.io.IOException.class, () -> new VoiceDownloadManager().download(info(url()), target));
        assertEquals("previous", Files.readString(target));
        noPartFiles();
    }
    @Test void oversizedChunkedResponseIsBounded() throws Exception {
        serve(200, new byte[expected.length + 1], true);
        Path target = directory.resolve("tiny.bin");
        assertThrows(java.io.IOException.class, () -> new VoiceDownloadManager().download(info(url()), target));
        assertFalse(Files.exists(target));
        noPartFiles();
    }
    @Test void wrongContentLengthDoesNotPublishModel() throws Exception {
        serve(200, new byte[1], false);
        Path target = directory.resolve("tiny.bin");
        assertThrows(java.io.IOException.class, () -> new VoiceDownloadManager().download(info(url()), target));
        assertFalse(Files.exists(target));
        noPartFiles();
    }
    @Test void httpErrorPreservesPreviousModel() throws Exception {
        serve(404, expected, false);
        Path target = Files.writeString(directory.resolve("tiny.bin"), "previous");
        assertThrows(java.io.IOException.class, () -> new VoiceDownloadManager().download(info(url()), target));
        assertEquals("previous", Files.readString(target));
        noPartFiles();
    }
    @Test void stalledBodyHasTotalDeadline() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/model", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, expected.length);
                exchange.getResponseBody().write(expected, 0, 1);
                exchange.getResponseBody().flush();
                try { release.await(3, TimeUnit.SECONDS); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            }
        });
        var downloader = new VoiceDownloadManager(HttpClient.newHttpClient(), Duration.ofMillis(300));
        Path target = Files.writeString(directory.resolve("tiny.bin"), "previous");
        try {
            assertThrows(HttpTimeoutException.class, () -> downloader.download(info(url()), target));
            assertEquals("previous", Files.readString(target));
            noPartFiles();
        } finally { release.countDown(); }
    }
    @Test void verifiedInstalledModelWorksWithoutNetwork() throws Exception {
        var model = info(url());
        var models = new VoiceModelManager(directory, List.of(model));
        Files.write(models.path(model), expected);
        server.stop(0);
        assertEquals(models.path(model), new VoiceInstallationService(models).ensure(Language.ES));
    }
    @Test void invalidReplacementDoesNotDeletePreviouslyInstalledModel() throws Exception {
        serve(500, expected, false);
        var model = info(url());
        var models = new VoiceModelManager(directory, List.of(model));
        Files.writeString(models.path(model), "old version");
        assertThrows(java.io.IOException.class, () -> new VoiceInstallationService(models).ensure(Language.ES));
        assertEquals("old version", Files.readString(models.path(model)));
    }
    @Test void modelIdCannotEscapeManagedDirectory() throws Exception {
        var model = info(url());
        var unsafe = new VoiceModelManager.ModelInfo("../outside", model.language(), model.version(), model.size(), model.sha256(), model.url(), 1);
        var models = new VoiceModelManager(directory, List.of(unsafe));
        assertThrows(IllegalArgumentException.class, () -> models.path(unsafe));
    }
    @Test void remotePlaintextUrlIsRejected() throws Exception {
        assertThrows(java.io.IOException.class, () -> new VoiceDownloadManager()
                .download(info("http://example.invalid/model"), directory.resolve("tiny.bin")));
        noPartFiles();
    }
}
