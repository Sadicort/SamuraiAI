package noppes.npcs.containers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

class SlotNPCArmor extends Slot {
   final EquipmentSlot armorType;

   SlotNPCArmor(Container iinventory, int i, int j, int k, EquipmentSlot l) {
      super(iinventory, i, j, k);
      this.armorType = l;
   }

   public int m_6641_() {
      return 1;
   }

   @OnlyIn(Dist.CLIENT)
   public Pair<ResourceLocation, ResourceLocation> m_7543_() {
      return Pair.of(InventoryMenu.f_39692_, SlotCompanionArmor.ARMOR_SLOT_TEXTURES[this.armorType.m_20749_()]);
   }

   public boolean m_5857_(ItemStack itemstack) {
      if (itemstack.m_41720_() instanceof ArmorItem) {
         return ((ArmorItem)itemstack.m_41720_()).m_40402_() == this.armorType;
      } else {
         return itemstack.m_41720_() instanceof BlockItem ? this.armorType == EquipmentSlot.HEAD : false;
      }
   }
}
