package noppes.npcs.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.client.gui.player.GuiQuestLog;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPlayerKeyPressed;
import noppes.npcs.packets.server.SPacketPlayerLeftClicked;
import noppes.npcs.packets.server.SPacketQuestCompletionCheckAll;
import noppes.npcs.packets.server.SPacketSceneReset;
import noppes.npcs.packets.server.SPacketSceneStart;

public class ClientTickHandler {
   private Level prevLevel;
   private boolean otherContainer = false;
   private final int[] ignoreKeys = new int[]{341, 340, 342, 343, 345, 344, 346, 347};

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ == null || !(mc.f_91074_.f_36096_ instanceof InventoryMenu)) {
            this.otherContainer = true;
         } else if (this.otherContainer) {
            Packets.sendServer(new SPacketQuestCompletionCheckAll());
            this.otherContainer = false;
         }

         CustomNpcs.ticks++;
         RenderNPCInterface.LastTextureTick++;
         if (this.prevLevel != mc.f_91073_) {
            this.prevLevel = mc.f_91073_;
            MusicController.Instance.stopMusic();
         }
      }
   }

   @SubscribeEvent
   public void onKey(Key event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc != null && mc.f_91073_ != null && mc.m_91403_() != null) {
         if (CustomNpcs.SceneButtonsEnabled) {
            if (ClientProxy.Scene1.m_90857_()) {
               Packets.sendServer(new SPacketSceneStart(1));
            }

            if (ClientProxy.Scene2.m_90857_()) {
               Packets.sendServer(new SPacketSceneStart(2));
            }

            if (ClientProxy.Scene3.m_90857_()) {
               Packets.sendServer(new SPacketSceneStart(3));
            }

            if (ClientProxy.SceneReset.m_90857_()) {
               Packets.sendServer(new SPacketSceneReset());
            }
         }

         if (ClientProxy.QuestLog.m_90857_()) {
            if (mc.f_91080_ == null) {
               NoppesUtil.openGUI(mc.f_91074_, new GuiQuestLog(mc.f_91074_));
            } else if (mc.f_91080_ instanceof GuiQuestLog) {
               mc.f_91067_.m_91601_();
            }
         }

         if (event.getAction() == 1 || event.getAction() == 0) {
            boolean isCtrlPressed = InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 341)
               || InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 345);
            boolean isShiftPressed = InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 340)
               || InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 344);
            boolean isAltPressed = InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 342)
               || InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 346);
            boolean isMetaPressed = InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 343)
               || InputConstants.m_84830_(Minecraft.m_91087_().m_91268_().m_85439_(), 347);
            String openGui = mc.f_91080_ == null ? "" : mc.f_91080_.getClass().getName();
            Packets.sendServer(
               new SPacketPlayerKeyPressed(event.getKey(), isCtrlPressed, isShiftPressed, isAltPressed, isMetaPressed, event.getAction() == 0, openGui)
            );
         }
      }
   }

   @SubscribeEvent
   public void invoke(LeftClickEmpty event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Packets.sendServer(new SPacketPlayerLeftClicked());
      }
   }

   private boolean isIgnoredKey(int key) {
      for (int i : this.ignoreKeys) {
         if (i == key) {
            return true;
         }
      }

      return false;
   }
}
