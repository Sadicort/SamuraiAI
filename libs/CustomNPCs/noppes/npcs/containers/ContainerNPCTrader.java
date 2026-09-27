package noppes.npcs.containers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.event.RoleEvent;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

public class ContainerNPCTrader extends ContainerNpcInterface {
   public RoleTrader role;
   private EntityNPCInterface npc;

   public ContainerNPCTrader(int containerId, Inventory playerInventory, int entityId) {
      super(CustomContainer.container_trader, containerId, playerInventory);
      this.npc = (EntityNPCInterface)playerInventory.f_35978_.f_19853_.m_6815_(entityId);
      this.role = (RoleTrader)this.npc.role;

      for (int i = 0; i < 18; i++) {
         int x = 53;
         x += i % 3 * 72;
         int y = 7;
         y += i / 3 * 21;
         this.m_38897_(new Slot(this.role.inventorySold, i, x, y));
      }

      for (int i1 = 0; i1 < 3; i1++) {
         for (int l1 = 0; l1 < 9; l1++) {
            this.m_38897_(new Slot(playerInventory, l1 + i1 * 9 + 9, 32 + l1 * 18, 140 + i1 * 18));
         }
      }

      for (int j1 = 0; j1 < 9; j1++) {
         this.m_38897_(new Slot(playerInventory, j1, 32 + j1 * 18, 198));
      }
   }

   @Override
   public ItemStack m_7648_(Player par1Player, int i) {
      return ItemStack.f_41583_;
   }

   public void m_150399_(int i, int j, ClickType par3, Player entityplayer) {
      if (par3 == ClickType.PICKUP) {
         if (i < 0 || i >= 18) {
            super.m_150399_(i, j, par3, entityplayer);
         } else if (j != 1) {
            Slot slot = (Slot)this.f_38839_.get(i);
            if (slot != null && slot.m_7993_() != null && !slot.m_7993_().m_41619_()) {
               ItemStack item = slot.m_7993_();
               if (this.canGivePlayer(item, entityplayer)) {
                  ItemStack currency = this.role.inventoryCurrency.m_8020_(i);
                  ItemStack currency2 = this.role.inventoryCurrency.m_8020_(i + 18);
                  if (!this.canBuy(currency, currency2, entityplayer)) {
                     RoleEvent.TradeFailedEvent event = new RoleEvent.TradeFailedEvent(entityplayer, this.npc.wrappedNPC, item, currency, currency2);
                     EventHooks.onNPCRole(this.npc, event);
                     if (event.receiving != null) {
                        this.m_142503_(event.receiving.getMCItemStack());
                     }
                  } else {
                     RoleEvent.TraderEvent event = new RoleEvent.TraderEvent(entityplayer, this.npc.wrappedNPC, item, currency, currency2);
                     if (!EventHooks.onNPCRole(this.npc, event)) {
                        if (event.currency1 != null && !event.currency1.isEmpty()) {
                           currency = event.currency1.getMCItemStack();
                        }

                        if (event.currency2 != null && !event.currency2.isEmpty()) {
                           currency2 = event.currency2.getMCItemStack();
                        }

                        if (this.canBuy(currency, currency2, entityplayer)) {
                           NoppesUtilPlayer.consumeItem(entityplayer, currency, this.role.ignoreDamage, this.role.ignoreNBT);
                           NoppesUtilPlayer.consumeItem(entityplayer, currency2, this.role.ignoreDamage, this.role.ignoreNBT);
                           ItemStack soldItem = ItemStack.f_41583_;
                           if (event.sold != null && !event.sold.isEmpty()) {
                              soldItem = event.sold.getMCItemStack();
                              this.givePlayer(soldItem.m_41777_(), entityplayer);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean canBuy(ItemStack currency, ItemStack currency2, Player player) {
      if (NoppesUtilServer.IsItemStackNull(currency) && NoppesUtilServer.IsItemStackNull(currency2)) {
         return true;
      }

      if (NoppesUtilServer.IsItemStackNull(currency)) {
         currency = currency2;
         currency2 = ItemStack.f_41583_;
      }

      if (NoppesUtilPlayer.compareItems(currency, currency2, this.role.ignoreDamage, this.role.ignoreNBT)) {
         currency = currency.m_41777_();
         currency.m_41769_(currency2.m_41613_());
         currency2 = ItemStack.f_41583_;
      }

      return NoppesUtilServer.IsItemStackNull(currency2)
         ? NoppesUtilPlayer.compareItems(player, currency, this.role.ignoreDamage, this.role.ignoreNBT)
         : NoppesUtilPlayer.compareItems(player, currency, this.role.ignoreDamage, this.role.ignoreNBT)
            && NoppesUtilPlayer.compareItems(player, currency2, this.role.ignoreDamage, this.role.ignoreNBT);
   }

   private boolean canGivePlayer(ItemStack item, Player entityplayer) {
      ItemStack itemstack3 = entityplayer.f_36096_.m_142621_();
      if (NoppesUtilServer.IsItemStackNull(itemstack3)) {
         return true;
      }

      if (NoppesUtilPlayer.compareItems(itemstack3, item, false, false)) {
         int k1 = item.m_41613_();
         if (k1 > 0 && k1 + itemstack3.m_41613_() <= itemstack3.m_41741_()) {
            return true;
         }
      }

      return false;
   }

   private void givePlayer(ItemStack item, Player entityplayer) {
      ItemStack itemstack3 = entityplayer.f_36096_.m_142621_();
      if (NoppesUtilServer.IsItemStackNull(itemstack3)) {
         entityplayer.f_36096_.m_142503_(item);
      } else if (NoppesUtilPlayer.compareItems(itemstack3, item, false, false)) {
         int k1 = item.m_41613_();
         if (k1 > 0 && k1 + itemstack3.m_41613_() <= itemstack3.m_41741_()) {
            itemstack3.m_41769_(k1);
         }
      }
   }
}
