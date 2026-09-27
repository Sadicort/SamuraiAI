package yadi.samuraiai.client.voice.audio;

/** In-memory PCM buffer. It deliberately has no file or network representation. */
public record AudioBuffer(byte[] pcm, int sampleRate, int channels, long capturedAtMillis) {
    public AudioBuffer {
        pcm = pcm == null ? new byte[0] : pcm.clone();
        if (sampleRate <= 0 || channels <= 0) throw new IllegalArgumentException("Formato de audio inválido");
    }

    @Override public byte[] pcm() { return pcm.clone(); }
}
