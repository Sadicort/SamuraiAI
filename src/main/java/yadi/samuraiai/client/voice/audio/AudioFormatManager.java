package yadi.samuraiai.client.voice.audio;

import javax.sound.sampled.AudioFormat;

public final class AudioFormatManager {
    private AudioFormatManager() {}
    public static AudioFormat whisperPcm() { return new AudioFormat(16_000f, 16, 1, true, false); }
}
