package yadi.samuraiai.ai.memory.model;

/** A stretch of a long experience (a journey, a battle), so it need not be one undifferentiated block. */
public record Chapter(long start, long end, String label) {
    public Chapter { label = label == null ? "" : label; }
}
