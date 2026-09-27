package noppes.npcs.api.wrapper;

import net.minecraft.world.entity.npc.Villager;
import noppes.npcs.api.entity.IVillager;

public class VillagerWrapper<T extends Villager> extends EntityLivingWrapper<T> implements IVillager {
   public VillagerWrapper(T entity) {
      super(entity);
   }

   public String getProfession() {
      return this.entity.m_7141_().m_35571_().toString();
   }

   public String VillagerType() {
      return this.entity.m_7141_().m_35560_().toString();
   }

   @Override
   public int getType() {
      return 9;
   }

   @Override
   public boolean typeOf(int type) {
      return type == 9 ? true : super.typeOf(type);
   }
}
