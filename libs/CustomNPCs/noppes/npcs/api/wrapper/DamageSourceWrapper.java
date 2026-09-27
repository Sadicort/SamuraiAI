package noppes.npcs.api.wrapper;

import net.minecraft.world.damagesource.DamageSource;
import noppes.npcs.api.IDamageSource;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;

public class DamageSourceWrapper implements IDamageSource {
   private DamageSource source;

   public DamageSourceWrapper(DamageSource source) {
      this.source = source;
   }

   @Override
   public String getType() {
      return this.source.m_19385_();
   }

   @Override
   public boolean isUnblockable() {
      return this.source.m_19376_();
   }

   @Override
   public boolean isProjectile() {
      return this.source.m_19360_();
   }

   @Override
   public DamageSource getMCDamageSource() {
      return this.source;
   }

   @Override
   public IEntity getTrueSource() {
      return NpcAPI.Instance().getIEntity(this.source.m_7639_());
   }

   @Override
   public IEntity getImmediateSource() {
      return NpcAPI.Instance().getIEntity(this.source.m_7640_());
   }
}
