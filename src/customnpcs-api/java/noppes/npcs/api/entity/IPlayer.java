package noppes.npcs.api.entity;

import net.minecraft.server.level.ServerPlayer;

/**
 * Empty bridge: SamuraiAI only uses IPlayer as the parameter type for
 * {@link ICustomNpc#sayTo} and to reach the real {@code ServerPlayer} via
 * the inherited {@link IEntity#getMCEntity()}. See {@link IMob} for why
 * trimming a stub interface down like this is safe.
 */
public interface IPlayer<T extends ServerPlayer> extends IEntityLiving<T> {
}
