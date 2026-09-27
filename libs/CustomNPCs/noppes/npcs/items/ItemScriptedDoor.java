package noppes.npcs.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import noppes.npcs.CustomTabs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class ItemScriptedDoor extends DoubleHighBlockItem {
   public ItemScriptedDoor(Block block) {
      super(block, new Properties().m_41487_(1).m_41491_(CustomTabs.tab));
   }

   public InteractionResult m_6225_(UseOnContext context) {
      InteractionResult res = super.m_6225_(context);
      if (res == InteractionResult.SUCCESS && !context.m_43725_().f_46443_) {
         PlayerData data = PlayerData.get(context.m_43723_());
         data.scriptBlockPos = context.m_8083_();
         SPacketGuiOpen.sendOpenGui(context.m_43723_(), EnumGuiType.ScriptDoor, null, context.m_8083_().m_7494_());
         return InteractionResult.SUCCESS;
      } else {
         return res;
      }
   }

   public ItemStack m_5922_(ItemStack stack, Level worldIn, LivingEntity playerIn) {
      return stack;
   }
}
