package noppes.npcs.api.wrapper;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.data.IMark;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.data.MarkData;

public class EntityLivingBaseWrapper<T extends LivingEntity> extends EntityWrapper<T> implements IEntityLiving {
   public EntityLivingBaseWrapper(T entity) {
      super(entity);
   }

   @Override
   public float getHealth() {
      return this.entity.m_21223_();
   }

   @Override
   public void setHealth(float health) {
      this.entity.m_21153_(health);
   }

   @Override
   public float getMaxHealth() {
      return this.entity.m_21233_();
   }

   @Override
   public void setMaxHealth(float health) {
      if (!(health < 0.0F)) {
         this.entity.m_21051_(Attributes.f_22276_).m_22100_(health);
      }
   }

   @Override
   public boolean isAttacking() {
      return this.entity.m_21188_() != null;
   }

   @Override
   public void setAttackTarget(IEntityLiving living) {
      if (living == null) {
         this.entity.m_6703_(null);
      } else {
         this.entity.m_6703_(living.getMCEntity());
      }
   }

   @Override
   public IEntityLiving getAttackTarget() {
      return (IEntityLiving)NpcAPI.Instance().getIEntity(this.entity.m_21188_());
   }

   @Override
   public IEntityLiving getLastAttacked() {
      return (IEntityLiving)NpcAPI.Instance().getIEntity(this.entity.m_21214_());
   }

   @Override
   public int getLastAttackedTime() {
      return this.entity.m_21215_();
   }

   @Override
   public boolean canSeeEntity(IEntity entity) {
      return this.entity.m_142582_(entity.getMCEntity());
   }

   @Override
   public void swingMainhand() {
      this.entity.m_6674_(InteractionHand.MAIN_HAND);
   }

   @Override
   public void swingOffhand() {
      this.entity.m_6674_(InteractionHand.OFF_HAND);
   }

   @Override
   public void addPotionEffect(int effect, int duration, int strength, boolean hideParticles) {
      MobEffect p = MobEffect.m_19453_(effect);
      if (p != null) {
         if (strength < 0) {
            strength = 0;
         } else if (strength > 255) {
            strength = 255;
         }

         if (duration < 0) {
            duration = 0;
         } else if (duration > 1000000) {
            duration = 1000000;
         }

         if (!p.m_8093_()) {
            duration *= 20;
         }

         if (duration == 0) {
            this.entity.m_21195_(p);
         } else {
            this.entity.m_7292_(new MobEffectInstance(p, duration, strength, false, hideParticles));
         }
      }
   }

   @Override
   public void clearPotionEffects() {
      this.entity.m_21219_();
   }

   @Override
   public int getPotionEffect(int effect) {
      MobEffectInstance pf = this.entity.m_21124_(MobEffect.m_19453_(effect));
      return pf == null ? -1 : pf.m_19564_();
   }

   @Override
   public IItemStack getMainhandItem() {
      return NpcAPI.Instance().getIItemStack(this.entity.m_21205_());
   }

   @Override
   public void setMainhandItem(IItemStack item) {
      this.entity.m_21008_(InteractionHand.MAIN_HAND, item == null ? ItemStack.f_41583_ : item.getMCItemStack());
   }

   @Override
   public IItemStack getOffhandItem() {
      return NpcAPI.Instance().getIItemStack(this.entity.m_21206_());
   }

   @Override
   public void setOffhandItem(IItemStack item) {
      this.entity.m_21008_(InteractionHand.OFF_HAND, item == null ? ItemStack.f_41583_ : item.getMCItemStack());
   }

   @Override
   public IItemStack getArmor(int slot) {
      if (slot >= 0 && slot <= 3) {
         return NpcAPI.Instance().getIItemStack(this.entity.m_6844_(this.getSlot(slot)));
      } else {
         throw new CustomNPCsException("Wrong slot id:" + slot);
      }
   }

   @Override
   public void setArmor(int slot, IItemStack item) {
      if (slot >= 0 && slot <= 3) {
         this.entity.m_8061_(this.getSlot(slot), item == null ? ItemStack.f_41583_ : item.getMCItemStack());
      } else {
         throw new CustomNPCsException("Wrong slot id:" + slot);
      }
   }

   private EquipmentSlot getSlot(int slot) {
      if (slot == 3) {
         return EquipmentSlot.HEAD;
      } else if (slot == 2) {
         return EquipmentSlot.CHEST;
      } else if (slot == 1) {
         return EquipmentSlot.LEGS;
      } else {
         return slot == 0 ? EquipmentSlot.FEET : null;
      }
   }

   @Override
   public float getRotation() {
      return this.entity.f_20883_;
   }

   @Override
   public void setRotation(float rotation) {
      this.entity.f_20883_ = rotation;
   }

   @Override
   public int getType() {
      return 5;
   }

   @Override
   public boolean typeOf(int type) {
      return type == 5 ? true : super.typeOf(type);
   }

   @Override
   public boolean isChild() {
      return this.entity.m_6162_();
   }

   @Override
   public IMark addMark(int type) {
      MarkData data = MarkData.get(this.entity);
      return data.addMark(type);
   }

   @Override
   public void removeMark(IMark mark) {
      MarkData data = MarkData.get(this.entity);
      data.marks.remove(mark);
      data.syncClients();
   }

   @Override
   public IMark[] getMarks() {
      MarkData data = MarkData.get(this.entity);
      return data.marks.toArray(new IMark[data.marks.size()]);
   }

   @Override
   public float getMoveForward() {
      return this.entity.f_20902_;
   }

   @Override
   public void setMoveForward(float move) {
      this.entity.f_20902_ = move;
   }

   @Override
   public float getMoveStrafing() {
      return this.entity.f_20900_;
   }

   @Override
   public void setMoveStrafing(float move) {
      this.entity.f_20900_ = move;
   }

   @Override
   public float getMoveVertical() {
      return this.entity.f_20901_;
   }

   @Override
   public void setMoveVertical(float move) {
      this.entity.f_20901_ = move;
   }
}
