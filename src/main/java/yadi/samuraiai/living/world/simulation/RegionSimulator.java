package yadi.samuraiai.living.world.simulation;

import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.streaming.SimulationLevel;

/**
 * Something that advances a region's state over a stretch of time without loading chunks or entities: the wildlife (inside
 * the world), and — registered by the hub — the region's villages, economy and families. A step may cover an hour or a week;
 * implementations must scale their rates by {@code to - from} and never loop per tick.
 */
public interface RegionSimulator {
    String name();

    void simulate(Region region, long from, long to, SimulationLevel level, boolean catchUp);
}
