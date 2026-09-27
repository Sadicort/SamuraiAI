package yadi.samuraiai.client.voice.audio;

import yadi.samuraiai.client.voice.VoicePermissionChecker;

/** Keeps permission/device probing behind the audio layer. */
public final class AudioPermissionChecker {
    private AudioPermissionChecker() {}
    public static VoicePermissionChecker.Result check(String preferred) { return VoicePermissionChecker.check(preferred); }
}
