package noppes.npcs.client.gui.custom.interfaces;

import com.mojang.blaze3d.vertex.PoseStack;
import noppes.npcs.api.gui.ICustomGuiComponent;

public interface IGuiComponent {
   int getID();

   void onRender(PoseStack var1, int var2, int var3, float var4);

   default void onRenderPost(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
   }

   void m_7856_();

   ICustomGuiComponent component();
}
