package noppes.npcs.client.gui.player.companion;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.constants.EnumCompanionTalent;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.containers.ContainerNPCCompanion;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;

public class GuiNpcCompanionInv extends GuiContainerNPCInterface<ContainerNPCCompanion> {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/companioninv.png");
   private final ResourceLocation slot = new ResourceLocation("customnpcs", "textures/gui/slot.png");
   private RoleCompanion role = (RoleCompanion)this.npc.role;

   public GuiNpcCompanionInv(ContainerNPCCompanion container, Inventory inv, Component titleIn) {
      super(NoppesUtil.getLastNpc(), container, inv, titleIn);
      this.f_97726_ = 171;
      this.f_97727_ = 166;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      GuiNpcCompanionStats.addTopMenu(this.role, this, 3);
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      int id = guibutton.id;
      if (id == 1) {
         CustomNpcs.proxy.openGui(this.npc, EnumGuiType.Companion);
      }

      if (id == 2) {
         CustomNpcs.proxy.openGui(this.npc, EnumGuiType.CompanionTalent);
      }
   }

   @Override
   protected void m_7027_(PoseStack matrixStack, int par1, int limbSwingAmount) {
   }

   @Override
   protected void m_7286_(PoseStack matrixStack, float f, int xMouse, int yMouse) {
      super.m_96558_(matrixStack, 0);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      this.m_93228_(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.f_97726_, this.f_97727_);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.slot);
      if (this.role.getTalentLevel(EnumCompanionTalent.ARMOR) > 0) {
         for (int i = 0; i < 4; i++) {
            this.m_93228_(matrixStack, this.guiLeft + 5, this.guiTop + 7 + i * 18, 0, 0, 18, 18);
         }
      }

      if (this.role.getTalentLevel(EnumCompanionTalent.SWORD) > 0) {
         this.m_93228_(matrixStack, this.guiLeft + 78, this.guiTop + 16, 0, this.npc.inventory.weapons.get(0) == null ? 18 : 0, 18, 18);
      }

      if (this.role.getTalentLevel(EnumCompanionTalent.RANGED) > 0) {
      }

      if (this.role.talents.containsKey(EnumCompanionTalent.INVENTORY)) {
         int size = (this.role.getTalentLevel(EnumCompanionTalent.INVENTORY) + 1) * 2;

         for (int i = 0; i < size; i++) {
            this.m_93228_(matrixStack, this.guiLeft + 113 + i % 3 * 18, this.guiTop + 7 + i / 3 * 18, 0, 0, 18, 18);
         }
      }
   }

   @Override
   public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      super.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
      super.drawNpc(52, 70);
   }

   @Override
   public void save() {
   }
}
