package yadi.samuraiai.ai.scheduler.group;

import java.util.UUID;

/** A call for help from a group member: where the trouble is and how serious (1 = unease, 3 = grave danger). */
public record Alarm(String groupId, UUID source, String dimension, double x, double y, double z, int level, long tick) { }
