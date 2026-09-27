package noppes.npcs;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerDialogData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.EntityProjectile;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketDialog;
import noppes.npcs.packets.client.PacketDialogDummy;
import noppes.npcs.packets.client.PacketGuiClose;
import noppes.npcs.packets.client.PacketGuiError;
import noppes.npcs.packets.client.PacketGuiScrollData;
import noppes.npcs.packets.client.PacketNpcEdit;
import noppes.npcs.packets.client.PacketParticle;
import noppes.npcs.packets.server.SPacketGuiOpen;
import noppes.npcs.shared.common.CommonUtil;
import noppes.npcs.shared.common.util.LogWriter;

public class NoppesUtilServer {
   private static HashMap<UUID, Quest> editingQuests = new HashMap<>();
   private static HashMap<UUID, Quest> editingQuestsClient = new HashMap<>();

   public static void setEditingNpc(Player player, EntityNPCInterface npc) {
      PlayerData data = PlayerData.get(player);
      data.editingNpc = npc;
      if (npc != null) {
         Packets.send((ServerPlayer)player, new PacketNpcEdit(npc.m_19879_()));
      }
   }

   public static EntityNPCInterface getEditingNpc(Player player) {
      PlayerData data = PlayerData.get(player);
      return data.editingNpc;
   }

   public static void setEditingQuest(Player player, Quest quest) {
      if (player.f_19853_.f_46443_) {
         editingQuestsClient.put(player.m_20148_(), quest);
      } else {
         editingQuests.put(player.m_20148_(), quest);
      }
   }

   public static Quest getEditingQuest(Player player) {
      return player.f_19853_.f_46443_ ? editingQuestsClient.get(player.m_20148_()) : editingQuests.get(player.m_20148_());
   }

   public static void openDialog(Player player, EntityNPCInterface npc, Dialog dia) {
      Dialog dialog = dia.copy(player);
      PlayerData playerdata = PlayerData.get(player);
      if (EventHooks.onNPCDialog(npc, player, dialog)) {
         playerdata.dialogId = -1;
      } else {
         playerdata.dialogId = dialog.id;
         if (!(npc instanceof EntityDialogNpc) && dia.id >= 0) {
            Packets.send((ServerPlayer)player, new PacketDialog(npc.m_19879_(), dialog.id));
         } else {
            dialog.hideNPC = true;
            Packets.send((ServerPlayer)player, new PacketDialogDummy(npc.m_7755_().getString(), dialog.save(new CompoundTag())));
         }

         dia.factionOptions.addPoints(player);
         if (dialog.hasQuest()) {
            PlayerQuestController.addActiveQuest(dialog.getQuest(), player);
         }

         if (!dialog.command.isEmpty()) {
            runCommand(npc, npc.m_7755_().getString(), dialog.command, player);
         }

         if (dialog.mail.isValid()) {
            PlayerDataController.instance.addPlayerMessage(player.m_20194_(), player.m_7755_().getString(), dialog.mail);
         }

         PlayerDialogData data = playerdata.dialogData;
         if (!data.dialogsRead.contains(dialog.id) && dialog.id >= 0) {
            data.dialogsRead.add(dialog.id);
            playerdata.updateClient = true;
         }

         setEditingNpc(player, npc);
         playerdata.questData.checkQuestCompletion(player, 1);
      }
   }

   public static String runCommand(Entity executer, String name, String command, Player player) {
      return runCommand(executer.m_20193_(), executer.m_20183_(), name, command, player, executer);
   }

