package noppes.npcs.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import noppes.npcs.ModelData;
import noppes.npcs.ModelEyeData;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.parts.MpmPartData;
import noppes.npcs.constants.EnumParts;

public class EntityCustomNpc extends EntityNPCFlying {
   public ModelData modelData = new ModelData(this);

   public EntityCustomNpc(EntityType<? extends PathfinderMob> type, Level world) {
      super(type, world);
   }

   @Override
   public void m_7378_(CompoundTag compound) {
      if (compound.m_128441_("NpcModelData")) {
         this.modelData.load(compound.m_128469_("NpcModelData"));
      }

      super.m_7378_(compound);
   }

   @Override
   public void m_7380_(CompoundTag compound) {
      super.m_7380_(compound);
      compound.m_128365_("NpcModelData", this.modelData.save());
   }

   public boolean m_20086_(CompoundTag compound) {
      boolean bo = super.m_20086_(compound);
      if (bo) {
         String s = this.m_20078_();
         if (s.equals("minecraft:customnpcs.customnpc")) {
            compound.m_128359_("id", "customnpcs:customnpc");
         }
      }

      return bo;
   }

   @Override
   public void m_8119_() {
      super.m_8119_();
      if (this.isClientSide()) {
         LivingEntity entity = this.modelData.getEntity(this);
         if (entity != null) {
            try {
               entity.m_8119_();
            } catch (Exception var3) {
            }

            EntityUtil.Copy(this, entity);
         }
      }

      for (MpmPartData pd : this.modelData.mpmParts) {
         if (pd instanceof ModelEyeData) {
            ((ModelEyeData)pd).update(this);
         }
      }
   }

   public boolean m_7998_(Entity par1Entity, boolean force) {
      boolean b = super.m_7998_(par1Entity, force);
      this.m_6210_();
      return b;
   }

   public void m_6210_() {
      Entity entity = this.modelData.getEntity(this);
      if (entity != null) {
         entity.m_6210_();
      }

      super.m_6210_();
   }

   @Override
   public EntityDimensions m_6972_(Pose pos) {
      Entity entity = this.modelData.getEntity(this);
      if (entity == null) {
         float height = 1.9F - this.modelData.getBodyY() + (this.modelData.getPartConfig(EnumParts.HEAD).scaleY - 1.0F) / 2.0F;
         if (this.baseSize.f_20378_ != height) {
            this.baseSize = new EntityDimensions(this.baseSize.f_20377_, height, false);
         }

         return super.m_6972_(pos);
      } else {
         EntityDimensions size = entity.m_6972_(pos);
         if (entity instanceof EntityNPCInterface) {
            return size;
         }

         float width = size.f_20377_ / 5.0F * this.display.getSize();
         float height = size.f_20378_ / 5.0F * this.display.getSize();
         if (width < 0.1F) {
            width = 0.1F;
         }

         if (height < 0.1F) {
            height = 0.1F;
         }

         if (this.display.getHitboxState() == 1 || this.isKilled() && this.stats.hideKilledBody) {
            width = 1.0E-5F;
         }

         if (width / 2.0F > this.f_19853_.getMaxEntityRadius()) {
            this.f_19853_.increaseMaxEntityRadius(width / 2.0F);
         }

         return new EntityDimensions(width, height, false);
      }
   }
}
