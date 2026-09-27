package yadi.samuraiai.ai.perception.awareness;

import yadi.samuraiai.ai.perception.engine.PerceptionSettings;

/**
 * How much the NPC suspects that something is wrong, 0..100. It rises with the events that make an NPC uneasy (movement
 * behind cover, an unexplained noise, a player who vanished, an opened door, a broken block), and drains away slowly.
 * Crossing the raise threshold upward is "raised"; falling below the clear threshold is "cleared", so it does not flap.
 */
public final class SuspicionMeter {
    public enum Change { NONE, RAISED, CLEARED }

    private double value;
    private boolean raised;
    private SuspicionSource lastSource = SuspicionSource.CUSTOM;

    public double value() { return value; }
    public boolean raised() { return raised; }
    public SuspicionSource lastSource() { return lastSource; }

    public Change raise(SuspicionSource source, double amount, double gain, PerceptionSettings s) {
        if (amount <= 0.0D) return Change.NONE;
        value = Math.min(100.0D, value + amount * gain);
        lastSource = source;
        if (!raised && value >= s.suspicionRaiseThreshold()) { raised = true; return Change.RAISED; }
        return Change.NONE;
    }

    public Change decay(long elapsedTicks, PerceptionSettings s) {
        if (value <= 0.0D) return Change.NONE;
        value = Math.max(0.0D, value - s.suspicionDecayPerTick() * Math.max(1L, elapsedTicks));
        if (raised && value < s.suspicionClearThreshold()) { raised = false; return Change.CLEARED; }
        return Change.NONE;
    }

    public void reset() { value = 0.0D; raised = false; }
}
