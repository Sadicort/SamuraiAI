package yadi.samuraiai.ai.scheduler.engine;

/** Where a candidate came from; among equals, the more authoritative source wins a conflict. */
public enum Source {
    ROUTINE(1), NEED(2), EMOTION(2), ZONE(3), GROUP(4), EVENT(5), SURVIVAL(6);

    private final int authority;
    Source(int authority) { this.authority = authority; }
    public int authority() { return authority; }
}
