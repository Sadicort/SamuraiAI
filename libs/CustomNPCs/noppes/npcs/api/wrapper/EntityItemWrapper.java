package noppes.npcs.api.wrapper;

import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntityItem;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.mixin.ItemEntityMixin;

public class EntityItemWrapper<T extends ItemEntity> extends EntityWrapper<T> implements IEntityItem {
   public EntityItemWrapper(T entity) {
      super(entity);
   }

   @Override
   public String getOwner() {
      return this.entity.m_32056_() == null ? null : this.entity.m_32056_().toString();
   }

   @Override
   public void setOwner(String name) {
      this.entity.m_32047_(UUID.fromString(name));
   }

   @Override
   public int getPickupDelay() {
      return ((ItemEntityMixin)this.entity).pickupDelay();
   }

   @Override
   public void setPickupDelay(int delay) {
      this.entity.m_32010_(delay);
   }

   @Override
   public int getType() {
      return 6;
   }

   @Override
   public long getAge() {
      return this.entity.m_32059_();
   }

   @Override
   public void setAge(long age) {
      age = Math.max(Math.min(age, 2147483647L), -2147483648L);
      ((ItemEntityMixin)this.entity).age((int)age);
   }

   @Override
   public int getLifeSpawn() {
      return this.entity.lifespan;
   }

   @Override
   public void setLifeSpawn(int age) {
      this.entity.lifespan = age;
   }

   @Override
   public IItemStack getItem() {
      return NpcAPI.Instance().getIItemStack(this.entity.m_32055_());
   }

   @Override
   public void setItem(IItemStack item) {
      ItemStack stack = item == null ? ItemStack.f_41583_ : item.getMCItemStack();
      this.entity.m_32045_(stack);
   }
}
