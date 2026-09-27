package yadi.samuraiai.ai.perception.sensors;

/** Lifecycle of one sensor of one NPC: created, ready, scanning, cooling down until its next scan, disabled, or failed (with a cooldown). */
public enum SensorState { CREATED, READY, SCANNING, COOLDOWN, DISABLED, FAILED }
