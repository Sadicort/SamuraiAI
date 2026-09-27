package noppes.npcs.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomEntities;
import noppes.npcs.ModelData;

public class EntityNpcPony extends EntityNPCInterface {
   public boolean isPegasus = false;
   public boolean isUnicorn = false;
   public boolean isFlying = false;
   public ResourceLocation checked = null;

   public EntityNpcPony(EntityType<? extends EntityNPCInterface> type, Level world) {
      super(type, world);
      this.display.setSkinTexture("customnpcs:textures/entity/ponies/minelpderpyhooves.png");
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
         data.setEntity(ForgeRegistries.ENTITY_TYPES.getKey(CustomEntities.entityNpcPony));
         this.f_19853_.m_7967_(npc);
      }

      super.m_8119_();
   }
}
