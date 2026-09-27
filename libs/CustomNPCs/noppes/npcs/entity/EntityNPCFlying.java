package noppes.npcs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class EntityNPCFlying extends EntityNPCInterface {
   public EntityNPCFlying(EntityType<? extends PathfinderMob> type, Level world) {
      super(type, world);
   }

   @Override
   public boolean canFly() {
      return this.ais.movementType > 0;
   }

   @Override
   public boolean m_142535_(float distance, float damageMultiplier, DamageSource source) {
      return !this.canFly() ? super.m_142535_(distance, damageMultiplier, source) : false;
   }

   protected void m_7840_(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
      if (!this.canFly()) {
         super.m_7840_(y, onGroundIn, state, pos);
      }
   }

   @Override
   public void m_7023_(Vec3 v) {
      if (this.canFly() && (!this.m_6084_() || !this.m_20160_() || !this.ais.mountControl || this.getControllingPassenger() == null)) {
         Vec3 m = this.m_20184_();
         if (!this.m_20069_() && this.ais.movementType == 2) {
            m = m.m_82492_(0.0, 0.15, 0.0);
         }

         if (this.m_20069_() && this.ais.movementType == 1) {
            this.m_19920_(0.02F, v);
            this.m_6478_(MoverType.SELF, m);
            m = this.m_20184_().m_82490_(0.8);
         } else if (this.m_20077_()) {
            this.m_19920_(0.02F, v);
            this.m_6478_(MoverType.SELF, m);
            m = this.m_20184_().m_82490_(0.5);
         } else {
            BlockPos ground = new BlockPos(this.m_20185_(), this.m_20186_() - 1.0, this.m_20189_());
            float f = 0.91F;
            if (this.f_19861_) {
               f = this.f_19853_.m_8055_(ground).getFriction(this.f_19853_, ground, this) * 0.91F;
            }

            float f1 = 0.16277137F / (f * f * f);
            f = 0.91F;
            if (this.f_19861_) {
               f = this.f_19853_.m_8055_(ground).getFriction(this.f_19853_, ground, this) * 0.91F;
            }

            this.m_19920_(this.f_19861_ ? 0.1F * f1 : 0.02F, v);
            this.m_6478_(MoverType.SELF, this.m_20184_());
            m = this.m_20184_().m_82490_(f);
         }

         this.m_20256_(m);
         this.m_21043_(this, false);
      } else {
         super.m_7023_(v);
      }
   }

   public boolean m_6147_() {
      return false;
   }
}
