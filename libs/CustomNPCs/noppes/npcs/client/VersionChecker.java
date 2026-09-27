package noppes.npcs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.world.entity.player.Player;

public class VersionChecker extends Thread {
   @Override
   public void run() {
      String name = "§2CustomNpcs§f";
      String link = "§9§nClick here";
      String text = name + " installed. For more info " + link;

      try {
         Player player = Minecraft.m_91087_().f_91074_;
      } catch (NoSuchMethodError e) {
         return;
      }

      LocalPlayer var8;
      while ((var8 = Minecraft.m_91087_().f_91074_) == null) {
         try {
            Thread.sleep(2000L);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }

      MutableComponent message = Component.m_237115_(text);
      message.m_6270_(message.m_7383_().m_131142_(new ClickEvent(Action.OPEN_URL, "http://www.kodevelopment.nl/minecraft/customnpcs/")));
      var8.m_213846_(message);
   }
}
