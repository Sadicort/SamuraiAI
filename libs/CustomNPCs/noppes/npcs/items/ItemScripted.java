package noppes.npcs.items;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;

public class ItemScripted extends Item {
   public ItemScripted(Properties props) {
      super(props);
   }

   public static ItemScriptedWrapper GetWrapper(ItemStack stack) {
      return (ItemScriptedWrapper)NpcAPI.Instance().getIItemStack(stack);
   }

   public boolean m_142522_(ItemStack stack) {
      IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
      return istack instanceof ItemScriptedWrapper ? ((ItemScriptedWrapper)istack).durabilityShow : super.m_142522_(stack);
   }

   public int m_142158_(ItemStack stack) {
      IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
      return istack instanceof ItemScriptedWrapper ? Math.round(13.0F - ((ItemScriptedWrapper)istack).durabilityValue * 13.0F) : super.m_142158_(stack);
   }

   public int m_142159_(ItemStack stack) {
      IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
      if (!(istack instanceof ItemScriptedWrapper)) {
         return super.m_142159_(stack);
      }

      int color = ((ItemScriptedWrapper)istack).durabilityColor;
      return color >= 0 ? color : Mth.m_14169_(Math.max(0.0F, 1.0F - this.m_142158_(stack)) / 3.0F, 1.0F, 1.0F);
   }

   public int getMaxStackSize(ItemStack stack) {
      IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
      return istack instanceof ItemScriptedWrapper ? ((ItemScriptedWrapper)istack).getMaxStackSize() : super.getMaxStackSize(stack);
   }

   public boolean m_7579_(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      return true;
   }

   public boolean m_41468_() {
      return true;
   }

   public CompoundTag getShareTag(ItemStack stack) {
      IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
      return istack instanceof ItemScriptedWrapper ? ((ItemScriptedWrapper)istack).getMCNbt() : null;
   }

   public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
      if (nbt != null) {
         IItemStack istack = NpcAPI.Instance().getIItemStack(stack);
         if (istack instanceof ItemScriptedWrapper) {
            ((ItemScriptedWrapper)istack).setMCNbt(nbt);
         }
      }
   }
}
