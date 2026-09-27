package yadi.samuraiai.ai.scheduler.stack;

/** A minor activity that runs alongside the main routine (the multi-behaviour stack): what it occupies and how important it is. */
public enum BackgroundBehavior {
    STAY_ALERT(Channel.LOOK, 50), SCAN_SURROUNDINGS(Channel.LOOK, 30), KEEP_FORMATION(Channel.PACE, 40),
    GREET_NEARBY(Channel.TALK, 20), IDLE_FIDGET(Channel.POSTURE, 10);

    private final Channel channel;
    private final int priority;
    BackgroundBehavior(Channel channel, int priority) { this.channel = channel; this.priority = priority; }
    public Channel channel() { return channel; }
    public int priority() { return priority; }
}
