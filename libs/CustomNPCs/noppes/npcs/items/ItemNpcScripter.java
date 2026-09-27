package noppes.npcs.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomTabs;
import noppes.npcs.constants.EnumGuiType;

public class ItemNpcScripter extends Item {
   public ItemNpcScripter() {
      super(new Properties().m_41487_(1).m_41491_(CustomTabs.tab));
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack itemstack = player.m_21120_(hand);
      if (level.f_46443_ && hand == InteractionHand.MAIN_HAND) {
         CustomNpcs.proxy.openGui(player, EnumGuiType.ScriptPlayers);
         return new InteractionResultHolder(InteractionResult.SUCCESS, itemstack);
      } else {
         return new InteractionResultHolder(InteractionResult.SUCCESS, itemstack);
      }
   }
}
