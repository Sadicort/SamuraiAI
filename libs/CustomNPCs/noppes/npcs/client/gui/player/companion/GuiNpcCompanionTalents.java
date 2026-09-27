package noppes.npcs.client.gui.player.companion;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketCompanionOpenInv;
import noppes.npcs.packets.server.SPacketCompanionTalentExp;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.util.NoppesStringUtils;

public class GuiNpcCompanionTalents extends GuiNPCInterface {
   private RoleCompanion role;
   private Map<Integer, GuiNpcCompanionTalents.GuiTalent> talents = new HashMap<>();
   private GuiButtonNop selected;
   private long lastPressedTime = 0L;
   private long startPressedTime = 0L;

   public GuiNpcCompanionTalents(EntityNPCInterface npc) {
      super(npc);
      this.role = (RoleCompanion)npc.role;
      this.setBackground("companion_empty.png");
      this.imageWidth = 171;
      this.imageHeight = 166;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.talents = new HashMap<>();
      int y = this.guiTop + 12;
      this.addLabel(new GuiLabel(0, NoppesStringUtils.translate("quest.exp", ": "), this.guiLeft + 4, this.guiTop + 10));
      GuiNpcCompanionStats.addTopMenu(this.role, this, 2);
      int i = 0;

      for (EnumCompanionTalent e : this.role.talents.keySet()) {
         this.addTalent(i++, e);
      }
   }

   private void addTalent(int i, EnumCompanionTalent talent) {
      int y = this.guiTop + 28 + i / 2 * 26;
      int x = this.guiLeft + 4 + i % 2 * 84;
      GuiNpcCompanionTalents.GuiTalent gui = new GuiNpcCompanionTalents.GuiTalent(this.role, talent, x, y);
      gui.m_6575_(this.f_96541_, this.f_96543_, this.f_96544_);
      this.talents.put(i, gui);
      if (this.role.getTalentLevel(talent) < 5) {
         this.addButton(new GuiButtonNop(this, i + 10, x + 26, y, 14, 14, "+"));
         y += 8;
      }

      this.addLabel(new GuiLabel(i, this.role.talents.get(talent) + "/" + this.role.getNextLevel(talent), x + 26, y + 8));
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      int id = guibutton.id;
      if (id == 1) {
         CustomNpcs.proxy.openGui(this.npc, EnumGuiType.Companion);
      }

      if (id == 3) {
         Packets.sendServer(new SPacketCompanionOpenInv());
      }

      if (id >= 10) {
         this.selected = guibutton;
         this.lastPressedTime = this.startPressedTime = this.f_96541_.f_91073_.m_46468_();
         this.addExperience(1);
      }
   }

   private void addExperience(int exp) {
      EnumCompanionTalent talent = this.talents.get(this.selected.id - 10).talent;
      if (this.role.canAddExp(-exp) || this.role.currentExp > 0) {
         if (exp > this.role.currentExp) {
            exp = this.role.currentExp;
         }

         Packets.sendServer(new SPacketCompanionTalentExp(talent, exp));
         this.role.talents.put(talent, this.role.talents.get(talent) + exp);
         this.role.addExp(-exp);
         this.getLabel(this.selected.id - 10).m_93666_(Component.m_237113_(this.role.talents.get(talent) + "/" + this.role.getNextLevel(talent)));
      }
   }

   @Override
   public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      super.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
      if (this.selected != null
         && this.f_96541_.f_91073_.m_46468_() - this.startPressedTime > 4L
         && this.lastPressedTime < this.f_96541_.f_91073_.m_46468_()
         && this.f_96541_.f_91073_.m_46468_() % 4L == 0L) {
         if (this.selected.m_6375_(mouseX, mouseY, 0)) {
            this.lastPressedTime = this.f_96541_.f_91073_.m_46468_();
            if (this.lastPressedTime - this.startPressedTime < 20L) {
               this.addExperience(1);
            } else if (this.lastPressedTime - this.startPressedTime < 40L) {
               this.addExperience(2);
            } else if (this.lastPressedTime - this.startPressedTime < 60L) {
               this.addExperience(4);
            } else if (this.lastPressedTime - this.startPressedTime < 90L) {
               this.addExperience(8);
            } else if (this.lastPressedTime - this.startPressedTime < 140L) {
               this.addExperience(14);
            } else {
               this.addExperience(28);
            }
         } else {
            this.lastPressedTime = 0L;
            this.selected = null;
         }
      }

      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, f_93098_);
      this.m_93228_(matrixStack, this.guiLeft + 4, this.guiTop + 20, 10, 64, 162, 5);
      if (this.role.currentExp > 0) {
         float v = 1.0F * this.role.currentExp / this.role.getMaxExp();
         if (v > 1.0F) {
            v = 1.0F;
         }

         this.m_93228_(matrixStack, this.guiLeft + 4, this.guiTop + 20, 10, 69, (int)(v * 162.0F), 5);
      }

      String s = this.role.currentExp + "\\" + this.role.getMaxExp();
      this.f_96541_
         .f_91062_
         .m_92883_(
            matrixStack,
            s,
            this.guiLeft + this.imageWidth / 2 - this.f_96541_.f_91062_.m_92895_(s) / 2,
            this.guiTop + 10,
            CustomNpcResourceListener.DefaultTextColor
         );

      for (GuiNpcCompanionTalents.GuiTalent talent : this.talents.values()) {
         talent.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
      }
   }

   @Override
   public void save() {
   }

   public static class GuiTalent extends Screen {
      private EnumCompanionTalent talent;
      private int x;
      private int y;
      private RoleCompanion role;
      private static final ResourceLocation resource = new ResourceLocation("customnpcs:textures/gui/talent.png");

      public GuiTalent(RoleCompanion role, EnumCompanionTalent talent, int x, int y) {
         super(Component.m_237119_());
         this.talent = talent;
         this.x = x;
         this.y = y;
         this.role = role;
      }

      public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
         Minecraft mc = Minecraft.m_91087_();
         RenderSystem.m_157427_(GameRenderer::m_172817_);
         RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.m_157456_(0, resource);
         ItemStack item = this.talent.item;
         if (item.m_41720_() == null) {
            item = new ItemStack(Blocks.f_50493_);
         }

         matrixStack.m_85836_();
         RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.m_69478_();
         boolean hover = this.x < mouseX && this.x + 24 > mouseX && this.y < mouseY && this.y + 24 > mouseY;
         this.m_93228_(matrixStack, this.x, this.y, 0, hover ? 24 : 0, 24, 24);
         this.m_93250_(100);
         this.f_96542_.f_115093_ = 100.0F;
         this.f_96542_.m_115203_(item, this.x + 4, this.y + 4);
         this.f_96542_.m_115169_(mc.f_91062_, item, this.x + 4, this.y + 4);
         matrixStack.m_85837_(0.0, 0.0, 200.0);
         m_93208_(matrixStack, mc.f_91062_, this.role.getTalentLevel(this.talent) + "", this.x + 20, this.y + 16, 16777215);
         this.f_96542_.f_115093_ = 0.0F;
         this.m_93250_(0);
         matrixStack.m_85849_();
      }
   }
}
