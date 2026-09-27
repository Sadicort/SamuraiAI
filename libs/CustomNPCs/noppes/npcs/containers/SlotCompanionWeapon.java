package noppes.npcs.containers;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.roles.RoleCompanion;

class SlotCompanionWeapon extends Slot {
   final RoleCompanion role;

   public SlotCompanionWeapon(RoleCompanion role, Container iinventory, int id, int x, int y) {
      super(iinventory, id, x, y);
      this.role = role;
   }

   public int m_6641_() {
      return 1;
   }

   public boolean m_5857_(ItemStack itemstack) {
      return NoppesUtilServer.IsItemStackNull(itemstack) ? false : this.role.canWearSword(NpcAPI.Instance().getIItemStack(itemstack));
   }
}
