package noppes.npcs.client.gui.custom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.api.wrapper.gui.CustomGuiWrapper;
import noppes.npcs.client.gui.custom.components.CustomGuiTexturedRect;
import noppes.npcs.client.gui.custom.interfaces.IGuiComponent;
import noppes.npcs.containers.ContainerCustomGui;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiSubGuiClosed;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiCustom extends AbstractContainerScreen<ContainerCustomGui> implements IGuiData {
   protected CustomGuiTexturedRect background;
   public CustomGuiWrapper guiWrapper;
   public List<Component> hoverText;
   protected GuiCustomComponents components = new GuiCustomComponents();
   protected GuiCustomScrollingPanel scrollingPanel = new GuiCustomScrollingPanel();
   public GuiCustom subgui = null;
   public GuiCustom parent = null;
   public Inventory inv;
   public GuiCustom.InitCallback initCallback;

   public GuiCustom(ContainerCustomGui container, Inventory inv, Component titleIn) {
      super(container, inv, titleIn);
      this.inv = inv;
   }

   public void m_7856_() {
      super.m_7856_();
      if (this.guiWrapper != null) {
         this.scrollingPanel.setComponents(this, this.guiWrapper.getScrollingPanel());
         this.components.setComponents(this, this.guiWrapper);
      }

      if (this.initCallback != null) {
         this.initCallback.init();
      }

      if (this.subgui != null) {
         this.subgui.m_7856_();
      }
   }

   public void m_181908_() {
      if (this.subgui != null) {
         this.subgui.m_181908_();
      } else {
         this.components.containerTick();
         this.scrollingPanel.containerTick();
      }
   }

   public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      this.hoverText = null;
      this.m_7333_(matrixStack);
      PoseStack posestack = RenderSystem.m_157191_();
      posestack.m_85836_();
      posestack.m_85837_(this.getGuiLeft(), this.getGuiTop(), 0.0);
      RenderSystem.m_157182_();
      matrixStack.m_85836_();
      if (this.background != null) {
         this.background.onRender(matrixStack, mouseX, mouseY, partialTicks);
      }

      this.components.render(matrixStack, mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), partialTicks);
      this.scrollingPanel.render(matrixStack, mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), partialTicks);
      if (this.hoverText != null && !this.hoverText.isEmpty() && this.subgui == null) {
         this.renderTooltip(matrixStack, this.hoverText, Optional.empty(), mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), this.f_96547_);
      }

      posestack.m_85849_();
      RenderSystem.m_157182_();
      super.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
      if (this.subgui == null) {
         this.m_7025_(matrixStack, mouseX, mouseY);
      }

      matrixStack.m_85849_();
      if (this.subgui != null) {
         matrixStack.m_85836_();
         posestack.m_85836_();
         posestack.m_85837_(0.0, 0.0, 40.0);
         RenderSystem.m_157182_();
         matrixStack.m_85837_(0.0, 0.0, 40.0);
         this.subgui.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
         matrixStack.m_85849_();
         posestack.m_85849_();
         RenderSystem.m_157182_();
      }
   }

   protected void m_7286_(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
   }

   protected void m_7027_(PoseStack matrixStack, int x, int y) {
   }

   public boolean m_5534_(char typedChar, int keyCode) {
      if (this.subgui != null) {
         return this.subgui.m_5534_(typedChar, keyCode);
      } else if (this.components.charTyped(typedChar, keyCode)) {
         return true;
      } else {
         return this.scrollingPanel.charTyped(typedChar, keyCode) ? true : super.m_5534_(typedChar, keyCode);
      }
   }

   public boolean m_7933_(int key, int p_keyPressed_2_, int p_keyPressed_3_) {
      if (this.subgui != null) {
         return this.subgui.m_7933_(key, p_keyPressed_2_, p_keyPressed_3_);
      } else if (this.components.keyPressed(key, p_keyPressed_2_, p_keyPressed_3_)) {
         return true;
      } else if (this.scrollingPanel.keyPressed(key, p_keyPressed_2_, p_keyPressed_3_)) {
         return true;
      } else {
         return this.f_96541_.f_91066_.f_92092_.isActiveAndMatches(InputConstants.m_84827_(key, p_keyPressed_2_))
            ? true
            : super.m_7933_(key, p_keyPressed_2_, p_keyPressed_3_);
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int mouseButton) {
      if (this.subgui != null) {
         return this.subgui.m_6375_(mouseX, mouseY, mouseButton);
      } else if (this.components.mouseClicked(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseButton)) {
         return true;
      } else {
         return this.scrollingPanel.mouseClicked(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseButton)
            ? true
            : super.m_6375_(mouseX, mouseY, mouseButton);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double mouseScrolled) {
      if (this.subgui != null) {
         return this.subgui.m_6050_(mouseX, mouseY, mouseScrolled);
      } else {
         return super.m_6050_(mouseX, mouseY, mouseScrolled)
            ? true
            : this.scrollingPanel.mouseScrolled(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseScrolled);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int mouseButton, double dx, double dy) {
      if (this.subgui != null) {
         return this.subgui.m_7979_(mouseX, mouseY, mouseButton, dx, dy);
      } else if (this.components.mouseDragged(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseButton, dx, dy)) {
         return true;
      } else {
         return this.scrollingPanel.mouseDragged(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseButton, dx, dy)
            ? true
            : super.m_7979_(mouseX, mouseY, mouseButton, dx, dy);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int mouseButton) {
      if (this.subgui != null) {
         return this.subgui.m_6348_(mouseX, mouseY, mouseButton);
      } else if (this.components.mouseReleased(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseButton)) {
         return true;
      } else {
         return this.scrollingPanel.mouseReleased(mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), mouseButton)
            ? true
            : super.m_6348_(mouseX, mouseY, mouseButton);
      }
   }

   public boolean m_7043_() {
      return this.guiWrapper != null ? this.guiWrapper.getDoesPauseGame() : true;
   }

   public void m_7379_() {
      if (this.subgui == null) {
         if (this.parent == null) {
            super.m_7379_();
         } else {
            Packets.sendServer(new SPacketCustomGuiSubGuiClosed());
            this.parent.subgui = null;
         }
      } else {
         this.subgui.m_7379_();
      }
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      this.setGuiWrapper((CustomGuiWrapper)new CustomGuiWrapper((IPlayer)NpcAPI.Instance().getIEntity(Minecraft.m_91087_().f_91074_)).fromNBT(compound));
      this.m_7856_();
   }

   public void m_6574_(Minecraft minecraft, int width, int height) {
      super.m_6574_(minecraft, width, height);
      if (this.subgui != null) {
         this.subgui.m_6574_(minecraft, width, height);
      }
   }

   public void setGuiWrapper(CustomGuiWrapper guiWrapper) {
      this.guiWrapper = guiWrapper;
      this.f_97726_ = guiWrapper.getWidth();
      this.f_97727_ = guiWrapper.getHeight();
      this.background = new CustomGuiTexturedRect(this, (CustomGuiTexturedRectWrapper)guiWrapper.getBackgroundRect());
      if (guiWrapper.hasSubGui()) {
         if (this.subgui == null) {
            this.subgui = new GuiCustom((ContainerCustomGui)this.f_97732_, Minecraft.m_91087_().f_91074_.m_150109_(), Component.m_237119_());
            this.subgui.m_6575_(this.f_96541_, this.f_96543_, this.f_96544_);
         }

         this.subgui.parent = this;
         this.subgui.setGuiWrapper(guiWrapper.getSubGui());
      } else {
         ((ContainerCustomGui)this.f_97732_).setGui(guiWrapper, Minecraft.m_91087_().f_91074_);
         this.subgui = null;
         if (this.parent == null) {
            this.m_7856_();
         }
      }
   }

   public IGuiComponent getComponent(UUID id) {
      Optional<IGuiComponent> c = this.components
         .components
         .values()
         .stream()
         .filter(t -> t.component() != null && t.component().getUniqueID().equals(id))
         .findFirst();
      if (c.isPresent()) {
         return c.get();
      } else {
         c = this.scrollingPanel.components.values().stream().filter(t -> t.component() != null && t.component().getUniqueID().equals(id)).findFirst();
         if (c.isPresent()) {
            return c.get();
         } else {
            return this.subgui != null ? this.subgui.getComponent(id) : null;
         }
      }
   }

   public int getTotalGuiLeft() {
      return this.parent != null ? this.parent.getTotalGuiLeft() + this.getGuiLeft() : this.getGuiLeft();
   }

   public int getTotalGuiTop() {
      return this.parent != null ? this.parent.getTotalGuiTop() + this.getGuiTop() : this.getGuiTop();
   }

   public void add(IGuiComponent component) {
      this.components.components.put(component.getID(), component);
   }

   public void addPanel(IGuiComponent component) {
      this.scrollingPanel.components.put(component.getID(), component);
   }

   public interface InitCallback {
      void init();
   }
}
