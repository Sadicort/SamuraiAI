package noppes.npcs.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;

public class ItemSoulstoneFilled extends Item {
   public ItemSoulstoneFilled() {
      super(new Properties().m_41487_(1));
   }

   @OnlyIn(Dist.CLIENT)
   public void m_7373_(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
      CompoundTag compound = stack.m_41783_();
      if (compound != null && compound.m_128425_("Entity", 10)) {
         Component name = Component.m_237115_(compound.m_128461_("Name"));
         if (compound.m_128441_("DisplayName")) {
            name = Component.m_237115_(compound.m_128461_("DisplayName")).m_130946_(" (").m_7220_(name).m_130946_(")");
         }

         list.add(Component.m_237113_(ChatFormatting.BLUE + "").m_7220_(name));
         if (stack.m_41783_().m_128441_("ExtraText")) {
            MutableComponent text = Component.m_237113_("");
            String[] split = compound.m_128461_("ExtraText").split(",");

            for (String s : split) {
               text.m_7220_(Component.m_237115_(s));
            }

            list.add(text);
         }
      } else {
         list.add(Component.m_237113_(ChatFormatting.RED + "Error"));
      }
   }

   public InteractionResult m_6225_(UseOnContext context) {
      if (context.m_43725_().f_46443_) {
         return InteractionResult.SUCCESS;
      }

      ItemStack stack = context.m_43722_();
      if (Spawn(context.m_43723_(), stack, context.m_43725_(), context.m_8083_()) == null) {
         return InteractionResult.FAIL;
      }

      if (!context.m_43723_().m_150110_().f_35937_) {
         stack.m_41620_(1);
      }

      return InteractionResult.SUCCESS;
   }

   public static Entity Spawn(Player player, ItemStack stack, Level level, BlockPos pos) {
      if (level.f_46443_) {
         return null;
      }

      if (stack.m_41783_() != null && stack.m_41783_().m_128425_("Entity", 10)) {
         CompoundTag compound = stack.m_41783_().m_128469_("Entity");
         Entity entity = (Entity)EntityType.m_20642_(compound, level).orElse(null);
         if (entity == null) {
            return null;
         }

         entity.m_6034_(pos.m_123341_() + 0.5, pos.m_123342_() + 1 + 0.2F, pos.m_123343_() + 0.5);
         if (entity instanceof EntityNPCInterface npc) {
            npc.ais.setStartPos(pos);
            npc.m_21153_(npc.m_21233_());
            npc.m_6034_(pos.m_123341_() + 0.5F, npc.getStartYPos(), pos.m_123343_() + 0.5F);
            if (npc.role.getType() == 6 && player != null) {
               PlayerData data = PlayerData.get(player);
               if (data.hasCompanion()) {
                  return null;
               }

               ((RoleCompanion)npc.role).setOwner(player);
               data.setCompanion(npc);
            }

            if (npc.role.getType() == 2 && player != null) {
               ((RoleFollower)npc.role).setOwner(player);
            }
         }

         if (!level.m_7967_(entity)) {
            player.m_213846_(Component.m_237115_("error.failedToSpawn"));
            return null;
         } else {
            return entity;
         }
      } else {
         return null;
      }
   }
}
