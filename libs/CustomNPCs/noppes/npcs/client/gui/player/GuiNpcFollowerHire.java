package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.containers.ContainerNPCFollowerHire;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFollowerHire;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

public class GuiNpcFollowerHire extends GuiContainerNPCInterface<ContainerNPCFollowerHire> {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/followerhire.png");
   private ContainerNPCFollowerHire container;
   private RoleFollower role;

   public GuiNpcFollowerHire(ContainerNPCFollowerHire container, Inventory inv, Component titleIn) {
      super(NoppesUtil.getLastNpc(), container, inv, titleIn);
      this.container = container;
      this.role = (RoleFollower)this.npc.role;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.addButton(new GuiButtonNop(this, 5, this.guiLeft + 26, this.guiTop + 60, 50, 20, I18n.m_118938_("follower.hire", new Object[0])));
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      if (guibutton.id == 5) {
         Packets.sendServer(new SPacketFollowerHire());
         this.close();
      }
   }

   @Override
   protected void m_7027_(PoseStack matrixStack, int par1, int limbSwingAmount) {
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float f, int i, int j) {
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      int l = (this.f_96543_ - this.f_97726_) / 2;
      int i1 = (this.f_96544_ - this.f_97727_) / 2;
      this.m_93228_(matrixStack, l, i1, 0, 0, this.f_97726_, this.f_97727_);
      int index = 0;

      for (int slot = 0; slot < this.role.inventory.items.size(); slot++) {
         ItemStack itemstack = (ItemStack)this.role.inventory.items.get(slot);
         if (!NoppesUtilServer.IsItemStackNull(itemstack)) {
            int days = 1;
            if (this.role.rates.containsKey(slot)) {
               days = this.role.rates.get(slot);
            }

            int yOffset = index * 26;
            int x = this.guiLeft + 78;
            int y = this.guiTop + yOffset + 10;
            this.f_96542_.m_115203_(itemstack, x + 11, y);
            this.f_96542_.m_115169_(this.f_96547_, itemstack, x + 11, y);
            String daysS = days + " " + (days == 1 ? I18n.m_118938_("follower.day", new Object[0]) : I18n.m_118938_("follower.days", new Object[0]));
            this.f_96547_.m_92883_(matrixStack, " = " + daysS, x + 27, y + 4, CustomNpcResourceListener.DefaultTextColor);
            if (this.m_6774_(x - this.guiLeft + 11, y - this.guiTop, 16, 16, this.mouseX, this.mouseY)) {
               this.m_6057_(matrixStack, itemstack, this.mouseX, this.mouseY);
            }

            index++;
         }
      }
   }

   @Override
   public void save() {
   }
}
