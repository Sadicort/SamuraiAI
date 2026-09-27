package noppes.npcs.containers;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilServer;

public class InventoryNPC implements Container {
   private String inventoryTitle;
   private int slotsCount;
   public final NonNullList<ItemStack> inventoryContents;
   private AbstractContainerMenu con;

   public InventoryNPC(String s, int i, AbstractContainerMenu con) {
      this.con = con;
      this.inventoryTitle = s;
      this.slotsCount = i;
      this.inventoryContents = NonNullList.m_122780_(i, ItemStack.f_41583_);
   }

   public ItemStack m_8020_(int i) {
      return (ItemStack)this.inventoryContents.get(i);
   }

   public ItemStack m_7407_(int index, int count) {
      return ContainerHelper.m_18969_(this.inventoryContents, index, count);
   }

   public void m_6836_(int index, ItemStack stack) {
      this.inventoryContents.set(index, stack);
      if (!stack.m_41619_() && stack.m_41613_() > this.m_6893_()) {
         stack.m_41764_(this.m_6893_());
      }
   }

   public int m_6643_() {
      return this.slotsCount;
   }

   public int m_6893_() {
      return 64;
   }

   public boolean m_6542_(Player entityplayer) {
      return false;
   }

   public ItemStack m_8016_(int i) {
      return ContainerHelper.m_18966_(this.inventoryContents, i);
   }

   public boolean m_7013_(int i, ItemStack itemstack) {
      return true;
   }

   public void m_6596_() {
      this.con.m_6199_(this);
   }

   public void m_5856_(Player player) {
   }

   public void m_5785_(Player player) {
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

   public void m_6211_() {
   }
}
