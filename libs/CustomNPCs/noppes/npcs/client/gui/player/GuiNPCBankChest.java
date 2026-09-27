package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCBankInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketBankUnlock;
import noppes.npcs.packets.server.SPacketBankUpgrade;
import noppes.npcs.packets.server.SPacketBanksSlotOpen;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiNPCBankChest extends GuiContainerNPCInterface<ContainerNPCBankInterface> implements IGuiData {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/bankchest.png");
   private ContainerNPCBankInterface container;
   private int availableSlots = 0;
   private int maxSlots = 1;
   private int unlockedSlots = 1;
   private ItemStack currency;

   public GuiNPCBankChest(ContainerNPCBankInterface container, Inventory inv, Component titleIn) {
      super(NoppesUtil.getLastNpc(), container, inv, titleIn);
      this.container = container;
      this.title = "";
      this.f_96546_ = false;
      this.f_97727_ = 235;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.availableSlots = 0;
      if (this.maxSlots > 1) {
         for (int i = 0; i < this.maxSlots; i++) {
            GuiButtonNop button = new GuiButtonNop(
               this, i, this.guiLeft - 50, this.guiTop + 10 + i * 24, 50, 20, I18n.m_118938_("gui.tab", new Object[0]) + " " + (i + 1)
            );
            if (i > this.unlockedSlots) {
               button.setEnabled(false);
            }

            this.addButton(button);
            this.availableSlots++;
         }

         if (this.availableSlots == 1) {
            this.f_169369_.clear();
         }
      }

      if (!this.container.isAvailable()) {
         this.addButton(new GuiButtonNop(this, 8, this.guiLeft + 48, this.guiTop + 48, 80, 20, I18n.m_118938_("bank.unlock", new Object[0])));
      } else if (this.container.canBeUpgraded()) {
         this.addButton(new GuiButtonNop(this, 9, this.guiLeft + 48, this.guiTop + 48, 80, 20, I18n.m_118938_("bank.upgrade", new Object[0])));
      }

      if (this.maxSlots > 1) {
         this.getButton(this.container.slot).f_93624_ = false;
         this.getButton(this.container.slot).setEnabled(false);
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      int id = guibutton.id;
      if (id < 6) {
         Packets.sendServer(new SPacketBanksSlotOpen(id, this.container.bankid));
      }

      if (id == 8) {
         Packets.sendServer(new SPacketBankUnlock());
      }

      if (id == 9) {
         Packets.sendServer(new SPacketBankUpgrade());
      }
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float partialTicks, int x, int y) {
      super.m_7286_(matrixStack, partialTicks, x, y);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      int l = (this.f_96543_ - this.f_97726_) / 2;
      int i1 = (this.f_96544_ - this.f_97727_) / 2;
      this.m_93228_(matrixStack, l, i1, 0, 0, this.f_97726_, 6);
      if (!this.container.isAvailable()) {
         this.m_93228_(matrixStack, l, i1 + 6, 0, 6, this.f_97726_, 64);
         this.m_93228_(matrixStack, l, i1 + 70, 0, 124, this.f_97726_, 98);
         int i = this.guiLeft + 30;
         int j = this.guiTop + 8;
         this.f_96547_.m_92883_(matrixStack, I18n.m_118938_("bank.unlockCosts", new Object[0]) + ":", i, j + 4, CustomNpcResourceListener.DefaultTextColor);
         this.drawItem(matrixStack, i + 90, j, this.currency, x, y);
      } else if (this.container.isUpgraded()) {
         this.m_93228_(matrixStack, l, i1 + 60, 0, 60, this.f_97726_, 162);
         this.m_93228_(matrixStack, l, i1 + 6, 0, 60, this.f_97726_, 64);
      } else if (this.container.canBeUpgraded()) {
         this.m_93228_(matrixStack, l, i1 + 6, 0, 6, this.f_97726_, 216);
         int i = this.guiLeft + 30;
         int j = this.guiTop + 8;
         this.f_96547_.m_92883_(matrixStack, I18n.m_118938_("bank.upgradeCosts", new Object[0]) + ":", i, j + 4, CustomNpcResourceListener.DefaultTextColor);
         this.drawItem(matrixStack, i + 90, j, this.currency, x, y);
      } else {
         this.m_93228_(matrixStack, l, i1 + 6, 0, 60, this.f_97726_, 162);
      }

      if (this.maxSlots > 1) {
         for (int ii = 0; ii < this.maxSlots && this.availableSlots != ii; ii++) {
            this.f_96547_.m_92883_(matrixStack, "Tab " + (ii + 1), this.guiLeft - 40, this.guiTop + 16 + ii * 24, 16777215);
         }
      }
   }

   private void drawItem(PoseStack matrixStack, int x, int y, ItemStack item, int mouseX, int mouseY) {
      if (!NoppesUtilServer.IsItemStackNull(item)) {
         this.f_96542_.m_115203_(item, x, y);
         this.f_96542_.m_115169_(this.f_96547_, item, x, y);
         if (this.m_6774_(x - this.guiLeft, y - this.guiTop, 16, 16, mouseX, mouseY)) {
            this.m_6057_(matrixStack, item, mouseX, mouseY);
         }
      }
   }

   @Override
   public void save() {
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      this.maxSlots = compound.m_128451_("MaxSlots");
      this.unlockedSlots = compound.m_128451_("UnlockedSlots");
      if (compound.m_128441_("Currency")) {
         this.currency = ItemStack.m_41712_(compound.m_128469_("Currency"));
      } else {
         this.currency = ItemStack.f_41583_;
      }

      if (this.container.currency != null) {
         this.container.currency.item = this.currency;
      }

      this.m_7856_();
   }
}
