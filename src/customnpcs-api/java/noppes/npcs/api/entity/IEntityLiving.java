package noppes.npcs.api.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Empty bridge: SamuraiAI never calls an IEntityLiving-specific method, only
 * ones inherited from {@link IEntity} or declared directly on
 * {@link ICustomNpc}/{@link IPlayer}. See {@link IMob} for why trimming a
 * stub interface down like this is safe.
 */
public interface IEntityLiving<T extends LivingEntity> extends IEntity<T> {
}
