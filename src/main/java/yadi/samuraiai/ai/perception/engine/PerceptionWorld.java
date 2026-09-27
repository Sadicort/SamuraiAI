package yadi.samuraiai.ai.perception.engine;

import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.perception.environment.EnvironmentSnapshot;

/**
 * The only door through which perception reads the world (the perception counterpart of the navigation world view).
 * Implementations are used on the thread that owns the world; the core never keeps one across a world restart.
 */
public interface PerceptionWorld {
    String dimension();
    long gameTick();
    /** Living entities, items and projectiles within the radius, except {@code exclude}. */
    List<SensedEntity> entitiesNear(double x, double y, double z, double radius, UUID exclude);
    /** 0 = fully transparent, 1 = fully opaque, for the block at these coordinates. */
    double opacity(int x, int y, int z);
    /** Combined block+sky light 0..15 at the block. */
    int lightLevel(int x, int y, int z);
    /** The interesting block kind here, or null when it is an ordinary block. */
    BlockInterest interest(int x, int y, int z);
    boolean isLoaded(int chunkX, int chunkZ);
    EnvironmentSnapshot environment(double x, double y, double z);
}
