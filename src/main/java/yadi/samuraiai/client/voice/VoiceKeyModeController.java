package yadi.samuraiai.client.voice;

/** Edge detector shared by the client tick hook; prevents hold mode from restarting every tick. */
public final class VoiceKeyModeController {
    public enum Action { NONE, START, STOP }
    private boolean previous;
    public Action update(boolean holdMode, boolean down) {
        if (!holdMode) { previous = false; return Action.NONE; }
        Action action = down && !previous ? Action.START : !down && previous ? Action.STOP : Action.NONE;
        previous = down; return action;
    }
    public void reset() { previous = false; }
}
