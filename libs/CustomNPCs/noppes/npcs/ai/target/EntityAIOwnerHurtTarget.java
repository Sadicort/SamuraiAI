package noppes.npcs.ai.target;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIOwnerHurtTarget extends TargetGoal {
   EntityNPCInterface npc;
   LivingEntity theTarget;
   private int field_142050_e;

   public EntityAIOwnerHurtTarget(EntityNPCInterface npc) {
      super(npc, false);
      this.npc = npc;
      this.m_7021_(EnumSet.of(Flag.TARGET));
   }

   public boolean m_8036_() {
      if (this.npc.isFollower() && this.npc.role.defendOwner()) {
         LivingEntity entitylivingbase = this.npc.getOwner();
         if (entitylivingbase == null) {
            return false;
         }

         this.theTarget = entitylivingbase.m_21214_();
         int i = entitylivingbase.m_21215_();
         return i != this.field_142050_e && this.m_26150_(this.theTarget, TargetingConditions.f_26872_);
      } else {
         return false;
      }
   }

   public void m_8056_() {
      this.npc.m_6710_(this.theTarget);
      LivingEntity entitylivingbase = this.npc.getOwner();
      if (entitylivingbase != null) {
         this.field_142050_e = entitylivingbase.m_21215_();
      }

      super.m_8056_();
   }
}
