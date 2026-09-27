package noppes.npcs.client;

import java.io.File;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.CustomNpcs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class NoppesUtil {
   private static EntityNPCInterface lastNpc;

   public static void requestOpenGUI(EnumGuiType gui) {
      requestOpenGUI(gui, BlockPos.f_121853_);
   }

   public static void requestOpenGUI(EnumGuiType gui, BlockPos pos) {
      Packets.sendServer(new SPacketGuiOpen(gui, pos));
   }

   public static EntityNPCInterface getLastNpc() {
      return lastNpc;
   }

   public static void setLastNpc(EntityNPCInterface npc) {
      lastNpc = npc;
   }

   public static void openGUI(Player player, Object guiscreen) {
      CustomNpcs.proxy.openGui(player, guiscreen);
   }

   public static void openFolder(File dir) {
      Util.m_137581_().m_137644_(dir);
   }

   public static void clickSound() {
      Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119752_(SoundEvents.f_12490_, 1.0F));
   }
}
