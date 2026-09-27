package yadi.samuraiai.ai.perception.sensors;

/**
 * One sense. A sensor observes through the {@link SensorContext} and reports stimuli, transitions, sounds and suspicion
 * requests into its output. It never decides anything and never touches the NPC's brain, movement or goals.
 */
public interface Sensor {
    SensorType type();

    /** Runs one scan. Runtime exceptions are caught by the engine and park this sensor in FAILED for a cooldown. */
    void scan(SensorContext context);
}
