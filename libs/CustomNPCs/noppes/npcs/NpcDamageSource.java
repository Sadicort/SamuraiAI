package noppes.npcs;

import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;

public class NpcDamageSource extends EntityDamageSource {
   public NpcDamageSource(String par1Str, Entity limbSwingAmountEntity) {
      super(par1Str, limbSwingAmountEntity);
   }

   public boolean m_7986_() {
      return false;
   }
}
