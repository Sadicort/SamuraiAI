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
import noppes.npcs.containers.ContainerNPCFollower;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketFollowerExtend;
import noppes.npcs.packets.server.SPacketFollowerState;
import noppes.npcs.packets.server.SPacketNpcRoleGet;
import noppes.npcs.roles.RoleFollower;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.listeners.IGuiData;

public class GuiNpcFollower extends GuiContainerNPCInterface<ContainerNPCFollower> implements IGuiData {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/follower.png");
   private RoleFollower role = (RoleFollower)this.npc.role;

   public GuiNpcFollower(ContainerNPCFollower container, Inventory inv, Component titleIn) {
      super(NoppesUtil.getLastNpc(), container, inv, titleIn);
      Packets.sendServer(new SPacketNpcRoleGet());
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.f_169369_.clear();
      this.addButton(
         new GuiButtonNop(
            this,
            4,
            this.guiLeft + 100,
            this.guiTop + 110,
            50,
            20,
            new String[]{I18n.m_118938_("follower.waiting", new Object[0]), I18n.m_118938_("follower.following", new Object[0])},
            this.role.isFollowing ? 1 : 0
         )
      );
      if (!this.role.infiniteDays) {
         this.addButton(new GuiButtonNop(this, 5, this.guiLeft + 8, this.guiTop + 30, 50, 20, I18n.m_118938_("follower.hire", new Object[0])));
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      int id = guibutton.id;
      if (id == 4) {
         Packets.sendServer(new SPacketFollowerState());
      }

      if (id == 5) {
         Packets.sendServer(new SPacketFollowerExtend());
      }
   }

   @Override
   protected void m_7027_(PoseStack matrixStack, int x, int y) {
      this.f_96547_
         .m_92883_(
            matrixStack,
            I18n.m_118938_("follower.health", new Object[0]) + ": " + this.npc.m_21223_() + "/" + this.npc.m_21233_(),
            62.0F,
            70.0F,
            CustomNpcResourceListener.DefaultTextColor
         );
      if (!this.role.infiniteDays) {
         if (this.role.getDays() <= 1) {
            this.f_96547_
               .m_92883_(
                  matrixStack,
                  I18n.m_118938_("follower.daysleft", new Object[0]) + ": " + I18n.m_118938_("follower.lastday", new Object[0]),
                  62.0F,
                  94.0F,
                  CustomNpcResourceListener.DefaultTextColor
               );
         } else {
            this.f_96547_
               .m_92883_(
                  matrixStack,
                  I18n.m_118938_("follower.daysleft", new Object[0]) + ": " + (this.role.getDays() - 1),
                  62.0F,
                  94.0F,
                  CustomNpcResourceListener.DefaultTextColor
               );
         }
      }
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float partialTicks, int x, int y) {
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      int l = this.guiLeft;
      int i1 = this.guiTop;
      this.m_93228_(matrixStack, l, i1, 0, 0, this.f_97726_, this.f_97727_);
      int index = 0;
      if (!this.role.infiniteDays) {
         for (int slot = 0; slot < this.role.inventory.items.size(); slot++) {
            ItemStack itemstack = (ItemStack)this.role.inventory.items.get(slot);
            if (!NoppesUtilServer.IsItemStackNull(itemstack)) {
               int days = 1;
               if (this.role.rates.containsKey(slot)) {
                  days = this.role.rates.get(slot);
               }

               int yOffset = index * 20;
               int i = this.guiLeft + 68;
               int j = this.guiTop + yOffset + 4;
               this.f_96542_.m_115203_(itemstack, x + 11, y);
               this.f_96542_.m_115169_(this.f_96547_, itemstack, x + 11, y);
               String daysS = days + " " + (days == 1 ? I18n.m_118938_("follower.day", new Object[0]) : I18n.m_118938_("follower.days", new Object[0]));
               this.f_96547_.m_92883_(matrixStack, " = " + daysS, i + 27, j + 4, CustomNpcResourceListener.DefaultTextColor);
               if (this.m_6774_(i - this.guiLeft + 11, j - this.guiTop, 16, 16, this.mouseX, this.mouseY)) {
                  this.m_6057_(matrixStack, itemstack, this.mouseX, this.mouseY);
               }

               index++;
            }
         }
      }

      this.drawNpc(33, 131);
   }

   @Override
   public void save() {
   }

   @Override
   public void setGuiData(CompoundTag compound) {
      this.npc.role.load(compound);
      this.m_7856_();
   }
}
