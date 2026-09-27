package yadi.samuraiai.client.voice.recognition;

/** Pure PCM normalization hook; a future provider can replace this implementation. */
public final class AudioNormalizer {
    public byte[] normalize(byte[] pcm, float gain) {
        if (pcm == null) return new byte[0];
        float safeGain = Math.max(.1f, Math.min(4f, gain));
        byte[] result = pcm.clone();
        for (int i = 0; i + 1 < result.length; i += 2) {
            short sample = (short)((result[i + 1] << 8) | (result[i] & 255));
            int normalized = Math.round(sample * safeGain);
            normalized = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, normalized));
            result[i] = (byte) normalized;
            result[i + 1] = (byte) (normalized >> 8);
        }
        return result;
    }
}
