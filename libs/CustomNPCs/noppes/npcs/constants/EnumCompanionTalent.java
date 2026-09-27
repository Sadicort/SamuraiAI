package noppes.npcs.constants;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public enum EnumCompanionTalent {
   INVENTORY(Item.m_41439_(Blocks.f_50091_)),
   ARMOR(Items.f_42469_),
   SWORD(Items.f_42388_),
   RANGED(Items.f_42411_),
   ACROBATS(Items.f_42463_),
   INTEL(Items.f_42517_);

   public ItemStack item;

   EnumCompanionTalent(Item item) {
      this.item = new ItemStack(item);
   }
}
