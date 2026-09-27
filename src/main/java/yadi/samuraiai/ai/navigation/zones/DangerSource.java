package yadi.samuraiai.ai.navigation.zones;

import java.util.function.Consumer;

/**
 * Contract through which other engines (perception threats, world events) feed the danger map without
 * navigation knowing their internals.
 */
@FunctionalInterface
public interface DangerSource {
    void collect(String dimension, long tick, Consumer<DangerZone> out);
}
