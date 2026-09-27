package yadi.samuraiai.client.voice;

import javax.sound.sampled.*;
import java.util.Arrays;

public final class VoicePermissionChecker {
    public record Result(boolean available, String device, String detail) {}
    public static Result check(String preferred) {
        try {
            Mixer.Info info = Arrays.stream(AudioSystem.getMixerInfo())
                    .filter(i -> preferred == null || preferred.isBlank() || i.getName().equalsIgnoreCase(preferred))
                    .filter(i -> AudioSystem.getMixer(i).isLineSupported(new DataLine.Info(TargetDataLine.class, VoiceRecorder.FORMAT)))
                    .findFirst().orElse(null);
            return info == null ? new Result(false, "", "No se encontró un micrófono compatible") : new Result(true, info.getName(), "Disponible");
        } catch (Exception error) { return new Result(false, "", error.getClass().getSimpleName() + ": " + error.getMessage()); }
    }
    private VoicePermissionChecker() {}
}
