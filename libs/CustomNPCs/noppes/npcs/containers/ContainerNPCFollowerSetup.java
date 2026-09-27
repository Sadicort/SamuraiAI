package noppes.npcs.containers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleFollower;

public class ContainerNPCFollowerSetup extends AbstractContainerMenu {
   private RoleFollower role;

   public ContainerNPCFollowerSetup(int containerId, Inventory playerInventory, int entityId) {
      super(CustomContainer.container_followersetup, containerId);
      EntityNPCInterface npc = (EntityNPCInterface)playerInventory.f_35978_.f_19853_.m_6815_(entityId);
      this.role = (RoleFollower)npc.role;

      for (int i1 = 0; i1 < 3; i1++) {
         this.m_38897_(new Slot(this.role.inventory, i1, 44, 39 + i1 * 25));
      }

      for (int i1 = 0; i1 < 3; i1++) {
         for (int l1 = 0; l1 < 9; l1++) {
            this.m_38897_(new Slot(playerInventory, l1 + i1 * 9 + 9, 8 + l1 * 18, 113 + i1 * 18));
         }
      }

      for (int j1 = 0; j1 < 9; j1++) {
         this.m_38897_(new Slot(playerInventory, j1, 8 + j1 * 18, 171));
      }
   }

   public ItemStack m_7648_(Player par1Player, int i) {
      ItemStack itemstack = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(i);
      if (slot != null && slot.m_6657_()) {
         ItemStack itemstack1 = slot.m_7993_();
         itemstack = itemstack1.m_41777_();
         if (i >= 0 && i < 3) {
            if (!this.m_38903_(itemstack1, 3, 38, true)) {
               return ItemStack.f_41583_;
            }
         } else if (i >= 3 && i < 30) {
            if (!this.m_38903_(itemstack1, 30, 38, false)) {
               return ItemStack.f_41583_;
            }
         } else if (i >= 30 && i < 38) {
            if (!this.m_38903_(itemstack1, 3, 29, false)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(itemstack1, 3, 38, false)) {
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

   public boolean m_6875_(Player entityplayer) {
      return true;
   }
}
