package yadi.samuraiai.ai.scheduler.engine;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Where the scheduler gets its facts. The world adapter implements it over the live server; tests implement it over a
 * simulated village. {@link #light} is cheap and asked often; {@link #input} builds the full picture and is asked only for
 * the NPCs being evaluated this tick.
 */
public interface InputSource {
    Collection<UUID> npcs();
    Optional<Light> light(UUID id);
    Optional<SchedulerInput> input(UUID id, long tick, long worldTime);
}
