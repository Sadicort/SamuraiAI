package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.EventHooks;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.IPos;
import noppes.npcs.api.ITimers;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.block.IBlock;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.entity.data.IPixelmonPlayerData;
import noppes.npcs.api.entity.data.IPlayerMail;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerDialogData;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;
import noppes.npcs.packets.client.PacketGuiClose;
import noppes.npcs.packets.client.PacketPlayMusic;
import noppes.npcs.packets.client.PacketPlaySound;
import noppes.npcs.packets.server.SPacketDimensionTeleport;
import noppes.npcs.shared.client.util.NoppesStringUtils;
import noppes.npcs.util.ValueUtil;

public class PlayerWrapper<T extends ServerPlayer> extends EntityLivingBaseWrapper<T> implements IPlayer {
   private IContainer inventory;
   private Object pixelmonPartyStorage;
   private Object pixelmonPCStorage;
   private final IData storeddata = new IData() {
      @Override
      public void put(String key, Object value) {
         CompoundTag compound = this.getStoredCompound();
         if (value instanceof Number) {
            compound.m_128347_(key, ((Number)value).doubleValue());
         } else if (value instanceof String) {
            compound.m_128359_(key, (String)value);
         }
      }

      @Override
      public Object get(String key) {
         CompoundTag compound = this.getStoredCompound();
         if (!compound.m_128441_(key)) {
            return null;
         }

         Tag base = compound.m_128423_(key);
         return base instanceof NumericTag ? ((NumericTag)base).m_7061_() : base.m_7916_();
      }

      @Override
      public void remove(String key) {
         CompoundTag compound = this.getStoredCompound();
         compound.m_128473_(key);
      }

      @Override
      public boolean has(String key) {
         return this.getStoredCompound().m_128441_(key);
      }

      @Override
      public void clear() {
         PlayerData data = PlayerData.get((Player)PlayerWrapper.this.entity);
         data.scriptStoreddata = new CompoundTag();
      }

      private CompoundTag getStoredCompound() {
         PlayerData data = PlayerData.get((Player)PlayerWrapper.this.entity);
         return data.scriptStoreddata;
      }

      @Override
      public String[] getKeys() {
         CompoundTag compound = this.getStoredCompound();
         return compound.m_128431_().toArray(new String[compound.m_128431_().size()]);
      }
   };
   private PlayerData data;

   public PlayerWrapper(T player) {
      super(player);
   }

   @Override
   public IData getStoreddata() {
      return this.storeddata;
   }

   @Override
   public String getName() {
      return this.entity.m_7755_().getString();
   }

   @Override
   public String getDisplayName() {
      return this.entity.m_5446_().getString();
   }

   @Override
   public int getHunger() {
      return this.entity.m_36324_().m_38702_();
   }

   @Override
   public void setHunger(int level) {
      this.entity.m_36324_().m_38705_(level);
   }

   @Override
   public boolean hasFinishedQuest(int id) {
      PlayerQuestData data = this.getData().questData;
      return data.finishedQuests.containsKey(id);
   }

   @Override
   public boolean hasActiveQuest(int id) {
      PlayerQuestData data = this.getData().questData;
      return data.activeQuests.containsKey(id);
   }

   @Override
   public IQuest[] getActiveQuests() {
      PlayerQuestData data = this.getData().questData;
      List<IQuest> quests = new ArrayList<>();

      for (int id : data.activeQuests.keySet()) {
         IQuest quest = QuestController.instance.quests.get(id);
         if (quest != null) {
            quests.add(quest);
         }
      }

      return quests.toArray(new IQuest[quests.size()]);
   }

   @Override
   public IQuest[] getFinishedQuests() {
      PlayerQuestData data = this.getData().questData;
      List<IQuest> quests = new ArrayList<>();

      for (int id : data.finishedQuests.keySet()) {
         IQuest quest = QuestController.instance.quests.get(id);
         if (quest != null) {
            quests.add(quest);
         }
      }

      return quests.toArray(new IQuest[quests.size()]);
   }