   public static String runCommand(final Level level, BlockPos pos, String name, String command, Player player, Entity executer) {
      if (!level.m_7654_().m_6993_()) {
         CommonUtil.NotifyOPs(level.m_7654_(), "Cant run commands if CommandBlocks are disabled");
         LogWriter.warn("Cant run commands if CommandBlocks are disabled");
         return "Cant run commands if CommandBlocks are disabled";
      }

      if (player != null) {
         command = command.replace("@dp", player.m_7755_().getString());
      }

      command = command.replace("@npc", name);
      final Component output = Component.m_237113_("");
      CommandSource icommandsender = new RconConsoleSource(level.m_7654_()) {
         public void m_213846_(Component component) {
            ((MutableComponent)output).m_7220_(component);
         }

         public boolean m_6999_() {
            return true;
         }

         public boolean m_6102_() {
            return level.m_46469_().m_46207_(GameRules.f_46138_);
         }

         public boolean m_7028_() {
            return true;
         }
      };
      int permLvl = CustomNpcs.NpcUseOpCommands ? 4 : 2;
      Vec3 point = new Vec3(pos.m_123341_() + 0.5, pos.m_123342_() + 0.5, pos.m_123343_() + 0.5);
      CommandSourceStack commandSource = new CommandSourceStack(
         icommandsender,
         point,
         Vec2.f_82462_,
         (ServerLevel)level,
         permLvl,
         "@CustomNPCs-" + name,
         Component.m_237113_("@CustomNPCs-" + name),
         level.m_7654_(),
         executer
      ) {
         public void m_81352_(Component text) {
            super.m_81352_(text);
            CommonUtil.NotifyOPs(level.m_7654_(), text);
         }
      };
      Commands icommandmanager = level.m_7654_().m_129892_();
      icommandmanager.m_230957_(commandSource, command);
      return output.getString().isEmpty() ? null : output.getString();
   }

   public static void sendOpenGui(Player player, EnumGuiType gui, EntityNPCInterface npc) {
      SPacketGuiOpen.sendOpenGui(player, gui, npc, BlockPos.f_121853_);
   }

   private static MenuType getType(EnumGuiType gui) {
      if (gui == EnumGuiType.PlayerAnvil) {
         return CustomContainer.container_carpentrybench;
      } else if (gui == EnumGuiType.CustomGui) {
         return CustomContainer.container_customgui;
      } else if (gui == EnumGuiType.PlayerBankUnlock) {
         return CustomContainer.container_bankunlock;
      } else if (gui == EnumGuiType.PlayerBankLarge) {
         return CustomContainer.container_banklarge;
      } else if (gui == EnumGuiType.PlayerBankUprade) {
         return CustomContainer.container_bankupgrade;
      } else if (gui == EnumGuiType.PlayerBankSmall) {
         return CustomContainer.container_banksmall;
      } else if (gui == EnumGuiType.PlayerMailman) {
         return CustomContainer.container_mail;
      } else if (gui == EnumGuiType.MainMenuInv) {
         return CustomContainer.container_inv;
      } else if (gui == EnumGuiType.QuestItem) {
         return CustomContainer.container_questtypeitem;
      } else if (gui == EnumGuiType.QuestReward) {
         return CustomContainer.container_questreward;
      } else if (gui == EnumGuiType.CompanionInv) {
         return CustomContainer.container_companion;
      } else if (gui == EnumGuiType.PlayerTrader) {
         return CustomContainer.container_trader;
      } else if (gui == EnumGuiType.PlayerFollower) {
         return CustomContainer.container_follower;
      } else if (gui == EnumGuiType.PlayerFollowerHire) {
         return CustomContainer.container_followerhire;
      } else if (gui == EnumGuiType.SetupTrader) {
         return CustomContainer.container_tradersetup;
      } else if (gui == EnumGuiType.SetupFollower) {
         return CustomContainer.container_followersetup;
      } else if (gui == EnumGuiType.SetupItemGiver) {
         return CustomContainer.container_itemgiver;
      } else {
         return gui == EnumGuiType.ManageBanks ? CustomContainer.container_managebanks : null;
      }
   }

   public static void openContainerGui(ServerPlayer player, final EnumGuiType gui, Consumer<FriendlyByteBuf> extraDataWriter) {
      final MenuType type = getType(gui);
      final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      extraDataWriter.accept(buffer);
      NetworkHooks.openScreen(player, new MenuProvider() {
         @Nullable
         public AbstractContainerMenu m_7208_(int p_createMenu_1_, Inventory p_createMenu_2_, Player p_createMenu_3_) {
            return type.create(p_createMenu_1_, p_createMenu_2_, buffer);
         }

         public Component m_5446_() {
            return Component.m_237113_(gui.name());
         }
      }, extraDataWriter);
   }

