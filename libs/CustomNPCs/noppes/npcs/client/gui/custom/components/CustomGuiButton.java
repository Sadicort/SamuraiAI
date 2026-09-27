package noppes.npcs.client.gui.custom.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiButtonWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.custom.interfaces.IGuiComponent;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCustomGuiButton;

public class CustomGuiButton extends Button implements IGuiComponent {
   protected GuiCustom parent;
   private CustomGuiTexturedRect background;
   public CustomGuiButtonWrapper component;
   protected boolean hovered;
   private int colour = 16777215;
   protected OnPress onPress;
   public int id;

   public CustomGuiButton(GuiCustom parent, CustomGuiButtonWrapper component) {
      super(component.getPosX(), component.getPosY(), component.getWidth(), component.getHeight(), Component.m_237115_(component.getLabel()), null);
      this.onPress = button -> {
         if (!component.disablePackets) {
            Packets.sendServer(new SPacketCustomGuiButton(component.getUniqueID()));
         } else {
            component.onPress(parent.guiWrapper);
         }
      };
      this.parent = parent;
      this.component = component;
      this.init();
   }

   public void m_5691_() {
      this.onPress.m_93750_(this);
   }

   public void init() {
      this.id = this.component.getID();
      this.f_93620_ = this.component.getPosX();
      this.f_93621_ = this.component.getPosY();
      this.m_93674_(this.component.getWidth());
      this.setHeight(this.component.getHeight());
      this.background = new CustomGuiTexturedRect(this.parent, this.component.getTextureRect());
      this.m_93666_(Component.m_237115_(this.component.getLabel()));
      this.f_93623_ = this.component.getEnabled() && this.component.getVisible();
      this.f_93624_ = this.component.getVisible();
   }

   public boolean m_7933_(int p_231046_1_, int p_231046_2_, int p_231046_3_) {
      return false;
   }

   @Override
   public int getID() {
      return this.id;
   }

   @Override
   public void onRender(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if (this.f_93624_) {
         matrixStack.m_85836_();
         matrixStack.m_85837_(this.f_93620_, this.f_93621_, 0.0);
         Minecraft mc = Minecraft.m_91087_();
         this.hovered = this.isHovered(mouseX, mouseY);
         int i = this.m_7202_(this.hovered);
         this.background.textureY = this.component.getTextureY() + i * this.component.getTextureHoverOffset();
         this.background.onRender(matrixStack, mouseX - this.f_93620_, mouseY - this.f_93621_, partialTicks);
         matrixStack.m_85837_(0.0, 0.0, 10.0);
         this.renderLabel(matrixStack);
         if (!this.component.getDisplayItem().isEmpty()) {
            int xx = (int)((this.f_93618_ - 16.0F) / 2.0F);
            int yy = (int)((this.f_93619_ - 16.0F) / 2.0F) + 1;
            this.m_93250_(-90);
            PoseStack posestack = RenderSystem.m_157191_();
            posestack.m_85836_();
            posestack.m_85837_(this.f_93620_, this.f_93621_, -90.0);
            RenderSystem.m_157182_();
            mc.m_91291_().m_115123_(this.component.getDisplayItem().getMCItemStack(), xx, yy);
            mc.m_91291_().m_115169_(mc.f_91062_, this.component.getDisplayItem().getMCItemStack(), xx, yy);
            posestack.m_85849_();
            RenderSystem.m_157182_();
         }

         if (this.hovered && this.component.hasHoverText()) {
            this.parent.hoverText = this.component.getHoverTextList();
         }

         matrixStack.m_85849_();
      }
   }

   public void renderLabel(PoseStack matrixStack) {
      if (!this.component.getLabel().isBlank()) {
         int j = 14737632;
         if (this.colour != 0) {
            j = this.colour;
         } else if (!this.f_93623_) {
            j = 10526880;
         } else if (this.hovered) {
            j = 16777120;
         }

         Minecraft mc = Minecraft.m_91087_();
         matrixStack.m_85837_(0.0, 0.0, 1.0);
         m_93215_(matrixStack, mc.f_91062_, this.m_6035_(), this.f_93618_ / 2, (this.f_93619_ - 8) / 2, j);
      }
   }

   public boolean isHovered(int mouseX, int mouseY) {
      return mouseX >= this.f_93620_ && mouseY >= this.f_93621_ && mouseX < this.f_93620_ + this.f_93618_ && mouseY < this.f_93621_ + this.f_93619_;
   }

   @Override
   public ICustomGuiComponent component() {
      return this.component;
   }

   public void setColour(int colour) {
      this.colour = colour;
   }

   protected int hoverState(boolean mouseOver) {
      int i = 0;
      if (mouseOver) {
         i = 1;
      }

      return i;
   }
}
