package noppes.npcs.api.entity;

import net.minecraft.world.entity.Entity;

/**
 * Trimmed to the members {@code integration.customnpcs.CustomNPCsController} actually calls (plus a
 * handful of harmless, dependency-free extras). See {@link IMob} for why
 * this is safe: the real CustomNPCs mod supplies the full interface at
 * runtime, and this stub only has to satisfy the compiler.
 */
public interface IEntity<T extends Entity> {

   double getX();

   double getY();

   double getZ();

   void setPosition(double var1, double var3, double var5);

   void setRotation(float var1);

   float getRotation();

   boolean isSneaking();

   boolean isAlive();

   void despawn();

   void kill();

   T getMCEntity();

   String getUUID();

   String getName();

   void setName(String var1);

   boolean hasCustomName();
}
