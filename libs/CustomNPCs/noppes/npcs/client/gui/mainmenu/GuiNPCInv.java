package noppes.npcs.client.gui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.constants.EnumMenuType;
import noppes.npcs.containers.ContainerNPCInv;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuGet;
import noppes.npcs.packets.server.SPacketMenuSave;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.components.GuiSliderNop;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;
import noppes.npcs.shared.client.gui.listeners.ISliderListener;

public class GuiNPCInv extends GuiContainerNPCInterface2<ContainerNPCInv> implements ISliderListener, IGuiData {
   private HashMap<Integer, Float> chances = new HashMap<>();
   private ContainerNPCInv container;
   private ResourceLocation slot;

   public GuiNPCInv(ContainerNPCInv container, Inventory inv, Component titleIn) {
      super(NoppesUtil.getLastNpc(), container, inv, titleIn, 4);
      this.setBackground("npcinv.png");
      this.container = container;
      this.f_97727_ = 200;
      this.slot = this.getResource("slot.png");
      Packets.sendServer(new SPacketMenuGet(EnumMenuType.INVENTORY));
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.addLabel(new GuiLabel(0, "inv.minExp", this.guiLeft + 118, this.guiTop + 18));
      this.addTextField(new GuiTextFieldNop(0, this, this.guiLeft + 108, this.guiTop + 29, 60, 20, this.npc.inventory.getExpMin() + ""));
      this.getTextField(0).numbersOnly = true;
      this.getTextField(0).setMinMaxDefault(0, 32767, 0);
      this.addLabel(new GuiLabel(1, "inv.maxExp", this.guiLeft + 118, this.guiTop + 52));
      this.addTextField(new GuiTextFieldNop(1, this, this.guiLeft + 108, this.guiTop + 63, 60, 20, this.npc.inventory.getExpMax() + ""));
      this.getTextField(1).numbersOnly = true;
      this.getTextField(1).setMinMaxDefault(0, 32767, 0);
      this.addButton(
         new GuiButtonNop(this, 10, this.guiLeft + 88, this.guiTop + 88, 80, 20, new String[]{"stats.normal", "inv.auto"}, this.npc.inventory.lootMode)
      );
      this.addLabel(new GuiLabel(2, "inv.npcInventory", this.guiLeft + 191, this.guiTop + 5));
      this.addLabel(new GuiLabel(3, "inv.inventory", this.guiLeft + 8, this.guiTop + 101));

      for (int i = 0; i < 9; i++) {
         float chance = 100.0F;
         if (this.npc.inventory.dropchance.containsKey(i)) {
            chance = this.npc.inventory.dropchance.get(i);
         }

         if (chance <= 0.0F || chance > 100.0F) {
            chance = 100.0F;
         }

         this.chances.put(i, chance);
         this.addSlider(new GuiSliderNop(this, i, this.guiLeft + 211, this.guiTop + 14 + i * 21, chance / 100.0F));
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      if (guibutton.id == 10) {
         this.npc.inventory.lootMode = guibutton.getValue();
      }
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float partialTicks, int x, int y) {
      super.m_7286_(matrixStack, partialTicks, x, y);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.slot);

      for (int id = 4; id <= 6; id++) {
         Slot slot = this.container.m_38853_(id);
         if (slot.m_6657_()) {
            this.m_93228_(matrixStack, this.guiLeft + slot.f_40220_ - 1, this.guiTop + slot.f_40221_ - 1, 0, 0, 18, 18);
         }
      }
   }

   @Override
   public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      super.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
      int showname = this.npc.display.getShowName();
      this.npc.display.setShowName(1);
      this.drawNpc(50, 84);
      this.npc.display.setShowName(showname);
   }

   @Override
   public void save() {
      this.npc.inventory.dropchance = this.chances;
      this.npc.inventory.setExp(this.getTextField(0).getInteger(), this.getTextField(1).getInteger());
      Packets.sendServer(new SPacketMenuSave(EnumMenuType.INVENTORY, this.npc.inventory.save(new CompoundTag())));
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      this.npc.inventory.load(compound);
      this.m_7856_();
   }

   @Override
   public void mouseDragged(GuiSliderNop guiNpcSlider) {
      guiNpcSlider.m_93666_(Component.m_237115_("inv.dropChance").m_130946_(": " + (int)(guiNpcSlider.sliderValue * 100.0F) + "%"));
   }

   @Override
   public void mousePressed(GuiSliderNop guiNpcSlider) {
   }

   @Override
   public void mouseReleased(GuiSliderNop guiNpcSlider) {
      this.chances.put(guiNpcSlider.id, guiNpcSlider.sliderValue * 100.0F);
   }
}
