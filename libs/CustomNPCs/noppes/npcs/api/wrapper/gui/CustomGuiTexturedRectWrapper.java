package noppes.npcs.api.wrapper.gui;

import net.minecraft.nbt.CompoundTag;
import noppes.npcs.api.gui.ITexturedRect;

public class CustomGuiTexturedRectWrapper extends CustomGuiComponentWrapper implements ITexturedRect {
   int textureX;
   int textureY = -1;
   float scale = 1.0F;
   String texture = "";
   public boolean hasRepeatingTexture = false;
   public int texRepWidth;
   public int texRepHeight;
   public int texRepBorderSize = 0;

   public CustomGuiTexturedRectWrapper() {
   }

   public CustomGuiTexturedRectWrapper(int id, String texture, int x, int y, int width, int height) {
      this.setID(id);
      this.setTexture(texture);
      this.setPos(x, y);
      this.setSize(width, height);
   }

   public CustomGuiTexturedRectWrapper(int id, String texture, int x, int y, int width, int height, int textureX, int textureY) {
      this(id, texture, x, y, width, height);
      this.setTextureOffset(textureX, textureY);
   }

   @Override
   public String getTexture() {
      return this.texture;
   }

   public CustomGuiTexturedRectWrapper setTexture(String texture) {
      this.texture = texture;
      return this;
   }

   @Override
   public float getScale() {
      return this.scale;
   }

   public CustomGuiTexturedRectWrapper setScale(float scale) {
      this.scale = scale;
      return this;
   }

   @Override
   public int getTextureX() {
      return this.textureX;
   }

   @Override
   public int getTextureY() {
      return this.textureY;
   }

   public CustomGuiTexturedRectWrapper setTextureOffset(int offsetX, int offsetY) {
      this.textureX = offsetX;
      this.textureY = offsetY;
      return this;
   }

   public CustomGuiTexturedRectWrapper setRepeatingTexture(int width, int height, int borderSize) {
      this.hasRepeatingTexture = true;
      this.texRepWidth = width;
      this.texRepHeight = height;
      this.texRepBorderSize = borderSize;
      return this;
   }

   @Override
   public int getType() {
      return 2;
   }

   @Override
   public CompoundTag toNBT(CompoundTag compound) {
      super.toNBT(compound);
      compound.m_128350_("scale", this.scale);
      compound.m_128359_("texture", this.texture);
      if (this.textureX >= 0 && this.textureY >= 0) {
         compound.m_128385_("texPos", new int[]{this.textureX, this.textureY});
      }

      compound.m_128379_("hasRepeatingTexture", this.hasRepeatingTexture);
      if (this.hasRepeatingTexture) {
         compound.m_128385_("repeatingTexture", new int[]{this.texRepWidth, this.texRepHeight, this.texRepBorderSize});
      }

      return compound;
   }

   @Override
   public CustomGuiComponentWrapper fromNBT(CompoundTag compound) {
      super.fromNBT(compound);
      this.setScale(compound.m_128457_("scale"));
      this.setTexture(compound.m_128461_("texture"));
      if (compound.m_128441_("texPos")) {
         int[] arr = compound.m_128465_("texPos");
         this.setTextureOffset(arr[0], arr[1]);
      }

      this.hasRepeatingTexture = compound.m_128471_("hasRepeatingTexture");
      if (this.hasRepeatingTexture) {
         int[] arr = compound.m_128465_("repeatingTexture");
         this.setRepeatingTexture(arr[0], arr[1], arr[2]);
      }

      return this;
   }
}
