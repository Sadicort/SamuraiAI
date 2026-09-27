package noppes.npcs.api.entity;

import net.minecraft.world.entity.Mob;

/**
 * Bridge interface missing from the decompiled dump this API stub was
 * extracted from (ICustomNpc, IAnimal, IMonster and IVillager all reference
 * it but no IMob.java existed in the source). Reconstructed here as an empty
 * type purely so this stub compiles; the real CustomNPCs mod supplies its
 * own IMob at runtime, and SamuraiAI never calls a method through this type
 * that isn't already declared on IEntityLiving/IEntity or ICustomNpc itself.
 */
public interface IMob<T extends Mob> extends IEntityLiving<T> {
}