   public static void spawnParticle(Entity entity, String particle, int dimension) {
      Packets.sendNearby(entity, new PacketParticle(entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), entity.m_20206_(), entity.m_20205_(), particle));
   }

   public static void sendScrollData(ServerPlayer player, Map<String, Integer> map) {
      Packets.send(player, new PacketGuiScrollData(map));
   }

   public static void sendGuiError(Player player, int i) {
      Packets.send((ServerPlayer)player, new PacketGuiError(i, new CompoundTag()));
   }

   public static void sendGuiClose(ServerPlayer player, int i, CompoundTag comp) {
      Packets.send(player, new PacketGuiClose(comp));
   }

   public static void GivePlayerItem(Entity entity, Player player, ItemStack item) {
      if (!entity.f_19853_.f_46443_ && item != null && !item.m_41619_()) {
         item = item.m_41777_();
         float f = 0.7F;
         double d = (double)(entity.f_19853_.f_46441_.m_188501_() * f) + (1.0F - f);
         double d1 = (double)(entity.f_19853_.f_46441_.m_188501_() * f) + (1.0F - f);
         double d2 = (double)(entity.f_19853_.f_46441_.m_188501_() * f) + (1.0F - f);
         ItemEntity entityitem = new ItemEntity(entity.f_19853_, entity.m_20185_() + d, entity.m_20186_() + d1, entity.m_20189_() + d2, item);
         entityitem.m_32010_(2);
         entity.f_19853_.m_7967_(entityitem);
         if (player.m_150109_().m_36054_(item)) {
            entity.f_19853_
               .m_6263_(
                  null,
                  player.m_20185_(),
                  player.m_20186_(),
                  player.m_20189_(),
                  SoundEvents.f_12019_,
                  SoundSource.PLAYERS,
                  0.2F,
                  ((player.m_217043_().m_188501_() - player.m_217043_().m_188501_()) * 0.7F + 1.0F) * 2.0F
               );
            player.m_7938_(entityitem, item.m_41613_());
            PlayerQuestData playerdata = PlayerData.get(player).questData;
            playerdata.checkQuestCompletion(player, 0);
            if (item.m_41613_() <= 0) {
               entityitem.m_142687_(RemovalReason.DISCARDED);
            }
         }
      }
   }

   public static BlockPos GetClosePos(BlockPos origin, Level level) {
      for (int x = -1; x < 2; x++) {
         for (int z = -1; z < 2; z++) {
            for (int y = 2; y >= -2; y--) {
               BlockPos pos = origin.m_7918_(x, y, z);
               BlockState state = level.m_8055_(pos.m_7494_());
               if (state.m_60796_(level, pos) && level.m_46859_(pos.m_7494_()) && level.m_46859_(pos.m_6630_(2))) {
                  return pos.m_7494_();
               }
            }
         }
      }

      return level.m_5452_(Types.MOTION_BLOCKING_NO_LEAVES, origin);
   }

   public static void playSound(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
      entity.f_19853_.m_6263_(null, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), sound, SoundSource.NEUTRAL, volume, pitch);
   }

   public static void playSound(Level level, BlockPos pos, SoundEvent sound, SoundSource cat, float volume, float pitch) {
      level.m_5594_(null, pos, sound, cat, volume, pitch);
   }

   public static Player getPlayer(MinecraftServer minecraftserver, UUID id) {
      for (Player player : minecraftserver.m_6846_().m_11314_()) {
         if (id.equals(player.m_20148_())) {
            return player;
         }
      }

      return null;
   }

   public static Entity GetDamageSourcee(DamageSource damagesource) {
      Entity entity = damagesource.m_7639_();
      if (entity == null) {
         entity = damagesource.m_7640_();
      }

      if (entity instanceof EntityProjectile && ((EntityProjectile)entity).m_37282_() instanceof LivingEntity) {
         entity = ((AbstractArrow)entity).m_37282_();
      } else if (entity instanceof ThrowableProjectile) {
         entity = ((ThrowableProjectile)entity).m_37282_();
      }

      return entity;
   }

   public static boolean IsItemStackNull(ItemStack is) {
      return is == null || is.m_41619_() || is == ItemStack.f_41583_ || is.m_41720_() == null;
   }

   public static ItemStack ChangeItemStack(ItemStack is, Item item) {
      CompoundTag comp = is.m_41739_(new CompoundTag());
      ResourceLocation resourcelocation = ForgeRegistries.ITEMS.getKey(item);
      comp.m_128359_("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
      return ItemStack.m_41712_(comp);
   }
}
