package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketAchievement;
import noppes.npcs.packets.client.PacketChat;

public class CmdQuest {
   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.m_82127_("quest");
      command.then(
         Commands.m_82127_("start")
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("players", EntityArgument.m_91470_()).requires(source -> source.m_6761_(2)))
                  .then(Commands.m_82129_("quest", IntegerArgumentType.integer(0)).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        QuestData questdata = new QuestData(quest);
                        data.questData.activeQuests.put(quest.id, questdata);
                        data.save(true);
                        Packets.send(player, new PacketAchievement(Component.m_237115_("quest.newquest"), Component.m_237115_(quest.title), 2));
                        Packets.send(player, new PacketChat(Component.m_237115_("quest.newquest").m_130946_(":").m_7220_(Component.m_237115_(quest.title))));
                     }

                     return 1;
                  }))
            )
      );
      command.then(
         Commands.m_82127_("finish")
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("players", EntityArgument.m_91470_()).requires(source -> source.m_6761_(2)))
                  .then(Commands.m_82129_("quest", IntegerArgumentType.integer(0)).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        data.questData.finishedQuests.put(quest.id, System.currentTimeMillis());
                        data.save(true);
                     }

                     return 1;
                  }))
            )
      );
      command.then(
         Commands.m_82127_("stop")
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("players", EntityArgument.m_91470_()).requires(source -> source.m_6761_(2)))
                  .then(Commands.m_82129_("quest", IntegerArgumentType.integer(0)).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        data.questData.activeQuests.remove(quest.id);
                        data.save(true);
                     }

                     return 1;
                  }))
            )
      );
      command.then(
         Commands.m_82127_("remove")
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("players", EntityArgument.m_91470_()).requires(source -> source.m_6761_(2)))
                  .then(Commands.m_82129_("quest", IntegerArgumentType.integer(0)).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        data.questData.activeQuests.remove(quest.id);
                        data.questData.finishedQuests.remove(quest.id);
                        data.save(true);
                     }

                     return 1;
                  }))
            )
      );
      command.then(
         Commands.m_82127_("objective")
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("players", EntityArgument.m_91470_()).requires(source -> source.m_6761_(2)))
                  .then(((RequiredArgumentBuilder)Commands.m_82129_("quest", IntegerArgumentType.integer(0)).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        if (data.questData.activeQuests.containsKey(quest.id)) {
                           IQuestObjective[] objectives = quest.questInterface.getObjectives(player);

                           for (IQuestObjective ob : objectives) {
                              player.m_213846_(ob.getMCText());
                           }
                        }
                     }

                     return 1;
                  })).then(((RequiredArgumentBuilder)Commands.m_82129_("objective", IntegerArgumentType.integer(0, 3)).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     int objective = IntegerArgumentType.getInteger(context, "objective");

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        if (data.questData.activeQuests.containsKey(quest.id)) {
                           IQuestObjective[] objectives = quest.questInterface.getObjectives(player);
                           if (objective < objectives.length) {
                              player.m_213846_(objectives[objective].getMCText());
                           }
                        }
                     }

                     return 1;
                  })).then(Commands.m_82129_("value", IntegerArgumentType.integer()).executes(context -> {
                     Collection<ServerPlayer> players = EntityArgument.m_91477_(context, "players");
                     if (players.isEmpty()) {
                        return 1;
                     }

                     Quest quest = QuestController.instance.quests.get(IntegerArgumentType.getInteger(context, "quest"));
                     if (quest == null) {
                        throw new CommandRuntimeException(Component.m_237113_("Unknown QuestID"));
                     }

                     int objective = IntegerArgumentType.getInteger(context, "objective");
                     int value = IntegerArgumentType.getInteger(context, "value");

                     for (ServerPlayer player : players) {
                        PlayerData data = PlayerData.get(player);
                        if (data.questData.activeQuests.containsKey(quest.id)) {
                           IQuestObjective[] objectives = quest.questInterface.getObjectives(player);
                           if (objective < objectives.length) {
                              objectives[objective].setProgress(value);
                           }
                        }
                     }

                     return 1;
                  }))))
            )
      );
      ((LiteralArgumentBuilder)command.requires(source -> source.m_6761_(4))).then(Commands.m_82127_("reload").executes(context -> {
         new QuestController().load();
         SyncController.syncAllQuests();
         return 1;
      }));
      return command;
   }
}