   @Override
   public void startQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null) {
         QuestData questdata = new QuestData(quest);
         PlayerData data = this.getData();
         data.questData.activeQuests.put(id, questdata);
         Packets.send(this.entity, new PacketAchievement(Component.m_237115_("quest.newquest"), Component.m_237115_(quest.title), 2));
         Component text = Component.m_237115_("quest.newquest").m_130946_(":").m_7220_(Component.m_237115_(quest.title));
         Packets.send(this.entity, new PacketChat(text));
         data.updateClient = true;
      }
   }

   @Override
   public void sendNotification(String title, String msg, int type) {
      if (type >= 0 && type <= 3) {
         Packets.send(this.entity, new PacketAchievement(Component.m_237115_(title), Component.m_237115_(msg), type));
      } else {
         throw new CustomNPCsException("Wrong type value given " + type);
      }
   }

   @Override
   public void finishQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null) {
         PlayerData data = this.getData();
         data.questData.finishedQuests.put(id, System.currentTimeMillis());
         data.updateClient = true;
      }
   }

   @Override
   public void stopQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null) {
         PlayerData data = this.getData();
         data.questData.activeQuests.remove(id);
         data.updateClient = true;
      }
   }

   @Override
   public void removeQuest(int id) {
      Quest quest = QuestController.instance.quests.get(id);
      if (quest != null) {
         PlayerData data = this.getData();
         data.questData.activeQuests.remove(id);
         data.questData.finishedQuests.remove(id);
         data.updateClient = true;
      }
   }

   @Override
   public boolean hasReadDialog(int id) {
      PlayerDialogData data = this.getData().dialogData;
      return data.dialogsRead.contains(id);
   }

   @Override
   public void showDialog(int id, String name) {
      Dialog dialog = DialogController.instance.dialogs.get(id);
      if (dialog == null) {
         throw new CustomNPCsException("Unknown Dialog id: " + id);
      }

      if (dialog.availability.isAvailable((Player)this.entity)) {
         EntityDialogNpc npc = new EntityDialogNpc(this.getWorld().getMCLevel());
         npc.display.setName(name);
         EntityUtil.Copy((LivingEntity)this.entity, npc);
         DialogOption option = new DialogOption();
         option.dialogId = id;
         option.title = dialog.title;
         npc.dialogs.put(0, option);
         NoppesUtilServer.openDialog((Player)this.entity, npc, dialog);
      }
   }

   @Override
   public void addFactionPoints(int faction, int points) {
      PlayerData data = this.getData();
      data.factionData.increasePoints((Player)this.entity, faction, points);
      data.updateClient = true;
   }

   @Override
   public int getFactionPoints(int faction) {
      Player var10001 = (Player)this.entity;
      return this.getData().factionData.getFactionPoints(var10001, faction);
   }

   @Override
   public float getRotation() {
      return this.entity.m_146908_();
   }

   @Override
   public void setRotation(float rotation) {
      this.entity.m_146922_(rotation);
   }

   @Override
   public void message(String message) {
      this.entity.m_213846_(Component.m_237115_(NoppesStringUtils.formatText(message, this.entity)));
   }

   @Override
   public int getGamemode() {
      return this.entity.f_8941_.m_9290_().m_46392_();
   }

   @Override
   public void setGamemode(int type) {
      this.entity.m_143403_(GameType.m_46393_(type));
   }

   @Override
   public int inventoryItemCount(IItemStack item) {
      int count = 0;

      for (int i = 0; i < this.entity.m_150109_().m_6643_(); i++) {
         ItemStack is = this.entity.m_150109_().m_8020_(i);
         if (is != null && this.isItemEqual(item.getMCItemStack(), is)) {
            count += is.m_41613_();
         }
      }

      return count;
   }

   private boolean isItemEqual(ItemStack stack, ItemStack other) {
      return other.m_41619_() ? false : stack.m_41720_() == other.m_41720_();
   }

   @Override
   public int inventoryItemCount(String id) {
      Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
      if (item == null) {
         throw new CustomNPCsException("Unknown item id: " + id);
      } else {
         return this.inventoryItemCount(NpcAPI.Instance().getIItemStack(new ItemStack(item, 1)));
      }
   }

   @Override
   public IContainer getInventory() {
      if (this.inventory == null) {
         this.inventory = new ContainerWrapper(this.entity.m_150109_());
      }

      return this.inventory;
   }

   @Override
   public IItemStack getInventoryHeldItem() {
      return NpcAPI.Instance().getIItemStack(this.entity.f_36096_.m_142621_());
   }

   @Override
   public boolean removeItem(IItemStack item, int amount) {
      int count = this.inventoryItemCount(item);
      if (amount > count) {
         return false;
      }

      if (count == amount) {
         this.removeAllItems(item);
      } else {
         for (int i = 0; i < this.entity.m_150109_().m_6643_(); i++) {
            ItemStack is = this.entity.m_150109_().m_8020_(i);
            if (is != null && this.isItemEqual(item.getMCItemStack(), is)) {
               if (amount < is.m_41613_()) {
                  is.m_41620_(amount);
                  break;
               }

               this.entity.m_150109_().m_6836_(i, ItemStack.f_41583_);
               amount -= is.m_41613_();
            }
         }
      }

      this.updatePlayerInventory();
      return true;
   }

   @Override
   public boolean removeItem(String id, int amount) {
      Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
      if (item == null) {
         throw new CustomNPCsException("Unknown item id: " + id);
      } else {
         return this.removeItem(NpcAPI.Instance().getIItemStack(new ItemStack(item, 1)), amount);
      }
   }

   @Override
   public boolean giveItem(IItemStack item) {
      ItemStack mcItem = item.getMCItemStack();
      if (mcItem.m_41619_()) {
         return false;
      }

      boolean bo = this.entity.m_150109_().m_36054_(mcItem.m_41777_());
      if (bo) {
         NoppesUtilServer.playSound(
            (LivingEntity)this.entity,
            SoundEvents.f_12019_,
            0.2F,
            ((this.entity.m_217043_().m_188501_() - this.entity.m_217043_().m_188501_()) * 0.7F + 1.0F) * 2.0F
         );
         this.updatePlayerInventory();
      }

      return bo;
   }

   @Override
   public boolean giveItem(String id, int amount) {
      Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
      if (item == null) {
         return false;
      }

      ItemStack mcStack = new ItemStack(item);
      IItemStack itemStack = NpcAPI.Instance().getIItemStack(mcStack);
      itemStack.setStackSize(amount);
      return this.giveItem(itemStack);
   }

   @Override
   public void updatePlayerInventory() {
      this.entity.f_36095_.m_38946_();
      this.entity
         .f_8906_
         .m_9829_(
            new ClientboundContainerSetSlotPacket(-2, 0, this.entity.m_150109_().f_35977_, this.entity.m_150109_().m_8020_(this.entity.m_150109_().f_35977_))
         );
      PlayerQuestData playerdata = this.getData().questData;
      playerdata.checkQuestCompletion((Player)this.entity, 0);
   }

   @Override
   public IBlock getSpawnPoint() {
      BlockPos pos = (BlockPos)this.entity.m_21257_().orElse(null);
      return pos == null ? this.getWorld().getSpawnPoint() : NpcAPI.Instance().getIBlock(this.entity.f_19853_, pos);
   }

   @Override
   public void setSpawnPoint(IBlock block) {
      this.setSpawnpoint(block.getX(), block.getY(), block.getZ());
   }

   @Override
   public void setSpawnpoint(int x, int y, int z) {
      x = ValueUtil.CorrectInt(x, -30000000, 30000000);
      z = ValueUtil.CorrectInt(z, -30000000, 30000000);
      y = ValueUtil.CorrectInt(y, 0, 256);
      this.entity.m_9158_(this.getWorld().getMCLevel().m_46472_(), new BlockPos(x, y, z), 0.0F, true, false);
   }

   @Override
   public void resetSpawnpoint() {
      this.entity.m_9158_(this.getWorld().getMCLevel().m_46472_(), null, 0.0F, true, false);
   }

   @Override
   public void removeAllItems(IItemStack item) {
      for (int i = 0; i < this.entity.m_150109_().m_6643_(); i++) {
         ItemStack is = this.entity.m_150109_().m_8020_(i);
         if (is != null && is.m_41656_(item.getMCItemStack())) {
            this.entity.m_150109_().m_6836_(i, ItemStack.f_41583_);
         }
      }
   }

   @Override
   public boolean hasAdvancement(String achievement) {
      Advancement advancement = this.entity.m_20194_().m_129889_().m_136041_(new ResourceLocation(achievement));
      if (advancement == null) {
         throw new CustomNPCsException("Advancement doesnt exist");
      }

      AdvancementProgress progress = this.entity.m_20194_().m_6846_().m_11296_(this.entity).m_135996_(advancement);
      return progress.m_8193_();
   }

   @Override
   public int getExpLevel() {
      return this.entity.f_36078_;
   }

   @Override
   public void setExpLevel(int level) {
      this.entity.m_6749_(level - this.entity.f_36078_);
   }

   @Override
   public void setPosition(double x, double y, double z) {
      SPacketDimensionTeleport.teleportPlayer(this.entity, x, y, z, this.entity.f_19853_.m_46472_());
   }

   @Override
   public void setPos(IPos pos) {
      SPacketDimensionTeleport.teleportPlayer(this.entity, pos.getX(), pos.getY(), pos.getZ(), this.entity.f_19853_.m_46472_());
   }

   @Override
   public int getType() {
      return 1;
   }

   @Override
   public boolean typeOf(int type) {
      return type == 1 ? true : super.typeOf(type);
   }

   @Override
   public boolean hasPermission(String permission) {
      for (PermissionNode<?> node : PermissionAPI.getRegisteredNodes()) {
         if (node.getNodeName().equals(permission)) {
            try {
               return CustomNpcsPermissions.hasPermission(this.entity, (PermissionNode<Boolean>)node);
            } catch (Throwable var5) {
            }
         }
      }

      return false;
   }

   public IPixelmonPlayerData getPixelmonData() {
      if (!PixelmonHelper.Enabled) {
         throw new CustomNPCsException("Pixelmon isnt installed");
      } else {
         return new IPixelmonPlayerData() {
            @Override
            public Object getParty() {
               if (PlayerWrapper.this.pixelmonPartyStorage == null) {
                  PlayerWrapper.this.pixelmonPartyStorage = PixelmonHelper.getParty((Player)PlayerWrapper.this.entity);
               }

               return PlayerWrapper.this.pixelmonPartyStorage;
            }

            @Override
            public Object getPC() {
               if (PlayerWrapper.this.pixelmonPCStorage == null) {
                  PlayerWrapper.this.pixelmonPCStorage = PixelmonHelper.getPc((Player)PlayerWrapper.this.entity);
               }

               return PlayerWrapper.this.pixelmonPCStorage;
            }
         };
      }
   }

   private PlayerData getData() {
      if (this.data == null) {
         this.data = PlayerData.get((Player)this.entity);
      }

      return this.data;
   }

   @Override
   public ITimers getTimers() {
      return this.getData().timers;
   }

   @Override
   public void removeDialog(int id) {
      PlayerData data = this.getData();
      data.dialogData.dialogsRead.remove(id);
      data.updateClient = true;
   }

   @Override
   public void addDialog(int id) {
      PlayerData data = this.getData();
      data.dialogData.dialogsRead.add(id);
      data.updateClient = true;
   }

   @Override
   public void closeGui() {
      this.entity.m_6915_();
      Packets.send(this.entity, new PacketGuiClose(new CompoundTag()));
   }

   @Override
   public int factionStatus(int factionId) {
      Faction faction = FactionController.instance.getFaction(factionId);
      if (faction == null) {
         throw new CustomNPCsException("Unknown faction: " + factionId);
      } else {
         return faction.playerStatus(this);
      }
   }

   @Override
   public void kick(String message) {
      this.entity.f_8906_.m_9942_(Component.m_237115_(message));
   }

   @Override
   public boolean canQuestBeAccepted(int questId) {
      return PlayerQuestController.canQuestBeAccepted((Player)this.entity, questId);
   }

   @Override
   public void showCustomGui(ICustomGui gui) {
      NoppesUtilServer.openContainerGui(this.getMCEntity(), EnumGuiType.CustomGui, buf -> buf.m_130079_(((CustomGuiWrapper)gui).toNBT()));
      ((ContainerCustomGui)this.getMCEntity().f_36096_).setGui((CustomGuiWrapper)gui, (Player)this.entity);
   }

   @Override
   public ICustomGui getCustomGui() {
      return this.entity.f_36096_ instanceof ContainerCustomGui ? ((ContainerCustomGui)this.entity.f_36096_).customGui : null;
   }

   @Override
   public void clearData() {
      PlayerData data = this.getData();
      data.setNBT(new CompoundTag());
      data.save(true);
   }

   @Override
   public IContainer getOpenContainer() {
      return NpcAPI.Instance().getIContainer(this.entity.f_36096_);
   }

   @Override
   public void playSound(String sound, float volume, float pitch) {
      BlockPos pos = this.entity.m_20183_();
      Packets.send(this.entity, new PacketPlaySound(sound, pos, volume, pitch));
   }

   @Override
   public void playMusic(String sound, boolean background, boolean loops) {
      Packets.send(this.entity, new PacketPlayMusic(sound, !background, loops));
   }

   @Override
   public void sendMail(IPlayerMail mail) {
      PlayerData data = this.getData();
      data.mailData.playermail.add(((PlayerMail)mail).copy());
      data.save(false);
   }

   @Override
   public void trigger(int id, Object... arguments) {
      EventHooks.onScriptTriggerEvent(PlayerData.get((Player)this.entity).scriptData, id, this.getWorld(), this.getPos(), null, arguments);
   }
}
