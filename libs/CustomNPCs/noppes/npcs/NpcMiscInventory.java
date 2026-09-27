package noppes.npcs;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NpcMiscInventory extends SimpleContainer {
   public final NonNullList<ItemStack> items;
   public int stackLimit = 64;
   private int size;

   public NpcMiscInventory(int size) {
      super(new ItemStack[0]);
      this.size = size;
      this.items = NonNullList.m_122780_(size, ItemStack.f_41583_);
   }

   public CompoundTag getToNBT() {
      CompoundTag nbttagcompound = new CompoundTag();
      nbttagcompound.m_128365_("NpcMiscInv", NBTTags.nbtItemStackList(this.items));
      return nbttagcompound;
   }

   public void setFromNBT(CompoundTag nbttagcompound) {
      NBTTags.getItemStackList(nbttagcompound.m_128437_("NpcMiscInv", 10), this.items);
   }

   public int m_6643_() {
      return this.size;
   }

   public ItemStack m_8020_(int index) {
      return (ItemStack)this.items.get(index);
   }

   public ItemStack m_7407_(int index, int count) {
      return ContainerHelper.m_18969_(this.items, index, count);
   }

   public boolean removeItem(ItemStack eating, int decrease) {
      for (int slot = 0; slot < this.items.size(); slot++) {
         ItemStack item = (ItemStack)this.items.get(slot);
         if (!item.m_41619_() && eating == item && item.m_41613_() >= decrease) {
            item.m_41620_(decrease);
            if (item.m_41613_() <= 0) {
               this.items.set(slot, ItemStack.f_41583_);
            }

            return true;
         }
      }

      return false;
   }

   public ItemStack m_8016_(int var1) {
      return (ItemStack)this.items.set(var1, ItemStack.f_41583_);
   }

   public void m_6836_(int var1, ItemStack var2) {
      if (var1 < this.m_6643_()) {
         this.items.set(var1, var2);
      }
   }

   public int m_6893_() {
      return this.stackLimit;
   }

   public boolean m_6542_(Player var1) {
      return true;
   }

   public boolean m_7013_(int i, ItemStack itemstack) {
      return true;
   }

   public void m_6596_() {
   }

   public boolean addItemStack(ItemStack item) {
      boolean merged = false;

      ItemStack mergable;
      while (!(mergable = this.getMergableItem(item)).m_41619_() && mergable.m_41613_() > 0) {
         int size = mergable.m_41741_() - mergable.m_41613_();
         if (size > item.m_41613_()) {
            mergable.m_41764_(mergable.m_41741_());
            item.m_41764_(item.m_41613_() - size);
            merged = true;
         } else {
            mergable.m_41764_(mergable.m_41613_() + item.m_41613_());
            item.m_41764_(0);
         }
      }

      if (item.m_41613_() <= 0) {
         return true;
      } else {
         int slot = this.firstFreeSlot();
         if (slot >= 0) {
            this.items.set(slot, item.m_41777_());
            item.m_41764_(0);
            return true;
         } else {
            return merged;
         }
      }
   }

   public ItemStack getMergableItem(ItemStack item) {
      for (ItemStack is : this.items) {
         if (NoppesUtilPlayer.compareItems(item, is, false, false) && is.m_41613_() < is.m_41741_()) {
            return is;
         }
      }

      return ItemStack.f_41583_;
   }

   public int firstFreeSlot() {
      for (int i = 0; i < this.m_6643_(); i++) {
         if (((ItemStack)this.items.get(i)).m_41619_()) {
            return i;
         }
      }

      return -1;
   }

   public void setSize(int i) {
      this.size = i;
   }

   public void m_5856_(Player player) {
   }

   public void m_5785_(Player player) {
   }

   public void m_6211_() {
   }

   public boolean m_7983_() {
      for (int slot = 0; slot < this.m_6643_(); slot++) {
         ItemStack item = this.m_8020_(slot);
         if (!NoppesUtilServer.IsItemStackNull(item) && !item.m_41619_()) {
            return false;
         }
      }

      return true;
   }
}
