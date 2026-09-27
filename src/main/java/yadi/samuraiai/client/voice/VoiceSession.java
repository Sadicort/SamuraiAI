package yadi.samuraiai.client.voice;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class VoiceSession {
    public enum State { IDLE, LISTENING, PROCESSING, SUCCESS, ERROR, CANCELLED }
    private final long startedAt = System.nanoTime();
    private final UUID id = UUID.randomUUID();
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
    private volatile float level;
    private volatile String text = "";
    private volatile String error = "";
    public State state() { return state.get(); }
    public UUID id() { return id; }
    public boolean transition(State expected, State next) { return state.compareAndSet(expected, next); }
    public void force(State next) { state.set(next); }
    public long durationMillis() { return (System.nanoTime() - startedAt) / 1_000_000L; }
    public float level() { return level; }
    public void level(float value) { level = Math.max(0f, Math.min(1f, value)); }
    public String text() { return text; }
    public void text(String value) { text = value == null ? "" : value; }
    public String error() { return error; }
    public void error(String value) { error = value == null ? "" : value; }
    public String startedAt() { return Instant.now().toString(); }
}
