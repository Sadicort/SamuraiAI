package noppes.npcs.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcs;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.CustomTabs;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.common.CommonUtil;

public class ItemSoulstoneEmpty extends Item {
   public ItemSoulstoneEmpty() {
      super(new Properties().m_41487_(64).m_41491_(CustomTabs.tab));
   }

   public boolean store(LivingEntity entity, ItemStack stack, Player player) {
      if (this.hasPermission(entity, player) && !(entity instanceof Player)) {
         ItemStack stone = new ItemStack(CustomItems.soulstoneFull);
         CompoundTag compound = new CompoundTag();
         if (!entity.m_20086_(compound)) {
            return false;
         }

         ServerCloneController.Instance.cleanTags(compound);
         stone.m_41700_("Entity", compound);
         String name = entity.m_20078_();
         if (name == null) {
            name = "generic";
         }

         stone.m_41700_("Name", StringTag.m_129297_(name));
         if (entity instanceof EntityNPCInterface npc) {
            stone.m_41700_("DisplayName", StringTag.m_129297_(entity.m_7755_().getString()));
            if (npc.role.getType() == 6) {
               RoleCompanion role = (RoleCompanion)npc.role;
               stone.m_41700_("ExtraText", StringTag.m_129297_("companion.stage,: ," + role.stage.name));
            }
         } else if (entity.m_8077_()) {
            stone.m_41700_("DisplayName", StringTag.m_129297_(entity.m_7770_().getString()));
         }

         NoppesUtilServer.GivePlayerItem(player, player, stone);
         if (!player.m_150110_().f_35937_) {
            stack.m_41620_(1);
            if (stack.m_41613_() <= 0) {
               player.m_150109_().m_36057_(stack);
            }
         }

         entity.m_146870_();
         return true;
      } else {
         return false;
      }
   }

   public boolean hasPermission(LivingEntity entity, Player player) {
      if (CommonUtil.isOp(player)) {
         return true;
      }

      if (CustomNpcsPermissions.hasPermission((ServerPlayer)player, CustomNpcsPermissions.SOULSTONE_ALL)) {
         return true;
      }

      if (entity instanceof EntityNPCInterface npc) {
         if (npc.role.getType() == 6) {
            RoleCompanion role = (RoleCompanion)npc.role;
            if (role.getOwner() == player) {
               return true;
            }
         }

         if (npc.role.getType() == 2) {
            RoleFollower role = (RoleFollower)npc.role;
            if (role.getOwner() == player) {
               return !role.refuseSoulStone;
            }
         }

         return CustomNpcs.SoulStoneNPCs;
      } else {
         return entity instanceof Animal ? CustomNpcs.SoulStoneAnimals : false;
      }
   }
}
