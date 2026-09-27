package yadi.samuraiai.client.voice.recognition;

public final class VoiceActivityDetector {
    public boolean containsSpeech(byte[] pcm, float threshold) {
        if (pcm == null) return false;
        int gate = (int)(Math.max(0f, Math.min(1f, threshold)) * Short.MAX_VALUE);
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            short sample = (short)((pcm[i + 1] << 8) | (pcm[i] & 255));
            if (Math.abs(sample) >= gate) return true;
        }
        return false;
    }
}
