package yadi.samuraiai.living.world.engine;

import java.util.Map;
import yadi.samuraiai.living.world.streaming.SimulationLevel;

/**
 * The World Runtime as the specification lists it, at one instant: regions by level of detail, open events, active NPCs (as
 * the adapter reports them), the regions simulated without chunks, the calendar and weather references, the economy
 * reference, the history size and the state of the simulation.
 */
public record WorldRuntime(Map<SimulationLevel, Integer> loadedRegions, int activeEvents, int activeNpcs, int abstractRegions, String calendarReference,
                           String weatherState, String economyReference, int worldHistory, long simulationSteps, long catchUps, int settlements, int roads, int population) {
    public WorldRuntime { loadedRegions = Map.copyOf(loadedRegions); }
}
