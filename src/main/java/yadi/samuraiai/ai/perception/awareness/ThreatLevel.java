package yadi.samuraiai.ai.perception.awareness;

public enum ThreatLevel {
    SAFE, WARNING, DANGER, CRITICAL;

    public boolean atLeast(ThreatLevel other) { return compareTo(other) >= 0; }
}
