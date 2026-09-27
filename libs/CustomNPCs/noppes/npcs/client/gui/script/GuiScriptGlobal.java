package noppes.npcs.client.gui.script;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

public class GuiScriptGlobal extends GuiNPCInterface {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/smallbg.png");

   public GuiScriptGlobal() {
      this.imageWidth = 176;
      this.imageHeight = 222;
      this.drawDefaultBackground = false;
      this.title = "";
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.addButton(new GuiButtonNop(this, 0, this.guiLeft + 38, this.guiTop + 20, 100, 20, "Players"));
      this.addButton(new GuiButtonNop(this, 1, this.guiLeft + 38, this.guiTop + 50, 100, 20, "Forge"));
   }

   @Override
   public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      this.m_7333_(matrixStack);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      this.m_93228_(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.imageWidth, this.imageHeight);
      super.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      if (guibutton.id == 0) {
         this.setScreen(new GuiScriptPlayers());
      }

      if (guibutton.id == 1) {
         this.setScreen(new GuiScriptForge());
      }
   }

   @Override
   public void save() {
   }
}
