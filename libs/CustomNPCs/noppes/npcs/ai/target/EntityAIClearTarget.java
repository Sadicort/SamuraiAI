package noppes.npcs.ai.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIClearTarget extends Goal {
   private EntityNPCInterface npc;
   private LivingEntity target;

   public EntityAIClearTarget(EntityNPCInterface npc) {
      this.npc = npc;
   }

   public boolean m_8036_() {
      this.target = this.npc.m_5448_();
      if (this.target == null) {
         return false;
      } else {
         return this.npc.getOwner() != null && !this.npc.isInRange(this.npc.getOwner(), this.npc.stats.aggroRange * 2)
            ? true
            : this.npc.combatHandler.checkTarget();
      }
   }

   public void m_8056_() {
      this.npc.m_6710_(null);
      if (this.target == this.npc.m_21188_()) {
         this.npc.m_6703_(null);
      }

      super.m_8056_();
   }

   public void m_8041_() {
      this.npc.m_21573_().m_26573_();
   }
}
