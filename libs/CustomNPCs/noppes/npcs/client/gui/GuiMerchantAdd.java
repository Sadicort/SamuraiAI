package noppes.npcs.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.containers.ContainerManageRecipes;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiWrapper;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

@OnlyIn(Dist.CLIENT)
public class GuiMerchantAdd extends AbstractContainerScreen<ContainerManageRecipes> implements IGuiInterface {
   public GuiMerchantAdd(ContainerManageRecipes container, Inventory inv, Component titleIn) {
      super(container, inv, titleIn);
   }

   protected void m_7286_(PoseStack matrixStack, float partialTicks, int x, int y) {
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
   }

   @Override
   public void save() {
   }

   @Override
   public boolean hasSubGui() {
      return false;
   }

   @Override
   public Screen getSubGui() {
      return null;
   }

   @Override
   public int getWidth() {
      return this.f_96543_;
   }

   @Override
   public int getHeight() {
      return this.f_96544_;
   }

   @Override
   public Screen getParent() {
      return null;
   }

   @Override
   public void elementClicked() {
   }

   @Override
   public void subGuiClosed(Screen subgui) {
   }

   @Override
   public GuiWrapper getWrapper() {
      return null;
   }

   @Override
   public void initGui() {
      this.m_7856_();
   }
}
