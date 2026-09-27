package noppes.npcs.client.gui.custom.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Matrix4f;
import java.util.List;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.api.gui.ICustomGuiComponent;
import noppes.npcs.api.wrapper.gui.CustomGuiTexturedRectWrapper;
import noppes.npcs.client.gui.custom.GuiCustom;
import noppes.npcs.client.gui.custom.interfaces.IGuiComponent;

public class CustomGuiTexturedRect extends GuiComponent implements IGuiComponent {
   private CustomGuiTexturedRectWrapper component = null;
   GuiCustom parent;
   ResourceLocation texture;
   public int id;
   public int x;
   public int y;
   public int width;
   public int height;
   public int textureX;
   public int textureY;
   float scale = 1.0F;
   List<Component> hoverText;
   public boolean hasRepeatingTexture = false;
   public int texRepWidth;
   public int texRepHeight;
   public int texRepBorderSize = 0;

   public CustomGuiTexturedRect(GuiCustom parent, CustomGuiTexturedRectWrapper component) {
      this.component = component;
      this.parent = parent;
      this.init();
   }

   public void init() {
      this.id = this.component.getID();
      this.texture = new ResourceLocation(this.component.getTexture());
      this.x = this.component.getPosX();
      this.y = this.component.getPosY();
      this.width = this.component.getWidth();
      this.height = this.component.getHeight();
      this.textureX = this.component.getTextureX();
      this.textureY = this.component.getTextureY();
      this.scale = this.component.getScale();
      this.hasRepeatingTexture = this.component.hasRepeatingTexture;
      this.texRepWidth = this.component.texRepWidth;
      this.texRepHeight = this.component.texRepHeight;
      this.texRepBorderSize = this.component.texRepBorderSize;
      if (this.component.hasHoverText()) {
         this.hoverText = this.component.getHoverTextList();
      }
   }

   public CustomGuiTexturedRect setRep(int texRepWidth, int texRepHeight, int texRepBorderSize) {
      this.texRepWidth = texRepWidth;
      this.texRepHeight = texRepHeight;
      this.texRepBorderSize = texRepBorderSize;
      this.hasRepeatingTexture = true;
      return this;
   }

   @Override
   public int getID() {
      return this.id;
   }

   @Override
   public void onRender(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      if (!this.component.getTexture().isEmpty() && this.component.getVisible()) {
         boolean hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
         matrixStack.m_85836_();
         RenderSystem.m_157427_(GameRenderer::m_172817_);
         RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.m_157456_(0, this.texture);
         Matrix4f m = matrixStack.m_85850_().m_85861_();
         if (!this.hasRepeatingTexture) {
            this.draw(m, this.x, this.y, this.textureX, this.textureY, this.width, this.height);
         } else {
            if (this.texRepBorderSize > 0) {
               this.draw(m, this.x, this.y, this.textureX, this.textureY, this.texRepBorderSize, this.texRepBorderSize);
               this.draw(
                  m,
                  this.x + this.width - this.texRepBorderSize,
                  this.y,
                  this.textureX + this.texRepWidth - this.texRepBorderSize,
                  this.textureY,
                  this.texRepBorderSize,
                  this.texRepBorderSize
               );
               this.draw(
                  m,
                  this.x,
                  this.y + this.height - this.texRepBorderSize,
                  this.textureX,
                  this.textureY + this.texRepHeight - this.texRepBorderSize,
                  this.texRepBorderSize,
                  this.texRepBorderSize
               );
               this.draw(
                  m,
                  this.x + this.width - this.texRepBorderSize,
                  this.y + this.height - this.texRepBorderSize,
                  this.textureX + this.texRepWidth - this.texRepBorderSize,
                  this.textureY + this.texRepHeight - this.texRepBorderSize,
                  this.texRepBorderSize,
                  this.texRepBorderSize
               );
            }

            float w = this.width - this.texRepBorderSize * 2.0F;
            float h = this.height - this.texRepBorderSize * 2.0F;
            float tw = this.texRepWidth - this.texRepBorderSize * 2.0F;
            float th = this.texRepHeight - this.texRepBorderSize * 2.0F;
            float mx = w / tw;
            float my = h / th;

            for (int i = 0; i < my; i++) {
               float dh = th * Math.min(1.0F, my - i);
               this.draw(m, this.x, this.y + this.texRepBorderSize + th * i, this.textureX, this.textureY + this.texRepBorderSize, this.texRepBorderSize, dh);
               this.draw(
                  m,
                  this.x + this.width - this.texRepBorderSize,
                  this.y + this.texRepBorderSize + th * i,
                  this.textureX + this.texRepWidth - this.texRepBorderSize,
                  this.textureY + this.texRepBorderSize,
                  this.texRepBorderSize,
                  dh
               );

               for (int j = 0; j < mx; j++) {
                  float dw = tw * Math.min(1.0F, mx - j);
                  this.draw(m, this.x + this.texRepBorderSize + tw * j, this.y, this.textureX + this.texRepBorderSize, this.textureY, dw, this.texRepBorderSize);
                  this.draw(
                     m,
                     this.x + this.texRepBorderSize + tw * j,
                     this.y + this.height - this.texRepBorderSize,
                     this.textureX + this.texRepBorderSize,
                     this.textureY + this.texRepHeight - this.texRepBorderSize,
                     dw,
                     this.texRepBorderSize
                  );
                  this.draw(
                     m,
                     this.x + this.texRepBorderSize + tw * j,
                     this.y + this.texRepBorderSize + th * i,
                     this.textureX + this.texRepBorderSize,
                     this.textureY + this.texRepBorderSize,
                     dw,
                     dh
                  );
               }
            }
         }

         if (hovered && this.hoverText != null && this.hoverText.size() > 0) {
            this.parent.hoverText = this.hoverText;
         }

         matrixStack.m_85849_();
      }
   }

   private void draw(Matrix4f m, float x, float y, float texX, float texY, float width, float height) {
      BufferBuilder bufferbuilder = Tesselator.m_85913_().m_85915_();
      bufferbuilder.m_166779_(Mode.QUADS, DefaultVertexFormat.f_85817_);
      bufferbuilder.m_85982_(m, x, y + height * this.scale, this.id).m_7421_(texX * 0.00390625F, (texY + height) * 0.00390625F).m_5752_();
      bufferbuilder.m_85982_(m, x + width * this.scale, y + height * this.scale, this.id)
         .m_7421_((texX + width) * 0.00390625F, (texY + height) * 0.00390625F)
         .m_5752_();
      bufferbuilder.m_85982_(m, x + width * this.scale, y, this.id).m_7421_((texX + width) * 0.00390625F, texY * 0.00390625F).m_5752_();
      bufferbuilder.m_85982_(m, x, y, this.id).m_7421_(texX * 0.00390625F, texY * 0.00390625F).m_5752_();
      BufferUploader.m_231202_(bufferbuilder.m_231175_());
   }

   public void setTexture(ResourceLocation texture) {
      this.texture = texture;
   }

   @Override
   public ICustomGuiComponent component() {
      return this.component;
   }
}
