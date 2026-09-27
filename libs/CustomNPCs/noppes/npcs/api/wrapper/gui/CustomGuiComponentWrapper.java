package noppes.npcs.api.wrapper.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import noppes.npcs.api.gui.ICustomGuiComponent;

public abstract class CustomGuiComponentWrapper implements ICustomGuiComponent {
   public UUID uniqueId = UUID.randomUUID();
   private int id;
   private int posX;
   private int posY;
   private int width;
   private int height;
   private List<Component> hoverText = new ArrayList<>();
   private boolean enabled = true;
   private boolean visible = true;
   public boolean disablePackets = false;

   public CustomGuiComponentWrapper setDisablePackets() {
      this.disablePackets = true;
      return this;
   }

   @Override
   public int getID() {
      return this.id;
   }

   public CustomGuiComponentWrapper setID(int id) {
      this.id = id;
      return this;
   }

   @Override
   public boolean getEnabled() {
      return this.enabled;
   }

   public CustomGuiComponentWrapper setEnabled(boolean bo) {
      this.enabled = bo;
      return this;
   }

   @Override
   public boolean getVisible() {
      return this.visible;
   }

   public CustomGuiComponentWrapper setVisible(boolean bo) {
      this.visible = bo;
      return this;
   }

   @Override
   public UUID getUniqueID() {
      return this.uniqueId;
   }

   @Override
   public int getPosX() {
      return this.posX;
   }

   @Override
   public int getPosY() {
      return this.posY;
   }

   public CustomGuiComponentWrapper setPos(int x, int y) {
      this.posX = x;
      this.posY = y;
      return this;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   public CustomGuiComponentWrapper setSize(int width, int height) {
      this.width = width;
      this.height = height;
      return this;
   }

   @Override
   public boolean hasHoverText() {
      return this.hoverText.size() > 0;
   }

   @Override
   public String[] getHoverText() {
      String[] ht = new String[this.hoverText.size()];

      for (int i = 0; i < this.hoverText.size(); i++) {
         ht[i] = ((TranslatableContents)this.hoverText.get(i).m_214077_()).m_237508_();
      }

      return ht;
   }

   public List<Component> getHoverTextList() {
      return this.hoverText;
   }

   public CustomGuiComponentWrapper setHoverText(String text) {
      this.hoverText = new ArrayList<>();
      this.hoverText.add(Component.m_237115_(text));
      return this;
   }

   public CustomGuiComponentWrapper setHoverText(String[] text) {
      List<Component> list = new ArrayList<>();

      for (String s : text) {
         list.add(Component.m_237115_(s));
      }

      this.hoverText = list;
      return this;
   }

   public CustomGuiComponentWrapper setHoverText(List<Component> list) {
      this.hoverText = list;
      return this;
   }

   public CompoundTag toNBT(CompoundTag nbt) {
      nbt.m_128405_("id", this.id);
      nbt.m_128379_("enabled", this.enabled);
      nbt.m_128379_("visible", this.visible);
      nbt.m_128362_("uniqueId", this.uniqueId);
      nbt.m_128385_("pos", new int[]{this.posX, this.posY});
      nbt.m_128385_("size", new int[]{this.width, this.height});
      if (this.hoverText != null) {
         ListTag list = new ListTag();

         for (Component s : this.hoverText) {
            list.add(StringTag.m_129297_(((TranslatableContents)s.m_214077_()).m_237508_()));
         }

         if (list.size() > 0) {
            nbt.m_128365_("hover", list);
         }
      }

      nbt.m_128405_("type", this.getType());
      return nbt;
   }

   public CustomGuiComponentWrapper fromNBT(CompoundTag nbt) {
      this.setID(nbt.m_128451_("id"));
      this.setEnabled(nbt.m_128471_("enabled"));
      this.setVisible(nbt.m_128471_("visible"));
      this.uniqueId = nbt.m_128342_("uniqueId");
      this.setPos(nbt.m_128465_("pos")[0], nbt.m_128465_("pos")[1]);
      this.setSize(nbt.m_128465_("size")[0], nbt.m_128465_("size")[1]);
      if (nbt.m_128441_("hover")) {
         ListTag list = nbt.m_128437_("hover", 8);
         String[] hoverText = new String[list.size()];

         for (int i = 0; i < list.size(); i++) {
            hoverText[i] = list.get(i).m_7916_();
         }

         this.setHoverText(hoverText);
      }

      return this;
   }

   public static CustomGuiComponentWrapper createFromNBT(CompoundTag nbt) {
      switch (nbt.m_128451_("type")) {
         case 0:
            return new CustomGuiButtonWrapper().fromNBT(nbt);
         case 1:
            return new CustomGuiLabelWrapper().fromNBT(nbt);
         case 2:
            return new CustomGuiTexturedRectWrapper().fromNBT(nbt);
         case 3:
            return new CustomGuiTextFieldWrapper().fromNBT(nbt);
         case 4:
            return new CustomGuiScrollWrapper().fromNBT(nbt);
         case 5:
            return new CustomGuiItemSlotWrapper().fromNBT(nbt);
         case 6:
            return new CustomGuiTextAreaWrapper().fromNBT(nbt);
         case 7:
            return new CustomGuiButtonListWrapper().fromNBT(nbt);
         case 8:
            return new CustomGuiSliderWrapper().fromNBT(nbt);
         case 9:
            return new CustomGuiEntityDisplayWrapper().fromNBT(nbt);
         case 10:
            return new CustomGuiAssetsSelectorWrapper().fromNBT(nbt);
         default:
            return null;
      }
   }
}
