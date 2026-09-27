package noppes.npcs.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.Toast.Visibility;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiAchievement implements Toast {
   private String title;
   private String subtitle;
   private int type;
   private long firstDrawTime;
   private boolean newDisplay;

   public GuiAchievement(Component titleComponent, Component subtitleComponent, int type) {
      this.title = titleComponent.getString();
      this.subtitle = subtitleComponent == null ? null : subtitleComponent.getString();
      this.type = type;
   }

   public Visibility m_7172_(PoseStack matrixStack, ToastComponent toastGui, long delta) {
      if (this.newDisplay) {
         this.firstDrawTime = delta;
         this.newDisplay = false;
      }

      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, f_94893_);
      toastGui.m_93228_(matrixStack, 0, 0, 0, 32 * this.type, 160, 32);
      int color1 = -256;
      int color2 = -1;
      if (this.type == 1 || this.type == 3) {
         color1 = -11534256;
         color2 = -16777216;
      }

      toastGui.m_94929_().f_91062_.m_92883_(matrixStack, this.title, 18.0F, 7.0F, color1);
      toastGui.m_94929_().f_91062_.m_92883_(matrixStack, this.subtitle, 18.0F, 18.0F, color2);
      return delta - this.firstDrawTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
   }
}
