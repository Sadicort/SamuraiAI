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
import noppes.npcs.roles.RoleCompanion;

public class SlotCompanionArmor extends Slot {
   public static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{
      InventoryMenu.f_39696_, InventoryMenu.f_39695_, InventoryMenu.f_39694_, InventoryMenu.f_39693_
   };
   final EquipmentSlot armorType;
   final RoleCompanion role;

   public SlotCompanionArmor(RoleCompanion role, Container iinventory, int id, int x, int y, EquipmentSlot type) {
      super(iinventory, id, x, y);
      this.armorType = type;
      this.role = role;
   }

   public int m_6641_() {
      return 1;
   }

   @OnlyIn(Dist.CLIENT)
   public Pair<ResourceLocation, ResourceLocation> m_7543_() {
      return Pair.of(InventoryMenu.f_39692_, ARMOR_SLOT_TEXTURES[this.armorType.m_20749_()]);
   }

   public boolean m_5857_(ItemStack itemstack) {
      if (itemstack.m_41720_() instanceof ArmorItem && this.role.canWearArmor(itemstack)) {
         return ((ArmorItem)itemstack.m_41720_()).m_40402_() == this.armorType;
      } else {
         return itemstack.m_41720_() instanceof BlockItem ? this.armorType == EquipmentSlot.HEAD : false;
      }
   }
}
