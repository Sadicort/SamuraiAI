package noppes.npcs.api.wrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.IMob;

public class EntityLivingWrapper<T extends Mob> extends EntityLivingBaseWrapper<T> implements IMob {
   public EntityLivingWrapper(T entity) {
      super(entity);
   }

   @Override
   public void navigateTo(double x, double y, double z, double speed) {
      this.entity.m_21573_().m_26573_();
      this.entity.m_21573_().m_26519_(x, y, z, speed * 0.7);
   }

   @Override
   public void clearNavigation() {
      this.entity.m_21573_().m_26573_();
   }

   @Override
   public IPos getNavigationPath() {
      if (!this.isNavigating()) {
         return null;
      }

      Node point = this.entity.m_21573_().m_26570_().m_77395_();
      return point == null ? null : new BlockPosWrapper(new BlockPos(point.f_77271_, point.f_77272_, point.f_77273_));
   }

   @Override
   public boolean isNavigating() {
      return !this.entity.m_21573_().m_26571_();
   }

   @Override
   public boolean isAttacking() {
      return super.isAttacking() || this.entity.m_5448_() != null;
   }

   @Override
   public void setAttackTarget(IEntityLiving living) {
      if (living == null) {
         this.entity.m_6710_(null);
      } else {
         this.entity.m_6710_(living.getMCEntity());
      }

      super.setAttackTarget(living);
   }

   @Override
   public IEntityLiving getAttackTarget() {
      IEntityLiving base = (IEntityLiving)NpcAPI.Instance().getIEntity(this.entity.m_5448_());
      return base != null ? base : super.getAttackTarget();
   }

   @Override
   public boolean canSeeEntity(IEntity entity) {
      return this.entity.m_21574_().m_148306_(entity.getMCEntity());
   }

   @Override
   public void jump() {
      this.entity.m_21569_().m_24901_();
   }
}
