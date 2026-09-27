package yadi.samuraiai.client.voice;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.*;
import java.util.function.Consumer;
import yadi.samuraiai.client.voice.util.VoiceThreadDispatcher;

/** Captures PCM only between start and stop; no file is ever created. */
public final class VoiceRecorder implements AutoCloseable {
    public static final AudioFormat FORMAT = new AudioFormat(16_000f, 16, 1, true, false);
    private final VoiceThreadDispatcher worker = new VoiceThreadDispatcher("capture");
    private volatile TargetDataLine line;
    private volatile CompletableFuture<?> capture;
    public synchronized boolean start(VoiceSession session, Consumer<byte[]> completed, Consumer<Throwable> failed) {
        if (capture != null && !capture.isDone()) return false;
        session.force(VoiceSession.State.LISTENING);
        capture = worker.submit(() -> {
            try {
                if (session.state() != VoiceSession.State.LISTENING) throw new CancellationException("Grabación cancelada antes de abrir el micrófono");
                openLine();
                capture(session, completed, failed);
            } catch (Throwable error) {
                stopLine();
                failed.accept(error);
            }
            return null;
        });
        return true;
    }
    private void openLine() throws LineUnavailableException {
            VoicePermissionChecker.Result available = VoicePermissionChecker.check(VoiceConfig.get().microphone());
            if (!available.available()) throw new LineUnavailableException(available.detail());
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            Mixer.Info selected = java.util.Arrays.stream(AudioSystem.getMixerInfo())
                    .filter(i -> VoiceConfig.get().microphone().isBlank() || i.getName().equalsIgnoreCase(VoiceConfig.get().microphone()))
                    .filter(i -> AudioSystem.getMixer(i).isLineSupported(info)).findFirst().orElse(null);
            if (selected == null) throw new LineUnavailableException("No hay mixer compatible");
            Mixer mixer = AudioSystem.getMixer(selected);
            TargetDataLine opened = (TargetDataLine) mixer.getLine(info); opened.open(FORMAT); opened.start(); line = opened;
            VoiceDebugLogger.info("[Voice] Micrófono iniciado: {}", available.device());
    }
    private void capture(VoiceSession session, Consumer<byte[]> completed, Consumer<Throwable> failed) {
        ByteArrayOutputStream pcm = new ByteArrayOutputStream(); byte[] buffer = new byte[4096];
        long deadline = System.nanoTime() + VoiceConfig.get().maxRecordingSeconds() * 1_000_000_000L;
        try {
            while (line != null && System.nanoTime() < deadline && session.state() == VoiceSession.State.LISTENING) {
                int count = line.read(buffer, 0, buffer.length); if (count > 0) { pcm.write(buffer, 0, count); session.level(level(buffer, count)); }
            }
            if (session.state() == VoiceSession.State.LISTENING) stopLine();
            if (pcm.size() == 0) throw new IllegalStateException("No se capturó audio");
            byte[] raw = pcm.toByteArray();
            try { completed.accept(wav(raw)); }
            finally { java.util.Arrays.fill(raw, (byte) 0); }
        } catch (Throwable error) { failed.accept(error); }
        finally { stopLine(); java.util.Arrays.fill(buffer, (byte) 0); }
    }
    private static float level(byte[] data, int length) { long sum = 0; for (int i = 0; i + 1 < length; i += 2) { short sample = (short)((data[i + 1] << 8) | (data[i] & 255)); sum += (long) sample * sample; } return (float)Math.min(1, Math.sqrt(sum / Math.max(1, length / 2)) / 32768d); }
    private static byte[] wav(byte[] pcm) throws Exception { ByteArrayOutputStream out = new ByteArrayOutputStream(); AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), FORMAT, pcm.length / FORMAT.getFrameSize()), AudioFileFormat.Type.WAVE, out); return out.toByteArray(); }
    public synchronized void stop(VoiceSession session) { if (session.state() == VoiceSession.State.LISTENING) session.force(VoiceSession.State.PROCESSING); stopLine(); }
    private synchronized void stopLine() { if (line != null) { line.stop(); line.close(); line = null; } }
    @Override public void close() { stopLine(); worker.close(); }
}
