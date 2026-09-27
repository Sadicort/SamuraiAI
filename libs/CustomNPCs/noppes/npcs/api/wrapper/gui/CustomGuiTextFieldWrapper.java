package noppes.npcs.api.wrapper.gui;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.function.gui.GuiComponentUpdate;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ITextField;

public class CustomGuiTextFieldWrapper extends CustomGuiComponentWrapper implements ITextField {
   private int color = 14737632;
   private int type = 0;
   private String text = "";
   private boolean focused = false;
   private GuiComponentUpdate<ITextField> onChange = null;
   private GuiComponentUpdate<ITextField> onFocusLost = null;
   private int min = Integer.MIN_VALUE;
   private int max = Integer.MAX_VALUE;

   public CustomGuiTextFieldWrapper() {
   }

   public CustomGuiTextFieldWrapper(int id, int x, int y, int width, int height) {
      this.setID(id);
      this.setPos(x, y);
      this.setSize(width, height);
   }

   @Override
   public String getText() {
      return this.text;
   }

   public CustomGuiTextFieldWrapper setText(String text) {
      this.text = Objects.requireNonNullElse(text, "");
      if (!this.text.isBlank() && (this.getCharacterType() == 1 || this.getCharacterType() == 2)) {
         this.setInteger(this.getInteger());
      }

      return this;
   }

   @Override
   public int getInteger() {
      if (this.type == 0) {
         throw new CustomNPCsException("Character Type 0 doesnt convert to integer");
      } else if (this.text.isBlank()) {
         return Math.max(this.min, 0);
      } else {
         return this.type == 1 ? Integer.parseInt(this.text) : Integer.parseInt(this.text, 16);
      }
   }

   public CustomGuiTextFieldWrapper setInteger(int i) {
      if (this.type == 0) {
         throw new CustomNPCsException("Character Type 0 doesnt support setInteger");
      }

      i = Math.max(this.min, i);
      i = Math.min(this.max, i);
      if (this.type == 1 || this.type == 3) {
         this.text = i + "";
      }

      if (this.type == 2) {
         this.text = String.format("%01x", i);
      }

      return this;
   }

   @Override
   public float getFloat() {
      if (this.type == 0) {
         throw new CustomNPCsException("Character Type 0 doesnt convert to float");
      } else if (this.text.isBlank()) {
         return Math.max(this.min, 0);
      } else if (this.type == 1) {
         return Integer.parseInt(this.text);
      } else {
         return this.type == 2 ? Integer.parseInt(this.text, 16) : Float.parseFloat(this.text);
      }
   }

   public CustomGuiTextFieldWrapper setFloat(float f) {
      if (this.type != 0 && this.type != 2) {
         f = Math.max(this.min, f);
         f = Math.min(this.max, f);
         if (this.type == 1) {
            this.text = f + "";
         }

         return this;
      } else {
         throw new CustomNPCsException("Character Type 0 doesnt support setFloat");
      }
   }

   @Override
   public int getColor() {
      return this.color;
   }

   public CustomGuiTextFieldWrapper setColor(int color) {
      this.color = color;
      return this;
   }

   public CustomGuiTextFieldWrapper setFocused(boolean bo) {
      this.focused = bo;
      return this;
   }

   @Override
   public boolean getFocused() {
      return this.focused;
   }

   public CustomGuiTextFieldWrapper setCharacterType(int type) {
      this.type = type;
      return this;
   }

   @Override
   public int getCharacterType() {
      return this.type;
   }

   public CustomGuiTextFieldWrapper setMinMax(int min, int max) {
      if (this.type == 0) {
         throw new CustomNPCsException("Character Type 0 doesnt support setInteger");
      }

      this.min = min;
      this.max = max;
      return this;
   }

   @Override
   public int getType() {
      return 3;
   }

   @Override
   public CompoundTag toNBT(CompoundTag nbt) {
      super.toNBT(nbt);
      nbt.m_128359_("default", this.text);
      nbt.m_128379_("focused", this.focused);
      nbt.m_128405_("color", this.color);
      nbt.m_128405_("character_type", this.type);
      nbt.m_128405_("min", this.min);
      nbt.m_128405_("max", this.max);
      return nbt;
   }

   @Override
   public CustomGuiComponentWrapper fromNBT(CompoundTag nbt) {
      super.fromNBT(nbt);
      this.setText(nbt.m_128461_("default"));
      this.setFocused(nbt.m_128471_("focused"));
      this.setColor(nbt.m_128451_("color"));
      this.setCharacterType(nbt.m_128451_("character_type"));
      this.min = nbt.m_128451_("min");
      this.max = nbt.m_128451_("max");
      return this;
   }

   public CustomGuiTextFieldWrapper setOnChange(GuiComponentUpdate<ITextField> onChange) {
      this.onChange = onChange;
      return this;
   }

   public CustomGuiTextFieldWrapper setOnFocusLost(GuiComponentUpdate<ITextField> onFocusChange) {
      this.onFocusLost = onFocusChange;
      return this;
   }

   public final void onChange(ICustomGui gui) {
      if (this.onChange != null) {
         this.onChange.onChange(gui, this);
      }
   }

   public final void onFocusLost(ICustomGui gui) {
      if (this.onFocusLost != null) {
         this.onFocusLost.onChange(gui, this);
      }
   }
}
