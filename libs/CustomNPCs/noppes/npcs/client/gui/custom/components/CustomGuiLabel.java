package noppes.npcs.client.gui.custom.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiLabelWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.custom.interfaces.IGuiComponent;

public class CustomGuiLabel extends AbstractWidget implements IGuiComponent {
   private CustomGuiLabelWrapper component;
   private int id;
   private GuiCustom parent;

   public CustomGuiLabel(GuiCustom parent, CustomGuiLabelWrapper component) {
      super(component.getPosX(), component.getPosY(), component.getWidth(), component.getHeight(), Component.m_237115_(component.getText()));
      this.component = component;
      this.parent = parent;
      this.init();
   }

   public void init() {
      this.id = this.component.getID();
      this.f_93620_ = this.component.getPosX();
      this.f_93621_ = this.component.getPosY();
      this.m_93674_(this.component.getWidth());
      this.setHeight(this.component.getHeight());
      this.f_93623_ = this.component.getEnabled() && this.component.getVisible();
      this.f_93624_ = this.component.getVisible();
      this.m_93666_(Component.m_237115_(this.component.getText()));
   }

   @Override
   public void onRender(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if (this.f_93623_) {
         matrixStack.m_85836_();
         matrixStack.m_85837_(0.0, 0.0, this.id);
         matrixStack.m_85841_(this.component.getScale(), this.component.getScale(), 0.0F);
         boolean hovered = mouseX >= this.f_93620_
            && mouseY >= this.f_93621_
            && mouseX < this.f_93620_ + this.f_93618_
            && mouseY < this.f_93621_ + this.f_93619_;
         if (this.component.getCentered()) {
            Minecraft.m_91087_()
               .f_91062_
               .m_92889_(
                  matrixStack,
                  this.m_6035_(),
                  this.f_93620_ + (this.f_93618_ - Minecraft.m_91087_().f_91062_.m_92852_(this.m_6035_())) / 2.0F,
                  this.f_93621_,
                  this.component.getColor()
               );
         } else {
            Minecraft.m_91087_().f_91062_.m_92889_(matrixStack, this.m_6035_(), this.f_93620_, this.f_93621_, this.component.getColor());
         }

         if (hovered && this.component.hasHoverText()) {
            this.parent.hoverText = this.component.getHoverTextList();
         }

         matrixStack.m_85849_();
      }
   }

   @Override
   public int getID() {
      return this.id;
   }

   public void m_142291_(NarrationElementOutput p_169152_) {
   }

   public void setText(String s) {
      this.m_93666_(Component.m_237115_(s));
   }

   @Override
   public ICustomGuiComponent component() {
      return this.component;
   }
}
