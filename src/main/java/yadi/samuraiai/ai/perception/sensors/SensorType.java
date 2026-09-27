package yadi.samuraiai.ai.perception.sensors;

import java.util.function.ToIntFunction;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;

/** The sensors an NPC has, each with the setting that gives its base scan interval (event-driven ones scan every tick, cheaply). */
public enum SensorType {
    ENTITY(PerceptionSettings::entityInterval), VISION(PerceptionSettings::visionInterval), TOUCH(PerceptionSettings::entityInterval),
    MOVEMENT(PerceptionSettings::movementInterval), HEARING(PerceptionSettings::hearingInterval), DAMAGE(s -> 1), CONVERSATION(s -> 1), VOICE(s -> 1),
    ENVIRONMENT(PerceptionSettings::environmentInterval), LIGHT(PerceptionSettings::lightInterval), WEATHER(PerceptionSettings::weatherInterval),
    BLOCK(PerceptionSettings::blockInterval), SMELL(PerceptionSettings::smellInterval);

    private final ToIntFunction<PerceptionSettings> interval;
    SensorType(ToIntFunction<PerceptionSettings> interval) { this.interval = interval; }
    public int baseInterval(PerceptionSettings settings) { return interval.applyAsInt(settings); }
}
