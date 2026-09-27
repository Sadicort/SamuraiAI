package noppes.npcs.controllers.data;

import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.EventHooks;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;
import noppes.npcs.quests.QuestInterface;

public class PlayerQuestData {
   public HashMap<Integer, QuestData> activeQuests = new HashMap<>();
   public HashMap<Integer, Long> finishedQuests = new HashMap<>();

   public void loadNBTData(CompoundTag mainCompound) {
      if (mainCompound != null) {
         CompoundTag compound = mainCompound.m_128469_("QuestData");
         ListTag list = compound.m_128437_("CompletedQuests", 10);
         if (list != null) {
            HashMap<Integer, Long> finishedQuests = new HashMap<>();

            for (int i = 0; i < list.size(); i++) {
               CompoundTag nbttagcompound = list.m_128728_(i);
               finishedQuests.put(nbttagcompound.m_128451_("Quest"), nbttagcompound.m_128454_("Date"));
            }

            this.finishedQuests = finishedQuests;
         }

         ListTag list2 = compound.m_128437_("ActiveQuests", 10);
         if (list2 != null) {
            HashMap<Integer, QuestData> activeQuests = new HashMap<>();

            for (int i = 0; i < list2.size(); i++) {
               CompoundTag nbttagcompound = list2.m_128728_(i);
               int id = nbttagcompound.m_128451_("Quest");
               Quest quest = QuestController.instance.quests.get(id);
               if (quest != null) {
                  QuestData data = new QuestData(quest);
                  data.readAdditionalSaveData(nbttagcompound);
                  activeQuests.put(id, data);
               }
            }

            this.activeQuests = activeQuests;
         }
      }
   }

   public void saveNBTData(CompoundTag maincompound) {
      CompoundTag compound = new CompoundTag();
      ListTag list = new ListTag();

      for (int quest : this.finishedQuests.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Quest", quest);
         nbttagcompound.m_128356_("Date", this.finishedQuests.get(quest));
         list.add(nbttagcompound);
      }

      compound.m_128365_("CompletedQuests", list);
      ListTag list2 = new ListTag();

      for (int quest : this.activeQuests.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Quest", quest);
         this.activeQuests.get(quest).addAdditionalSaveData(nbttagcompound);
         list2.add(nbttagcompound);
      }

      compound.m_128365_("ActiveQuests", list2);
      maincompound.m_128365_("QuestData", compound);
   }

   public QuestData getQuestCompletion(Player player, EntityNPCInterface npc) {
      for (QuestData data : this.activeQuests.values()) {
         Quest quest = data.quest;
         if (quest != null
            && quest.completion == EnumQuestCompletion.Npc
            && quest.completerNpc.equals(npc.m_7755_().getString())
            && quest.questInterface.isCompleted(player)) {
            return data;
         }
      }

      return null;
   }

   public boolean checkQuestCompletion(Player player, int type) {
      boolean bo = false;

      for (QuestData data : this.activeQuests.values()) {
         if (data.quest.type == type || type < 0) {
            QuestInterface inter = data.quest.questInterface;
            if (inter.isCompleted(player)) {
               if (!data.isCompleted) {
                  if (!data.quest.complete(player, data)) {
                     Packets.send((ServerPlayer)player, new PacketAchievement(Component.m_237115_("quest.completed"), Component.m_237115_(data.quest.title), 2));
                     Packets.send(
                        (ServerPlayer)player,
                        new PacketChat(Component.m_237115_("quest.completed").m_130946_(": ").m_7220_(Component.m_237115_(data.quest.title)))
                     );
                  }

                  data.isCompleted = true;
                  bo = true;
                  EventHooks.onQuestFinished(PlayerData.get(player).scriptData, data.quest);
               }
            } else {
               data.isCompleted = false;
            }
         }
      }

      return bo;
   }
}
