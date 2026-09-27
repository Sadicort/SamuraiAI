package noppes.npcs.containers;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomContainer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.ServerEventsHandler;

public class ContainerMerchantAdd extends ContainerNpcInterface {
   private Merchant theMerchant = ServerEventsHandler.Merchant;
   private SimpleContainer merchantInventory;
   private final Level level;

   public ContainerMerchantAdd(int containerId, Inventory playerInventory) {
      super(CustomContainer.container_merchantadd, containerId, playerInventory);
      this.level = playerInventory.f_35978_.f_19853_;
      this.merchantInventory = new SimpleContainer(3);
      this.m_38897_(new Slot(this.merchantInventory, 0, 36, 53));
      this.m_38897_(new Slot(this.merchantInventory, 1, 62, 53));
      this.m_38897_(new Slot(this.merchantInventory, 2, 120, 53));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.m_38897_(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.m_38897_(new Slot(playerInventory, var5, 8 + var5 * 18, 142));
      }
   }

   @Override
   public ItemStack m_7648_(Player par1Player, int limbSwingAmount) {
      ItemStack itemstack = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(limbSwingAmount);
      if (slot != null && slot.m_6657_()) {
         ItemStack itemstack1 = slot.m_7993_();
         itemstack = itemstack1.m_41777_();
         if (limbSwingAmount != 0 && limbSwingAmount != 1 && limbSwingAmount != 2) {
            if (limbSwingAmount >= 3 && limbSwingAmount < 30) {
               if (!this.m_38903_(itemstack1, 30, 39, false)) {
                  return ItemStack.f_41583_;
               }
            } else if (limbSwingAmount >= 30 && limbSwingAmount < 39 && !this.m_38903_(itemstack1, 3, 30, false)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(itemstack1, 3, 39, false)) {
            return ItemStack.f_41583_;
         }

         if (itemstack1.m_41613_() == 0) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }

         if (itemstack1.m_41613_() == itemstack.m_41613_()) {
            return ItemStack.f_41583_;
         }

         slot.m_142406_(par1Player, itemstack1);
      }

      return itemstack;
   }

   public void m_6877_(Player par1Player) {
      super.m_6877_(par1Player);
      super.m_6877_(par1Player);
      if (!this.level.f_46443_) {
         ItemStack itemstack = this.merchantInventory.m_8016_(0);
         if (!NoppesUtilServer.IsItemStackNull(itemstack)) {
            par1Player.m_36176_(itemstack, false);
         }

         itemstack = this.merchantInventory.m_8016_(1);
         if (!NoppesUtilServer.IsItemStackNull(itemstack)) {
            par1Player.m_36176_(itemstack, false);
         }
      }
   }
}
