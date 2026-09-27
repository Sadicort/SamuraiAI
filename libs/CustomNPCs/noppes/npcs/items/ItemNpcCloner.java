package noppes.npcs.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import noppes.npcs.CustomTabs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class ItemNpcCloner extends Item {
   public ItemNpcCloner() {
      super(new Properties().m_41487_(1).m_41491_(CustomTabs.tab));
   }

   public InteractionResult m_6225_(UseOnContext context) {
      if (!context.m_43725_().f_46443_) {
         SPacketGuiOpen.sendOpenGui(context.m_43723_(), EnumGuiType.MobSpawner, null, context.m_8083_());
      }

      return InteractionResult.SUCCESS;
   }
}
