package noppes.npcs;

import java.util.ArrayList;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.ServerScoreboard.Method;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import noppes.npcs.controllers.MassBlockController;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.controllers.SyncController;
import noppes.npcs.controllers.VisibilityController;
import noppes.npcs.controllers.data.Availability;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.entity.data.DataScenes;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSync;
import noppes.npcs.shared.client.util.AnalyticsTracking;

public class ServerTickHandler {
   public static final float PLAYER_START_HEALTH = 120.0F;
   public int ticks = 0;

   private static void applyStartHealth(ServerPlayer player) {
      player.m_21051_(Attributes.f_22276_).m_22100_(PLAYER_START_HEALTH);
      player.m_21153_(PLAYER_START_HEALTH);
   }

   @SubscribeEvent
   public void onServerTick(PlayerTickEvent event) {
      if (event.side == LogicalSide.SERVER && event.phase == Phase.START) {
         Player player = event.player;
         PlayerData data = PlayerData.get(player);
         if (player.m_20193_().m_46468_() % 24000L == 1L || player.m_20193_().m_46468_() % 240000L == 12001L) {
            VisibilityController.instance.onUpdate((ServerPlayer)player);
         }

         if (data.updateClient) {
            Packets.send((ServerPlayer)player, new PacketSync(8, data.getSyncNBT(), true));
            VisibilityController.instance.onUpdate((ServerPlayer)player);
            data.updateClient = false;
         }

         if (data.prevHeldItem != player.m_21205_() && (data.prevHeldItem.m_41720_() == CustomItems.wand || player.m_21205_().m_41720_() == CustomItems.wand)) {
            VisibilityController.instance.onUpdate((ServerPlayer)player);
         }

         data.prevHeldItem = player.m_21205_();
      }
   }

   @SubscribeEvent
   public void onServerTick(LevelTickEvent event) {
      if (event.side == LogicalSide.SERVER && event.phase == Phase.START) {
         NPCSpawning.findChunksForSpawning((ServerLevel)event.level);
      }
   }

   @SubscribeEvent
   public void onServerTick(ServerTickEvent event) {
      if (event.side == LogicalSide.SERVER && event.phase == Phase.START && this.ticks++ >= 20) {
         SchematicController.Instance.updateBuilding();
         MassBlockController.Update();
         this.ticks = 0;

         for (DataScenes.SceneState state : DataScenes.StartedScenes.values()) {
            if (!state.paused) {
               state.ticks++;
            }
         }

         for (DataScenes.SceneContainer entry : DataScenes.ScenesToRun) {
            entry.update();
         }

         DataScenes.ScenesToRun = new ArrayList<>();
      }
   }

   @SubscribeEvent
   public void playerLogin(PlayerLoggedInEvent event) {
      final ServerPlayer player = (ServerPlayer)event.getEntity();
      MinecraftServer server = event.getEntity().m_20194_();
      applyStartHealth(player);

      for (ServerLevel level : server.m_129785_()) {
         ServerScoreboard board = level.m_6188_();

         for (String objective : Availability.scores) {
            Objective so = board.m_83477_(objective);
            if (so != null) {
               if (board.m_136237_(so) == 0) {
                  player.f_8906_.m_9829_(new ClientboundSetObjectivePacket(so, 0));
               }

               Score sco = board.m_83471_(player.m_6302_(), so);
               player.f_8906_.m_9829_(new ClientboundSetScorePacket(Method.CHANGE, so.m_83320_(), sco.m_83405_(), sco.m_83400_()));
            }
         }
      }

      player.f_36095_.m_38893_(new ContainerListener() {
         public void m_7934_(AbstractContainerMenu container, int slotInd, ItemStack stack) {
            if (!player.f_19853_.f_46443_) {
               PlayerQuestData playerdata = PlayerData.get(player).questData;
               playerdata.checkQuestCompletion(player, 0);
            }
         }

         public void m_142153_(AbstractContainerMenu container, int varToUpdate, int newValue) {
         }
      });
      PlayerData data = PlayerData.get(event.getEntity());
      String serverName = "local";
      if (server.m_6982_()) {
         serverName = "server";
      } else if (server.m_6992_()) {
         serverName = "lan";
      }

      AnalyticsTracking.sendData(data.iAmStealingYourDatas, "join", serverName);
      SyncController.syncPlayer((ServerPlayer)event.getEntity());
   }

   @SubscribeEvent
   public void playerRespawn(PlayerRespawnEvent event) {
      applyStartHealth((ServerPlayer)event.getEntity());
   }
}
