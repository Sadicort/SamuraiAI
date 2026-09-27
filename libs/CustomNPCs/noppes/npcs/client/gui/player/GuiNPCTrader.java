package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCTrader;
import noppes.npcs.roles.RoleTrader;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

public class GuiNPCTrader extends GuiContainerNPCInterface<ContainerNPCTrader> {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/trader.png");
   private final ResourceLocation slot = new ResourceLocation("customnpcs", "textures/gui/slot.png");
   private RoleTrader role;
   private ContainerNPCTrader container;

   public GuiNPCTrader(ContainerNPCTrader container, Inventory inv, Component titleIn) {
      super(NoppesUtil.getLastNpc(), container, inv, titleIn);
      this.container = container;
      this.role = (RoleTrader)this.npc.role;
      this.f_97727_ = 224;
      this.f_97726_ = 223;
      this.title = "role.trader";
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float partialTicks, int x, int y) {
      super.m_7333_(matrixStack);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      this.m_93228_(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.f_97726_, this.f_97727_);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.slot);

      for (int slot = 0; slot < 18; slot++) {
         int i = this.guiLeft + slot % 3 * 72 + 10;
         int j = this.guiTop + slot / 3 * 21 + 6;
         ItemStack item = (ItemStack)this.role.inventoryCurrency.items.get(slot);
         ItemStack item2 = (ItemStack)this.role.inventoryCurrency.items.get(slot + 18);
         if (NoppesUtilServer.IsItemStackNull(item)) {
            item = item2;
            item2 = ItemStack.f_41583_;
         }

         if (NoppesUtilPlayer.compareItems(item, item2, false, false)) {
            item = item.m_41777_();
            item.m_41764_(item.m_41613_() + item2.m_41613_());
            item2 = ItemStack.f_41583_;
         }

         ItemStack sold = (ItemStack)this.role.inventorySold.items.get(slot);
         RenderSystem.m_157427_(GameRenderer::m_172817_);
         RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.m_157456_(0, this.slot);
         this.m_93228_(matrixStack, i + 42, j, 0, 0, 18, 18);
         if (!NoppesUtilServer.IsItemStackNull(item) && !NoppesUtilServer.IsItemStackNull(sold)) {
            if (!NoppesUtilServer.IsItemStackNull(item2)) {
               this.f_96542_.m_115203_(item2, i, j + 1);
               this.f_96542_.m_115169_(this.f_96547_, item2, i, j + 1);
            }

            this.f_96542_.m_115203_(item, i + 18, j + 1);
            this.f_96542_.m_115169_(this.f_96547_, item, i + 18, j + 1);
            this.f_96547_.m_92883_(matrixStack, "=", i + 36, j + 5, CustomNpcResourceListener.DefaultTextColor);
         }
      }
   }

   @Override
   protected void m_7027_(PoseStack matrixStack, int x, int y) {
      for (int slot = 0; slot < 18; slot++) {
         int i = slot % 3 * 72 + 10;
         int j = slot / 3 * 21 + 6;
         ItemStack item = (ItemStack)this.role.inventoryCurrency.items.get(slot);
         ItemStack item2 = (ItemStack)this.role.inventoryCurrency.items.get(slot + 18);
         if (NoppesUtilServer.IsItemStackNull(item)) {
            item = item2;
            item2 = ItemStack.f_41583_;
         }

         if (NoppesUtilPlayer.compareItems(item, item2, this.role.ignoreDamage, this.role.ignoreNBT)) {
            item = item.m_41777_();
            item.m_41764_(item.m_41613_() + item2.m_41613_());
            item2 = ItemStack.f_41583_;
         }

         ItemStack sold = (ItemStack)this.role.inventorySold.items.get(slot);
         if (!NoppesUtilServer.IsItemStackNull(sold)) {
            if (this.m_6774_(i + 43, j + 1, 16, 16, x, y)) {
               if (!this.container.canBuy(item, item2, this.player)) {
                  matrixStack.m_85837_(0.0, 0.0, 300.0);
                  if (!item.m_41619_() && !NoppesUtilPlayer.compareItems(this.player, item, this.role.ignoreDamage, this.role.ignoreNBT)) {
                     this.m_93179_(matrixStack, i + 17, j, i + 35, j + 18, 1886851088, 1886851088);
                  }

                  if (!item2.m_41619_() && !NoppesUtilPlayer.compareItems(this.player, item2, this.role.ignoreDamage, this.role.ignoreNBT)) {
                     this.m_93179_(matrixStack, i - 1, j, i + 17, j + 18, 1886851088, 1886851088);
                  }

                  String title = I18n.m_118938_("trader.insufficient", new Object[0]);
                  this.f_96547_.m_92883_(matrixStack, title, (this.f_97726_ - this.f_96547_.m_92895_(title)) / 2, 131.0F, 14483456);
                  matrixStack.m_85837_(0.0, 0.0, -300.0);
               } else {
                  String title = I18n.m_118938_("trader.sufficient", new Object[0]);
                  this.f_96547_.m_92883_(matrixStack, title, (this.f_97726_ - this.f_96547_.m_92895_(title)) / 2, 131.0F, 56576);
               }
            }

            if (this.m_6774_(i, j, 16, 16, x, y) && !NoppesUtilServer.IsItemStackNull(item2)) {
               this.m_6057_(matrixStack, item2, x - this.guiLeft, y - this.guiTop);
            }

            if (this.m_6774_(i + 18, j, 16, 16, x, y)) {
               this.m_6057_(matrixStack, item, x - this.guiLeft, y - this.guiTop);
            }
         }
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop button) {
   }

   @Override
   public void save() {
   }
}
