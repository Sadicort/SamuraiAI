package yadi.samuraiai.client.voice.audio;

import javax.sound.sampled.AudioSystem;
import java.util.Arrays;
import java.util.List;

/** Enumerates input devices without keeping a microphone open. */
public final class AudioDeviceManager {
    public record Device(String name, boolean input) {}
    public List<Device> devices() {
        return Arrays.stream(AudioSystem.getMixerInfo()).map(info -> {
            var mixer = AudioSystem.getMixer(info);
            boolean input = mixer.isLineSupported(new javax.sound.sampled.DataLine.Info(javax.sound.sampled.TargetDataLine.class, AudioFormatManager.whisperPcm()));
            return new Device(info.getName(), input);
        }).filter(Device::input).toList();
    }
}
