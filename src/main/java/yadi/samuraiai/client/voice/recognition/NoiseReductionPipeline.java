package yadi.samuraiai.client.voice.recognition;

/** Lightweight deterministic gate. No additional model or external process is used. */
public final class NoiseReductionPipeline {
    public byte[] process(byte[] pcm, float threshold) {
        if (pcm == null) return new byte[0];
        byte[] result = pcm.clone();
        int gate = (int)(Math.max(0f, Math.min(1f, threshold)) * Short.MAX_VALUE);
        for (int i = 0; i + 1 < result.length; i += 2) {
            short sample = (short)((result[i + 1] << 8) | (result[i] & 255));
            if (Math.abs(sample) < gate) { result[i] = 0; result[i + 1] = 0; }
        }
        return result;
    }
}
