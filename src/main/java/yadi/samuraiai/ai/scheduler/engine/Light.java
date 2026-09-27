package yadi.samuraiai.ai.scheduler.engine;

/** The cheap facts about an NPC needed to decide how often to look after it: what it is, where, and how far the nearest player is. */
public record Light(String typeId, String dimension, double x, double y, double z, double playerDistance) { }
