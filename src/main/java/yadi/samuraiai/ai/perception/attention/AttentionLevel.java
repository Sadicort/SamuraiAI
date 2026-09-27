package yadi.samuraiai.ai.perception.attention;

public enum AttentionLevel {
    NONE, LOW, MEDIUM, HIGH, FOCUSED, CRITICAL;

    public boolean atLeast(AttentionLevel other) { return compareTo(other) >= 0; }
}
