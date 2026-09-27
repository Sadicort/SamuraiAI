package yadi.samuraiai.ai.navigation.graph;

import yadi.samuraiai.ai.navigation.terrain.BlockProfile;

/**
 * The only door through which the navigation core reads the world. Implementations must be used on the
 * thread that owns the world (the server thread in Minecraft); the core never caches a view across ticks.
 */
public interface NavWorldView {
    String dimension();
    int minY();
    int maxY();
    boolean isLoaded(int chunkX, int chunkZ);
    BlockProfile profile(int x, int y, int z);
    NavEnvironment environment(NavPos at);
    /** Monotonic game time in ticks, used for expiry of caches and danger zones. */
    long gameTick();

    default BlockProfile profile(NavPos p) { return profile(p.x(), p.y(), p.z()); }
    default boolean isLoaded(NavPos p) { return isLoaded(p.chunkX(), p.chunkZ()); }
}
