package noppes.npcs.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomEntities;
import noppes.npcs.ModelData;

public class EntityNpcSlime extends EntityNPCInterface {
   public EntityNpcSlime(EntityType<? extends EntityNPCInterface> type, Level world) {
      super(type, world);
      this.scaleX = 2.0F;
      this.scaleY = 2.0F;
      this.scaleZ = 2.0F;
      this.display.setSkinTexture("customnpcs:textures/entity/slime/slime.png");
      this.baseSize = new EntityDimensions(0.8F, 0.8F, false);
   }

   @Override
   public EntityDimensions m_6972_(Pose pos) {
      return new EntityDimensions(0.8F, 0.8F, false);
   }

   @Override
   public void m_8119_() {
      this.m_146870_();
      this.m_21557_(true);
      if (!this.f_19853_.f_46443_) {
         CompoundTag compound = new CompoundTag();
         this.m_7380_(compound);
         EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, this.f_19853_);
         npc.m_7378_(compound);
         ModelData data = npc.modelData;
         data.setEntity(ForgeRegistries.ENTITY_TYPES.getKey(CustomEntities.entityNpcSlime));
         this.f_19853_.m_7967_(npc);
      }

      super.m_8119_();
   }
}
