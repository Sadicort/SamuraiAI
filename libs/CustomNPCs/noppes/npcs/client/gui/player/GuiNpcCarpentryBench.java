package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerCarpentryBench;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

public class GuiNpcCarpentryBench extends GuiContainerNPCInterface<ContainerCarpentryBench> {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/carpentry.png");
   private ContainerCarpentryBench container;
   private GuiButtonNop button;

   public GuiNpcCarpentryBench(ContainerCarpentryBench container, Inventory inv, Component titleIn) {
      super(null, container, inv, titleIn);
      this.container = container;
      this.title = "";
      this.f_96546_ = false;
      this.f_97727_ = 180;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.addButton(this.button = new GuiButtonNop(this, 0, this.guiLeft + 158, this.guiTop + 4, 12, 20, "..."));
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      this.setScreen(new GuiRecipes());
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float partialTicks, int x, int y) {
      this.button.f_93623_ = RecipeController.instance != null && !RecipeController.instance.anvilRecipes.isEmpty();
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      int l = (this.f_96543_ - this.f_97726_) / 2;
      int i1 = (this.f_96544_ - this.f_97727_) / 2;
      String title = I18n.m_118938_("block.customnpcs.npccarpentybench", new Object[0]);
      this.m_93228_(matrixStack, l, i1, 0, 0, this.f_97726_, this.f_97727_);
      this.f_96547_.m_92883_(matrixStack, title, this.guiLeft + 4, this.guiTop + 4, CustomNpcResourceListener.DefaultTextColor);
      this.f_96547_
         .m_92883_(
            matrixStack, I18n.m_118938_("container.inventory", new Object[0]), this.guiLeft + 4, this.guiTop + 87, CustomNpcResourceListener.DefaultTextColor
         );
   }

   @Override
   public void save() {
   }
}
