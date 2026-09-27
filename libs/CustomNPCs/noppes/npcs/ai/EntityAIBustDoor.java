package noppes.npcs.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

public class EntityAIBustDoor extends DoorInteractGoal {
   private int breakingTime;
   private int field_75358_j = -1;

   public EntityAIBustDoor(Mob par1EntityLiving) {
      super(par1EntityLiving);
   }

   public boolean m_8036_() {
      return !super.m_8036_() ? false : !this.m_25200_();
   }

   public void m_8056_() {
      super.m_8056_();
      this.breakingTime = 0;
   }

   public boolean m_8045_() {
      return this.breakingTime <= 240 && !this.m_25200_() && this.f_25189_.m_20183_().m_123331_(this.f_25190_) < 4.0;
   }

   public void m_8041_() {
      super.m_8041_();
      this.f_25189_.f_19853_.m_6801_(this.f_25189_.m_19879_(), this.f_25190_, -1);
   }

   public void m_8037_() {
      super.m_8037_();
      if (this.f_25189_.m_217043_().m_188503_(20) == 0) {
         this.f_25189_.f_19853_.m_5898_((Player)null, 1010, this.f_25190_, 0);
         this.f_25189_.m_6674_(InteractionHand.MAIN_HAND);
      }

      this.breakingTime++;
      int var1 = (int)(this.breakingTime / 240.0F * 10.0F);
      if (var1 != this.field_75358_j) {
         this.f_25189_.f_19853_.m_6801_(this.f_25189_.m_19879_(), this.f_25190_, var1);
         this.field_75358_j = var1;
      }

      if (this.breakingTime == 240) {
         this.f_25189_.f_19853_.m_7471_(this.f_25190_, false);
         this.f_25189_.f_19853_.m_5898_((Player)null, 1012, this.f_25190_, 0);
         this.f_25189_.f_19853_.m_5898_((Player)null, 2001, this.f_25190_, Block.m_49956_(this.f_25189_.f_19853_.m_8055_(this.f_25190_)));
      }
   }
}
