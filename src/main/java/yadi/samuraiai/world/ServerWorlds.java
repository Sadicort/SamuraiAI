package yadi.samuraiai.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Narrow lookup helpers over the running server.
 *
 * <p>Concentrating them here means the perception system, the controller and
 * the chat listener share one implementation of "which level is this
 * dimension key?" instead of three subtly different ones, and every caller
 * gets the same null-safety: before the server exists, or after it stops,
 * everything returns empty rather than throwing.
 */
public final class ServerWorlds {

    private ServerWorlds() {
    }

    public static Optional<MinecraftServer> server() {
        return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer());
    }

    /**
     * Resolves a dimension key such as "minecraft:overworld".
     *
     * <p>Iterating the loaded levels and comparing their keys avoids building
     * a {@code ResourceKey} from a string that may not parse — a malformed key
     * here yields empty instead of an exception on the server thread.
     */
    public static Optional<ServerLevel> level(String dimensionKey) {

        if (dimensionKey == null) {
            return Optional.empty();
        }

        return server().flatMap(server -> {

            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().location().toString().equals(dimensionKey)) {
                    return Optional.of(level);
                }
            }

            return Optional.empty();
        });
    }

    public static String dimensionKeyOf(ServerLevel level) {
        return level.dimension().location().toString();
    }

    public static Optional<ServerPlayer> playerByName(String name) {

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return server().map(server -> server.getPlayerList().getPlayerByName(name));
    }

    public static Optional<ServerPlayer> playerById(UUID id) {

        if (id == null) {
            return Optional.empty();
        }

        return server().map(server -> server.getPlayerList().getPlayer(id));
    }

    public static List<ServerPlayer> onlinePlayers() {
        return server().map(server -> server.getPlayerList().getPlayers()).orElseGet(List::of);
    }
}
