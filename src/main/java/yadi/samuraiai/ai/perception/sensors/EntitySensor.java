package yadi.samuraiai.ai.perception.sensors;

/**
 * Builds the catalog of nearby entities (players, NPCs, hostiles, animals, villagers, objects, projectiles) that vision,
 * touch, movement and hearing all read. It reports no stimuli of its own: it is the census, not an observation, so one
 * world query serves every other sensor in the pass.
 */
public final class EntitySensor implements Sensor {
    @Override public SensorType type() { return SensorType.ENTITY; }

    @Override public void scan(SensorContext ctx) { ctx.refreshCatalog(); }
}
